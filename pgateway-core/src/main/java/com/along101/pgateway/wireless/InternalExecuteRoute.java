package com.along101.pgateway.wireless;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.dianping.cat.Cat;
import com.dianping.cat.Cat.Context;
import com.dianping.cat.message.Transaction;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Builder;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.CatContext;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.GateHeaders;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.hystrix.GateCommandHelper;
import com.along101.pgateway.hystrix.RestClient;
import com.along101.pgateway.hystrix.RestClientFactory;
import com.along101.pgateway.hystrix.RibbonRequestCommandForSemaphoreIsolation;
import com.along101.pgateway.hystrix.RibbonRequestCommandForThreadIsolation;
import com.along101.pgateway.util.Debug;
import com.along101.pgateway.util.HTTPRequestUtil;

public class InternalExecuteRoute extends GateFilter {
	
	
	private static final DynamicStringProperty clientRefresher = DynamicPropertyFactory.getInstance().getStringProperty("ribbon.refresh.serviceclient", "");
	                                         
	
	static{
		clientRefresher.addCallback(new Runnable(){
			public void run(){
				RestClientFactory.closeRestClient(clientRefresher.get().trim());
			}
		});
	}

	@Override
	public String filterType() {
		return "route";
	}

	public int filterOrder() {
		return 20;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return ctx.getRoute() != null && ctx.sendGateResponse();
	}

	@Override
	public Object run() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("Filter", "ExecuteRibbon");
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		String uri = ctx.getRoute();
		String serviceName = ctx.getServiceName();
		try {

			String groupName = ctx.getRouteGroup();
			String routeName = ctx.getRouteName();

			if (groupName == null)
				groupName = Constants.DefaultGroup;
			if (routeName == null)
				routeName = Constants.DefaultName;
			int contentLength = request.getContentLength();
			String verb = request.getMethod().toUpperCase();
			Collection<Header> headers = buildGateRequestHeaders(RequestContext.getCurrentContext().getRequest());
			InputStream requestEntity = getRequestBody(RequestContext.getCurrentContext().getRequest());
			IClientConfig clientConfig = buildRequestConfig(serviceName,routeName,groupName);
			
			RestClient client = RestClientFactory.getRestClient(serviceName, clientConfig);
			Cat.logEvent("route.serviceName", serviceName);
			HttpResponse response = forward(client,clientConfig, verb, uri, serviceName, headers, requestEntity, contentLength,
					groupName, routeName);
			setResponse(response);
			tran.setStatus(Transaction.SUCCESS);
		} catch (Exception e) {
			tran.setStatus(e);
			Exception ex = e;
			String errorMsg = "[${ex.class.simpleName}]{${ex.message}}   ";
			Throwable cause = null;
			while ((cause = ex.getCause()) != null) {
				ex = (Exception) cause;
				errorMsg = "${errorMsg}[${ex.class.simpleName}]{${ex.message}}   ";
			}

			Cat.logError("Service Execution Error,serviceName: ${serviceName}\nCause: ${errorMsg}\nUri:${uri}", e);
			throw new GateException(errorMsg, 500, ",serviceName: ${serviceName}\nCause: ${errorMsg}\nUri:${uri}");
		} finally {
			tran.complete();
		}
		return null;
	}

	public HttpResponse forward(AbstractLoadBalancerAwareClient client,IClientConfig clientConfig, String verb, String uri, String serviceName,
			Collection<Header> headers, InputStream entity, int contentLength, String groupName, String routeName)
			throws IOException, URISyntaxException {
		entity = debug(verb, serviceName, headers, entity, contentLength);
		HttpRequest request;
		Builder builder = HttpRequest.newBuilder();
		for (Header header : headers) {
			builder.header(header.getName(), header.getValue());
		}
    	Context ctx = new CatContext();
    	Cat.logRemoteCallClient(ctx);
    	builder.header(Constants.CAT_ROOT_MESSAGE_ID, ctx.getProperty(Cat.Context.ROOT));
    	builder.header(Constants.CAT_PARENT_MESSAGE_ID, ctx.getProperty(Cat.Context.PARENT));
    	builder.header(Constants.CAT_CHILD_MESSAGE_ID, ctx.getProperty(Cat.Context.CHILD));
    	
		switch (verb) {
		case "POST":
			builder.verb(Verb.POST);
			request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
			break;
		case "PUT":
			builder.verb(Verb.PUT);
			request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
			break;
		default:
			builder.verb(getVerb(verb));
			request = builder.entity(entity).overrideConfig(clientConfig).uri(new URI(uri)).build();
		}

		String isolationStrategy = DynamicPropertyFactory.getInstance()
				.getStringProperty(routeName + ".isolation.strategy", null).get();
		if (isolationStrategy == null) {
			isolationStrategy = DynamicPropertyFactory.getInstance()
					.getStringProperty(groupName + ".isolation.strategy", null).get();
		}
		if (isolationStrategy == null) {
			isolationStrategy = DynamicPropertyFactory.getInstance()
					.getStringProperty("gate.isolation.strategy.global", "SEMAPHORE").get();
		}

		long start = System.currentTimeMillis();
		try {
			if ("THREAD".equalsIgnoreCase(isolationStrategy)) {
				return new RibbonRequestCommandForThreadIsolation(client,request, serviceName, groupName, routeName).execute();
			} else {
				return new RibbonRequestCommandForSemaphoreIsolation(client,request, serviceName, groupName, routeName)
						.execute();
			}
		} finally {
			RequestContext.getCurrentContext().set("remoteCallCost", System.currentTimeMillis() - start);
		}
	}

	void setResponse(HttpResponse response) throws Exception {

		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.setResponseStatusCode(response.getStatus());
		boolean isOriginResponseGZipped = false;
		String headerValue;
		Map<String, Collection<String>> map = response.getHeaders();		
		for (String headerName : map.keySet()) {
			headerValue = (map.get(headerName).toArray()[0]).toString();
			if (isValidGateResponseHeader(headerName)) {
				ctx.addGateResponseHeader(headerName, headerValue);
			}
			if (headerName.equalsIgnoreCase(GateHeaders.CONTENT_LENGTH)) {
				ctx.setOriginContentLength(headerValue);
			}
			if (headerName.equalsIgnoreCase(GateHeaders.CONTENT_ENCODING)) {
				if (HTTPRequestUtil.isGzipped(headerValue)) {
					isOriginResponseGZipped = true;
				}
			}
		}
		ctx.setResponseGZipped(isOriginResponseGZipped);
		InputStream inputStream = null;

		inputStream = response.getInputStream();
		if (Debug.debugRequest()) {
			if (inputStream == null) {
				Debug.addRequestDebug("ORIGIN_RESPONSE:: < null ");
			} else {
				byte[] origBytes = getBytes(inputStream, 8096);
				byte[] contentBytes = origBytes;
				contentBytes = getBytes(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), 8096);
				String entity = new String(contentBytes);
				Debug.addRequestDebug("ORIGIN_RESPONSE:: < ${entity}");
				inputStream = new ByteArrayInputStream(origBytes);
			}
		}

		ctx.setResponseDataStream(inputStream);
	}

	public InputStream debug(String verb, String url, Collection<Header> headers, InputStream requestEntity,
			int contentLength) throws IOException {
		if (Debug.debugRequest()) {
			RequestContext.getCurrentContext().addGateResponseHeader("x-target-url", url);
			Debug.addRequestDebug("GATE:: url=${url}");
			for (Header it : headers) {
				Debug.addRequestDebug("GATE::> ${it.name}  ${it.value}");
			}
			if (requestEntity != null) {
				requestEntity = debugRequestEntity(requestEntity, contentLength);
			}
		}
		return requestEntity;
	}

	private boolean isValidGateResponseHeader(String name) {
		switch (name.toLowerCase()) {
		case "connection":
		case "content-length":
		case "content-encoding":
		case "server":
		case "transfer-encoding":
		case "access-control-allow-origin":
		case "access-control-allow-headers":
			return false;
		default:
			return true;
		}
	}

	private InputStream debugRequestEntity(InputStream inputStream, int contentLength) throws IOException {
		if (Debug.debugRequestHeadersOnly())
			return inputStream;
		if (inputStream == null)
			return null;
		byte[] entityBytes = getBytes(inputStream, contentLength);
		String entity = new String(entityBytes);
		Debug.addRequestDebug("GATE::> ${entity}");
		return new ByteArrayInputStream(entityBytes);
	}

	private byte[] getBytes(InputStream is, int contentLength) throws IOException {
		ByteArrayOutputStream answer = new ByteArrayOutputStream();
		// reading the content of the file within a byte buffer
		byte[] byteBuffer = new byte[contentLength];
		int nbByteRead /* = 0 */;
		try {
			while ((nbByteRead = is.read(byteBuffer)) != -1) {
				// appends buffer
				answer.write(byteBuffer, 0, nbByteRead);
			}
		} finally {
			// closeWithWarning(is);
		}
		return answer.toByteArray();
	}

	private Collection<Header> buildGateRequestHeaders(HttpServletRequest request) {
		Map<String, Header> headersMap = new HashMap<>();

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = ((String) headerNames.nextElement()).toLowerCase();
			String value = request.getHeader(name);
			if (isValidGateRequestHeader(name)) {
				headersMap.put(name, new BasicHeader(name, value));
			}
		}

		Map<String, String> gateRequestHeaders = RequestContext.getCurrentContext().getGateRequestHeaders();
		for (String key : gateRequestHeaders.keySet()) {
			String name = key.toLowerCase();
			String value = gateRequestHeaders.get(key);
			headersMap.put(name, new BasicHeader(name, value));
		}

		if (RequestContext.getCurrentContext().getResponseGZipped()) {
			String name = "accept-encoding";
			String value = "gzip";
			headersMap.put(name, new BasicHeader(name, value));
		}
		return headersMap.values();
	}

	public boolean isValidGateRequestHeader(String name) {
		if (name.toLowerCase().contains("content-length")) {
			return false;
		}
		if (!RequestContext.getCurrentContext().getResponseGZipped()) {
			if (name.toLowerCase().contains("accept-encoding")) {
				return false;
			}
		}
		return true;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		} catch (IOException e) {
			// no requestBody is ok.
		}
		return requestEntity;
	}
	
	private IClientConfig buildRequestConfig(String serviceName,String routeName,String routeGroup) {
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.loadProperties(serviceName);
		clientConfig.setProperty(CommonClientConfigKey.NIWSServerListClassName, "com.along101.pgateway.hystrix.DiscoveryServerList");
		clientConfig.setProperty(CommonClientConfigKey.ClientClassName, "com.along101.pgateway.hystrix.RestClient");
		//clientConfig.setProperty(CommonClientConfigKey.NIWSServerListClassName, "com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList");
		clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerRuleClassName, GateCommandHelper.getRibbonLoadBalanceRule(routeGroup, routeName));
		clientConfig.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, GateCommandHelper.getRibbonMaxHttpConnectionsPerHost(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxTotalHttpConnections, GateCommandHelper.getRibbonMaxTotalHttpConnections(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxAutoRetries, GateCommandHelper.getRibbonMaxAutoRetries(serviceName));
		clientConfig.setProperty(CommonClientConfigKey.MaxAutoRetriesNextServer, GateCommandHelper.getRibbonMaxAutoRetriesNextServer(serviceName));		
		clientConfig.setProperty(CommonClientConfigKey.ConnectTimeout, GateCommandHelper.getRibbonConnectTimeout(routeGroup,routeName));
		clientConfig.setProperty(CommonClientConfigKey.ReadTimeout, GateCommandHelper.getRibbonReadTimeout(routeGroup,routeName));
		clientConfig.setProperty(CommonClientConfigKey.RequestSpecificRetryOn,GateCommandHelper.getRibbonRequestSpecificRetryOn(routeGroup, routeName));
		clientConfig.setProperty(CommonClientConfigKey.OkToRetryOnAllOperations,GateCommandHelper.getRibbonOkToRetryOnAllOperations(routeGroup, routeName));
        
		return clientConfig;
	}

	private Verb getVerb(String verb) {
		switch (verb) {
		case "POST":
			return Verb.POST;
		case "PUT":
			return Verb.PUT;
		case "DELETE":
			return Verb.DELETE;
		case "HEAD":
			return Verb.HEAD;
		case "OPTIONS":
			return Verb.OPTIONS;
		case "GET":
			return Verb.GET;
		default:
			return Verb.GET;
		}
	}
}
