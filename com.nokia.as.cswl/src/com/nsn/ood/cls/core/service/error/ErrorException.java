/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 * 
 */
public class ErrorException extends CLSException {
	private static final long serialVersionUID = -8654415178965467108L;

	private final ErrorCode errorCode;
	private final String name;
	private final Object value;
	private final String info;

	ErrorException(final ErrorCode errorCode, final Throwable cause, final String name, final Object value) {
		super(cause.getMessage(), cause);
		this.errorCode = errorCode;
		this.name = name;
		this.value = value;
		this.info = null;
	}

	ErrorException(final ErrorCode errorCode, final Throwable cause, final String name, final Object value,
			final String info) {
		super(cause.getMessage(), cause);
		this.errorCode = errorCode;
		this.name = name;
		this.value = value;
		this.info = info;
	}

	ErrorException(final ErrorCode errorCode, final String message, final String name, final Object value) {
		super(message);
		this.errorCode = errorCode;
		this.name = name;
		this.value = value;
		this.info = null;
	}

	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

	public String getName() {
		return this.name;
	}

	public Object getValue() {
		return this.value;
	}

	public String getInfo() {
		return this.info;
	}
}
