package scripts.pre

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.route.Gateway


public class ValidateSign extends GateFilter {

	private static Logger logger = LoggerFactory.getLogger(ValidateSign.class);
	
	private final static int TIMESTAMP_EXPIRED_SECS = 100000;

	private static final DynamicBooleanProperty HEADER_DEBUG_DISABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("gate.header.debug.disable", false);

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (!ctx.sendGateResponse())
			return false;
		if(ctx.getThrowable() != null)
			return false;
		if (ctx.getServiceName() != null)
			return false;
		return true;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 60;
	}
	
	private void throwException(int httpStatus, String code, String message) throws GateException {
		HttpServletResponse response = RequestContext.getCurrentContext().getResponse();

		response.setHeader(Constants.HTTP_ERROR_CODE_HEADER, code);
		response.setHeader(Constants.HTTP_ERROR_MESSAGE_HEADER, message);
		
		throw new GateException(code, httpStatus, message);
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
		
		Transaction tran = Cat.getProducer().newTransaction("ValidateSignFilter", ctx.getRequest().getRequestURL().toString());
		try {
			
			
			Gateway gateway = (Gateway) ctx.get(Constants.GateWayData);
			
			if (gateway == null) {
				//throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				return;
			}
			
			if (gateway.getValidateToken()) {
						
				HttpServletRequest request = ctx.getRequest();
				
				Enumeration<String> headers = ctx.getRequest().getHeaders("X-ALONG-APPID");
	
				if (!headers.hasMoreElements()) {
					fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-BRQ-NOHEADER", new String("缺少X-ALONG-APPID头信息".getBytes(),"utf-8"));
					return;
				}
				
				String appId = headers.nextElement();
				
				if (appId.trim().isEmpty()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDAPP", new String("X-ALONG-APPID头信息内容为空".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDAPP", new String("X-ALONG-APPID头信息内容为空".getBytes(),"utf-8"));
					return;
				}
				
				headers = ctx.getRequest().getHeaders("X-ALONG-TIMESTAMP");
				if (!headers.hasMoreElements()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("缺少X-ALONG-TIMESTAMP头信息".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("缺少X-ALONG-TIMESTAMP头信息".getBytes(),"utf-8"));
					return;
				}
	
				String timestampStr = headers.nextElement();
				if (timestampStr.trim().isEmpty()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("X-ALONG-TIMESTAMP头信息内容为空".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("X-ALONG-TIMESTAMP头信息内容为空".getBytes(),"utf-8"));
					return;
				}else{
					long timestamp = 0;
					try {
						timestamp = Long.parseLong(timestampStr);
						
						long expiredTime = TIMESTAMP_EXPIRED_SECS + timestamp;
						
						long now = System.currentTimeMillis() / 1000;
						if (now > expiredTime) {
							//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("时间戳已失效".getBytes(),"utf-8"));
							fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("时间戳已失效".getBytes(),"utf-8"));
							Cat.logEvent("token", new String("时间戳已失效".getBytes(),"utf-8"));
							return;
						}					
						
					} catch (Exception ex) {
						//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("时间戳格式不正确".getBytes(),"utf-8"));
						fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("时间戳格式不正确".getBytes(),"utf-8"));
						Cat.logEvent("token", new String("时间戳格式不正确".getBytes(),"utf-8"));
						return;
					}
				}
				
				String appSecret = DynamicPropertyFactory.getInstance().getStringProperty("security."+appId, null).get();
				
				if(appSecret == null){
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("没有申请appSecret".getBytes(),"utf-8"));
					Cat.logEvent("token", new String("没有申请appSecret".getBytes(),"utf-8"));
					return;
				}
				
				
				headers = ctx.getRequest().getHeaders("X-ALONG-SIGN");
				if (!headers.hasMoreElements()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("缺少X-ALONG-TIMESTAMP头信息".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("缺少X-ALONG-SIGN头信息".getBytes(),"utf-8"));
					Cat.logEvent("token",new String("缺少X-ALONG-SIGN头信息".getBytes(),"utf-8"));
					return;
				}
	
				String requestSign = headers.nextElement();
				if (requestSign.trim().isEmpty()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", new String("X-ALONG-TIMESTAMP头信息内容为空".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDSIGN", new String("X-ALONG-SIGN头信息内容为空".getBytes(),"utf-8"));
					Cat.logEvent("token", new String("X-ALONG-SIGN头信息内容为空".getBytes(),"utf-8"));
					return;
				}
				
				String queryStr = request.getQueryString();
	
	
				String checkSign = DigestUtils.md5Hex(appId + timestampStr + queryStr + appSecret);
	
				if (!requestSign.equals(checkSign)) {
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDSIGN", new String((appId + timestampStr + queryStr+"签名验证失败:"+checkSign).getBytes(),"utf-8"));
					Cat.logEvent("token",new String((appId + timestampStr + queryStr+"签名验证失败:"+checkSign).getBytes(),"utf-8"));
					return;
				}
			}

			tran.setStatus(Transaction.SUCCESS);
		} catch (Throwable e) {
			tran.setStatus(e);
			throw e;
		} finally {
			tran.complete();
		}
		return null;
	}

}
