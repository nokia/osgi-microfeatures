/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 * 
 */
public class UpdateException extends CLSException {
	private static final long serialVersionUID = -181526150459868158L;

	private final int index;

	public UpdateException(final Throwable cause) {
		super(cause.getMessage(), cause);
		this.index = 0;
	}

	public UpdateException(final Throwable cause, final int index) {
		super(cause.getMessage(), cause);
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}
}