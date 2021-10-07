/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.convert.Client2StringConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.license.QueryLicensesFeatureCodesForStateUpdate;
import com.nsn.ood.cls.core.db.reservation.LockReservations;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForClient;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForExpiredClients;
import com.nsn.ood.cls.core.db.reservation.QueryReservationsFeatureCodesForExpiredLicenses;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	FeatureLockOperation.class })
public class FeatureLockOperationTest {
	private static final DateTime DATE = new DateTime(2015, 8, 24, 16, 47);
	private FeatureLockOperation operation;
	private QueryExecutor queryExecutorMock;
	private Converter<Client, String> converterMock;
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private Converter<License.Type, Integer> licenseType2IntegerConverter;

	@Before
	public void setUp() throws Exception {
		this.queryExecutorMock = createMock(QueryExecutor.class);
		this.converterMock = createMock(Client2StringConverter.class);
		this.timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		this.licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);

		this.operation = new FeatureLockOperation();
		setInternalState(this.operation, "client2StringConverter", this.converterMock);
		setInternalState(this.operation, "timestamp2DateTimeConverter", this.timestamp2DateTimeConverter);
		setInternalState(this.operation, "licenseType2IntegerConverter", this.licenseType2IntegerConverter);
		setInternalState(this.operation, queryExecutorMock);
	}

	@Test
	public void testLockOne() throws Exception {
		final LockReservations queryMock = createMockAndExpectNew(LockReservations.class, Arrays.asList(1L));
		this.queryExecutorMock.execute(queryMock);

		replayAll();
		this.operation.lock(1L);
		verifyAll();
	}

	@Test
	public void testLock() throws Exception {
		final LockReservations queryMock = createMockAndExpectNew(LockReservations.class, Arrays.asList(1L, 2L));
		this.queryExecutorMock.execute(queryMock);

		replayAll();
		assertEquals(Arrays.asList(1L, 2L), this.operation.lock(Arrays.asList(1L, 2L)));
		verifyAll();
	}

	@Test
	public void testLockWhenNoFeatures() throws Exception {
		replayAll();
		assertTrue(this.operation.lock(Collections.<Long> emptyList()).isEmpty());
		verifyAll();
	}

	@Test
	public void testLockFeaturesWithExceptionDuringLock() throws Exception {
		this.queryExecutorMock.execute(isA(LockReservations.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		try {
			this.operation.lock(Arrays.asList(1L));
			fail();
		} catch (final LockException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testLockFeaturesForClient() throws Exception {
		expect(this.converterMock.convertTo(client("abc"))).andReturn("client");
		final QueryReservationsFeatureCodesForClient queryMock = createMockAndExpectNew(
				QueryReservationsFeatureCodesForClient.class, client("abc"));
		this.queryExecutorMock.execute(queryMock);
		expect(queryMock.getValues()).andReturn(Arrays.asList(2L, 3L));
		this.queryExecutorMock.execute(isA(LockReservations.class));

		replayAll();
		assertEquals(Arrays.asList(2L, 3L), this.operation.lockForClient(client("abc")));
		verifyAll();
	}

	@Test
	public void testLockFeaturesForClientWithException() throws Exception {
		expect(this.converterMock.convertTo(client("abc"))).andReturn("client");
		this.queryExecutorMock.execute(isA(QueryReservationsFeatureCodesForClient.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this.operation, "client2StringConverter", this.converterMock);
		setInternalState(this.operation, "timestamp2DateTimeConverter", this.timestamp2DateTimeConverter);
		setInternalState(this.operation, "licenseType2IntegerConverter", this.licenseType2IntegerConverter);
		setInternalState(this.operation, queryExecutorMock);
		try {
			this.operation.lockForClient(client("abc"));
			fail();
		} catch (final LockException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testLockForExpiredClients() throws Exception {
		final QueryReservationsFeatureCodesForExpiredClients queryMock = createMockAndExpectNew(
				QueryReservationsFeatureCodesForExpiredClients.class, DATE, this.timestamp2DateTimeConverter, this.licenseType2IntegerConverter);
		this.queryExecutorMock.execute(queryMock);
		expect(queryMock.getValues()).andReturn(Arrays.asList(3L, 4L));
		this.queryExecutorMock.execute(isA(LockReservations.class));

		replayAll();
		assertEquals(Arrays.asList(3L, 4L), this.operation.lockForExpiredClients(DATE));
		verifyAll();
	}

	@Test
	public void testLockForExpiredLicenses() throws Exception {
		final QueryReservationsFeatureCodesForExpiredLicenses queryMock = createMockAndExpectNew(
				QueryReservationsFeatureCodesForExpiredLicenses.class, DATE, this.timestamp2DateTimeConverter);
		this.queryExecutorMock.execute(queryMock);
		expect(queryMock.getValues()).andReturn(Arrays.asList(4L, 5L));
		this.queryExecutorMock.execute(isA(LockReservations.class));

		replayAll();
		assertEquals(Arrays.asList(4L, 5L), this.operation.lockForExpiredLicenses(DATE));
		verifyAll();
	}

	@Test
	public void testLockForLicensesState() throws Exception {
		final QueryLicensesFeatureCodesForStateUpdate queryMock = createMockAndExpectNew(
				QueryLicensesFeatureCodesForStateUpdate.class, DATE, this.timestamp2DateTimeConverter);
		this.queryExecutorMock.execute(queryMock);
		expect(queryMock.getValues()).andReturn(Arrays.asList(5L, 6L));
		this.queryExecutorMock.execute(isA(LockReservations.class));

		replayAll();
		assertEquals(Arrays.asList(5L, 6L), this.operation.lockForLicensesState(DATE));
		verifyAll();
	}
}
