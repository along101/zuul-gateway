package com.along101.pgateway.esb;

import com.along101.pgateway.util.StringUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by zhangyicong on 2017/3/17.
 */
public class TestPpUtil {

    @Test
    public void testTrimEnd() {
        String url = "http://wirelessgateway.along101.com/Demo/DemoService/GetSomething";
        assertEquals(url, StringUtil.trimEnd(url + "/", '/'));
    }
}
