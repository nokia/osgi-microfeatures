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
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.license.QueryStoredLicenses;
import com.nsn.ood.cls.core.db.mapper.StoredLicenseConditionsMapper;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	StoredLicenseRetrieveOperation.class })
public class StoredLicenseRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final StoredLicenseConditionsMapper mapperMock = createMock(StoredLicenseConditionsMapper.class);
		final LicenseCreator creatorMock = createMock(LicenseCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryStoredLicenses queryStoredLicensesMock = createMockAndExpectNew(QueryStoredLicenses.class,
				conditionsMock, mapperMock, creatorMock);

		replayAll();
		final StoredLicenseRetrieveOperation operation = new StoredLicenseRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryStoredLicensesMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
