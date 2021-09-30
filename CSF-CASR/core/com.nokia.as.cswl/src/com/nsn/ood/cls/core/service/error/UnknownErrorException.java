package com.nsn.ood.cls.core.service.error;

import com.nsn.ood.cls.util.exception.CLSException;


public final class UnknownErrorException extends CLSException {

	public UnknownErrorException(final String message, final Throwable e) {
		super(message, e);
	}

	public UnknownErrorException(final String message) {
		super(message);
	}

	private static final long serialVersionUID = -2635292825721034509L;

}
