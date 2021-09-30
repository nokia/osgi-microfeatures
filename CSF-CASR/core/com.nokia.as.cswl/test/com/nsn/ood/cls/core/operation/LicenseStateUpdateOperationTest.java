/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.license.UpdateLicensesStateToActive;
import com.nsn.ood.cls.core.db.license.UpdateLicensesStateToExpired;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	LicenseStateUpdateOperation.class })
public class LicenseStateUpdateOperationTest {
	private static final DateTime DATE = new DateTime(2015, 10, 29, 11, 1);

	@Test
	public void testUpdateState() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		final UpdateLicensesStateToActive updateLicensesStateToActiveMock = createMockAndExpectNew(
				UpdateLicensesStateToActive.class, DATE, converterMock);
		updateExecutorMock.execute(updateLicensesStateToActiveMock);
		final UpdateLicensesStateToExpired updateLicensesStateToExpiredMock = createMockAndExpectNew(
				UpdateLicensesStateToExpired.class, DATE, converterMock);
		updateExecutorMock.execute(updateLicensesStateToExpiredMock);

		replayAll();
		final LicenseStateUpdateOperation operation = new LicenseStateUpdateOperation();
		setInternalState(operation, updateExecutorMock, converterMock);
		operation.updateState(DATE);
		verifyAll();
	}

	@Test
	public void testUpdateStateWithExceptionDuringStateChangeToActive() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateLicensesStateToActive.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final LicenseStateUpdateOperation operation = new LicenseStateUpdateOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.updateState(DATE);
			fail();
		} catch (final UpdateException e) {
			assertNotNull(e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testUpdateStateWithExceptionDuringStateChangeToExpired() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateLicensesStateToActive.class));
		updateExecutorMock.execute(isA(UpdateLicensesStateToExpired.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final LicenseStateUpdateOperation operation = new LicenseStateUpdateOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.updateState(DATE);
			fail();
		} catch (final UpdateException e) {
			assertNotNull(e.getMessage());
		}
		verifyAll();
	}
}
