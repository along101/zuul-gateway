package com.along101.pgateway.common;

/**
 * All handled exceptions in GateKeeper are GateExceptions
 */
public class GateException extends Exception {
    public int nStatusCode;
    public String errorCause;

    /**
     * Source Throwable, message, status code and info about the cause
     * @param throwable
     * @param sMessage
     * @param nStatusCode
     * @param errorCause
     */
    public GateException(Throwable throwable, String sMessage, int nStatusCode, String errorCause) {
        super(sMessage, throwable);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
    }

    /**
     * error message, status code and info about the cause
     * @param sMessage
     * @param nStatusCode
     * @param errorCause
     */
    public GateException(String sMessage, int nStatusCode, String errorCause) {
        super(sMessage);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
    }

    /**
     * Source Throwable,  status code and info about the cause
     * @param throwable
     * @param nStatusCode
     * @param errorCause
     */
    public GateException(Throwable throwable, int nStatusCode, String errorCause) {
        super(throwable.getMessage(), throwable);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
    }



}
