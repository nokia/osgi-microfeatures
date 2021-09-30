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
public class CLSExceptionTest {

	@Test
	public void testCLSException() throws Exception {
		final Exception cause = new Exception("eee");

		assertException(new CLSException(), null, null);
		assertException(new CLSException("mmm"), "mmm", null);
		assertException(new CLSException(cause), "java.lang.Exception: eee", cause);
		assertException(new CLSException("mmm", cause), "mmm", cause);
	}
}
