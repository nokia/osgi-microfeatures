/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;


/**
 * @author marynows
 * 
 */
public class ErrorExceptionTestUtil {

	public static List<ErrorException> errorExceptionsList(final ErrorException... errorExceptions) {
		return Arrays.asList(errorExceptions);
	}

	public static ErrorException errorException() {
		return new ErrorExceptionFactory().error(null, new Exception());
	}

	public static ErrorException errorException(final ErrorCode errorCode) {
		return new ErrorExceptionFactory().error(errorCode, new Exception());
	}
}
