/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ConditionProcessingExceptionTest {

	@Test
	public void testException() throws Exception {
		final Exception cause = new Exception("message2");

		final ConditionProcessingException exception = new ConditionProcessingException("message1", "field", "value",
				cause);

		assertEquals("message1", exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(violationError("message2", "field", "value"), exception.getError());
	}
}
