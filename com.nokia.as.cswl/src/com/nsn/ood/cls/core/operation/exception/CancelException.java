/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import com.nokia.licensing.interfaces.LicenseException;


/**
 * @author marynows
 *
 */
public class CancelException extends InstallException {
	private static final long serialVersionUID = 6369357341315035287L;

	public CancelException(final LicenseException e) {
		super(e);
	}

	public CancelException(final Throwable cause) {
		super(cause);
	}
}