package com.along101.pgateway.filters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.google.common.collect.Maps;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.FilterInfo;

public class GateFilterPoller {

	private static final Logger LOGGER = LoggerFactory.getLogger(GateFilterPoller.class);

	private Map<String, FilterInfo> runningFilters = Maps.newHashMap();

	private DynamicBooleanProperty pollerEnabled = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.GateFilterPollerEnabled, true);

	private DynamicLongProperty pollerInterval = DynamicPropertyFactory.getInstance()
			.getLongProperty(Constants.GateFilterPollerInterval, 30000);

	private DynamicBooleanProperty active = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.GateUseActiveFilters, true);
	private DynamicBooleanProperty canary = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.GateUseCanaryFilters, false);

	private DynamicStringProperty preFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateFilterPrePath, null);
	private DynamicStringProperty routeFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateFilterRoutePath, null);
	private DynamicStringProperty postFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateFilterPostPath, null);
	private DynamicStringProperty errorFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateFilterErrorPath, null);
	private DynamicStringProperty customFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateFilterCustomPath, null);

	private static GateFilterPoller instance = null;

	private volatile boolean running = true;

	private Thread checherThread = new Thread("GateFilterPoller") {

		public void run() {
			while (running) {
				try {
					if (!pollerEnabled.get())
						continue;
					if (canary.get()) {
						Transaction tran = Cat.getProducer().newTransaction("FilterPoller", "canary-"+GateFilterDaoFactory.getCurrentType());
						
						try{
							Map<String, FilterInfo> filterSet = Maps.newHashMap();
	
							List<FilterInfo> activeScripts = GateFilterDaoFactory.getGateFilterDao().getAllActiveFilters();
	
							if (!activeScripts.isEmpty()) {
								for (FilterInfo filterInfo : activeScripts) {
									filterSet.put(filterInfo.getFilterId(), filterInfo);
								}
							}
	
							List<FilterInfo> canaryScripts = GateFilterDaoFactory.getGateFilterDao().getAllCanaryFilters();
							if (!canaryScripts.isEmpty()) {
								for (FilterInfo filterInfo : canaryScripts) {
									filterSet.put(filterInfo.getFilterId(), filterInfo);
								}
							}
	
							for (FilterInfo filterInfo : filterSet.values()) {
								doFilterCheck(filterInfo);
							}
							tran.setStatus(Transaction.SUCCESS);
						}catch(Throwable t){
							tran.setStatus(t);
							Cat.logError(t);
						}finally{
							tran.complete();
						}
					} else if (active.get()) {
						Transaction tran = Cat.getProducer().newTransaction("FilterPoller", "active-"+GateFilterDaoFactory.getCurrentType());
						
						try{
							List<FilterInfo> newFilters = GateFilterDaoFactory.getGateFilterDao().getAllActiveFilters();
							
							tran.setStatus(Transaction.SUCCESS);
							if (newFilters.isEmpty())
								continue;
							for (FilterInfo newFilter : newFilters) {
								doFilterCheck(newFilter);
							}
						}catch(Throwable t){
							tran.setStatus(t);
							Cat.logError(t);
						}finally{
							tran.complete();
						}
					}
				} catch (Throwable t) {
					LOGGER.error("GateFilterPoller run error!", t);
				} finally {
					try {
						sleep(pollerInterval.get());
					} catch (InterruptedException e) {
						LOGGER.error("GateFilterPoller sleep error!", e);
					}
				}
			}
		}
	};
	
	private GateFilterPoller(){

		this.checherThread.start();
	}
	
	
	public static void start(){
		if(instance == null){
			synchronized(GateFilterPoller.class){
				if(instance == null){
					instance = new GateFilterPoller() ;
				}
			}
		}
	}
	
	public static GateFilterPoller getInstance(){
		return instance;
	}

	public void stop(){
		this.running = false;
	}
	private void doFilterCheck(FilterInfo newFilter) throws IOException {
		FilterInfo existFilter = runningFilters.get(newFilter.getFilterId());
		if (existFilter == null || !existFilter.equals(newFilter)) {
			LOGGER.info("adding filter to disk" + newFilter.toString());
			writeFilterToDisk(newFilter);
			runningFilters.put(newFilter.getFilterId(), newFilter);
		}
	}

	private void writeFilterToDisk(FilterInfo newFilter) throws IOException {
		String filterType = newFilter.getFilterType();

		String path = preFiltersPath.get();
		if (filterType.equals("post")) {
			path = postFiltersPath.get();
		} else if (filterType.equals("route")) {
			path = routeFiltersPath.get();
		} else if (filterType.equals("error")) {
			path = errorFiltersPath.get();
		} else if (!filterType.equals("pre") && customFiltersPath.get() != null) {
			path = customFiltersPath.get();
		}

		File f = new File(path, newFilter.getFilterName() + ".groovy");
		FileWriter file = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(file);
		out.write(newFilter.getFilterCode());
		out.close();
		file.close();
		LOGGER.info("filter written " + f.getPath());
	}
}
