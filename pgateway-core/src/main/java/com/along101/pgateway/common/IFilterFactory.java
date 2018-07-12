package com.along101.pgateway.common;

import com.along101.pgateway.filters.GateFilter;

/**
 * Interface to provide instances of GateFilter from a given class.
 */
public interface IFilterFactory {

	/**
	 * Returns an instance of the specified class.
	 * 
	 * @param clazz
	 *            the Class to instantiate
	 * @return an instance of GateFilter
	 * @throws Exception
	 *             if an error occurs
	 */
	public GateFilter newInstance(Class clazz) throws Exception;
}