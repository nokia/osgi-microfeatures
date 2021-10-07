/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
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
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.license.QueryLicenses;
import com.nsn.ood.cls.core.db.mapper.LicenseConditionsMapper;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	LicenseRetrieveOperation.class })
public class LicenseRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final LicenseConditionsMapper mapperMock = createMock(LicenseConditionsMapper.class);
		final LicenseCreator creatorMock = createMock(LicenseCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryLicenses queryLicensesMock = createMockAndExpectNew(QueryLicenses.class, conditionsMock, mapperMock,
				creatorMock);

		replayAll();
		final LicenseRetrieveOperation operation = new LicenseRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryLicensesMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
