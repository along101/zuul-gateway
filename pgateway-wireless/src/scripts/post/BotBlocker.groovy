package scripts.post

import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import com.alibaba.fastjson.JSON
import com.dianping.cat.Cat
import com.dianping.cat.message.Transaction
import com.netflix.config.DynamicIntProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.DynamicStringProperty
import com.along101.logmetric.common.core.RingBuffer
import com.along101.pgateway.common.*
import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter

public class BotBlocker extends GateFilter {
	private static final DynamicIntProperty interval = DynamicPropertyFactory.getInstance().getIntProperty("gate.botblocker.send.interval", 50);
	private static final DynamicIntProperty fetchSize = DynamicPropertyFactory.getInstance().getIntProperty("gate.botblocker.send.fetchSize", 500);
	private static final DynamicStringProperty topic = DynamicPropertyFactory.getInstance().getStringProperty("gate.botblocker.send.topic", "BOT-BLOCKER");
	private static final DynamicStringProperty kafkaServer = DynamicPropertyFactory.getInstance().getStringProperty("gate.botblocker.kafka", "localhost:9092");
	
	private static final  RingBuffer<AccessLog> buffer = new RingBuffer<AccessLog>(4096);
	private static final Timer managerTimer = new Timer();
	
	private static final AtomicReference<Producer<String, String>> producerRef = new AtomicReference<Producer<String, String>>(createProducer());
	
	static {
		
		kafkaServer.addCallback(new Runnable() {
				@Override
				void run() {
					Producer<String, String> oldProducer = producerRef.get();
					producerRef.set(BotBlocker.createProducer());
					if(oldProducer != null){
						try{
							oldProducer.close()
						}catch(Throwable t){
							Cat.logError("kafka producer close error.", t);
						}
					}
				}
		});
		
		managerTimer.schedule(new TimerTask() {
			@Override
			void run() {
				try {
					List<AccessLog> list = buffer.takeForTimeout(fetchSize.get(), 50);
					
					if(!list.isEmpty()){
						
						for(AccessLog accessLog:list){
							Producer<String, String> producer = producerRef.get();
							if(producer != null){
								try{
									ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic.get(), JSON.toJSONString(accessLog));
									producer.send(record);
									Cat.logEvent("BotBlocker","success")
								}catch(Throwable t){
									Cat.logEvent("BotBlocker","error")
									Cat.logError("kafka producer close error.", t);
								}
							}
						}
			
					}
				} catch (Throwable t) {
					Cat.logError("kafka producer close error.", t);
				}
			}
		}, 300, interval.get())
	}
	
	private static Producer<String, String> createProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaServer.get());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "wirelessgate");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
		return new KafkaProducer<>(props);
	}
	
	@Override
	public String filterType() {
		return 'post';
	}

	@Override
	public int filterOrder() {
		return -200;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.currentContext;
		Transaction tran = Cat.getProducer().newTransaction("BotBlockerFilter", ctx.getRequest().getRequestURL().toString());

		try{
			tran.setStatus(Transaction.SUCCESS);
			AccessLog accesslog = new AccessLog();
			
			Enumeration<String> appId = ctx.getRequest().getHeaders("X-ALONG-APPID");
			
			if (appId.hasMoreElements()) {
				accesslog.setAppId(appId.nextElement());
			}
			
			String userid = ctx.getGateRequestHeaders().get("X-ALONG-USER");
			if(userid != null){
				accesslog.setUserId(Integer.valueOf(userid));				
			}
			Enumeration<String> clientIP = ctx.getRequest().getHeaders("X-Forwarded-For");
			
			if(clientIP.hasMoreElements()) {
				accesslog.setClientIp(StringUtils.substringBefore(clientIP.nextElement(), ","));
			}
						
			accesslog.setSource("wirelessgateway");
			accesslog.setAccessUrl(ctx.getRouteUrl().toString());
			
			int miss = buffer.add(accesslog)
			if(miss == 1){
				Cat.logEvent("BotBlocker","miss")
			}
		}catch(Throwable t){
			tran.setStatus(t);
		}finally{
			tran.complete();
		}
	}
	
	class AccessLog{
		private String source;
		private long timestamp;
		private String clientIp;
		private String appId;
		private int userId;
		private String accessUrl;
		
		public AccessLog(){
			this.timestamp = System.currentTimeMillis();
			this.userId = 0;
		}
	
		public String getSource() {
			return source;
		}
	
		public void setSource(String source) {
			this.source = source;
		}
	
		public long getTimestamp() {
			return timestamp;
		}
	
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	
		public String getClientIp() {
			return clientIp;
		}
	
		public void setClientIp(String clientIp) {
			this.clientIp = clientIp;
		}
	
		public String getAppId() {
			return appId;
		}
	
		public void setAppId(String appId) {
			this.appId = appId;
		}
	
		public int getUserId() {
			return userId;
		}
	
		public void setUserId(int userId) {
			this.userId = userId;
		}
	
		public String getAccessUrl() {
			return accessUrl;
		}
	
		public void setAccessUrl(String accessUrl) {
			this.accessUrl = accessUrl;
		}
	}
}