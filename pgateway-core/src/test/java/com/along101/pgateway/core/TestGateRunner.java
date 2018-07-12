package com.along101.pgateway.core;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.along101.pgateway.common.HttpServletRequestWrapper;
import com.along101.pgateway.common.HttpServletResponseWrapper;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TestGateRunner {
	@Mock
	GateFilter filter;

	@Mock
	HttpServletRequest servletRequest;

	@Mock
	HttpServletResponse servletResponse;

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

		GateRunner runner = new GateRunner();
		runner = spy(runner);
		RequestContext context = spy(RequestContext.getCurrentContext());

		try {
			FilterProcessor.setProcessor(processor);
			RequestContext.testSetCurrentContext(context);
			when(servletResponse.getWriter()).thenReturn(writer);

			runner.init(servletRequest, servletResponse);
			verify(runner, times(1)).init(servletRequest, servletResponse);
			assertTrue(RequestContext.getCurrentContext().getRequest() instanceof HttpServletRequestWrapper);
			assertTrue(RequestContext.getCurrentContext().getResponse() instanceof HttpServletResponseWrapper);

			runner.preRoute();
			verify(processor, times(1)).preRoute();

			runner.postRoute();
			verify(processor, times(1)).postRoute();

			runner.route();
			verify(processor, times(1)).route();
			RequestContext.testSetCurrentContext(null);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
