/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.exception;

/**
 * @author marynows
 * 
 */
public class CLSIllegalArgumentException extends IllegalArgumentException {
	private static final long serialVersionUID = -7963722469115508055L;

	public CLSIllegalArgumentException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CLSIllegalArgumentException(final String message) {
		super(message);
	}
}
