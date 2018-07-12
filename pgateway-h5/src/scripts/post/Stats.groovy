package scripts.post

import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.monitor.StatManager


class Stats extends GateFilter {
    @Override
    String filterType() {
        return "post"
    }

    @Override
    int filterOrder() {
        return 20000
    }

    @Override
    boolean shouldFilter() {
        return true
    }

    @Override
    Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        int status = ctx.getResponseStatusCode();
        StatManager sm = StatManager.manager
        sm.collectRequestStats(ctx.getRequest());
        sm.collectRouteStatusStats(ctx.routeName, status);
    }

}
