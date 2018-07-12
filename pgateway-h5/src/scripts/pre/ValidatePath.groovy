package scripts.pre

import java.nio.charset.Charset

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.InputStreamEntity
import org.apache.http.entity.StringEntity
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.along101.pgateway.common.CatContext;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.route.Contract
import com.along101.pgateway.route.Gateway
import com.along101.pgateway.route.GatewayService
import com.along101.pgateway.route.TokenRequest
import com.along101.pgateway.route.TokenResult

public class ValidatePath extends GateFilter {

	private static Logger logger = LoggerFactory.getLogger(ValidatePath.class);

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
		return 40;
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

		Transaction tran = Cat.getProducer().newTransaction("ValidatePathFilter", ctx.getRequest().getRequestURL().toString());
		try {


			Gateway gateway = (Gateway) ctx.get(Constants.GateWayData);

			if (gateway == null) {
				//throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				return;
			}

			if ( !validatePath("soa."+gateway.getArea()+"."+gateway.getService())) {
				//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTOKEN",new String(String.format("令牌校验失败：‘%s’", result.getResultMessage()).getBytes(),"utf-8"));
				fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTOKEN",new String(String.format("服务没有开放：‘%s’", "soa."+gateway.getArea()+"."+gateway.getService()).getBytes(),"utf-8"));
				return;
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

	private boolean validatePath(String path) throws Exception {
		return DynamicPropertyFactory.getInstance().getBooleanProperty(path.toLowerCase(),false).get();
	}
}
