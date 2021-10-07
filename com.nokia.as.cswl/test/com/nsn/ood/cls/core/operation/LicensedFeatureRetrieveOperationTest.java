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
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.core.db.feature.QueryLicensedFeatures;
import com.nsn.ood.cls.core.db.mapper.LicensedFeatureConditionsMapper;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	LicensedFeatureRetrieveOperation.class })
public class LicensedFeatureRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final LicensedFeatureConditionsMapper mapperMock = createMock(LicensedFeatureConditionsMapper.class);
		final LicensedFeatureCreator creatorMock = createMock(LicensedFeatureCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryLicensedFeatures queryLicensedFeaturesMock = createMockAndExpectNew(QueryLicensedFeatures.class,
				conditionsMock, mapperMock, creatorMock);

		replayAll();
		final LicensedFeatureRetrieveOperation operation = new LicensedFeatureRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryLicensedFeaturesMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
