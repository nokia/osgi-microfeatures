/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.errors.FeatureError;


/**
 * @author marynows
 * 
 */
public class FeatureErrorTestUtil {

	public static FeatureError featureError(final Long featureCode) {
		return new FeatureError().withFeatureCode(featureCode);
	}

	public static FeatureError featureError(final Long featureCode, final Long capacity) {
		return featureError(featureCode).withCapacity(capacity);
	}

	public static FeatureError featureError(final Long featureCode, final Long requestedCapacity,
			final Long remainingCapacity) {
		return featureError(featureCode).withRequestedCapacity(requestedCapacity).withRemainingCapacity(
				remainingCapacity);
	}

	public static List<FeatureError> featureErrorsList(final FeatureError... featureErrors) {
		return Arrays.asList(featureErrors);
	}

	public static void assertFeatureError(final FeatureError featureError, final Long expectedFeatureCode,
			final Long expectedRequestedCapacity, final Long expectedRemainingCapacity, final Long expectedCapacity) {
		assertEquals(expectedFeatureCode, featureError.getFeatureCode());
		assertEquals(expectedRequestedCapacity, featureError.getRequestedCapacity());
		assertEquals(expectedRemainingCapacity, featureError.getRemainingCapacity());
		assertEquals(expectedCapacity, featureError.getCapacity());
	}
}
