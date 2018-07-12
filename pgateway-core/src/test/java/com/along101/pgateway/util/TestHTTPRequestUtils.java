package com.along101.pgateway.util;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public  class TestHTTPRequestUtils {


        @Test
        public void detectsGzip() {
            assertTrue(HTTPRequestUtil.isGzipped("gzip"));
        }

        @Test
        public void detectsNonGzip() {
            assertFalse(HTTPRequestUtil.isGzipped("identity"));
        }

        @Test
        public void detectsGzipAmongOtherEncodings() {
            assertTrue(HTTPRequestUtil.isGzipped("gzip, deflate"));
        }

    }