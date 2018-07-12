package com.along101.compatibility.test;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangyicong on 2017/3/16.
 */
public abstract class TestValidateBase {

	protected static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

	protected Server server;
	protected int SERVER_PORT = 8080;
	//protected String SERVER_HOST = "localhost";
	protected String SERVER_HOST = "localhost";
	protected OkHttpClient client = new OkHttpClient();

	@Before
	public void setUp() throws Exception {
		client.setConnectTimeout(30000, TimeUnit.MILLISECONDS);
		client.setReadTimeout(30000, TimeUnit.MILLISECONDS);
		if (SERVER_HOST.equals("localhost")) {
			SERVER_PORT = ThreadLocalRandom.current().nextInt(10000, 40000);
		}

		
	}

	@After
	public void tearDown() throws Exception {
		if (server != null) {
			// server.stop();
		}
	}
}
