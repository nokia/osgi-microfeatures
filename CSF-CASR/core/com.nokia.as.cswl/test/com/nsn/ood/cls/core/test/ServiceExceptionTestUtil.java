/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import static org.junit.Assert.assertEquals;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;


/**
 * @author marynows
 * 
 */
public class ServiceExceptionTestUtil {

	public static void assertServiceException(final ServiceException e, final int errorsCount) {
		assertEquals(errorsCount, e.getExceptions().size());
	}

	public static void assertErrorException(final ServiceException e, final int index, final ErrorCode errorCode,
			final String message, final Throwable cause, final String name, final Object value) {
		final ErrorException errorException = e.getExceptions().get(index);
		assertEquals(errorCode, errorException.getErrorCode());
		assertEquals(message, errorException.getMessage());
		assertEquals(cause, errorException.getCause());
		assertEquals(name, errorException.getName());
		assertEquals(value, errorException.getValue());
	}
}
