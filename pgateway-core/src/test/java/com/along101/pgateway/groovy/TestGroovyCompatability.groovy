package com.along101.pgateway.groovy

import com.along101.pgateway.context.RequestContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Unit test class to verify groovy compatibility with RequestContext
 * 
 */
class TestGroovyCompatability {


    @RunWith(MockitoJUnitRunner.class)
    public static class TestUnit {

        @Mock
        HttpServletResponse response
        @Mock
        HttpServletRequest request

        @Test
        public void testRequestContext() {
            RequestContext.getCurrentContext().setRequest(request)
            RequestContext.getCurrentContext().setResponse(response)
            assertNotNull(RequestContext.getCurrentContext().getRequest())
            assertNotNull(RequestContext.getCurrentContext().getResponse())
            assertEquals(RequestContext.getCurrentContext().request, request)
            RequestContext.getCurrentContext().test = "moo"
            assertNotNull(RequestContext.getCurrentContext().test)
            assertEquals(RequestContext.getCurrentContext().test, "moo")
            assertNotNull(RequestContext.getCurrentContext().get("test"))
            assertEquals(RequestContext.getCurrentContext().get("test"), "moo")
            RequestContext.getCurrentContext().set("test", "ik")
            assertEquals(RequestContext.getCurrentContext().get("test"), "ik")
            assertEquals(RequestContext.getCurrentContext().test, "ik")
            assertNotNull(RequestContext.currentContext)
            assertEquals(RequestContext.currentContext.test, "ik")

        }

    }

}
