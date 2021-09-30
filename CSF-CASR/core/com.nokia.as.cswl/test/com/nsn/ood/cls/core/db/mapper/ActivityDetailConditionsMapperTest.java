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
public class ActivityDetailConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final ActivityDetailConditionsMapper mapper = new ActivityDetailConditionsMapper();
		mapper.start();

		assertMapperColumn(mapper, "activityId", "activityid");
		assertMapperColumn(mapper, "fileName", "filename");
		assertMapperColumn(mapper, "errorCode", "errorcode");
		assertMapperColumn(mapper, "status", "status");
		assertMapperColumn(mapper, "featureName", "featurename");
		assertMapperColumn(mapper, "featureCode", "featurecode");
		assertMapperColumn(mapper, "settingKey", "settingkey");
		assertMapperColumn(mapper, "settingValue", "settingvalue");
	}
}
