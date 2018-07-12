package com.along101.pgateway.monitor;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;

public class MetricPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricPoller.class);

    final static PollScheduler scheduler = PollScheduler.getInstance();

    public static void startPoller(){
        scheduler.start();
        final int heartbeatInterval = 60;

        MetricObserver transform = new CounterToRateMetricTransform(
                new DashboardMetricObserver("GateMetrics"),
                heartbeatInterval, TimeUnit.SECONDS);

        PollRunnable task = new PollRunnable(
                new MonitorRegistryMetricPoller(),
                BasicMetricFilter.MATCH_ALL,
                transform);

        final int samplingInterval = 10;
        scheduler.addPoller(task, samplingInterval, TimeUnit.SECONDS);

    }

}