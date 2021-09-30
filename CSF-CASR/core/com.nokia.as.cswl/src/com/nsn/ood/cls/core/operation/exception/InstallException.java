/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.util.exception.CLSException;


/**
 * @author marynows
 *
 */
public class InstallException extends CLSException {
	private static final long serialVersionUID = 1695327423631912435L;

	private final LicenseErrorType errorType;
	private final String cljlErrorCode;

	public InstallException(final LicenseException e) {
		super(e.getErrorCode() + ": " + e.getMessage(), e);
		this.errorType = LicenseErrorType.CLJL;
		this.cljlErrorCode = e.getErrorCode();
	}

	public InstallException(final String message, final Throwable cause) {
		super(message, cause);
		this.errorType = LicenseErrorType.VERIFICATION;
		this.cljlErrorCode = null;
	}

	public InstallException(final Throwable cause) {
		super(cause.getMessage(), cause);
		this.errorType = LicenseErrorType.DB;
		this.cljlErrorCode = null;
	}

	public LicenseErrorType getErrorType() {
		return this.errorType;
	}

	public String getCljlErrorCode() {
		return this.cljlErrorCode;
	}
}