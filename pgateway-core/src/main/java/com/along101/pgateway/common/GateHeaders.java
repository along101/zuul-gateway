package com.along101.pgateway.common;
/**
 * HTTP Headers that are accessed or added by PGateWay
 */
public class GateHeaders {
    public static final String TRANSFER_ENCODING = "transfer-encoding";
    public static final String CHUNKED = "chunked";
    public static final String CONTENT_ENCODING = "content-encoding";
    public static final String CONTENT_LENGTH = "content-length";
    public static final String ACCEPT_ENCODING = "accept-encoding";
    public static final String CONNECTION = "connection";
    public static final String KEEP_ALIVE = "keep-alive";
    public static final String HOST = "host";
    public static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";

    public static final String X_GATE = "x-gate";
    public static final String X_GATE_INSTANCE = "x-gate-instance";
    public static final String X_ORIGINATING_URL = "x-originating-url";
    public static final String X_GATE_ERROR_CAUSE = "x-gate-error-cause";
    public static final String X_GATE_CLIENT_HOST = "x-gate-client-host";
    public static final String X_GATE_CLIENT_PROTO = "x-gate-client-proto";
    public static final String X_GATE_SURGICAL_FILTER = "x-gate-surgical-filter";
    public static final String X_GATE_FILTER_EXECUTION_STATUS = "x-gate-filter-executions";
    public static final String X_GATE_REQUEST_TOPLEVEL_ID = "x-gate-request.toplevel.uuid";
}