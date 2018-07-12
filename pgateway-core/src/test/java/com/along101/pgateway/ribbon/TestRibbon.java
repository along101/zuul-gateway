package com.along101.pgateway.ribbon;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.along101.pgateway.common.Constants;

public class TestRibbon {

	public static void main(String[] args) {

//		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
//		
//		config.setProperty(CommonClientConfigKey.NIWSServerListClassName, "com.netflix.loadbalancer.DiscoveryEnabledNIWSServerList");
//		config.setProperty(CommonClientConfigKey.NFLoadBalancerRuleClassName, "com.netflix.loadbalancer.RetryRule");
//		
//		config.setClientName("ARCHDEMOSERVICE");
//		
//		
//		RestClient client = new RestClient();
//		client.initWithNiwsConfig(config);
//		
//		//client.execute(task)
		
		
		System.setProperty(Constants.DeployConfigUrl, "http://localhost:8080/configs/apollo/10021");
		String applicationID = ConfigurationManager.getConfigInstance().getString(Constants.DeploymentApplicationID);
		
		
		HystrixCommandProperties props = new HystrixCommandProperties(HystrixCommandKey.Factory.asKey("soa.ac.auth.validate")){
			
		};
		
		System.out.println(props.executionIsolationThreadTimeoutInMilliseconds().get());
		
		System.out.println("fsafasfasd");
		
		
	}

}
