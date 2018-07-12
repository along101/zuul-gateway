package com.along101.pgateway.monitor;


import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.servo.Metric;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.tag.Tag;
import com.along101.pgateway.util.MetricUtil;

public class DashboardMetricObserver extends BaseMetricObserver{
    private DynamicStringProperty appName = DynamicPropertyFactory.getInstance().getStringProperty("archaius.deployment.applicationId", "pgateway");

    /**
     * Creates a new instance with a given name.
     *
     * @param name
     */
    public DashboardMetricObserver(String name) {
        super(name);
    }

    @Override
    public void updateImpl(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        String name;
        TreeMap<String, String> tags;
        String prefix = appName.get();
        for (Metric metric : metrics) {
            name = prefix + "." + metric.getConfig().getName();
            tags = new  TreeMap<String, String>();
            for (Tag tag : metric.getConfig().getTags()) {
                tags.put(tag.getKey(), tag.getValue());
            }
            
            try{
            	MetricUtil.log(name, Float.parseFloat(metric.getValue().toString()),tags, new Date(metric.getTimestamp()));
            }catch(Throwable t){
            }
         
        }
    }
}