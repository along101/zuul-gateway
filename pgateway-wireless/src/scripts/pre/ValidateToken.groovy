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

public class ValidateToken extends GateFilter {

	private static Logger logger = LoggerFactory.getLogger(ValidateToken.class);

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

		Transaction tran = Cat.getProducer().newTransaction("ValidateTokenFilter", ctx.getRequest().getRequestURL().toString());
		try {


			Gateway gateway = (Gateway) ctx.get(Constants.GateWayData);

			if (gateway == null) {
				//throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				fomatResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-UNKNOW", new String("已加载的服务信息丢失".getBytes(),"utf-8"));
				Cat.logEvent("token","token service empty.");
				return;
			}

			if (gateway.getValidateToken()) {
				Enumeration<String> headers = ctx.getRequest().getHeaders("X-ALONG-TOKEN");
				if (!headers.hasMoreElements()) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("访问令牌不存在".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("访问令牌不存在".getBytes(),"utf-8"));
					Cat.logEvent("token","token is not existed.");
					return;
				}

				String token = StringUtils.trim(headers.nextElement());
				if (StringUtils.isEmpty(token)) {
					//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("访问令牌内容为空".getBytes(),"utf-8"));
					fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-NOHEADER", new String("访问令牌内容为空".getBytes(),"utf-8"));
					Cat.logEvent("token", "token is empty.");
					return;
				}

				String deviceId = "";
				headers = ctx.getRequest().getHeaders("X-ALONG-DEVICEID");
				if (headers.hasMoreElements()) {
					deviceId = StringUtils.trim(headers.nextElement());
				}

				TokenRequest tokenRequest = new TokenRequest(token, deviceId);
				try {
					TokenResult result = validateToken(tokenRequest);
					if (result.getResult() != 0) {
						//throwException(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTOKEN",new String(String.format("令牌校验失败：‘%s’", result.getResultMessage()).getBytes(),"utf-8"));
						fomatResponse(HttpServletResponse.SC_BAD_REQUEST, "GTW-BRQ-INVALIDTOKEN",new String(String.format("令牌校验失败：‘%s’", result.getResultMessage()).getBytes(),"utf-8"));
						Cat.logEvent("token", "token validate fail."+result.getResultMessage());
						return;
					}
					ctx.addGateRequestHeader("X-ALONG-USER", String.valueOf(result.getUserId()));
					Cat.logEvent("X-ALONG-USER", String.valueOf(result.getUserId()));
				} catch (GateException ex) {
					// 注意此段代码不能去掉
					throw ex;
				} catch (Exception ex) {
					logger.error(String.format("令牌服务响应异常！token=%s, deviceID=%s, error=%s", token, deviceId, ex.getMessage()),ex);
					Cat.logError(String.format("令牌服务响应异常！token=%s, deviceID=%s, error=%s", token, deviceId, ex.getMessage()), ex)
					throwException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GTW-SRV-TOKENSERVICEERROR",new String("令牌服务响应异常".getBytes(),"utf-8"));
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

	private TokenResult validateToken(TokenRequest request) throws Exception {
		//String tokeEsbValidate = "along101.AuthService.Contract.ITokenValidateService";
		String tokeEsbValidate = "along101.AuthToken.Contract.ITokenService";
		Contract contract = GatewayService.instance().findContract(tokeEsbValidate);
		if (contract == null) {
			throw new Exception("Could not found url for contract: " + tokeEsbValidate);
		}

		String url = contract.getUrl();
		if (url == null || url.trim().isEmpty()) {
			throw new Exception("Could not found url for contract: " + tokeEsbValidate);
		}

		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		//url = url + "/Validate";
		url = url + "/ValidateToken";
		Transaction tran = Cat.newTransaction("CallValidateToken", url);
		try {
//			CatContext context = new CatContext();
			Cat.logEvent("Call.server", new URI(url).getHost());
			Cat.logEvent("Call.app", tokeEsbValidate);

			Map<String, String> headers = new HashMap<String, String>();
//			headers.put(Constants.CAT_ROOT_MESSAGE_ID, context.getProperty(Cat.Context.ROOT));
//			headers.put(Constants.CAT_PARENT_MESSAGE_ID, context.getProperty(Cat.Context.PARENT));
//			headers.put(Constants.CAT_CHILD_MESSAGE_ID, context.getProperty(Cat.Context.CHILD));
			headers.put(Constants.CAT_ALONG_APP, Cat.getManager().getDomain());

			TokenResult result = GatewayService.instance().postData(url, request, headers, TokenResult.class);
			
			tran.setStatus(Transaction.SUCCESS);
			return result;
		} catch (Throwable t) {
			tran.setStatus(t);
			throw t;
		} finally {
			tran.complete();
		}
	}

}
