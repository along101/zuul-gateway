package com.along101.pgateway.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.along101.pgateway.common.ExecutionStatus;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.GateFilterResult;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class TestFilterProcessor {
	  @Mock
      GateFilter filter;

      @Before
      public void before() {
          //MonitoringUtils.initMocks();
          MockitoAnnotations.initMocks(this);
      }

      @Test
      public void testProcessGateFilter() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              processor.processGateFilter(filter);
              verify(processor, times(1)).processGateFilter(filter);
              verify(filter, times(1)).runFilter();

          } catch (Throwable e) {
              e.printStackTrace();
          }
      }

      @Test
      public void testProcessGateFilterException() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);

          try {
              GateFilterResult r = new GateFilterResult(ExecutionStatus.FAILED);
              r.setException(new Exception("Test"));
              when(filter.runFilter()).thenReturn(r);
              processor.processGateFilter(filter);
              assertFalse(true);
          } catch (Throwable e) {
              assertEquals(e.getCause().getMessage(), "Test");
          }
      }


      @Test
      public void testPostProcess() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              processor.postRoute();
              verify(processor, times(1)).runFilters("post");
          } catch (Throwable e) {
              e.printStackTrace();
          }
      }

      @Test
      public void testPreProcess() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              processor.preRoute();
              verify(processor, times(1)).runFilters("pre");
          } catch (Throwable e) {
              e.printStackTrace();
          }
      }

      @Test
      public void testRouteProcess() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              processor.route();
              verify(processor, times(1)).runFilters("route");
          } catch (Throwable e) {
              e.printStackTrace();
          }
      }

      @Test
      public void testRouteProcessHttpException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              when(processor.runFilters("route")).thenThrow(new GateException("test", 400, "test"));
              processor.route();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 400);
          } catch (Throwable e) {
              e.printStackTrace();
              assertFalse(true);

          }

      }

      @Test
      public void testRouteProcessException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);

          try {
              when(processor.runFilters("route")).thenThrow(new Throwable("test"));
              processor.route();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 500);
          } catch (Throwable e) {
              assertFalse(true);
          }

      }

      @Test
      public void testPreProcessException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);

          try {
              when(processor.runFilters("pre")).thenThrow(new Throwable("test"));
              processor.preRoute();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 500);
          } catch (Throwable e) {
              assertFalse(true);
          }

      }

      @Test
      public void testPreProcessHttpException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              when(processor.runFilters("pre")).thenThrow(new GateException("test", 400, "test"));
              processor.preRoute();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 400);
          } catch (Throwable e) {
              e.printStackTrace();
              assertFalse(true);

          }

      }


      @Test
      public void testPostProcessException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);

          try {
              when(processor.runFilters("post")).thenThrow(new Throwable("test"));
              processor.postRoute();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 500);
          } catch (Throwable e) {
              assertFalse(true);
          }

      }

      @Test
      public void testPostProcessHttpException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              when(processor.runFilters("post")).thenThrow(new GateException("test", 400, "test"));
              processor.postRoute();
          } catch (GateException e) {
              assertEquals(e.getMessage(), "test");
              assertEquals(e.nStatusCode, 400);
          } catch (Throwable e) {
              e.printStackTrace();
              assertFalse(true);

          }

      }


      @Test
      public void testErrorException() {
          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);

          try {
              when(processor.runFilters("error")).thenThrow(new Exception("test"));
              processor.error();
              assertTrue(true);
          } catch (Throwable e) {
              assertFalse(true);
          }

      }

      @Test
      public void testErrorHttpException() {
          HttpServletRequest request = mock(HttpServletRequest.class);
          HttpServletResponse response = mock(HttpServletResponse.class);
          RequestContext.getCurrentContext().setRequest(request);
          RequestContext.getCurrentContext().setResponse(response);

          FilterProcessor processor = new FilterProcessor();
          processor = spy(processor);
          try {
              when(processor.runFilters("error")).thenThrow(new GateException("test", 400, "test"));
              processor.error();
              assertTrue(true);
          } catch (Throwable e) {
              e.printStackTrace();
              assertFalse(true);

          }

      }
}
