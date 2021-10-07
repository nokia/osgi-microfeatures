/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;
import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumnValue;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SettingConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final SettingConditionsMapper mapper = new SettingConditionsMapper();
		mapper.start();

		assertMapperColumn(mapper, "key", "settingskey");
		assertMapperColumnValue(mapper, "key", "floatingReleaseTime", "FLOATING_RELEASE_TIME");
	}
}
