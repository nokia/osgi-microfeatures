/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.creator.SettingCreator;
import com.nsn.ood.cls.core.db.mapper.SettingConditionsMapper;
import com.nsn.ood.cls.core.db.setting.QuerySettings;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	SettingRetrieveOperation.class })
public class SettingRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final SettingConditionsMapper mapperMock = createMock(SettingConditionsMapper.class);
		final SettingCreator creatorMock = createMock(SettingCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QuerySettings querySettingsMock = createMockAndExpectNew(QuerySettings.class, conditionsMock, mapperMock,
				creatorMock);

		replayAll();
		final SettingRetrieveOperation operation = new SettingRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(querySettingsMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
