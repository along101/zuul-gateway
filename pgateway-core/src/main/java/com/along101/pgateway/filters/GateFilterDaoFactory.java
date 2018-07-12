package com.along101.pgateway.filters;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.along101.pgateway.common.Constants;
import com.along101.pgateway.common.IGateFilterDao;

public class GateFilterDaoFactory {
    private static final DynamicStringProperty daoType = DynamicPropertyFactory.getInstance().getStringProperty(Constants.GateFilterDaoType, "jdbc");
    
    private static ConcurrentMap<String, IGateFilterDao> daoCache = Maps.newConcurrentMap();

    private GateFilterDaoFactory(){
    	
    }
    
    public static IGateFilterDao getGateFilterDao(){
    	IGateFilterDao dao = daoCache.get(daoType.get());
    	
    	if(dao != null){
    		return dao;
    	}
    	
    	if("jdbc".equalsIgnoreCase(daoType.get())){
    		dao = new JDBCGateFilterDaoBuilder().build();  
    	}else if("http".equalsIgnoreCase(daoType.get())){
    		dao =  new HttpGateFilterDaoBuilder().build();
    	}else{
    		dao =  new JDBCGateFilterDaoBuilder().build();
    	}
    	
    	daoCache.putIfAbsent(daoType.get(), dao);
    	
    	return dao;
    }
    
    public static String getCurrentType(){
    	return daoType.get();
    }
    
}
