/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class UpdateExceptionTest {

	@Test
	public void testException() throws Exception {
		final Exception cause = new Exception("message");

		final UpdateException exception = new UpdateException(cause);

		assertEquals("message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(0, exception.getIndex());
	}

	@Test
	public void testExceptionwithIndex() throws Exception {
		final Exception cause = new Exception("message");

		final UpdateException exception = new UpdateException(cause, 7);

		assertEquals("message", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(7, exception.getIndex());
	}
}
