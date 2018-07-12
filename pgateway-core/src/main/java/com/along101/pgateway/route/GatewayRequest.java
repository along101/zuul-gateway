package com.along101.pgateway.route;

public class GatewayRequest {
	private String gateway;
	
	public GatewayRequest(String gateway) {
		this.gateway = gateway;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}
}