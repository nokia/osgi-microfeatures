/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ErrorExceptionTest {

	@Test
	public void testCreateForException() throws Exception {
		final Exception cause = new Exception("message");
		final ErrorException errorException = new ErrorException(ErrorCode.NOT_ENOUGH_CAPACITY, cause, "name", "value");
		assertErrorException(errorException, "message", cause, ErrorCode.NOT_ENOUGH_CAPACITY, "name", "value", null);
	}

	@Test
	public void testCreateForExceptionWithInfo() throws Exception {
		final Exception cause = new Exception("message");
		final ErrorException errorException = new ErrorException(ErrorCode.NOT_ENOUGH_CAPACITY, cause, "name", "value",
				"info");
		assertErrorException(errorException, "message", cause, ErrorCode.NOT_ENOUGH_CAPACITY, "name", "value", "info");
	}

	@Test
	public void testCreateForMessage() throws Exception {
		final ErrorException errorException = new ErrorException(ErrorCode.CONDITIONS_FAIL, "message", "name", 23L);
		assertErrorException(errorException, "message", null, ErrorCode.CONDITIONS_FAIL, "name", 23L, null);
	}

	public static void assertErrorException(final ErrorException exception, final String expectedMessage,
			final Throwable expectedCause, final ErrorCode expectedErrorCode, final String expectedName,
			final Object expectedValue, final String expectedInfo) {
		assertEquals(expectedMessage, exception.getMessage());
		assertEquals(expectedCause, exception.getCause());
		assertEquals(expectedErrorCode, exception.getErrorCode());
		assertEquals(expectedName, exception.getName());
		assertEquals(expectedValue, exception.getValue());
		assertEquals(expectedInfo, exception.getInfo());
	}
}
