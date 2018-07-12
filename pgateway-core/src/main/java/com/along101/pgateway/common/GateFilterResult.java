package com.along101.pgateway.common;

public final class GateFilterResult {

	private Object result;
	private Throwable exception;
	private ExecutionStatus status;

	public GateFilterResult(Object result, ExecutionStatus status) {
		this.result = result;
		this.status = status;
	}

	public GateFilterResult(ExecutionStatus status) {
		this.status = status;
	}

	public GateFilterResult() {
		this.status = ExecutionStatus.DISABLED;
	}

	/**
	 * @return the result
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(Object result) {
		this.result = result;
	}

	/**
	 * @return the status
	 */
	public ExecutionStatus getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	/**
	 * @return the exception
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * @param exception
	 *            the exception to set
	 */
	public void setException(Throwable exception) {
		this.exception = exception;
	}

}
