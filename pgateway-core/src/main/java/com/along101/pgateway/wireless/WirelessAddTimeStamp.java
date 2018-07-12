package com.along101.pgateway.wireless;

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class WirelessAddTimeStamp extends GateFilter {

	private final static int TIMESTAMP_EXPIRED_SECS = 600;

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public boolean shouldFilter() {		
		return true;
	}
	@Override
	public int filterOrder() {
		return 30;

	}
	@Override
	public Object run() throws GateException {
		RequestContext.getCurrentContext().addGateResponseHeader("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis()/1000));
		return true;
	}
}
