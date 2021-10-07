/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.test;

import static org.junit.Assert.assertEquals;


/**
 * @author marynows
 * 
 */
public class ExceptionTestUtil {

	public static void assertException(final Exception e, final String expectedMessage, final Throwable expectedCause) {
		assertEquals(expectedMessage, e.getMessage());
		assertEquals(expectedCause, e.getCause());
	}
}
