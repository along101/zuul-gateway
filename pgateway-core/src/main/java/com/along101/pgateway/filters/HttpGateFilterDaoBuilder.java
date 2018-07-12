package com.along101.pgateway.filters;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.IGateFilterDao;
import com.along101.pgateway.common.IGateFilterDaoBuilder;

public class HttpGateFilterDaoBuilder implements IGateFilterDaoBuilder {

	private static final DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DeploymentApplicationID, Constants.ApplicationName);

	public HttpGateFilterDaoBuilder() {

	}

	@Override
	public IGateFilterDao build() {
		return new HttpGateFilterDao(appName.get());

	}

}
