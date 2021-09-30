/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 * 
 */
public class ConditionProcessingException extends CLSException {
	private static final long serialVersionUID = 6397099955751800856L;

	private final ViolationError error;

	public ConditionProcessingException(final String message, final String field, final String value,
			final Throwable cause) {
		super(message, cause);
		this.error = new ViolationError().withMessage(cause.getMessage()).withPath(field).withValue(value);
	}

	public ViolationError getError() {
		return this.error;
	}
}
