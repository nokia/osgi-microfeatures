/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.errors.ViolationError;


/**
 * @author marynows
 * 
 */
public class ViolationErrorTestUtil {

	public static List<ViolationError> violationErrorsList(final ViolationError... violationErrors) {
		return Arrays.asList(violationErrors);
	}

	public static ViolationError violationError(final String message) {
		return new ViolationError().withMessage(message);
	}

	public static ViolationError violationError(final String message, final String path, final String value) {
		return violationError(message).withPath(path).withValue(value);
	}
}
