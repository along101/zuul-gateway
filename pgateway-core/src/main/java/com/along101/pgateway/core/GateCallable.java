package com.along101.pgateway.core;

import java.util.concurrent.Callable;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.monitor.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.Cat.Context;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.internal.DefaultMessageProducer;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.monitor.MetricReporter;

public class GateCallable implements Callable {

	private static Logger LOGGER = LoggerFactory.getLogger(GateCallable.class);

	private AsyncContext ctx;
	private GateRunner gateRunner;
	private Context catCtx ;
	private HttpServletRequest request;
	public GateCallable(Context catContext,AsyncContext asyncContext, GateRunner gateRunner,HttpServletRequest request) {
		this.ctx = asyncContext;
		this.gateRunner = gateRunner;
		this.catCtx = catContext;
		this.request = request;
	}

	@Override
	public Object call() throws Exception {
		Cat.logRemoteCallServer(catCtx);
		RequestContext.getCurrentContext().unset();
		Transaction tran = ((DefaultMessageProducer)Cat.getProducer()).newTransaction("GateCallable", request.getRequestURL().toString());
		RequestContext gateContext = RequestContext.getCurrentContext();
		long start = System.currentTimeMillis();
		try {
			tran.setStatus(Transaction.SUCCESS);
			service(ctx.getRequest(), ctx.getResponse());
		} catch (Throwable t) {
			LOGGER.error("GateCallable execute error.", t);
			Cat.logError(t);
			tran.setStatus(t);
		} finally {
            try {
            	statReporter(gateContext, start);
            } catch (Throwable t) {
            	Cat.logError("GateCallable collect stats error.", t);
            }
            try {
                ctx.complete();
            } catch (Throwable t) {
                Cat.logError("AsyncContext complete error.", t);
            }
			gateContext.unset();
		
			tran.complete();
		}
		return null;
	}

	private void service(ServletRequest req, ServletResponse res) {
		try {

			init((HttpServletRequest) req, (HttpServletResponse) res);

			// marks this request as having passed through the "Gate engine", as
			// opposed to servlets
			// explicitly bound in web.xml, for which requests will not have the
			// same data attached
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
	private void postRoute() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("GateCallable", "postRoute");
		try {
			gateRunner.postRoute();
			tran.setStatus(Transaction.SUCCESS);
		} catch(Throwable e){
			tran.setStatus(e);
			throw e;
		}finally {
			tran.complete();
		}
	}

	/**
	 * executes "route" filters
	 *
	 * @throws GateException
	 */
	private void route() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("GateCallable", "route");
		try {
			gateRunner.route();
			tran.setStatus(Transaction.SUCCESS);
		} catch(Throwable e){
			tran.setStatus(e);
			throw e;
		}finally {
			tran.complete();
		}
	}

	/**
	 * executes "pre" filters
	 *
	 * @throws GateException
	 */
	private void preRoute() throws GateException {
		Transaction tran = Cat.getProducer().newTransaction("GateCallable", "preRoute");
		try {
			gateRunner.preRoute();
			tran.setStatus(Transaction.SUCCESS);
		} catch(Throwable e){
			tran.setStatus(e);
			throw e;
		}finally {
			tran.complete();
		}
	}

	/**
	 * initializes request
	 *
	 * @param servletRequest
	 * @param servletResponse
	 */
	private void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		gateRunner.init(servletRequest, servletResponse);
	}

	/**
	 * sets error context info and executes "error" filters
	 *
	 * @param e
	 */
	private void error(GateException e) {
		Transaction tran = Cat.getProducer().newTransaction("GateCallable", "postRoute");
		try {
			RequestContext.getCurrentContext().setThrowable(e);
			gateRunner.error();
			tran.setStatus(Transaction.SUCCESS);
		}catch(Throwable t){ 
			Cat.logError(t);
		}finally {
			tran.complete();
			Cat.logError(e);
		}
	}

	private void statReporter(RequestContext gateContext, long start) {

		long remoteServiceCost = 0l;
		Object remoteCallCost = gateContext.get("remoteCallCost");
		if (remoteCallCost != null) {
			try {
				remoteServiceCost = Long.parseLong(remoteCallCost.toString());
			} catch (Exception ignore) {
			}
		}

		long replyClientCost = 0l;
		Object sendResponseCost = gateContext.get("sendResponseCost");
		if (sendResponseCost != null) {
			try {
				replyClientCost = Long.parseLong(sendResponseCost.toString());
			} catch (Exception ignore) {
			}
		}

		long replyClientReadCost = 0L;
		Object sendResponseReadCost = gateContext.get("sendResponseCost:read");
		if (sendResponseReadCost != null) {
			try {
				replyClientReadCost = Long.parseLong(sendResponseReadCost.toString());
			} catch (Exception ignore) {
			}
		}

		long replyClientWriteCost = 0L;
		Object sendResponseWriteCost = gateContext.get("sendResponseCost:write");
		if (sendResponseWriteCost != null) {
			try {
				replyClientWriteCost = Long.parseLong(sendResponseWriteCost.toString());
			} catch (Exception ignore) {
			}
		}

		String routeName = gateContext.getRouteName();
		if (routeName == null || routeName.equals("")) {
			routeName = "unknown";
			LOGGER.warn("Unknown Route: [ {"+ gateContext.getRequest().getRequestURL()+"} ]");
		}

		MetricReporter.statCost(System.currentTimeMillis() - start,
		 remoteServiceCost, replyClientCost,
		 replyClientReadCost, replyClientWriteCost, routeName);
	}
}
