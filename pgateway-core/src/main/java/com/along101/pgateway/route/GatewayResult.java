package com.along101.pgateway.route;

public class GatewayResult {
	private int result;
	private String resultMessage;
	private Gateway[] services;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getResultMessage() {
		return resultMessage;
	}

	public void setResultMessage(String resultMessage) {
		this.resultMessage = resultMessage;
	}

	public Gateway[] getServices() {
		return services;
	}

	public void setServices(Gateway[] services) {
		this.services = services;
	}
}