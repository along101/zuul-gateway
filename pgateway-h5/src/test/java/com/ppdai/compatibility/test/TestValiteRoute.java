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
 * Created by zhangyicong on 2017/3/17.
 */
public class TestValiteRoute extends TestValidateBase {

    @Test
    public void testServiceNotFound() throws IOException {
        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/notexist/validate", SERVER_HOST, SERVER_PORT))
                .header("X-Real-IP", "localhost")
                .header("X-ALONG-VIAGATEWAY", "1")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
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
        assertEquals("服务ac.notexist在网关可访问服务配置中不存在", json.getString("Message"));
        assertNotNull(response.header("X-ALONG-CODE"));
    }

    @Test
    public void testServiceNoProvider() throws IOException {
        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/unittest/noprovider/validate", SERVER_HOST, SERVER_PORT))
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
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
        assertEquals("服务unittest.noprovider在网关可访问服务配置中不存在", json.getString("Message"));
        assertNotNull(response.header("X-ALONG-CODE"));
    }
}
