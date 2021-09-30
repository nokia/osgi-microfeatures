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
public class ActivityConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final ActivityConditionsMapper mapper = new ActivityConditionsMapper();
		mapper.start();

		assertMapperColumn(mapper, "id", "id");
		assertMapperColumn(mapper, "clientId", "clientid");
		assertMapperColumn(mapper, "activityTime", "activitytime");
		assertMapperColumn(mapper, "operationType", "operationtype");
		assertMapperColumn(mapper, "result", "result");
	}
}
