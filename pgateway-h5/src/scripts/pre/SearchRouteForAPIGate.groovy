package scripts.pre

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.util.Debug;

public class SearchRouteForAPI extends GateFilter {
	private static final DynamicStringProperty API_GATE_ADDR = DynamicPropertyFactory.getInstance().getStringProperty("gate.api.address", "http://api.along101.com:8080");
	
	private static Logger logger = LoggerFactory.getLogger(SearchRouteForAPI.class);
	
	private static final DynamicBooleanProperty HEADER_DEBUG_DISABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("gate.header.debug.disable", false);

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (!ctx.sendGateResponse())
			return false;
		if (ctx.getRouteUrl() != null)
			return false;
		if(ctx.getThrowable() != null)
			return false;

		String serviceName = ctx.getRequest().getHeader(Constants.HTTP_ALONG_SERVICEID);
		if(serviceName == null)
			return false;
		return true;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 5;
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
		RequestContext ctx = RequestContext.getCurrentContext();
		Transaction tran = Cat.getProducer().newTransaction("SearchRouteAPIFilter", ctx.getRequest().getRequestURL().toString());
		try{

			HttpServletRequest request = ctx.getRequest();

		
			//先取header头信息,取得默认信息
			String domain = request.getHeader(Constants.HTTP_ALONG_DOMAIN);
			String serviceName = request.getHeader(Constants.HTTP_ALONG_SERVICEID);
			
			if(StringUtils.isEmpty(serviceName)){
				//throw new GateException( "GTW-SERVICE_EMPTY",HttpServletResponse.SC_BAD_REQUEST,"X-ALONG-SERVICE头为空!");
				fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-SERVICE_EMPTY", new String("X-ALONG-SERVICE头为空!".getBytes(),"utf-8"));
				return null;
			}
			
			StringBuilder postUrl = new StringBuilder(API_GATE_ADDR.get() + request.getRequestURI());
			
			if(StringUtils.isNotEmpty(request.getQueryString())){
				postUrl.append("?" + request.getQueryString());
			}

			try {
				ctx.setServiceName(serviceName);
				ctx.setRouteUrl(new URL(postUrl.toString()));
			} catch (Exception e) {
				Cat.logError("地址：" + postUrl + "不合法", e);
			}

			ctx.setRouteName("soa."+domain+"."+serviceName);
			ctx.setRouteGroup("soa."+domain);

			if (Debug.debugRequest()) {
				Debug.addRequestDebug("SOA::> domain: ${domain}");
				Debug.addRequestDebug("SOA::> serviceName: ${serviceName}");
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

}