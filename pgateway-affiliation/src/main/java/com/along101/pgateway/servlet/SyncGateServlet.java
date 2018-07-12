package com.along101.pgateway.servlet;

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.dianping.cat.Cat;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.core.GateRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SyncGateServlet extends HttpServlet {
	
	private static Logger LOGGER = LoggerFactory.getLogger(SyncGateServlet.class);
	
    private GateRunner gateRunner = new GateRunner();

    @Override
    public void service(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) throws javax.servlet.ServletException, java.io.IOException {
        try {


            init((HttpServletRequest) req, (HttpServletResponse) res);

            // marks this request as having passed through the "Gate engine", as opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the same data attached
            RequestContext.getCurrentContext().setGateEngineRan();

            try {
                preRoute();
            } catch (GateException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                route();
            } catch (GateException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                postRoute();
            } catch (GateException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            error(new GateException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        }
    }

    /**
     * executes "post" GateFilters
     *
     * @throws GateException
     */
    void postRoute() throws GateException {
    	gateRunner.postRoute();
    }

    /**
     * executes "route" filters
     *
     * @throws GateException
     */
    void route() throws GateException {
    	gateRunner.route();
    }

    /**
     * executes "pre" filters
     *
     * @throws GateException
     */
    void preRoute() throws GateException {
    	gateRunner.preRoute();
    }

    /**
     * initializes request
     *
     * @param servletRequest
     * @param servletResponse
     */
    void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    	gateRunner.init(servletRequest, servletResponse);
    }

	/**
	 * sets error context info and executes "error" filters
	 *
	 * @param e
	 * @throws GateException
	 */
	void error(GateException e) {
		try {
			RequestContext.getCurrentContext().setThrowable(e);
			gateRunner.error();
		} catch (Throwable t) {
			Cat.logError(t);
			LOGGER.error(e.getMessage(), e);
		}finally{
			Cat.logError(e);
		}
	}

}
