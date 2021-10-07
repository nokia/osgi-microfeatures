/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;


/**
 * @author marynows
 *
 */
public class InstallExceptionTest {

	@Test
	public void testForCLJL() throws Exception {
		final LicenseException cause = new LicenseException("code", "message");
		final InstallException exception = new InstallException(cause);
		assertEquals("code: message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(LicenseErrorType.CLJL, exception.getErrorType());
		assertEquals("code", exception.getCljlErrorCode());
	}

	@Test
	public void testForVerification() throws Exception {
		final Exception cause = new Exception("message");
		final InstallException exception = new InstallException("MMM", cause);
		assertEquals("MMM", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(LicenseErrorType.VERIFICATION, exception.getErrorType());
		assertNull(exception.getCljlErrorCode());
	}

	@Test
	public void testForDB() throws Exception {
		final Exception cause = new Exception("message");
		final InstallException exception = new InstallException(cause);
		assertEquals("message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(LicenseErrorType.DB, exception.getErrorType());
		assertNull(exception.getCljlErrorCode());
	}
}
