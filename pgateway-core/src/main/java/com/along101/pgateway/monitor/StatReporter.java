package com.along101.pgateway.monitor;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.util.MetricUtil;

public class StatReporter {

	private static Logger LOGGER = LoggerFactory.getLogger(StatReporter.class);

	private ReporterThread reporterThread = null;
	private static CostStater costStater = new CostStater();;

	public StatReporter(AtomicReference<ThreadPoolExecutor> poolExecutorRef, AtomicLong rejectedRequests) {
		this.reporterThread = new ReporterThread(poolExecutorRef, rejectedRequests);
	}

	public void start() {
		if (null != reporterThread) {
			reporterThread.start();
		}
	}

	public void shutdown() {
		if (null != reporterThread) {
			reporterThread.shutdown();
		}
	}

	public static void statCost(long cost, long remoteServiceCost, long replyClientCost, long replyClientReadCost,
			long replyClientWriteCost, String routeName) {
		costStater.statCost(cost, remoteServiceCost, replyClientCost, replyClientReadCost, replyClientWriteCost,
				routeName);
		costStater.statCost(cost, remoteServiceCost, replyClientCost, replyClientReadCost, replyClientWriteCost,
				"gate");
	}

	static class ReporterThread extends Thread {
		private DynamicLongProperty reportInterval = DynamicPropertyFactory.getInstance()
				.getLongProperty("gate.asyncservlet.reporter.interval", 60000);
		private DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
				.getStringProperty("archaius.deployment.applicationId", "pgateway");

		private long preCompletedTasks = 0;
		private long preTotalTasks = 0;
		private long preRejectTasks = 0;
		protected volatile boolean running = true;
		private AtomicReference<ThreadPoolExecutor> poolExecutorRef;
		private AtomicLong rejectedRequests;

		public ReporterThread(AtomicReference<ThreadPoolExecutor> poolExecutorRef, AtomicLong rejectedRequests) {
			super("AsyncServlet-Reporter-Thread");
			this.poolExecutorRef = poolExecutorRef;
			this.rejectedRequests = rejectedRequests;
		}

		public void shutdown() {
			this.running = false;
		}

		public void run() {

			try {
				while (this.running) {
					try {
						Date date = new Date();
						reportCountStats(date);
						reportCostStats(date);
					} catch (Throwable e) {
						LOGGER.error("Encounter an error while reporting.", e);
					} finally {
						sleep(reportInterval.get());
					}
				}
			} catch (InterruptedException e) {
				LOGGER.error("Async Servlet Reporter stopped because some error.", e);
			}
		}

		private void reportCountStats(Date date) {
			ThreadPoolExecutor p = poolExecutorRef.get();
			if (p == null)
				return;

			int activeTasks = p.getActiveCount();
			long completedTasks = p.getCompletedTaskCount();
			long totalTasks = p.getTaskCount();
			int waitingTasks = p.getQueue().size();
			int threads = p.getPoolSize();
			long rejectTasks = rejectedRequests.get();

			long completedTasksThisRound = completedTasks - preCompletedTasks;
			long totalTasksThisRound = totalTasks - preTotalTasks;
			long rejectTasksThisRound = rejectTasks - preRejectTasks;

			preCompletedTasks = completedTasks;
			preTotalTasks = totalTasks;

			String prefix = appName.get();

			MetricUtil.log(prefix + ".request.processing", activeTasks, null, date);
			MetricUtil.log(prefix + ".request.waiting", waitingTasks, null, date);
			MetricUtil.log(prefix + ".request.completed", completedTasksThisRound, null, date);
			MetricUtil.log(prefix + ".request.rejected", rejectTasksThisRound, null, date);
			MetricUtil.log(prefix + ".request.request", totalTasksThisRound, null, date);
			MetricUtil.log(prefix + ".thread-pool.size", threads, null, date);

			LOGGER.info("\nGatekeeper stats:\n" + "\trequests processing:\t{}\n" + "\trequests waiting:\t{}\n" + "\n"
					+ "\trequests complected in a round:\t{}\n" + "\trequests rejected in a round:\t{}\n"
					+ "\trequests request in a round:\t{}\n" + "\n" + "\trequests complected total:\t{}\n"
					+ "\trequests rejected total:\t{}\n" + "\trequests total:\t{}\n" + "" + "\tthread pool size:\t{}\n",
					activeTasks, waitingTasks, completedTasksThisRound, rejectTasksThisRound, totalTasksThisRound,
					completedTasks, rejectTasks, totalTasks, threads);
		}


		private void reportCostStats(Date date) {
			Map<String, AtomicReference<AvgCost>> map = costStater.getMap();

			String prefix = appName.get() + ".request.";
			String key, routeName, metricName;
			int separateIndex;
			TreeMap<String, String> tags;

			AvgCost stats;
			for (Map.Entry<String, AtomicReference<AvgCost>> entry : map.entrySet()) {

				key = entry.getKey();
				stats = entry.getValue().getAndSet(new AvgCost());

				separateIndex = key.lastIndexOf(".");

				routeName = key.substring(0, separateIndex);
				metricName = prefix + key.substring(separateIndex + 1);

				tags = new TreeMap<String, String>();
				tags.put("routeName", routeName);

				if (key.endsWith(".count") && !routeName.equals("gate")) {
					MetricUtil.log(metricName, stats.getCount(), tags, date);
				} else {
					MetricUtil.log(metricName, stats.getAvg(), tags, date);
					MetricUtil.log(metricName + ".min", stats.getMinCost(), tags, date);
					MetricUtil.log(metricName + ".max", stats.getMaxCost(), tags, date);
				}
			}
		}
	}

	static class CostStater {
		private ConcurrentHashMap<String, AtomicReference<AvgCost>> map = new ConcurrentHashMap<String, AtomicReference<AvgCost>>();

		public void statCost(long cost, long remoteServiceCost, long replyClientCost, long replyClientReadCost,
				long replyClientWriteCost, String routeName) {
			AtomicReference<AvgCost> ref = get(routeName + ".cost");
			ref.get().addCost(cost);

			ref = get(routeName + ".service-cost");
			ref.get().addCost(remoteServiceCost);

			ref = get(routeName + ".reply-client-cost");
			ref.get().addCost(replyClientCost);

			ref = get(routeName + ".reply-client-cost-read");
			ref.get().addCost(replyClientReadCost);

			ref = get(routeName + ".reply-client-cost-write");
			ref.get().addCost(replyClientWriteCost);

			ref = get(routeName + ".gate-cost");
			ref.get().addCost(cost - remoteServiceCost);

			ref = get(routeName + ".count");
			ref.get().addCost(0);
		}

		private AtomicReference<AvgCost> get(String routeName) {
			AtomicReference<AvgCost> ref = map.get(routeName);
			if (ref == null) {
				ref = new AtomicReference<AvgCost>(new AvgCost());
				AtomicReference<AvgCost> found = map.putIfAbsent(routeName, ref);
				if (found != null) {
					ref = found;
				}
			}
			return ref;
		}

		public ConcurrentHashMap<String, AtomicReference<AvgCost>> getMap() {
			return map;
		}
	}

	static class AvgCost {
		private AtomicInteger count = new AtomicInteger(0);
		private AtomicLong totalCost = new AtomicLong(0);
		private AtomicLong minCost = new AtomicLong(0);
		private AtomicLong maxCost = new AtomicLong(0);

		public void addCost(long c) {
			count.getAndIncrement();
			totalCost.getAndAdd(c);

			while (true) {
				long min = minCost.get();
				if (min == 0 || min > c) {
					if (minCost.compareAndSet(min, c)) {
						break;
					}
				} else {
					break;
				}
			}

			while (true) {
				long max = maxCost.get();
				if (max == 0 || max < c) {
					if (maxCost.compareAndSet(max, c)) {
						break;
					}
				} else {
					break;
				}
			}
		}

		public int getCount() {
			return count.get();
		}

		public long getMinCost() {
			return minCost.get();
		}

		public long getMaxCost() {
			return maxCost.get();
		}

		public long getAvg() {
			int c = count.get();
			long t = totalCost.get();
			return c == 0 ? 0 : t / c;
		}
	}

}
