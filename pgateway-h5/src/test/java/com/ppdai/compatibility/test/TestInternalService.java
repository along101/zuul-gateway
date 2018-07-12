package com.along101.compatibility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class TestInternalService {

	protected static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

	protected Server server;
	protected int SERVER_PORT = 8080;
	protected String SERVER_HOST = "localhost";
	//protected String SERVER_HOST = "localhost";
	
	protected OkHttpClient client = new OkHttpClient();

	@Before
	public void setUp() throws Exception {
		client.setConnectTimeout(30000, TimeUnit.MILLISECONDS);
		client.setReadTimeout(30000, TimeUnit.MILLISECONDS);
		if (SERVER_HOST.equals("localhost")) {
			SERVER_PORT = ThreadLocalRandom.current().nextInt(10000, 40000);
		}
	}


	
    @Test
    public void testSlowService() throws IOException {
        RequestBody body = RequestBody.create(JSON_TYPE, "{\"sleep\": 60000}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ArchDemoService/arch/gateway/random?t=0.1&s=1024", SERVER_HOST, SERVER_PORT))
                //.url("http://wirelessgateway.along101.com/ac/auth/validate")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", String.valueOf((long)System.currentTimeMillis() / 1000))
                .header("X-ALONG-APPVERSION", "4.6.0")
                .header("X-ALONG-SIGNVERSION", "1")
                .header("X-ALONG-KEYVERSION", "1")
                .header("X-ALONG-USER", "42502637")
                .post(body)
                .build();
        
        
        Response response = client.newCall(request).execute();
        
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        System.out.println(response.body().string());
        
    }
    
	@After
	public void tearDown() throws Exception {
		if (server != null) {
			// server.stop();
		}
	}
}
