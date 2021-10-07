/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.exception;

import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class ViolationException extends CLSRuntimeException {
	private static final long serialVersionUID = 7448743447680778834L;

	private final ViolationError error;

	public ViolationException(final String message, final String path, final String value) {
		super(message);
		this.error = new ViolationError().withMessage(message).withPath(path).withValue(value);
	}

	public ViolationError getError() {
		return this.error;
	}
}
