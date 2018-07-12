package com.along101.pgateway.filters;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;



public class FilterRegistry {

    private static final FilterRegistry instance = new FilterRegistry();

    public static final FilterRegistry instance() {
        return instance;
    }

    private final ConcurrentHashMap<String, GateFilter> filters = new ConcurrentHashMap<String, GateFilter>();

    private FilterRegistry() {
    }

    public GateFilter remove(String key) {
        return this.filters.remove(key);
    }

    public GateFilter get(String key) {
        return this.filters.get(key);
    }

    public void put(String key, GateFilter filter) {
        this.filters.putIfAbsent(key, filter);
    }

    public int size() {
        return this.filters.size();
    }

    public Collection<GateFilter> getAllFilters() {
        return this.filters.values();
    }

}