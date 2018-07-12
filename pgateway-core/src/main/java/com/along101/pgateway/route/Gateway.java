package com.along101.pgateway.route;

public class Gateway {

	private String area;
	private String contract;
	private String service;
	private String[] tags;
	private String url;
	private boolean validateToken;

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getContract() {
		return contract;
	}

	public void setContract(String contract) {
		this.contract = contract;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean getValidateToken() {
		return validateToken;
	}

	public void setValidateToken(boolean validateToken) {
		this.validateToken = validateToken;
	}

}
