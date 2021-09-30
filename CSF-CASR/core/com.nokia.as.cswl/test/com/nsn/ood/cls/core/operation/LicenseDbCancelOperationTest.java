/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.feature.DeleteFeature;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.DeleteLicense;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.model.gen.licenses.License;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	LicenseDbCancelOperation.class })
public class LicenseDbCancelOperationTest {
	private static final License LICENSE = license("123").withFeatures(featuresList(feature(1234L, null)));

	@Test
	public void testCancel() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		final DeleteLicense deleteLicenseMock = createMockAndExpectNew(DeleteLicense.class, "123");
		updateExecutorMock.execute(deleteLicenseMock);
		final UpdateFeatureCapacity updateFeatureForLicenseMock = createMockAndExpectNew(UpdateFeatureCapacity.class,
				1234L);
		updateExecutorMock.execute(updateFeatureForLicenseMock);
		final DeleteFeature deleteFeatureMock = createMockAndExpectNew(DeleteFeature.class, 1234L);
		updateExecutorMock.execute(deleteFeatureMock);

		replayAll();
		final LicenseDbCancelOperation operation = new LicenseDbCancelOperation();
		setInternalState(operation, updateExecutorMock);
		operation.cancel(LICENSE);
		verifyAll();
	}

	@Test
	public void testCancelwithExceptionDuringDeleteFeature() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(DeleteLicense.class));
		updateExecutorMock.execute(isA(UpdateFeatureCapacity.class));
		updateExecutorMock.execute(isA(DeleteFeature.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final LicenseDbCancelOperation operation = new LicenseDbCancelOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.cancel(LICENSE);
			fail();
		} catch (final CancelException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.DB, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
		verifyAll();
	}

	@Test
	public void testCancelWithExceptionDuringUpdate() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(DeleteLicense.class));
		updateExecutorMock.execute(isA(UpdateFeatureCapacity.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final LicenseDbCancelOperation operation = new LicenseDbCancelOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.cancel(LICENSE);
			fail();
		} catch (final CancelException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.DB, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
		verifyAll();
	}

	@Test
	public void testCancelWithExceptionDuringDeleteLicense() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(DeleteLicense.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		final LicenseDbCancelOperation operation = new LicenseDbCancelOperation();
		setInternalState(operation, updateExecutorMock);
		try {
			operation.cancel(LICENSE);
			fail();
		} catch (final CancelException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.DB, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
		verifyAll();
	}
}
