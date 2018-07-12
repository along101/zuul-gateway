package com.along101.pgateway.monitor;

import com.along101.pgateway.common.IMonitor;
import com.along101.pgateway.common.INamedCount;

public class MonitorRegistry {

    private static  final MonitorRegistry instance = new MonitorRegistry();
    private IMonitor publisher;

    /**
     * A Monitor implementation should be set here
     * @param publisher
     */
    public void setPublisher(IMonitor publisher) {
        this.publisher = publisher;
    }



    public static MonitorRegistry getInstance() {
        return instance;
    }

    public void registerObject(INamedCount monitorObj) {
      if(publisher != null) publisher.register(monitorObj);
    }
}