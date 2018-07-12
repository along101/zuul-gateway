package com.along101.compatibility.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by zhangyicong on 2017/3/16.
 */
public class TestValidateToken extends TestValidateBase {

    private String token = "";
    @Override
    public void setUp() throws Exception {
        super.setUp();
        token = loginAndGetToken();
    }

    private String loginAndGetToken() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"UserName\":\"joysunn\",\"Password\":\"e5CaNcfS2ruub/FEJsFrsOZqUKjeco/xQJeQ7B8eCdrcjFmKRKUftCJrDFQP1iMW7THKX8NRsi4f9EBEfbmKmiIw1d7/eV7E+F9jTXhpafJWFS3jvCM1FPjRboBpq7xHArxrGINha4yozYH1lI39s31lRuwdBXRKMGlrjz4T7D8=\",\"LoginSource\":2,\"LoginTime\":\"1970-01-01T00:00:00\",\"ExtraInfo\":null}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/Auth/AuthService/Login", SERVER_HOST, SERVER_PORT))
                .header("X-ALONG-APPID", "10000001")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-SIGN", "Wo+fsTDN6KbAQxzhiFJtgBZOz4XZUE+9L+IFuxO+pgT+7gUJ7SHPzKTRVyMFMTDNPhmR4zywc+dIMpVj9+c7NC0mRFSVyX81Qkt2yeejO77R4m9cfSQpHMGSFzLVUpB/aCln8M89a2Zijc+ACM9oQQFDehVJb+0l0tRe6nCMQgw=")
                .header("X-ALONG-DEVICEID", "1111")
                .header("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis() / 1000))
                .header("X-ALONG-APPVERSION", "v1.0")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-KEYVERSION", "1")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("Result"));
        assertEquals(0, json.getIntValue("Result"));
        assertNotNull(json.get("UserID"));
        assertEquals(79, json.getIntValue("UserID"));
        assertNotNull(json.get("UserName"));
        assertEquals("joysunn", json.getString("UserName"));
        assertNotNull(json.get("AuthID"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));

        return json.getString("AuthID");
    }

    @Test
    public void testNormalRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"UserID\":1}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/Demo/DemoService/GetSomething", SERVER_HOST, SERVER_PORT))
                .header("X-ALONG-APPID", "10250011")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-SIGN", "ePPKcz6aZ5cAbEfKvKCzdd4RV50Iv9fXm1dg82YL0pO5WjdEK8Lai106pSOOIPPuazoHCljzqXyvUJftdCKBS5YahIA0J8zBKCH1DaxAhNgniHIlX7ZA/N/xZUxyzDbE3129GBKL1ZUS1xsOHS+sUKz8IoQCwFe2dbOkpnJF0aM=")
                .header("X-ALONG-DEVICEID", "1111")
                .header("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis() / 1000))
                .header("X-ALONG-APPVERSION", "v1.0")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-KEYVERSION", "1")
                .header("X-ALONG-TOKEN", token)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("Result"));
        assertEquals(0, json.getIntValue("Result"));
        assertNotNull(json.get("UserName"));
        assertNotNull(json.get("MobilePhone"));
        assertNotNull(json.get("ResultMessage"));
        assertEquals("获取成功", json.getString("ResultMessage"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testBadSignRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"UserID\":1}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/Demo/DemoService/GetSomething", SERVER_HOST, SERVER_PORT))
                .header("X-ALONG-APPID", "10250011")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-SIGN", "/N/xZUxyzDbE3129GBKL1ZUS1xsOHS+sUKz8IoQCwFe2dbOkpnJF0aM")
                .header("X-ALONG-DEVICEID", "1111")
                .header("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis() / 1000))
                .header("X-ALONG-APPVERSION", "v1.0")
                .header("X-ALONG-KEY", "tc-001")
                .header("X-ALONG-KEYVERSION", "1")
                .header("X-ALONG-TOKEN", token)
                .header("X-ALONG-SIGNVERSION", "1")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        assertFalse(response.isSuccessful());
        assertEquals(400, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("Message"));
        assertEquals("客户端请求参数的签名无效", json.getString("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testInvalidTokenRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"UserID\":1}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/Demo/DemoService/GetSomething", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "ePPKcz6aZ5cAbEfKvKCzdd4RV50Iv9fXm1dg82YL0pO5WjdEK8Lai106pSOOIPPuazoHCljzqXyvUJftdCKBS5YahIA0J8zBKCH1DaxAhNgniHIlX7ZA/N/xZUxyzDbE3129GBKL1ZUS1xsOHS+sUKz8IoQCwFe2dbOkpnJF0aM=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis() / 1000))
                .header("X-ALONG-APPVERSION", "4.6.0")
                .header("X-ALONG-SIGNVERSION", "1")
                .header("X-ALONG-KEYVERSION", "1")
                .header("X-ALONG-USER", "42502637")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        assertFalse(response.isSuccessful());
        assertEquals(400, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("Message"));
        assertEquals("令牌校验失败：‘令牌不存在！’", json.get("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }
}
