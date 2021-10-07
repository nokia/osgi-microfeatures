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
import com.nsn.ood.cls.core.db.creator.FeatureCreator;
import com.nsn.ood.cls.core.db.feature.QueryFeatures;
import com.nsn.ood.cls.core.db.mapper.FeatureConditionsMapper;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	FeatureRetrieveOperation.class })
public class FeatureRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final FeatureConditionsMapper mapperMock = createMock(FeatureConditionsMapper.class);
		final FeatureCreator creatorMock = createMock(FeatureCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryFeatures queryFeaturesMock = createMockAndExpectNew(QueryFeatures.class, conditionsMock, mapperMock,
				creatorMock);

		replayAll();
		final FeatureRetrieveOperation operation = new FeatureRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryFeaturesMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
