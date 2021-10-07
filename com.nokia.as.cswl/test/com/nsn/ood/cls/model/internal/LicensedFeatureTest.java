/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.assertLicensedFeature;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class LicensedFeatureTest {

	@Test
	public void testEmptyLicensedFeature() throws Exception {
		assertLicensedFeature(new LicensedFeature(), null, null, null, null, null, null, null);
	}

	@Test
	public void testLicensedFeature() throws Exception {
		assertLicensedFeature(new LicensedFeature().withFeatureCode(12L), 12L, null, null, null, null, null, null);
		assertLicensedFeature(new LicensedFeature().withFeatureName("name"), null, "name", null, null, null, null, null);
		assertLicensedFeature(new LicensedFeature().withCapacityUnit("unit"), null, null, "unit", null, null, null,
				null);
		assertLicensedFeature(new LicensedFeature().withTargetType("type"), null, null, null, "type", null, null, null);
		assertLicensedFeature(new LicensedFeature().withTotalCapacity(13L), null, null, null, null, 13L, null, null);
		assertLicensedFeature(new LicensedFeature().withUsedCapacity(14L), null, null, null, null, null, 14L, null);
		assertLicensedFeature(new LicensedFeature().withRemainingCapacity(15L), null, null, null, null, null, null, 15L);

		assertLicensedFeature(new LicensedFeature().withFeatureCode(71L).withFeatureName("nnn").withCapacityUnit("uuu")
				.withTargetType("ttt").withTotalCapacity(82L).withUsedCapacity(63L).withRemainingCapacity(76L), 71L,
				"nnn", "uuu", "ttt", 82L, 63L, 76L);
	}

	@Test
	public void testLicensedFeatureSetters() throws Exception {
		final LicensedFeature licensedFeature = new LicensedFeature();
		licensedFeature.setFeatureCode(21L);
		licensedFeature.setFeatureName("aaa");
		licensedFeature.setCapacityUnit("bbb");
		licensedFeature.setTargetType("ccc");
		licensedFeature.setTotalCapacity(33L);
		licensedFeature.setUsedCapacity(44L);
		licensedFeature.setRemainingCapacity(55L);

		assertLicensedFeature(licensedFeature, 21L, "aaa", "bbb", "ccc", 33L, 44L, 55L);
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(LicensedFeature.class, "featureCode", "featureName", "capacityUnit", "targetType",
				"totalCapacity", "usedCapacity", "remainingCapacity");
		assertJsonProperty(LicensedFeature.class, "featureCode", "featureCode");
		assertJsonProperty(LicensedFeature.class, "featureName", "featureName");
		assertJsonProperty(LicensedFeature.class, "capacityUnit", "capacityUnit");
		assertJsonProperty(LicensedFeature.class, "targetType", "targetType");
		assertJsonProperty(LicensedFeature.class, "totalCapacity", "totalCapacity");
		assertJsonProperty(LicensedFeature.class, "usedCapacity", "usedCapacity");
		assertJsonProperty(LicensedFeature.class, "remainingCapacity", "remainingCapacity");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new LicensedFeature().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final LicensedFeature feature = new LicensedFeature().withFeatureCode(71L);

		assertFalse(feature.equals(null));
		assertFalse(feature.equals("test"));
		assertEquals(feature, feature);

		assertFalse(feature.equals(new LicensedFeature()));
		assertNotEquals(feature.hashCode(), new LicensedFeature().hashCode());

		final LicensedFeature feature2 = new LicensedFeature().withFeatureCode(71L);
		assertEquals(feature, feature2);
		assertEquals(feature.hashCode(), feature2.hashCode());
	}
}
