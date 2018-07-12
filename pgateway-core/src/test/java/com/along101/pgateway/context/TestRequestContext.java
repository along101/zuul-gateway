package com.along101.pgateway.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestRequestContext {
    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Test
    public void testGetContext() {
        RequestContext context = RequestContext.getCurrentContext();
        assertNotNull(context);
    }

    @Test
    public void testSetContextVariable() {
        RequestContext context = RequestContext.getCurrentContext();
        assertNotNull(context);
        context.set("test", "moo");
        assertEquals(context.get("test"), "moo");
    }

    @Test
    public void testSet() {
        RequestContext context = RequestContext.getCurrentContext();
        assertNotNull(context);
        context.set("test");
        assertEquals(context.get("test"), Boolean.TRUE);
    }

    @Test
    public void testBoolean() {
        RequestContext context = RequestContext.getCurrentContext();
        assertEquals(context.getBoolean("boolean_test"), Boolean.FALSE);
        assertEquals(context.getBoolean("boolean_test", true), true);

    }

    @Test
    public void testCopy() {
        RequestContext context = RequestContext.getCurrentContext();

        context.put("test", "test");
        context.put("test1", "test1");
        context.put("test2", "test2");

        RequestContext copy = context.copy();

        assertEquals(copy.get("test"), "test");
        assertEquals(copy.get("test1"), "test1");
        assertEquals(copy.get("test2"), "test2");

    }


    @Test
    public void testResponseHeaders() {
        RequestContext context = RequestContext.getCurrentContext();
        context.addGateRequestHeader("header", "test");
        Map headerMap = context.getGateRequestHeaders();
        assertNotNull(headerMap);
        assertEquals(headerMap.get("header"), "test");
    }

    @Test
    public void testAccessors() {

        RequestContext context = new RequestContext();
        RequestContext.testSetCurrentContext(context);

        context.setRequest(request);
        context.setResponse(response);


        Throwable th = new Throwable();
        context.setThrowable(th);
        assertEquals(context.getThrowable(), th);

        assertEquals(context.debugRouting(), false);
        context.setDebugRouting(true);
        assertEquals(context.debugRouting(), true);

        assertEquals(context.debugRequest(), false);
        context.setDebugRequest(true);
        assertEquals(context.debugRequest(), true);

        context.setDebugRequest(false);
        assertEquals(context.debugRequest(), false);

        context.setDebugRouting(false);
        assertEquals(context.debugRouting(), false);


        try {
            URL url = new URL("http://www.moldfarm.com");
            context.setRouteUrl(url);
            assertEquals(context.getRouteUrl(), url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        InputStream in = mock(InputStream.class);
        context.setResponseDataStream(in);
        assertEquals(context.getResponseDataStream(), in);

        assertEquals(context.sendGateResponse(), true);
        context.setSendGateResponse(false);
        assertEquals(context.sendGateResponse(), false);

        context.setResponseStatusCode(100);
        assertEquals(context.getResponseStatusCode(), 100);

    }
}
