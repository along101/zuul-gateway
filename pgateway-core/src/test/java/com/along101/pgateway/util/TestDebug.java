package com.along101.pgateway.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDebug {
	  @Test
      public void testRequestDebug() {
          assertFalse(Debug.debugRouting());
          assertFalse(Debug.debugRequest());
          Debug.setDebugRouting(true);
          Debug.setDebugRequest(true);
          assertTrue(Debug.debugRouting());
          assertTrue(Debug.debugRequest());

          Debug.addRoutingDebug("test1");
          assertTrue(Debug.getRoutingDebug().contains("test1"));

          Debug.addRequestDebug("test2");
          assertTrue(Debug.getRequestDebug().contains("test2"));


      }
}
