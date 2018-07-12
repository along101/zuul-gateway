package com.along101.pgateway.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestRouteErrorMonitor {

    @Test
    public void testUpdateStats() {
        RouteErrorMonitor sd = new RouteErrorMonitor("route", "test");
        assertEquals(sd.error_cause, "test");
        sd.update();
        assertEquals(sd.count.get(), 1);
        sd.update();
        assertEquals(sd.count.get(), 2);
    }


    @Test
    public void testEquals() {
        RouteErrorMonitor sd = new RouteErrorMonitor("route", "test");
        RouteErrorMonitor sd1 = new RouteErrorMonitor("route", "test");
        RouteErrorMonitor sd2 = new RouteErrorMonitor("route", "test1");
        RouteErrorMonitor sd3 = new RouteErrorMonitor("route", "test");

        assertTrue(sd.equals(sd1));
        assertTrue(sd1.equals(sd));
        assertTrue(sd.equals(sd));
        assertFalse(sd.equals(sd2));
        assertFalse(sd2.equals(sd3));
    }
}
