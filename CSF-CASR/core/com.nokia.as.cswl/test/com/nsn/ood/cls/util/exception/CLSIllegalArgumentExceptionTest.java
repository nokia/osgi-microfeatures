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
public class CLSIllegalArgumentExceptionTest {

	@Test
	public void testCLSIllegalArgumentException() throws Exception {
		final Exception cause = new Exception("eee");

		assertException(new CLSIllegalArgumentException("mmm"), "mmm", null);
		assertException(new CLSIllegalArgumentException("mmm", cause), "mmm", cause);
	}
}
