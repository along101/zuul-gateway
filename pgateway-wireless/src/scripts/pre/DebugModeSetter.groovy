package scripts.pre


import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.DynamicStringProperty
import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter


class DebugModeSetter extends GateFilter {

    static final DynamicBooleanProperty couldSetDebug =
            DynamicPropertyFactory.getInstance().getBooleanProperty("gate.could.set.debug", false);
    static final DynamicBooleanProperty routingDebug =
            DynamicPropertyFactory.getInstance().getBooleanProperty("gate.debug.request", false);
    static final DynamicStringProperty debugParameter =
            DynamicPropertyFactory.getInstance().getStringProperty("gate.debug.parameter", "debugRequest");

    @Override
    String filterType() {
        return 'pre'
    }

    @Override
    int filterOrder() {
        return -100;
    }

    boolean shouldFilter() {
        if (!couldSetDebug.get()) {
            return false
        }
        if ("true".equals(RequestContext.currentContext.getRequest().getParameter(debugParameter.get()))) return true;
        return routingDebug.get();
    }

    Object run() {
        RequestContext.getCurrentContext().setDebugRequest(true)
        RequestContext.getCurrentContext().setDebugRouting(true)
        return null;
    }
}



