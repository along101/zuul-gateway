package com.along101.pgateway.route;
public class ContractRequest {
	
	
	private int application;

	public ContractRequest(int application){
		this.application = application;
	}
	
	public int getApplication() {
		return application;
	}

	public void setApplication(int application) {
		this.application = application;
	}
}