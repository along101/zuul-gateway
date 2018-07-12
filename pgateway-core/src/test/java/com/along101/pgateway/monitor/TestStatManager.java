package com.along101.pgateway.monitor;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

public class TestStatManager {
	   @Test
       public void testCollectRouteStats() {
           String route = "test";
           int status = 500;

           StatManager sm = StatManager.getManager();
           assertNotNull(sm);

           // 1st request
           sm.collectRouteStatusStats(route, status);

           ConcurrentHashMap<Integer, RouteStatusCodeMonitor> routeStatusMap = sm.routeStatusMap.get("test");
           assertNotNull(routeStatusMap);


           RouteStatusCodeMonitor routeStatusMonitor = routeStatusMap.get(status);


           // 2nd request
           sm.collectRouteStatusStats(route, status);

       }

       @Test
       public void testGetRouteStatusCodeMonitor() {
    	   StatManager sm = StatManager.getManager();
           assertNotNull(sm);
           sm.collectRouteStatusStats("test", 500);
           assertNotNull(sm.getRouteStatusCodeMonitor("test", 500));
       }

       @Test
       public void testCollectRequestStats() {
           final String host = "api.netflix.com";
           final String proto = "https";

           final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
           when(req.getHeader(StatManager.HOST_HEADER)).thenReturn(host);
           when(req.getHeader(StatManager.X_FORWARDED_PROTO_HEADER)).thenReturn(proto);
           when(req.getRemoteAddr()).thenReturn("127.0.0.1");

           final StatManager sm = StatManager.getManager();
           sm.collectRequestStats(req);

           final NamedCountingMonitor hostMonitor = sm.getHostMonitor(host);
           assertNotNull("hostMonitor should not be null", hostMonitor);

           final NamedCountingMonitor protoMonitor = sm.getProtocolMonitor(proto);
           assertNotNull("protoMonitor should not be null", protoMonitor);

           assertEquals(1, hostMonitor.getCount());
           assertEquals(1, protoMonitor.getCount());
       }

       @Test
       public void createsNormalizedHostKey() {

       }

       @Test
       public void extractsClientIpFromXForwardedFor() {
           final String ip1 = "hi";
           final String ip2 = "hey";
           assertEquals(ip1, StatManager.extractClientIpFromXForwardedFor(ip1));
           assertEquals(ip1, StatManager.extractClientIpFromXForwardedFor(String.format("%s,%s", ip1, ip2)));
           assertEquals(ip1, StatManager.extractClientIpFromXForwardedFor(String.format("%s, %s", ip1, ip2)));
       }

       @Test
       public void isIPv6() {
           assertTrue(StatManager.isIPv6("0:0:0:0:0:0:0:1"));
           assertTrue(StatManager.isIPv6("2607:fb10:2:232:72f3:95ff:fe03:a6e7"));
           assertFalse(StatManager.isIPv6("127.0.0.1"));
           assertFalse(StatManager.isIPv6("localhost"));
       }
       
       @Test
       public void testPutStats() {
    	   StatManager sm = new StatManager();
           assertNotNull(sm);
           sm.collectRouteErrorStats("test", "cause");
           assertNotNull(sm.routeErrorMap.get("test"));
           ConcurrentHashMap<String, RouteErrorMonitor> map = sm.routeErrorMap.get("test");
           RouteErrorMonitor sd = map.get("cause");
           assertEquals(sd.count.get(), 1);
           sm.collectRouteErrorStats("test", "cause");
           assertEquals(sd.count.get(), 2);
       }


       @Test
       public void testGetStats() {
    	   StatManager sm = new StatManager();
           assertNotNull(sm);
           sm.collectRouteErrorStats("test", "cause");
           assertNotNull(sm.getStats("test", "cause"));
       }

}
