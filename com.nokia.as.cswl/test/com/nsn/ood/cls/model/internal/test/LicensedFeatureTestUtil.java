/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.internal.LicensedFeature;


/**
 * @author marynows
 * 
 */
public class LicensedFeatureTestUtil {

	public static List<LicensedFeature> licensedFeaturesList(final LicensedFeature... licensedFeatures) {
		return Arrays.asList(licensedFeatures);
	}

	public static LicensedFeature licensedFeature() {
		return new LicensedFeature();
	}

	public static LicensedFeature licensedFeature(final Long featureCode) {
		return licensedFeature().withFeatureCode(featureCode);
	}

	public static void assertLicensedFeature(final LicensedFeature licensedFeature, final Long expectedFeatureCode,
			final String expectedFeatureName, final String expectedCapacityUnit, final String expectedTargetType,
			final Long expectedTotalCapacity, final Long expectedUsedCapacity, final Long expectedRemainingCapacity) {
		assertEquals(expectedFeatureCode, licensedFeature.getFeatureCode());
		assertEquals(expectedFeatureName, licensedFeature.getFeatureName());
		assertEquals(expectedCapacityUnit, licensedFeature.getCapacityUnit());
		assertEquals(expectedTargetType, licensedFeature.getTargetType());
		assertEquals(expectedTotalCapacity, licensedFeature.getTotalCapacity());
		assertEquals(expectedUsedCapacity, licensedFeature.getUsedCapacity());
		assertEquals(expectedRemainingCapacity, licensedFeature.getRemainingCapacity());
	}

	public static void assertLicensedFeaturesList(final List<LicensedFeature> licensedFeatures,
			final LicensedFeature... expectedLicensedFeatures) {
		assertEquals(Arrays.asList(expectedLicensedFeatures), licensedFeatures);
	}
}
