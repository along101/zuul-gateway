package com.along101.pgateway.wireless;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;

public class DebugModeSetter extends GateFilter {

    static final DynamicBooleanProperty couldSetDebug =
            DynamicPropertyFactory.getInstance().getBooleanProperty("gate.could.set.debug", true);
    static final DynamicBooleanProperty routingDebug =
            DynamicPropertyFactory.getInstance().getBooleanProperty(Constants.GateDebugRequest, true);
    static final DynamicStringProperty debugParameter =
            DynamicPropertyFactory.getInstance().getStringProperty(Constants.GateDebugParameter, "debugRequest");

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return -100;
    }

    public boolean shouldFilter() {
        if (!couldSetDebug.get()) {
            return false;
        }
        System.out.println(RequestContext.getCurrentContext().getRequest().getParameter(debugParameter.get()));
        if ("true".equals(RequestContext.getCurrentContext().getRequest().getParameter(debugParameter.get()))) {
        	
        	return true;
        }
        return routingDebug.get();
    }

    public Object run() {
        RequestContext.getCurrentContext().setDebugRequest(true);
        RequestContext.getCurrentContext().setDebugRouting(true);
        return null;
    }
}



