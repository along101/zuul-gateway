package com.along101.pgateway.filters;

import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.FilterInfo;

import groovy.lang.GroovyClassLoader;

/**
 * verifies that the given source code is compilable in Groovy, can be
 * instanciated, and is a GateFilter type
 *
 * 
 */
public class FilterVerifier {
	private static final FilterVerifier instance = new FilterVerifier();

	/**
	 * @return Singleton
	 */
	public static FilterVerifier getInstance() {
		return instance;
	}

	/**
	 * verifies compilation, instanciation and that it is a GateFilter
	 *
	 * @param sFilterCode
	 * @return a FilterInfo object representing that code
	 * @throws org.codehaus.groovy.control.CompilationFailedException
	 *
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public FilterInfo verifyFilter(String sFilterCode) throws org.codehaus.groovy.control.CompilationFailedException,
			IllegalAccessException, InstantiationException {
		Class groovyClass = compileGroovy(sFilterCode);
		Object instance = instanciateClass(groovyClass);
		checkGateFilterInstance(instance);
		GateFilter filter = (GateFilter) instance;

		String filter_id = FilterInfo.buildFilterId(Constants.ApplicationName, filter.filterType(),
				groovyClass.getSimpleName());

		return new FilterInfo(filter_id, sFilterCode, filter.filterType(), groovyClass.getSimpleName(),
				filter.disablePropertyName(), "" + filter.filterOrder(), Constants.ApplicationName);
	}

	Object instanciateClass(Class groovyClass) throws InstantiationException, IllegalAccessException {
		return groovyClass.newInstance();
	}

	void checkGateFilterInstance(Object gateFilter) throws InstantiationException {
		if (!(gateFilter instanceof GateFilter)) {
			throw new InstantiationException("Code is not a GateFilter Class ");
		}
	}

	/**
	 * compiles the Groovy source code
	 *
	 * @param sFilterCode
	 * @return
	 * @throws org.codehaus.groovy.control.CompilationFailedException
	 *
	 */
	public Class compileGroovy(String sFilterCode) throws org.codehaus.groovy.control.CompilationFailedException {
		GroovyClassLoader loader = new GroovyClassLoader();
		return loader.parseClass(sFilterCode);
	}

}
