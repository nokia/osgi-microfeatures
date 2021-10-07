/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class LicensedFeatureConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final LicensedFeatureConditionsMapper mapper = new LicensedFeatureConditionsMapper();
		mapper.start();

		assertMapperColumn(mapper, "featureCode", "featurecode");
		assertMapperColumn(mapper, "featureName", "featurename");
		assertMapperColumn(mapper, "capacityUnit", "capacityunit");
		assertMapperColumn(mapper, "targetType", "targettype");
		assertMapperColumn(mapper, "totalCapacity", "total");
		assertMapperColumn(mapper, "usedCapacity", "used");
		assertMapperColumn(mapper, "remainingCapacity", "remaining");
	}
}
