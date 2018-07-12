package com.along101.compatibility.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.squareup.okhttp.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Created by zhangyicong on 2017/3/15.
 */
public class TestValidateTimestamp extends TestValidateBase {

    @Test
    public void testTimestampNotValidRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", "1489058842")
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
        assertEquals("时间戳已失效", json.get("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testTimestampEmptyRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", "")
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
        assertEquals("时间戳内容为空", json.get("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testTimestampBadFormatRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", "abc")
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
        assertEquals("时间戳格式不正确", json.get("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testTimestampNotExistRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
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
        assertEquals("缺少必要的消息头:X-ALONG-TIMESTAMP", json.get("Message"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testNormalRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
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
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("userId"));
        assertEquals(1, json.get("userId"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }

    @Test
    public void testTimestampIn10MinRequest() throws IOException {

        RequestBody body = RequestBody.create(JSON_TYPE, "{\"username\": \"zhangyicong\"}");
        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/ac/auth/validate", SERVER_HOST, SERVER_PORT))
                .header("Host", "demo.along101corp.com")
                .header("X-Real-IP", "localhost")
                .header("X-Forwarded-For", "localhost")
                .header("User-Agent", "loan-4.6.0  (OPPO;OPPO R7sm;860270035270550;5.1.1;1)")
                .header("X-ALONG-APPID", "10080004")
                .header("X-ALONG-KEY", "tc-002")
                .header("X-ALONG-TOKEN", "8e40b09c-fb60-4348-9f16-092264adbac8")
                .header("X-ALONG-SIGN", "BedAJaCDc2TPWiA9adaBKvprLVQUifwQp4GBaTlXP8CIapPZKtdy+Uhs6qQC75ra2gLX3KJOFXKor8uxfp7ghLJ0UJC4gwn3jK/nHdNuGQ1zkIHfPwX0YqbgxpjNnegIX4GaakI/MfQKzZwxAiORWqHyYmruZD4TQktGF0n+Gew=")
                .header("X-ALONG-APPOS", "2")
                .header("X-ALONG-DEVICEID", "860270035270550")
                .header("X-ALONG-TIMESTAMP", String.valueOf(System.currentTimeMillis() / 1000 - 300))
                .header("X-ALONG-APPVERSION", "4.6.0")
                .header("X-ALONG-SIGNVERSION", "1")
                .header("X-ALONG-KEYVERSION", "1")
                .header("X-ALONG-USER", "42502637")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        JSONObject json = JSON.parseObject(response.body().string());
        assertNotNull(json);
        assertNotNull(json.get("userId"));
        assertEquals(1, json.get("userId"));
        assertNotNull(response.header("X-ALONG-TIMESTAMP"));
    }
}
