/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.exception;

import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author wro50095
 * 
 */
public class CLSCertificateException extends CLSException {
	/**  */
	private static final long serialVersionUID = 3719585310088809081L;

	public CLSCertificateException() {
		super();
	}

	public CLSCertificateException(final String message, final Throwable e) {
		super(message, e);
	}
}
