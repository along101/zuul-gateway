package com.along101.pgateway.wireless;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.unidal.lookup.util.StringUtils;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.google.common.collect.Lists;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.util.Debug;

public class InternalSearchRoute extends GateFilter {

	private static final DynamicStringProperty gateURL = DynamicPropertyFactory.getInstance()
			.getStringProperty("apigate.url.postfix", ".api.along101.com");
	
	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (!ctx.sendGateResponse())
			return false;
		if (ctx.getRoute() != null)
			return false;
		
		return true;
	}

	public int filterOrder() {
		return 1;

	}

	private void throwException(int httpStatus, String code, String message) throws GateException {
		HttpServletResponse response = RequestContext.getCurrentContext().getResponse();
		response.setHeader(Constants.HTTP_ERROR_CODE_HEADER, code);
		response.setHeader(Constants.HTTP_ERROR_MESSAGE_HEADER, message);
		throw  new GateException(code, httpStatus, message);
	}
	
	private List<String> getPath(String uri) throws GateException{
		List<String> list = Lists.newArrayList();
//		String[] path = uri.split("\\?");
//		
//		String[] routes = path[0].split("/");
//		
//		if(path[0].startsWith("/")){
//			if(routes.length > 2){
//				list.add(routes[1]);
//				list.add(routes[2]);
//				list.add(uri.substring(routes[1].length()+routes[2].length()+2));
//			}
//		}else{
//			if(routes.length > 1){
//				list.add(routes[0]);
//				list.add(routes[1]);
//				list.add(uri.substring(routes[0].length()+routes[1].length()+1));
//			}
//		}
//		 
		
		String[] paths = uri.split("\\?");
		String[] routes = paths[0].split("/");
		
		String serviceName = null;
		String path = null;
		for(String route:routes){
			if(!"".equals(route.trim())){
				serviceName = route;
				break;
			}
		}
		
		if(serviceName == null){
			throwException(HttpServletResponse.SC_NOT_FOUND, "Service-NotFoud-1", "请求格式不正确,serviceName为空！") ;
		}
		list.add(serviceName);
		if(paths[0].startsWith("/")){
			list.add(uri.substring(serviceName.length()+1));			
		}else{			
			list.add(uri.substring(serviceName.length()));			
		}
		return list;
	}

	@Override
	public Object run() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("Filter", "SearchRoute");
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			HttpServletRequest request = ctx.getRequest();
					
			//先取header头信息,取得默认信息
			String domain = request.getHeader(Constants.HTTP_ALONG_DOMAIN);
			String serviceName = request.getHeader(Constants.HTTP_ALONG_SERVICEID);
			
			Enumeration<String> headers = request.getHeaderNames();
			
			while(headers.hasMoreElements()){
				System.out.println(headers.nextElement());
			}
			
			String route = ctx.getRequest().getRequestURI();
			if(StringUtils.isEmpty(domain) && StringUtils.isEmpty(serviceName)){
				
//				List<String> routes = getPath(ctx.getRequest().getRequestURI());				
//				serviceName = routes.get(0);
//				
//				if(routes.size() == 2){
//					route = routes.get(1);
//				}
				String host = request.getHeader("host");
				
				if(StringUtils.isNotEmpty(host)){
					int index = host.lastIndexOf(gateURL.get());
					
					if(index < 0){
						throw new GateException("GWT-NO-HOST", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, host+"访问域名不正确！");
					}else{
						serviceName = host.substring(0, index);
					}
				}
				
			}
			String query= ctx.getRequest().getQueryString();	
			if(!StringUtils.isEmpty(query)){
				route+="?"+query;
			}
			ctx.setRoute(route);
			ctx.setServiceName(serviceName);
			
			if(domain == null){
				domain = Constants.DefaultDomain;
			}
			
			ctx.setRouteName(("soa."+domain+"."+serviceName).toLowerCase());
			ctx.setRouteGroup(("soa."+domain).toLowerCase());

			if (Debug.debugRequest()) {
				Debug.addRequestDebug("SOA::> service: ${domain}");
				Debug.addRequestDebug("SOA::> route: ${serviceName}");
			}
			tran.setStatus(Transaction.SUCCESS);
		} catch (Exception e) {
			tran.setStatus(e);
			throw e;
		} finally {
			tran.complete();
		}
		return null;
	}
	
	public static void main(String args[]){
//		String uri = "//biz/test/aga?fsafas=fsa?fsaf?fs";
//		String[] paths = uri.split("\\?");
//		String[] routes = paths[0].split("/");
//		
//		String serviceName = null;
//		String path = null;
//		for(String route:routes){
//			if(!"".equals(route.trim())){
//				serviceName = route;
//				break;
//			}
//		}
//		
//		if(paths[0].startsWith("/")){
//			path = uri.substring(serviceName.length()+1);			
//		}else{			
//			path = uri.substring(serviceName.length());			
//		}
//		
//		System.out.println(path);
//		System.out.println(serviceName);
		
		String host = "myaser.api.along101.com";
		String serviceName = "";
		
		int index = host.lastIndexOf(gateURL.get());
		
		if(index < 0){
			System.out.println("error");
		}else{
			serviceName = host.substring(0, index);
		}
		
		System.out.println(serviceName);
	


		
	}
}
