package com.along101.pgateway.wireless;

import javax.servlet.http.HttpServletResponse;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.route.GatewayService;

public class HealthCheck extends GateFilter{
	private static final DynamicBooleanProperty IS_HEALTH = DynamicPropertyFactory.getInstance().getBooleanProperty("gate.is-health", true);
	
	@Override
	public String filterType() {
		return "pre";
	}
	
	public String uri() {
		return "/";
	}
	
	@Override
	public boolean shouldFilter() {
		String path = RequestContext.getCurrentContext().getRequest().getRequestURI();
		return path.equalsIgnoreCase(uri());
	}
	
	public int filterOrder(){
		return 0;
	}
	
	public String responseBody() {
		if (IS_HEALTH.get()) {
			return "4008206666";
		}else{
			return GatewayService.instance().printJson();
			//return "212323";
		}
	}
	
	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// Set the default response code for static filters to be 200
		ctx.getResponse().setStatus(HttpServletResponse.SC_OK);
		// first StaticResponseFilter instance to match wins, others do not set body and/or status
		if (ctx.getResponseBody() == null) {
			ctx.setResponseBody(responseBody());
			ctx.setSendGateResponse(false);
		}
		return null;
	}
}
