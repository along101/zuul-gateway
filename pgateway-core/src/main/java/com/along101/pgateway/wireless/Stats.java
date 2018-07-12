package com.along101.pgateway.wireless;

import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.monitor.StatManager;


public class Stats extends GateFilter {
    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 20000;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        int status = ctx.getResponseStatusCode();
        StatManager sm = StatManager.getManager();
        sm.collectRequestStats(ctx.getRequest());
        sm.collectRouteStatusStats(ctx.getRouteName(), status);
        return null;
    }
}
