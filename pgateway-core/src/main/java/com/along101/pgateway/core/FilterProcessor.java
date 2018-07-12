package com.along101.pgateway.core;

import java.util.List;

import com.along101.pgateway.common.ExecutionStatus;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.GateFilterResult;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.util.Debug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.along101.pgateway.common.ExecutionStatus;
import com.along101.pgateway.common.GateException;
import com.along101.pgateway.common.GateFilterResult;
import com.along101.pgateway.context.RequestContext;
import com.along101.pgateway.filters.GateFilter;
import com.along101.pgateway.util.Debug;

public class FilterProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterProcessor.class);

	private static FilterProcessor instance = new FilterProcessor();

	protected FilterProcessor() {
	}

	/**
	 * @return the singleton FilterProcessor
	 */
	public static FilterProcessor getInstance() {
		return instance;
	}

	/**
	 * sets a singleton processor in case of a need to override default behavior
	 *
	 * @param processor
	 */
	public static void setProcessor(FilterProcessor processor) {
		instance = processor;
	}

	/**
	 * runs "post" filters which are called after "route" filters.
	 * GateExceptions from GateFilters are thrown. Any other Throwables are
	 * caught and a GateException is thrown out with a 500 status code
	 *
	 * @throws GateException
	 */
	public void postRoute() throws GateException {
		try {
			runFilters("post");
		} catch (Throwable e) {
			if (e instanceof GateException) {
				throw (GateException) e;
			}
			throw new GateException(e, 500, "UNCAUGHT_EXCEPTION_IN_POST_FILTER_" + e.getClass().getName());
		}

	}

	/**
	 * runs all "error" filters. These are called only if an exception occurs.
	 * Exceptions from this are swallowed and logged so as not to bubble up.
	 * @throws GateException 
	 */
	public void error() throws GateException {
		try {
			runFilters("error");
		} catch (Throwable e) {
			if (e instanceof GateException) {
				throw (GateException) e;
			}
			throw new GateException(e, 500, "UNCAUGHT_EXCEPTION_IN_POST_FILTER_" + e.getClass().getName());
		}
	}

	/**
	 * Runs all "route" filters. These filters route calls to an origin.
	 *
	 * @throws GateException
	 *             if an exception occurs.
	 */
	public void route() throws GateException {
		try {
			runFilters("route");
		} catch (Throwable e) {
			if (e instanceof GateException) {
				throw (GateException) e;
			}
			throw new GateException(e, 500, "UNCAUGHT_EXCEPTION_IN_ROUTE_FILTER_" + e.getClass().getName());
		}
	}

	/**
	 * runs all "pre" filters. These filters are run before routing to the
	 * orgin.
	 *
	 * @throws GateException
	 */
	public void preRoute() throws GateException {
		try {
			runFilters("pre");
		} catch (Throwable e) {
			if (e instanceof GateException) {
				throw (GateException) e;
			}
			throw new GateException(e, 500, "UNCAUGHT_EXCEPTION_IN_PRE_FILTER_" + e.getClass().getName());
		}
	}

	/**
	 * runs all filters of the filterType sType/ Use this method within filters
	 * to run custom filters by type
	 *
	 * @param sType
	 *            the filterType.
	 * @return
	 * @throws Throwable
	 *             throws up an arbitrary exception
	 */
	public Object runFilters(String sType) throws Throwable {
		if (RequestContext.getCurrentContext().debugRouting()) {
			Debug.addRoutingDebug("Invoking {" + sType + "} type filters");
		}
		boolean bResult = false;
		List<GateFilter> list = FilterLoader.getInstance().getFiltersByType(sType);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				GateFilter gateFilter = list.get(i);
				Object result = processGateFilter(gateFilter);
				if (result != null && result instanceof Boolean) {
					bResult |= ((Boolean) result);
				}
			}
		}
		return bResult;
	}

	/**
	 * Processes an individual GateFilter. This method adds Debug information.
	 * Any uncaught Thowables are caught by this method and converted to a
	 * GateException with a 500 status code.
	 *
	 * @param filter
	 * @return the return value for that filter
	 * @throws GateException
	 */
	public Object processGateFilter(GateFilter filter) throws GateException {
		RequestContext ctx = RequestContext.getCurrentContext();
		boolean bDebug = ctx.debugRouting();
		long execTime = 0;
		String filterName = "";

		try {
			long ltime = System.currentTimeMillis();
			filterName = filter.getClass().getSimpleName();

			RequestContext copy = null;
			Object o = null;
			Throwable t = null;

			if (bDebug) {
				Debug.addRoutingDebug("Filter " + filter.filterType() + " " + filter.filterOrder() + " " + filterName);
				copy = ctx.copy();
			}

			GateFilterResult result = filter.runFilter();
			ExecutionStatus s = result.getStatus();
			execTime = System.currentTimeMillis() - ltime;

			switch (s) {
				case FAILED:
                    t = result.getException();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                    break;					
				case SUCCESS:
                    o = result.getResult();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.SUCCESS.name(), execTime);
                    if (bDebug) {
                        Debug.addRoutingDebug("Filter {" + filterName + " TYPE:" + filter.filterType() + " ORDER:" + filter.filterOrder() + "} Execution time = " + execTime + "ms");
                        Debug.compareContextState(filterName, copy);
                    }
					break;
				default:
					break;
			}
            if (t != null) throw t;
            return o;
		} catch (Throwable e) {
            if (bDebug) {
                Debug.addRoutingDebug("Running Filter failed " + filterName + " type:" + filter.filterType() + " order:" + filter.filterOrder() + " " + e.getMessage());
            }
            
            if (e instanceof GateException) {
                throw (GateException) e;
            } else {
            	GateException ex = new GateException(e, "Filter threw Exception", 500, filter.filterType() + ":" + filterName);
                ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                throw ex;
            }
		}

	}

}
