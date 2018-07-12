package com.along101.pgateway.util;

import com.along101.pgateway.common.ITracer;
import com.along101.pgateway.monitor.CounterFactory;
import com.along101.pgateway.monitor.TracerFactory;
import com.along101.pgateway.common.ITracer;
import com.along101.pgateway.monitor.CounterFactory;
import com.along101.pgateway.monitor.TracerFactory;

/**
 * Dummy implementations of CounterFactory, TracerFactory, and Tracer
 */
public class MonitoringUtil {

    public static final void initMocks() {
        CounterFactory.initialize(new CounterFactoryImpl());
        TracerFactory.initialize(new TracerFactoryImpl());
    }

    private static final class CounterFactoryImpl extends CounterFactory {
        @Override
        public void increment(String name) {}
    }

    private static final class TracerFactoryImpl extends TracerFactory {
        @Override
        public ITracer startMicroTracer(String name) {
            return new TracerImpl();
        }
    }

    private static final class TracerImpl implements ITracer {
        @Override
        public void setName(String name) {}

        @Override
        public void stopAndLog() {}
    }

}