/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 * 
 */
public class ServiceException extends CLSException {
	private static final long serialVersionUID = -7081568882988153724L;

	private final List<ErrorException> exceptions;
	private final boolean notFound;

	ServiceException(final ErrorException exception, final boolean notFound) {
		super();
		this.exceptions = Arrays.asList(exception);
		this.notFound = notFound;
	}

	ServiceException(final List<ErrorException> exceptions) {
		super();
		this.exceptions = exceptions;
		this.notFound = false;
	}

	public List<ErrorException> getExceptions() {
		return this.exceptions;
	}

	public boolean isNotFound() {
		return this.notFound;
	}
}
