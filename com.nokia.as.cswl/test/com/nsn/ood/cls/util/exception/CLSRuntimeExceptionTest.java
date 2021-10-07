/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.exception;

import static com.nsn.ood.cls.util.test.ExceptionTestUtil.assertException;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class CLSRuntimeExceptionTest {

	@Test
	public void testCLSRuntimeException() throws Exception {
		final Exception cause = new Exception("eee");

		assertException(new CLSRuntimeException(), null, null);
		assertException(new CLSRuntimeException("mmm"), "mmm", null);
		assertException(new CLSRuntimeException(cause), "java.lang.Exception: eee", cause);
		assertException(new CLSRuntimeException("mmm", cause), "mmm", cause);
	}
}
