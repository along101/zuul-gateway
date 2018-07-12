package com.along101.pgateway.route;

public class Contract {	
	//契约
    private String contract;
    /// 服务地址
    private String url;

    /// TCC操作服务
    private boolean isTccService;

    /// 事务服务
    private boolean isTransactionService;

    /// 服务标签
    private String[] tags;
    public String getContract() {
		return contract;
	}

	public void setContract(String contract) {
		this.contract = contract;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isTccService() {
		return isTccService;
	}

	public void setTccService(boolean isTccService) {
		this.isTccService = isTccService;
	}

	public boolean isTransactionService() {
		return isTransactionService;
	}

	public void setTransactionService(boolean isTransactionService) {
		this.isTransactionService = isTransactionService;
	}

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}
    
}
