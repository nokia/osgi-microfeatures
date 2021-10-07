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
import com.nsn.ood.cls.core.db.activity.QueryActivityDetails;
import com.nsn.ood.cls.core.db.creator.ActivityCreator;
import com.nsn.ood.cls.core.db.mapper.ActivityDetailConditionsMapper;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	ActivityDetailRetrieveOperation.class })
public class ActivityDetailRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final ActivityDetailConditionsMapper mapperMock = createMock(ActivityDetailConditionsMapper.class);
		final ActivityCreator creatorMock = createMock(ActivityCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryActivityDetails queryActivityDetailsMock = createMockAndExpectNew(QueryActivityDetails.class,
				conditionsMock, mapperMock, creatorMock);

		replayAll();
		final ActivityDetailRetrieveOperation operation = new ActivityDetailRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryActivityDetailsMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
