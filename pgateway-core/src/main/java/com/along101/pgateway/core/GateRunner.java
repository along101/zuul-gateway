package com.along101.pgateway.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.HttpServletRequestWrapper;
import com.along101.pgateway.common.HttpServletResponseWrapper;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.HttpServletRequestWrapper;
import com.along101.pgateway.common.HttpServletResponseWrapper;
import com.along101.pgateway.context.RequestContext;

/**
 * This class initializes servlet requests and responses into the RequestContext
 * and wraps the FilterProcessor calls to preRoute(), route(), postRoute(), and
 * error() methods
 *
 */
public class GateRunner {

	/**
	 * Creates a new <code>GateRunner</code> instance.
	 */
	public GateRunner() {
	}

	/**
	 * sets HttpServlet request and HttpResponse
	 *
	 * @param servletRequest
	 * @param servletResponse
	 */
	public void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		RequestContext.getCurrentContext().setRequest(new HttpServletRequestWrapper(servletRequest));
		RequestContext.getCurrentContext().setResponse(new HttpServletResponseWrapper(servletResponse));
	}

	/**
	 * executes "pre" filterType GateFilters
	 *
	 * @throws GateException
	 */
	public void preRoute() throws GateException {
		FilterProcessor.getInstance().preRoute();
	}

	/**
	 * executes "route" filterType GateFilters
	 *
	 * @throws GateException
	 */
	public void route() throws GateException {
		FilterProcessor.getInstance().route();
	}

	/**
	 * executes "post" filterType GateFilters
	 *
	 * @throws GateException
	 */
	public void postRoute() throws GateException {
		FilterProcessor.getInstance().postRoute();
	}

	/**
	 * executes "error" filterType GateFilters
	 * @throws GateException 
	 */
	public void error() throws GateException {
		FilterProcessor.getInstance().error();
	}

}
