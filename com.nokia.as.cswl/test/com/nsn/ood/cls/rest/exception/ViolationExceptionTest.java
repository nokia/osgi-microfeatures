/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.exception;

import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ViolationExceptionTest {

	@Test
	public void testViolationException() throws Exception {
		final ViolationException exception = new ViolationException("message", "path", "value");
		assertEquals("message", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(violationError("message", "path", "value"), exception.getError());
	}
}
