/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.expect;
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
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseState2StringConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.feature.InsertFeature;
import com.nsn.ood.cls.core.db.feature.QueryFeatureExist;
import com.nsn.ood.cls.core.db.feature.UpdateFeatureCapacity;
import com.nsn.ood.cls.core.db.license.InsertLicense;
import com.nsn.ood.cls.core.db.license.QueryLicenseExist;
import com.nsn.ood.cls.core.model.LicenseState;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	LicenseDbInstallOperation.class })
public class LicenseDbInstallOperationTest {
	private static final String SERIAL_NUMBER = "123";
	private static final long FEATURE_CODE = 202L;
	private static final License LICENSE = license(SERIAL_NUMBER).withFeatures(
			featuresList(feature(FEATURE_CODE, null)));

	private QueryExecutor queryExecutorMock;
	private UpdateExecutor updateExecutorMock;
	private Converter<Timestamp, DateTime> timestamp2DatetimeConverter;
	private Converter<LicenseState, String> licenseState2StringConverter;
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter;
	private Converter<License.Type, Integer> licenseType2IntegerConverter;
	private LicenseDbInstallOperation operation;

	@Before
	public void setUp() throws Exception {
		this.queryExecutorMock = createMock(QueryExecutor.class);
		this.updateExecutorMock = createMock(UpdateExecutor.class);
		this.timestamp2DatetimeConverter = createMock(Timestamp2DateTimeConverter.class);
		this.licenseState2StringConverter = createMock(LicenseState2StringConverter.class);
		this.licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
		this.licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
		this.operation = new LicenseDbInstallOperation();
		setInternalState(this.operation, "queryExecutor", this.queryExecutorMock);
		setInternalState(this.operation, "updateExecutor", this.updateExecutorMock);
		setInternalState(this.operation, "timestamp2DatetimeConverter", this.timestamp2DatetimeConverter);
		setInternalState(this.operation, "licenseState2StringConverter", this.licenseState2StringConverter);
		setInternalState(this.operation, "licenseMode2IntegerConverter", this.licenseMode2IntegerConverter);
		setInternalState(this.operation, "licenseType2IntegerConverter", this.licenseType2IntegerConverter);
	}

	@Test
	public void testInstallWhenNoFeatureAndNoLicense() throws Exception {
		mockIsFeatureExist(false);
		mockIsLicenseExist(false);

		final InsertFeature insertFeatureMock = createMockAndExpectNew(InsertFeature.class, LICENSE);
		this.updateExecutorMock.execute(insertFeatureMock);

		final InsertLicense insertLicenseMock = createMockAndExpectNew(InsertLicense.class, LICENSE, this.timestamp2DatetimeConverter, this.licenseMode2IntegerConverter,
												this.licenseType2IntegerConverter, this.licenseState2StringConverter);
		this.updateExecutorMock.execute(insertLicenseMock);

		final UpdateFeatureCapacity updateFeatureForLicenseMock = createMockAndExpectNew(UpdateFeatureCapacity.class,
				FEATURE_CODE);
		this.updateExecutorMock.execute(updateFeatureForLicenseMock);

		replayAll();
		this.operation.install(LICENSE);
		verifyAll();
	}

	@Test
	public void testInstallWhenFeatureAndNoLicense() throws Exception {
		mockIsFeatureExist(true);
		mockIsLicenseExist(false);

		final InsertLicense insertLicenseMock = createMockAndExpectNew(InsertLicense.class, LICENSE, this.timestamp2DatetimeConverter, this.licenseMode2IntegerConverter,
												this.licenseType2IntegerConverter, this.licenseState2StringConverter);
		this.updateExecutorMock.execute(insertLicenseMock);

		final UpdateFeatureCapacity updateFeatureForLicenseMock = createMockAndExpectNew(UpdateFeatureCapacity.class,
				FEATURE_CODE);
		this.updateExecutorMock.execute(updateFeatureForLicenseMock);

		replayAll();
		this.operation.install(LICENSE);
		verifyAll();
	}

	@Test
	public void testInstallWhenNoFeatureAndLicense() throws Exception {
		mockIsFeatureExist(false);
		mockIsLicenseExist(true);

		final InsertFeature insertFeatureMock = createMockAndExpectNew(InsertFeature.class, LICENSE);
		this.updateExecutorMock.execute(insertFeatureMock);

		final UpdateFeatureCapacity updateFeatureForLicenseMock = createMockAndExpectNew(UpdateFeatureCapacity.class,
				FEATURE_CODE);
		this.updateExecutorMock.execute(updateFeatureForLicenseMock);

		replayAll();
		this.operation.install(LICENSE);
		verifyAll();
	}

	@Test
	public void testInstallWhenFeatureAndLicense() throws Exception {
		mockIsFeatureExist(true);
		mockIsLicenseExist(true);

		replayAll();
		this.operation.install(LICENSE);
		verifyAll();
	}

	@Test
	public void testInstallWithExceptionDuringQuery() throws Exception {
		final QueryFeatureExist queryFeatureExistMock = createMockAndExpectNew(QueryFeatureExist.class, FEATURE_CODE);
		this.queryExecutorMock.execute(queryFeatureExistMock);
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		try {
			this.operation.install(LICENSE);
			fail();
		} catch (final InstallException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.DB, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
		verifyAll();
	}

	@Test
	public void testInstallWithExceptionDuringUpdate() throws Exception {
		mockIsFeatureExist(false);
		mockIsLicenseExist(false);

		final InsertFeature insertFeatureMock = createMockAndExpectNew(InsertFeature.class, LICENSE);
		this.updateExecutorMock.execute(insertFeatureMock);
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		try {
			this.operation.install(LICENSE);
			fail();
		} catch (final InstallException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.DB, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
		verifyAll();
	}

	private void mockIsFeatureExist(final boolean value) throws Exception {
		final QueryFeatureExist queryFeatureExistMock = createMockAndExpectNew(QueryFeatureExist.class, FEATURE_CODE);
		this.queryExecutorMock.execute(queryFeatureExistMock);
		expect(queryFeatureExistMock.getValue()).andReturn(value);
	}

	private void mockIsLicenseExist(final boolean value) throws Exception {
		final QueryLicenseExist queryLicenseExistMock = createMockAndExpectNew(QueryLicenseExist.class, SERIAL_NUMBER);
		this.queryExecutorMock.execute(queryLicenseExistMock);
		expect(queryLicenseExistMock.getValue()).andReturn(value);
	}
}
