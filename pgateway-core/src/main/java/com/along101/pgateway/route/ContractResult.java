package com.along101.pgateway.route;

public class ContractResult {
	/// 查询结果
	private int result;
	
	/// 查询结果描述
	private String resultMessage;

	/// 服务列表
	private Contract[] services;

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

	public Contract[] getServices() {
		return services;
	}

	public void setServices(Contract[] services) {
		this.services = services;
	}

}
