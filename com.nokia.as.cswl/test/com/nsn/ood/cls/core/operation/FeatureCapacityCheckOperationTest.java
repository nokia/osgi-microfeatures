/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeaturesList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.core.db.feature.QueryLicensedFeaturesForCapacityCheck;
import com.nsn.ood.cls.core.db.feature.UpdateLicensedFeaturesForCapacityCheck;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	FeatureCapacityCheckOperation.class })
public class FeatureCapacityCheckOperationTest {
	private static final DateTime CHECK_TIME = new DateTime(2015, 8, 26, 16, 17);

	private UpdateExecutor updateExecutorMock;
	private QueryExecutor queryExecutorMock;
	private LicensedFeatureCreator licensedFeatureCreatorMock;
	private Converter<Timestamp, DateTime> converterMock;
	private FeatureCapacityCheckOperation operation;

	@Before
	public void setUp() throws Exception {
		this.updateExecutorMock = createMock(UpdateExecutor.class);
		this.queryExecutorMock = createMock(QueryExecutor.class);
		this.licensedFeatureCreatorMock = createMock(LicensedFeatureCreator.class);
		this.converterMock = createMock(Timestamp2DateTimeConverter.class);

		this.operation = new FeatureCapacityCheckOperation();
		setInternalState(this.operation, this.licensedFeatureCreatorMock, this.converterMock);
		setInternalState(this.operation, "updateExecutor", this.updateExecutorMock);
		setInternalState(this.operation, "queryExecutor", this.queryExecutorMock);
	}

	@Test
	public void testRetrieve() throws Exception {
		final QueryLicensedFeaturesForCapacityCheck queryMock = createMockAndExpectNew(
				QueryLicensedFeaturesForCapacityCheck.class, 77L, CHECK_TIME.withTimeAtStartOfDay(),
				this.licensedFeatureCreatorMock, this.converterMock);
		this.queryExecutorMock.execute(queryMock);
		expect(queryMock.getList()).andReturn(licensedFeaturesList(licensedFeature(1L)));

		replayAll();
		assertEquals(licensedFeaturesList(licensedFeature(1L)), this.operation.retrieve(77L, CHECK_TIME));
		verifyAll();
	}

	@Test
	public void testRetrieveWithException() throws Exception {
		final QueryLicensedFeaturesForCapacityCheck queryMock = createMockAndExpectNew(
				QueryLicensedFeaturesForCapacityCheck.class, 77L, CHECK_TIME.withTimeAtStartOfDay(),
				this.licensedFeatureCreatorMock, this.converterMock);
		this.queryExecutorMock.execute(queryMock);
		expectLastCall().andThrow(new SQLException("message"));
		expect(queryMock.getList()).andReturn(licensedFeaturesList());

		replayAll();
		assertEquals(licensedFeaturesList(), this.operation.retrieve(77L, CHECK_TIME));
		verifyAll();
	}

	@Test
	public void testUpdate() throws Exception {
		final List<LicensedFeature> features = licensedFeaturesList(licensedFeature(1L), licensedFeature(2L));

		final UpdateLicensedFeaturesForCapacityCheck updateMock = createMockAndExpectNew(
				UpdateLicensedFeaturesForCapacityCheck.class, features, CHECK_TIME, this.converterMock);
		this.updateExecutorMock.execute(updateMock);

		replayAll();
		this.operation.update(features, CHECK_TIME);
		verifyAll();
	}

	@Test
	public void testUpdateWithException() throws Exception {
		final List<LicensedFeature> features = licensedFeaturesList(licensedFeature(1L), licensedFeature(2L));

		final UpdateLicensedFeaturesForCapacityCheck updateMock = createMockAndExpectNew(
				UpdateLicensedFeaturesForCapacityCheck.class, features, CHECK_TIME, this.converterMock);
		this.updateExecutorMock.execute(updateMock);
		expectLastCall().andThrow(new SQLException("message"));
		expect(updateMock.getIndex()).andReturn(3);

		replayAll();
		try {
			this.operation.update(features, CHECK_TIME);
			fail();
		} catch (final UpdateException e) {
			assertEquals("message", e.getMessage());
			assertEquals(3, e.getIndex());
		}
		verifyAll();
	}
}
