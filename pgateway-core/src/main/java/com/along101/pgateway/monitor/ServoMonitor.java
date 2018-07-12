package com.along101.pgateway.monitor;

import com.netflix.servo.monitor.Monitors;
import com.along101.pgateway.common.IMonitor;
import com.along101.pgateway.common.INamedCount;

public class ServoMonitor implements IMonitor {
    @Override
    public void register(INamedCount monitorObj) {
        Monitors.registerObject(monitorObj);
    }
}