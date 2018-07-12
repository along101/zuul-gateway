package com.along101.pgateway.servlet;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.along101.pgateway.common.HttpServletRequestWrapper;
import com.along101.pgateway.common.HttpServletResponseWrapper;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.core.FilterProcessor;

public class TestSyncGateServlet {

    @Mock
    HttpServletRequest servletRequest;
    @Mock
    HttpServletResponseWrapper servletResponse;
    @Mock
    FilterProcessor processor;
    @Mock
    PrintWriter writer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProcessGateFilter() {

    	SyncGateServlet gateServlet = new SyncGateServlet();
        gateServlet = spy(gateServlet);
        RequestContext context = spy(RequestContext.getCurrentContext());


        try {
            FilterProcessor.setProcessor(processor);
            RequestContext.testSetCurrentContext(context);
            when(servletResponse.getWriter()).thenReturn(writer);

            gateServlet.init(servletRequest, servletResponse);
            verify(gateServlet, times(1)).init(servletRequest, servletResponse);
            assertTrue(RequestContext.getCurrentContext().getRequest() instanceof HttpServletRequestWrapper);
            assertTrue(RequestContext.getCurrentContext().getResponse() instanceof HttpServletResponseWrapper);

            gateServlet.preRoute();
            verify(processor, times(1)).preRoute();

            gateServlet.postRoute();
            verify(processor, times(1)).postRoute();

            gateServlet.route();
            verify(processor, times(1)).route();
            RequestContext.testSetCurrentContext(null);

        } catch (Exception e) {
        	e.printStackTrace();
        }


    }
}
