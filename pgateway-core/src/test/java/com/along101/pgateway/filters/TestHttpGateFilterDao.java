package com.along101.pgateway.filters;

import org.junit.Test;

public class TestHttpGateFilterDao {


	@Test
	public void testCase() throws Exception {
		HttpGateFilterDao dao = new HttpGateFilterDao("wirelessgate");
		
		
		dao.getAllActiveFilters();
		dao.getAllCanaryFilters();
	}
}
