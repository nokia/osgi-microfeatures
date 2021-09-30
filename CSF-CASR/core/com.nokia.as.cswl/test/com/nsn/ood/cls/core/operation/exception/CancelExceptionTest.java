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
public class CancelExceptionTest {

	@Test
	public void testForCLJL() throws Exception {
		final LicenseException cause = new LicenseException("code", "message");
		final CancelException exception = new CancelException(cause);
		assertEquals("code: message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(LicenseErrorType.CLJL, exception.getErrorType());
		assertEquals("code", exception.getCljlErrorCode());
	}

	@Test
	public void testForDB() throws Exception {
		final Exception cause = new Exception("message");
		final CancelException exception = new CancelException(cause);
		assertEquals("message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(LicenseErrorType.DB, exception.getErrorType());
		assertNull(exception.getCljlErrorCode());
	}
}
