package scripts.post

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.runners.MockitoJUnitRunner

import com.along101.pgateway.context.RequestContext
import com.along101.pgateway.filters.GateFilter
import com.along101.pgateway.monitor.MetricReporter
import com.along101.pgateway.monitor.StatManager
import com.along101.pgateway.util.MonitoringUtil


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

    @RunWith(MockitoJUnitRunner.class)
    public static class TestUnit {

        @Mock
        HttpServletResponse response
        @Mock
        HttpServletRequest request
		
		@Before
		public void before() {
			MonitoringUtil.initMocks();
			MockitoAnnotations.initMocks(this);
		}

        @Test
        public void testHeaderResponse() {

            def f = new Stats();
            RequestContext.getCurrentContext().setRequest(request)
            RequestContext.getCurrentContext().setResponse(response)

            RequestContext.getCurrentContext().routeName = "testStats"
            RequestContext.getCurrentContext().setResponseStatusCode(200);

            f.runFilter()

            Assert.assertTrue(StatManager.manager.getRouteStatusCodeMonitor("testStats", 200) != null)

            Assert.assertTrue(f.filterType().equals("post"))
            Assert.assertTrue(f.shouldFilter())
        }

    }

}
