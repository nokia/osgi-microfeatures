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
public class CreateExceptionTest {

	@Test
	public void testException() throws Exception {
		final Exception cause = new Exception("message");

		final CreateException exception = new CreateException(cause);

		assertEquals("message", exception.getMessage());
		assertEquals(cause, exception.getCause());
	}
}
