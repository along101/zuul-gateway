package scripts.error

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.along101.pgateway.common.GateException
import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.monitor.MetricReporter

/**
 * Generate a error response while there is an error.
 */
class ErrorResponse extends GateFilter {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass())

    @Override
    String filterType() {
        return 'error'
    }

    @Override
    int filterOrder() {
        return 10
    }

    @Override
    boolean shouldFilter() {
        def context = RequestContext.getCurrentContext()
        return context.getThrowable() != null && !context.errorHandled()
    }


    @Override
    Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        Throwable ex = ctx.getThrowable()
        try {
            String errorCause="Gate-Error-Unknown-Cause";
            int responseStatusCode;

            if (ex instanceof GateException) {
                String cause = ex.errorCause
                if(cause!=null) errorCause = cause;
                responseStatusCode = ex.nStatusCode;
				
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

            if (responseStatusCode == 404) {
				MetricReporter.statRouteErrorStatus("ROUTE_NOT_FOUND", errorCause)
            } else {
                MetricReporter.statRouteErrorStatus(ctx.routeName, errorCause)
            }

            ctx.setResponseStatusCode(responseStatusCode);
			
    
			ctx.addGateResponseHeader("Content-Type", "application/json; charset=utf-8");
            ctx.setSendGateResponse(false)
			ctx.setResponseBody("{\"Message\":\""+errorCause+"\"}");
        } finally {
            ctx.setErrorHandled(true) //ErrorResponse was handled
            return null;
        }
    }

   

 


}