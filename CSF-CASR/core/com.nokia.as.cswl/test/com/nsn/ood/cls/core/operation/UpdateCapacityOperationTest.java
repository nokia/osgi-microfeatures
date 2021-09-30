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
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.UpdateLicensesCapacity;
import com.nsn.ood.cls.core.operation.exception.UpdateException;


/**
 * @author marynows
 *
 */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		UpdateCapacityOperation.class })
public class UpdateCapacityOperationTest {

	@Test
	public void testUpdateCapacity() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		final UpdateFeatureCapacity updateFeatureCapacityMock = createMockAndExpectNew(UpdateFeatureCapacity.class,
				Arrays.asList(1L, 2L));
		updateExecutorMock.execute(updateFeatureCapacityMock);
		final UpdateLicensesCapacity updateLicensesCapacityMock = createMockAndExpectNew(UpdateLicensesCapacity.class,
				Arrays.asList(1L, 2L));
		updateExecutorMock.execute(updateLicensesCapacityMock);

		replayAll();
		final UpdateCapacityOperation operation = new UpdateCapacityOperation();
		setInternalState(operation, updateExecutorMock);
		operation.updateCapacity(Arrays.asList(1L, 2L));
		verifyAll();
	}

	@Test
	public void testUpdateCapacityWithExceptionDuringFeature() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateFeatureCapacity.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final UpdateCapacityOperation operation = new UpdateCapacityOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.updateCapacity(Arrays.asList(1L, 2L));
			fail();
		} catch (final UpdateException e) {
			assertNotNull(e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testUpdateCapacityWithExceptionDuringLicense() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateFeatureCapacity.class));
		updateExecutorMock.execute(isA(UpdateLicensesCapacity.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final UpdateCapacityOperation operation = new UpdateCapacityOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.updateCapacity(Arrays.asList(1L, 2L));
			fail();
		} catch (final UpdateException e) {
			assertNotNull(e.getMessage());
		}
		verifyAll();
	}
}
