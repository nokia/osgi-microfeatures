/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.exception;

/**
 * @author marynows
 * 
 */
public class CLSRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -7963722469115508055L;

	public CLSRuntimeException() {
		super();
	}

	public CLSRuntimeException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CLSRuntimeException(final String message) {
		super(message);
	}

	public CLSRuntimeException(final Throwable cause) {
		super(cause);
	}
}
