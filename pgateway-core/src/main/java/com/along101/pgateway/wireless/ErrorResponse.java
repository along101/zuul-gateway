package com.along101.pgateway.wireless;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.GateHeaders;
import com.along101.pgateway.context.RequestContext;
import  com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.monitor.MetricReporter;

/**
 * Generate a error response while there is an error.
 */
public class ErrorResponse extends GateFilter {
    private static final Logger logger = LoggerFactory.getLogger(ErrorResponse.class);

    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public  int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        return context.getThrowable() != null && !context.errorHandled();
    }


    @SuppressWarnings("finally")
	@Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        Throwable ex = ctx.getThrowable();
        try {
            String errorCause="Gate-Error-Unknown-Cause";
            int responseStatusCode;

            if (ex instanceof GateException) {
            	GateException exception=(GateException)ex;
                String cause = exception.errorCause;
                if(cause!=null) errorCause = cause;
                responseStatusCode = exception.nStatusCode;
                
				Enumeration<String> headerIt = ctx.getRequest().getHeaderNames();
				StringBuilder sb = new StringBuilder(ctx.getRequest().getRequestURI()+":"+errorCause);
				while (headerIt.hasMoreElements()) {
					String name = (String) headerIt.nextElement();
					String value = ctx.getRequest().getHeader(name);
					sb.append("REQUEST:: > " + name + ":" + value+"\n");
				}
				logger.error(sb.toString());
            }else{
                responseStatusCode = 500;
            }

            ctx.getResponse().addHeader(GateHeaders.X_GATE_ERROR_CAUSE, errorCause);

            if (responseStatusCode == 404) {
				MetricReporter.statRouteErrorStatus("ROUTE_NOT_FOUND", errorCause);
            } else {
                MetricReporter.statRouteErrorStatus(ctx.getRouteName(), errorCause);
            }
            
			Enumeration<String> headerIt = ctx.getRequest().getHeaderNames();
			StringBuilder sb = new StringBuilder();
			while (headerIt.hasMoreElements()) {
				String name = (String) headerIt.nextElement();
				String value = ctx.getRequest().getHeader(name);
				sb.append("REQUEST:: > " + name + ":" + value+"\n");
			}
			logger.error(sb.toString());

            if (getOverrideStatusCode()) {
                ctx.setResponseStatusCode(200);
            } else {
                ctx.setResponseStatusCode(responseStatusCode);
            }

            ctx.setSendGateResponse(false);
			ctx.setResponseBody("Message\":\""+errorCause+"\"}");
        } finally {
            ctx.setErrorHandled(true); //ErrorResponse was handled
            return null;
        }
    }
    
   private boolean getOverrideStatusCode() {
        String override = RequestContext.getCurrentContext().getRequest().getParameter("override_error_status");
        if (getCallback() != null) return true;
        if (override == null) return false;
        return Boolean.valueOf(override);

    }

   private String getCallback() {
        String callback = RequestContext.getCurrentContext().getRequest().getParameter("callback");
        if (callback == null) return null;
        return callback;
    }

   private String getOutputType() {
        String output = RequestContext.getCurrentContext().getRequest().getParameter("output");
        if (output == null) return "json";
        return output;
    }  

}