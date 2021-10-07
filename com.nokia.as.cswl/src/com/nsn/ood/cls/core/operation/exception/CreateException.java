/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 * 
 */
public class CreateException extends CLSException {
	private static final long serialVersionUID = -2003471123693976908L;

	public CreateException(final Throwable cause) {
		super(cause.getMessage(), cause);
	}
}