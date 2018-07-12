package com.along101.pgateway.route;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.management.RuntimeErrorException;

import com.along101.pgateway.common.CatContext;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.hystrix.GateRequestCommandForSemaphoreIsolation;
import com.along101.pgateway.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.util.SleepUtil;

import groovy.util.logging.Slf4j;

@Slf4j
public class GatewayService {

	private DynamicBooleanProperty pollerEnabled = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.GateRoutePollerEnabled, true);
	private DynamicLongProperty pollerInterval = DynamicPropertyFactory.getInstance()
			.getLongProperty(Constants.GateRoutePollerInterval, 200);
	private DynamicStringProperty pollerUrl = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateRoutePollerUrl, "http://%s:%s/Esb/EsbService/QueryApplicationServices");
	private final DynamicIntProperty gateEsbApplication = DynamicPropertyFactory.getInstance()
			.getIntProperty(Constants.GateRouteEsbApplication, 10010001);
	private final DynamicStringProperty gateEsbTags = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateRouteEsbTags, "null");
	private final DynamicStringProperty gateEsbName = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.GateRouteEsbGateName, "WirelessGateway");
	
	private static final DynamicIntProperty MAX_CONNECTIONS = DynamicPropertyFactory.getInstance()
			.getIntProperty("token.client.max.connections", 100);
	private static final DynamicIntProperty MAX_CONNECTIONS_PER_ROUTE = DynamicPropertyFactory.getInstance()
			.getIntProperty("token.client.route.max.connections", 50);

	protected volatile boolean running = true;

	private final Timer managerTimer = new Timer();

	private AtomicReference<Map<String, List<Contract>>> contractMapRef = new AtomicReference<Map<String, List<Contract>>>();
	private AtomicReference<Map<String, List<Gateway>>> gatewayMapRef = new AtomicReference<Map<String, List<Gateway>>>(
			new HashMap<String, List<Gateway>>());
	private final AtomicReference<CloseableHttpClient> clientRef = new AtomicReference<CloseableHttpClient>(
			newClient());

	private static GatewayService instance;

	private GatewayService() {

		Thread poller = new Thread(
				this.getClass().getSimpleName() + (new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()))) {
			public void run() {
				while (running) {

					try {
						if (!pollerEnabled.get())
							continue;
						doPoller();
					} catch (Throwable t) {
						Cat.logError("GateWay Poller Encounter an error.", t);
					} finally {
						SleepUtil.sleep(pollerInterval.get());
					}
				}
			}
		};
		poller.start();

		managerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					final CloseableHttpClient hc = clientRef.get();
					if (hc == null)
						return;
					hc.getConnectionManager().closeExpiredConnections();
				} catch (Throwable t) {
					Cat.logError("error closing expired connections", t);
				}
			}
		}, 30000, 5000);

		Runnable loader = new Runnable() {
			@Override
			public void run() {
				loadClient();
			}
		};

		pollerInterval.addCallback(loader);

		MAX_CONNECTIONS.addCallback(loader);
		MAX_CONNECTIONS_PER_ROUTE.addCallback(loader);
	}

	public static GatewayService instance() {
		if (instance == null) {
			synchronized (GatewayService.class) {
				if (instance == null) {
					instance = new GatewayService();
				}
			}
		}

		return instance;
	}

	@PostConstruct
	public void close() {
		this.running = false;
	}

	private void doPoller() {
		loadContract();
		loadGateService();
	}

	private void loadGateService() {
		String esbservice = "along101.Esb.Contract.IEsbGatewayService";
		Contract serviceData = findContract(esbservice);
		if (serviceData == null) {
			throw new RuntimeErrorException(null, "服务契约" + esbservice + "在注册中心中不存在或无权访问！");
		}
		if (StringUtils.isEmpty(serviceData.getUrl())) {
			throw new RuntimeErrorException(null, "服务'" + esbservice + "'在注册中心中调用地址为空！");
		}
		String url = StringUtil.trimEnd(serviceData.getUrl(), '/');
		String postUrl = String.format("%s/QueryGatewayServices", url);
		CatContext context = new CatContext();

		Transaction tran = Cat.newTransaction("ESBCall", postUrl);
		try {
			Cat.logRemoteCallClient(context);
			Cat.logEvent("Call.server", new URI(postUrl).getHost());
			Cat.logEvent("Call.app", esbservice);
			Map<String, String> headers = new HashMap<String, String>();
			headers.put(Constants.CAT_ALONG_APP, Cat.getManager().getDomain());

			GatewayRequest gatewayRequest = new GatewayRequest(gateEsbName.get());

			GatewayResult gatewayResult = postData(postUrl, gatewayRequest, headers,
					GatewayResult.class);

			if (gatewayResult.getResult() != 0) {
				throw new RuntimeErrorException(null, String.format("查询网关服务异常，异常代码:%s，异常消息:%s",
						gatewayResult.getResult(), gatewayResult.getResultMessage()));
			}
			if (gatewayResult.getServices() == null) {
				gatewayResult.setServices(new Gateway[0]);
			}

			HashMap<String, List<Gateway>> map = new HashMap<String, List<Gateway>>();
			for (Gateway gateway : gatewayResult.getServices()) {
				String key = String
						.format("%s-%s", StringUtils.trim(gateway.getArea()), StringUtils.trim(gateway.getService()))
						.toLowerCase();
				if (!map.containsKey(key)) {
					map.put(key, new ArrayList<Gateway>());
				}
				map.get(key).add(gateway);
			}
			gatewayMapRef.set(map);

			tran.setStatus(Transaction.SUCCESS);
		} catch (Throwable t) {
			tran.setStatus(t);
			Cat.logError(t);
		} finally {
			tran.complete();
		}
	}

	private void loadContract() {
		Transaction tran = null;

		try {
			tran = Cat.newTransaction("ESBCall", pollerUrl.get());
			Cat.Context context = new CatContext();
			Cat.logRemoteCallClient(context);
			String call = new URI(pollerUrl.get()).getHost();
			Cat.logEvent("Call.server", call);
			Cat.logEvent("Call.app", call);

			Map<String, String> headers = new HashMap<String, String>();
			headers.put(Constants.CAT_ALONG_APP, Cat.getManager().getDomain());

			ContractResult contractResult = postData(pollerUrl.get(),new ContractRequest(gateEsbApplication.get()), headers, ContractResult.class);

			if (contractResult.getResult() != 0) {
				Cat.logEvent("ServicePoller", "loadContract", "Error", String.format("查询应用服务异常，异常代码:%s，异常消息:%s",
						contractResult.getResult(), contractResult.getResultMessage()));
			}

			if (contractResult.getServices() == null) {
				contractResult.setServices(new Contract[0]);
			}
			Map<String, List<Contract>> map = new HashMap<String, List<Contract>>();
			for (Contract contract : contractResult.getServices()) {
				String key = StringUtils.trim(contract.getContract());
				if (StringUtils.isNotEmpty(key)) {
					key = key.toLowerCase();
				}
				if (!map.containsKey(key)) {
					map.put(key, new ArrayList<Contract>());
				}
				map.get(key).add(contract);
			}
			contractMapRef.set(map);

			tran.setStatus(Transaction.SUCCESS);
		} catch (Throwable t) {
			tran.setStatus(t);
			Cat.logError(t);
		} finally {
			tran.complete();
		}
	}

	public Gateway findGateway(String area, String service) {
		Map<String, List<Gateway>> map = gatewayMapRef.get();
		if (map == null) {
			return null;
		}
		String key = String.format("%s-%s", StringUtils.trim(area), StringUtils.trim(service)).toLowerCase();
		if (map.containsKey(key)) {
			List<Gateway> temp = map.get(key);
			for (Gateway gateway : temp) {
				if (gateway.getTags() != null && StringUtil.contains(gateway.getTags(), gateEsbTags.get().split(","))) {
					return gateway;
				}
			}
		}
		return null;
	}

	public Contract findContract(String contractName) {

		if (StringUtils.isNotEmpty(contractName)) {
			contractName = contractName.toLowerCase();
		}

		Map<String, List<Contract>> map = contractMapRef.get();
		if (map == null) {
			return null;
		}
		if (map.containsKey(contractName)) {
			List<Contract> temp = map.get(contractName);
			for (Contract serviceData : temp) {
				if (serviceData.getTags() != null
						&& StringUtil.contains(serviceData.getTags(), gateEsbTags.get().split(","))) {
					return serviceData;
				}
			}
		}
		return null;
	}

//	private ContractResult postData(Object data, Map<String, String> header)
//			throws ClientProtocolException, IOException {
//		HttpPost method = new HttpPost(pollerUrl.get());
//		method.setHeader("Content-type", "application/json; charset=utf-8");
//		method.setHeader("Accept", "application/json");
//		if (header != null) {
//			for (String key : header.keySet()) {
//				method.setHeader(key, header.get(key));
//			}
//		}
//		if (data != null) {
//			String parameters = JSON.toJSONString(data, SerializerFeature.DisableCircularReferenceDetect);
//			HttpEntity entity = new StringEntity(parameters, Charset.forName("UTF-8"));
//			method.setEntity(entity);
//		}
//		CloseableHttpClient httpClient = clientRef.get();
//		if (httpClient == null)
//			return null;
//		CloseableHttpResponse response = httpClient.execute(method);
//		String body = EntityUtils.toString(response.getEntity());
//		return JSON.parseObject(body, ContractResult.class);
//	}

	private void loadClient() {
		final CloseableHttpClient oldClient = clientRef.get();
		clientRef.set(newClient());
		if (oldClient != null) {
			managerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						oldClient.close();
					} catch (Throwable t) {
						Cat.logError("GatewayService error shutting down old connection manager", t);
					}
				}
			}, 30000);
		}

	}

	private HttpClientConnectionManager newConnectionManager() {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(MAX_CONNECTIONS.get());
		cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE.get());
		return cm;
	}

	private CloseableHttpClient newClient() {
		RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(0, false);
		RedirectStrategy redirectStrategy = new RedirectStrategy() {
			@Override
			public boolean isRedirected(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
				return false;
			}

			@Override
			public HttpUriRequest getRedirect(HttpRequest httpRequest, HttpResponse httpResponse,
					HttpContext httpContext) {
				return null;
			}
		};
		CloseableHttpClient httpclient = HttpClients.custom().disableContentCompression()
				.setConnectionManager(newConnectionManager()).setDefaultRequestConfig(config)
				.setRetryHandler(retryHandler).setRedirectStrategy(redirectStrategy).disableCookieManagement().build();
		return httpclient;
	}

	public <T> T postData(String url, Object par, Map<String, String> header, Class<T> clazz)
			throws ClientProtocolException, IOException {
		// return HttpUtil.postData(clientRef.get(), url, par, header, clazz);

		HttpUriRequest httpUriRequest = new HttpPost(url);

		httpUriRequest.setHeader("Content-type", "application/json; charset=utf-8");
		httpUriRequest.setHeader("Accept", "application/json");
		if (header != null) {
			for (String key : header.keySet()) {
				httpUriRequest.setHeader(key, header.get(key));
			}
		}

		RequestConfig requestConfig = buildRequestConfig("soa.validateToken", "soa.validateToken");

		((HttpPost) httpUriRequest).setConfig(requestConfig);

		if (par != null) {
			String parameters = JSON.toJSONString(par, SerializerFeature.DisableCircularReferenceDetect);
			HttpEntity entity = new StringEntity(parameters, Charset.forName("UTF-8"));
			((HttpPost) httpUriRequest).setEntity(entity);
		}

		HttpResponse response = new GateRequestCommandForSemaphoreIsolation(clientRef.get(), httpUriRequest,"soa.validateToken", "soa.validateToken").execute();

		String body = EntityUtils.toString(response.getEntity());
		return JSON.parseObject(body, clazz);
	}

	private RequestConfig buildRequestConfig(String routName, String groupName) {

		RequestConfig.Builder builder = RequestConfig.custom();

		int connectTimeout = DynamicPropertyFactory.getInstance().getIntProperty(routName + ".connect.timeout", 0)
				.get();
		if (connectTimeout == 0) {
			connectTimeout = DynamicPropertyFactory.getInstance().getIntProperty(groupName + ".connect.timeout", 0)
					.get();
		}
		if (connectTimeout == 0) {
			connectTimeout = DynamicPropertyFactory.getInstance().getIntProperty("gate.connect.timeout.global", 2000)
					.get();
		}
		builder.setConnectTimeout(connectTimeout);

		int socketTimeout = DynamicPropertyFactory.getInstance().getIntProperty(routName + ".socket.timeout", 0).get();
		if (socketTimeout == 0) {
			socketTimeout = DynamicPropertyFactory.getInstance().getIntProperty(groupName + ".socket.timeout", 0).get();
		}
		if (socketTimeout == 0) {
			socketTimeout = DynamicPropertyFactory.getInstance().getIntProperty("gate.socket.timeout.global", 10000)
					.get();
		}
		builder.setSocketTimeout(socketTimeout);

		int requestConnectionTimeout = DynamicPropertyFactory.getInstance()
				.getIntProperty(routName + ".request.connection.timeout", 0).get();
		if (requestConnectionTimeout == 0) {
			requestConnectionTimeout = DynamicPropertyFactory.getInstance()
					.getIntProperty(groupName + ".request.connection.timeout", 0).get();
		}
		if (requestConnectionTimeout == 0) {
			requestConnectionTimeout = DynamicPropertyFactory.getInstance()
					.getIntProperty("gate.request.connection.timeout.global", 10).get();
		}
		builder.setConnectionRequestTimeout(requestConnectionTimeout);

		return builder.build();
	}

	public String printJson() {
		Map<String, List<Contract>> mapContract = contractMapRef.get();
		Map<String, List<Gateway>> mapGateway = gatewayMapRef.get();
		String rs = "the contract json is: ";
		rs += System.getProperty("line.separator", "\n");
		if (mapContract != null) {
			rs += JSON.toJSONString(mapContract);
		}
		rs += System.getProperty("line.separator", "\n");
		rs += System.getProperty("line.separator", "\n");
		rs += "the gateway json is: ";
		rs += System.getProperty("line.separator", "\n");
		if (mapGateway != null) {
			rs += JSON.toJSONString(mapGateway);
		}
		return rs;
	}
}
