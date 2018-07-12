package com.along101.pgateway.filters;

import com.along101.pgateway.common.IFilterFactory;
import com.along101.pgateway.common.IFilterFactory;

/**
 * Default factory for creating instances of GateFilter. 
 */
public class DefaultFilterFactory implements IFilterFactory {

    /**
     * Returns a new implementation of GateFilter as specified by the provided 
     * Class. The Class is instantiated using its nullary constructor.
     * 
     * @param clazz the Class to instantiate
     * @return A new instance of GateFilter
     */
    @Override
    public GateFilter newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
        return (GateFilter) clazz.newInstance();
    }

}