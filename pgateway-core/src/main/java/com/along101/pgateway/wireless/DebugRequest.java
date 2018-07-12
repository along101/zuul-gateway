package com.along101.pgateway.wireless;


import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.util.Debug;


public class DebugRequest extends GateFilter {
    private static final String GATE_BODY_DEBUG_DISABLE = "gate.body.debug.disable";
    private static final String GATE_HEADER_DEBUG_DISABLE = "gate.header.debug.disable";
    static final DynamicBooleanProperty BODY_DEBUG_DISABLED =
            DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_BODY_DEBUG_DISABLE, false);
    static final DynamicBooleanProperty HEADER_DEBUG_DISABLED =
            DynamicPropertyFactory.getInstance().getBooleanProperty(GATE_HEADER_DEBUG_DISABLE, true);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
    	return -10;
    }

    @Override
    public boolean shouldFilter() {
    	return Debug.debugRequest();
    }

    @Override
    public Object run() {
        HttpServletRequest req = RequestContext.getCurrentContext().getRequest();

        Debug.addRequestDebug("REQUEST:: " + req.getScheme() + " " + req.getRemoteAddr() + ":" + req.getRemotePort());
        Debug.addRequestDebug("REQUEST:: > " + req.getMethod() + " " + req.getRequestURI() + " " + req.getProtocol());

        Enumeration<String> headerIt = req.getHeaderNames();
        while (headerIt.hasMoreElements()) {
            String name = (String) headerIt.nextElement();
            String value = req.getHeader(name);
            Debug.addRequestDebug("REQUEST:: > " + name + ":" + value);
        }

//        final RequestContext ctx = RequestContext.getCurrentContext();
//        if (!ctx.isChunkedRequestBody() && !BODY_DEBUG_DISABLED.get()) {
//            InputStream inp = ctx.getRequest().getInputStream();
//            String body = null;
//            if (inp != null) {
//                body = inp.getText();
//                Debug.addRequestDebug("REQUEST:: > " + body);
//            }
//        }
        return null;
    }

   
}
