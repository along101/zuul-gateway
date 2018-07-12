package com.along101.pgateway.wireless;

import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class WirelessValidateTimeStamp extends GateFilter {

	private final static int TIMESTAMP_EXPIRED_SECS = 600;

	private static Logger logger = LoggerFactory.getLogger(WirelessValidateTimeStamp.class);
	
	private static final String GATE_BODY_DEBUG_DISABLE = "gate.body.debug.disable";
	private static final String GATE_HEADER_DEBUG_DISABLE = "gate.header.debug.disable";
	private static final DynamicBooleanProperty BODY_DEBUG_DISABLED =
	DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_BODY_DEBUG_DISABLE, false);
	private static final DynamicBooleanProperty HEADER_DEBUG_DISABLED =
	DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_HEADER_DEBUG_DISABLE, false);
	
	@Override
	public String filterType() {
		return "pre";
	}

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

	public int filterOrder() {
		return 30;

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
		RequestContext ctx = RequestContext.getCurrentContext();

		Enumeration<String> headers = ctx.getRequest().getHeaders("X-ALONG-TIMESTAMP");
		if (!headers.hasMoreElements()) {
			//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", "时间戳不存在");
			fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", "时间戳不存在");			
			return null;
		}

		String timestampStr = headers.nextElement();
		if (timestampStr.trim().isEmpty()) {
			//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳内容为空");
			fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳内容为空");
			return null;
		}

		long timestamp = 0;
		try {
			timestamp = Long.parseLong(timestampStr);
		} catch (Exception ex) {
			//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳格式不正确");
			fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳格式不正确");
			return null;
		}

		long expiredTime = TIMESTAMP_EXPIRED_SECS + timestamp;

		long now = System.currentTimeMillis() / 1000;
		if (now > expiredTime) {
			//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳已失效");
			fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTIMESTAMP", "时间戳已失效");
			return null;
		}

		return true;
	}
}
