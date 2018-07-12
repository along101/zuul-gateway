package com.along101.pgateway.util;
public class SleepUtil {

	private SleepUtil() {
	}

	public static void sleep(long timeout) {
		try {
			Thread.sleep(timeout);
		} catch (Throwable t) {
		}
	}
}