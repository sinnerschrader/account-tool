package com.sinnerschrader.s2b.accounttool.logic.exception;

/** */
public class BusinessException extends Exception {

	private final String code;

	private final Object[] args;

	public BusinessException(String msg, String code) {
		this(msg, code, (Object[]) null);
	}

	public BusinessException(String msg, String code, Object[] args) {
		super(msg);
		this.args = args;
		this.code = code;
	}

	public BusinessException(String msg, String code, Throwable t) {
		this(msg, code, null, t);
	}

	public BusinessException(String msg, String code, Object[] args, Throwable t) {
		super(msg, t);
		this.args = args;
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public Object[] getArgs() {
		return args;
	}
}
