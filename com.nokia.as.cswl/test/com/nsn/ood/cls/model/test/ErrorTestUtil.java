/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.errors.Error;


/**
 * @author marynows
 * 
 */
public class ErrorTestUtil {

	public static List<Error> errorsList(final Error... errors) {
		return Arrays.asList(errors);
	}

	public static Error error() {
		return new Error();
	}

	public static Error error(final Long errorCode) {
		return error().withErrorCode(errorCode);
	}
}
