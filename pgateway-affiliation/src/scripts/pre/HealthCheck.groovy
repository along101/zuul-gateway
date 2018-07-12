package scripts.pre

import javax.servlet.http.HttpServletResponse

import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.route.GatewayService

public class HealthCheck extends GateFilter{
	private static final DynamicBooleanProperty IS_HEALTH = DynamicPropertyFactory.getInstance().getBooleanProperty("gate.is-health", true)
	
	@Override
	public String filterType() {
		return "pre";
	}
	
	public Object uri() {
		return "/hs";
	}
	
	@Override
	boolean shouldFilter() {
		String path = RequestContext.currentContext.getRequest().getRequestURI()
		return path.equalsIgnoreCase(uri());
	}
	
	public int filterOrder(){
		return 0;
	}
	
	public String responseBody() {
		if (IS_HEALTH.get()) {
			return "OK";
		}else{
			return GatewayService.instance().printJson();
		}
	}
	
	@Override
	Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// Set the default response code for static filters to be 200
		ctx.getResponse().setStatus(HttpServletResponse.SC_OK);
		// first StaticResponseFilter instance to match wins, others do not set body and/or status
		if (ctx.getResponseBody() == null) {
			ctx.setResponseBody(responseBody())
			ctx.sendGateResponse = false;
		}
	}
}
