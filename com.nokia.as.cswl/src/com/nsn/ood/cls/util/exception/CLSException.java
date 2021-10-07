/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.exception;

/**
 * @author marynows
 * 
 */
public class CLSException extends Exception {
	private static final long serialVersionUID = -4765414547573237128L;

	public CLSException() {
		super();
	}

	public CLSException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CLSException(final String message) {
		super(message);
	}

	public CLSException(final Throwable cause) {
		super(cause);
	}
}
