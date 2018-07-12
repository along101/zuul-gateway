package com.along101.pgateway.wireless;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.route.Gateway;
import com.along101.pgateway.route.GatewayService;
import com.along101.pgateway.util.Debug;
import com.along101.pgateway.util.IPUtil;
import com.along101.pgateway.util.StringUtil;

public class WirelessSearchRoute extends GateFilter {

	private static Logger logger = LoggerFactory.getLogger(WirelessSearchRoute.class);

	private static final DynamicBooleanProperty HEADER_DEBUG_DISABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("gate.header.debug.disable", false);
	
	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (!ctx.sendGateResponse())
			return false;
		if (ctx.getServiceName() != null)
			return false;
		if(ctx.getThrowable() != null)
			return false;
		return true;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 20;
	}

	private String[] getRoutes(HttpServletRequest request) {
		String uri = request.getRequestURI();
		uri = uri.replaceAll("//", "/");
		String contextPath = request.getContextPath();			
		if (StringUtils.length(contextPath) > 0) {
			uri = StringUtils.substring(uri, contextPath.length());
		}
		if (StringUtils.length(uri) > 0) {
			uri = uri.substring(1, StringUtils.length(uri));
		}
		String query= request.getQueryString();	
		if(!StringUtils.isEmpty(query)){
			uri+="?"+query;
		}
		return uri.split("/");
	}

	private void throwException(int httpStatus, String code, String message) throws GateException {
		HttpServletResponse response = RequestContext.getCurrentContext().getResponse();

		response.setHeader(Constants.HTTP_ERROR_CODE_HEADER, code);
		response.setHeader(Constants.HTTP_ERROR_MESSAGE_HEADER, message);
		GateException gateException = new GateException(code, httpStatus, message);
		RequestContext.getCurrentContext().setThrowable(gateException);
		throw gateException;
	}
	
	private void fomatResponse(int httpStatus, String code, String message){
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletResponse response = RequestContext.getCurrentContext().getResponse();

		response.setHeader(Constants.HTTP_ERROR_CODE_HEADER, code);
		response.setHeader(Constants.HTTP_ERROR_MESSAGE_HEADER, message);
		ctx.setResponseStatusCode(httpStatus);
		ctx.addGateResponseHeader("Content-Type", "application/json; charset=utf-8");
		ctx.setSendGateResponse(false);
		ctx.setResponseBody("{\"Message\":\""+message+"\"}");
		StringBuilder sb = new StringBuilder(ctx.getRequest().getRequestURI()+":\n"+message);
		if(HEADER_DEBUG_DISABLED.get()){
			Enumeration<String> headerIt = ctx.getRequest().getHeaderNames();
			while (headerIt.hasMoreElements()) {
				String name = (String) headerIt.nextElement();
				String value = ctx.getRequest().getHeader(name);
				sb.append("REQUEST:: > " + name + ":" + value+"\n");
			}
		}
		logger.warn(sb.toString());
	}

	@Override
	public Object run() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("Filter", "SearchRoute");
		try{

			RequestContext ctx = RequestContext.getCurrentContext();
			HttpServletRequest request = ctx.getRequest();

			String[] routes = getRoutes(request);
			if (routes == null || routes.length != 3) {
				//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-RTE-NOACTION", "Action路由信息不存在");
				fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-RTE-NOACTION", "Action路由信息不存在");
				return null;
			}
			String area = routes[0];
			String service = routes[1];
			String serviceName = routes[2];
			Gateway gateway = null;

			try {
				gateway = GatewayService.instance().findGateway(area, service);
				ctx.set(Constants.GateWayData, gateway);
			} catch (Exception e) {
				//throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-ESB-NOTREADY", "网关可访问服务注册信息未正常加载");
				fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-ESB-NOTREADY", "网关可访问服务注册信息未正常加载");
				return null;
			}

			if (gateway == null) {
				//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-ESB-NOSERVICE",String.format("服务%s.%s在网关可访问服务配置中不存在", area, service));
				fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-ESB-NOSERVICE",String.format("服务%s.%s在网关可访问服务配置中不存在", area, service));
				return null;
			}

			if (StringUtils.isEmpty(gateway.getUrl())) {
				//throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-ESB-INVALIDSERVICEURL",String.format("服务%s在管理中心中配置的地址无效", gateway.getContract()));
				fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-ESB-INVALIDSERVICEURL",String.format("服务%s在管理中心中配置的地址无效", gateway.getContract()));
				return null;
			}
			char s = '/';
			String postUrl = String.format("%s/%s", StringUtil.trimEnd(gateway.getUrl(), s), serviceName);

			ctx.addGateRequestHeader("uri", postUrl);

			ctx.addGateRequestHeader("host", URI.create(gateway.getUrl()).getAuthority());

			ctx.addGateRequestHeader("X-ALONG-VIAGATEWAY", "1");
			String ipaddress = request.getHeader("X-ALONG-IPADDRESS");
			if (StringUtils.isEmpty(ipaddress)) {
				ctx.addGateRequestHeader("X-ALONG-IPADDRESS", IPUtil.getClientIpAddr(request));
			}
			String useragent = request.getHeader("X-ALONG-USERAGENT");
			if (StringUtils.isEmpty(useragent)) {
				System.out.println(request.getHeader("User-Agent"));
				ctx.addGateRequestHeader("X-ALONG-USERAGENT", request.getHeader("User-Agent"));
			}

			try {
				ctx.setRouteUrl(new URL(postUrl));
			} catch (Exception e) {
				Cat.logError("地址：" + postUrl + "不合法", e);
			}
			ctx.setRouteName("soa."+area+"."+service+"."+formatServiceName(routes[2]));
			ctx.setRouteGroup("soa."+area+"."+service);

			if (Debug.debugRequest()) {
				Debug.addRequestDebug("SOA::> area: ${area}");
				Debug.addRequestDebug("SOA::> service: ${service}");
				Debug.addRequestDebug("SOA::> route: ${serviceName}");
			}
			tran.setStatus(Transaction.SUCCESS);
		}catch(Throwable e){
			tran.setStatus(e);
			e.printStackTrace();
			throw e;
		}finally{
			tran.complete();
		}
		return null;
	}
	
	private String formatServiceName(String route){
		if(route.contains("?")){
			return route.substring(0, route.indexOf("?"));
		}
		return route;
	}
}