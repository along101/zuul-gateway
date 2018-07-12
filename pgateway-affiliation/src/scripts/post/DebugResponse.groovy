package scripts.post

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.util.Pair
import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.util.Debug


class DebugResponse extends GateFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugResponse.class);
	
	private static final String GATE_BODY_DEBUG_DISABLE = "gate.body.debug.disable";
	private static final String GATE_HEADER_DEBUG_DISABLE = "gate.header.debug.disable";
	static final DynamicBooleanProperty BODY_DEBUG_DISABLED =
	DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_BODY_DEBUG_DISABLE, false);
	static final DynamicBooleanProperty HEADER_DEBUG_DISABLED =
	DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_HEADER_DEBUG_DISABLE, true);

    @Override
    String filterType() {
        return 'post'
    }

    @Override
    int filterOrder() {
        return 1000
    }

    @Override
    boolean shouldFilter() {
        return Debug.debugRequest();
    }

    @Override
    Object run() {
        RequestContext.getCurrentContext().getGateResponseHeaders()?.each { Pair<String, String> it ->
            Debug.addRequestDebug("OUTBOUND: <  " + it.first() + ":" + it.second())
        }
        dumpRoutingDebug()
        dumpRequestDebug()
        return null;
    }

    public void dumpRequestDebug() {
        List<String> rd = (List<String>) RequestContext.getCurrentContext().get("requestDebug");
        StringBuilder b = new StringBuilder("");
        rd?.each {
            b.append("REQUEST_DEBUG::${it}\n");
        }
        LOGGER.info(b.toString());
    }

    public void dumpRoutingDebug() {
        List<String> rd = (List<String>) RequestContext.getCurrentContext().get("routingDebug");
        StringBuilder b = new StringBuilder("");
        rd?.each {
            b.append("GATE_DEBUG::${it}\n");
        }
        LOGGER.info(b.toString());
    }
}
