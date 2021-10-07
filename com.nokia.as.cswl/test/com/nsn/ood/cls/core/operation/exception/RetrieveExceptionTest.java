/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.exception;

import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.nsn.ood.cls.core.db.util.ConditionProcessingException;


/**
 * @author marynows
 * 
 */
public class RetrieveExceptionTest {

	@Test
	public void testExceptionForConditionProcessingException() throws Exception {
		final ConditionProcessingException e = new ConditionProcessingException("message1", "field", "value",
				new Exception("message2"));

		final RetrieveException exception = new RetrieveException(e);

		assertEquals("message1", exception.getMessage());
		assertEquals(e, exception.getCause());
		assertEquals(violationError("message2", "field", "value"), exception.getError());
	}

	@Test
	public void testException() throws Exception {
		final RetrieveException exception = new RetrieveException("message", "value");

		assertEquals("message", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(violationError("message", null, "value"), exception.getError());
	}
}
