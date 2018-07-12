package com.along101.pgateway.route;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class TokenRequest {
	private Integer acAppVersion;
//	private String deviceId;
//	private String token;
	
	private String DeviceID;
	private String TokenID;

	public TokenRequest(String token,String deviceId){
		this.TokenID = token;
		this.DeviceID = deviceId;
	}
	
	public Integer getAcAppVersion() {
		return acAppVersion;
	}

	public void setAcAppVersion(Integer acAppVersion) {
		this.acAppVersion = acAppVersion;
	}
	@JSONField(name="DeviceID")
	public String getDeviceID() {
		return DeviceID;
	}

	public void setDeviceID(String deviceID) {
		DeviceID = deviceID;
	}

	@JSONField(name="TokenID")
	public String getTokenID() {
		return TokenID;
	}

	public void setTokenID(String tokenID) {
		TokenID = tokenID;
	}
	
	@JSONField(name="deviceId")
	public String getDeviceId() {
		return DeviceID;
	}

	public void setDeviceId(String deviceId) {
		this.DeviceID = deviceId;
	}

	public String getToken() {
		return TokenID;
	}

	public void setToken(String token) {
		this.TokenID = token;
	}
	
}