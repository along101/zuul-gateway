package scripts.post

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class AddTimeStamp extends GateFilter {

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
		return 1;

	}
	@Override
	public Object run() throws GateException {
		long time = System.currentTimeMillis()/1000;
		RequestContext.getCurrentContext().addGateResponseHeader("X-ALONG-TIMESTAMP", String.valueOf(time));
		return true;
	}
}
