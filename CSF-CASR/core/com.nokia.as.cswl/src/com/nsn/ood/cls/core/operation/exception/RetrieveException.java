/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.model.gen.errors.ViolationError;
import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 *
 */
public class RetrieveException extends CLSException {
	private static final long serialVersionUID = -1869917577321484573L;

	private final ViolationError error;

	public RetrieveException(final ConditionProcessingException e) {
		super(e.getMessage(), e);
		this.error = e.getError();
	}

	public RetrieveException(final String message, final String value) {
		super(message);
		this.error = new ViolationError().withMessage(message).withValue(value);
	}

	// public RetrieveException(final String message, final SQLException e) {
	// super(message, e);
	// this.error = new ViolationError().withMessage(message).withValue(e.getMessage());
	// }

	public ViolationError getError() {
		return this.error;
	}
}