/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Reservation2StringConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.mapper.LicenseModeValueConverter;
import com.nsn.ood.cls.core.db.mapper.LicenseTypeValueConverter;
import com.nsn.ood.cls.core.db.mapper.ReservationConditionsMapper;
import com.nsn.ood.cls.core.db.reservation.DeleteReservations;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsByFeatureCode;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsByLicenseType;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsForExpiredClients;
import com.nsn.ood.cls.core.db.reservation.DeleteReservationsForExpiredLicenses;
import com.nsn.ood.cls.core.db.reservation.QueryReservations;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	FeatureReleaseOperation.class })
public class FeatureReleaseOperationTest extends FeatureReleaseOperation {
	private static final DateTime DATE = new DateTime(2015, 8, 24, 14, 49);
	private static final Client CLIENT = client("12345");
	private static final Reservation RESERVATION1_FLOATING = reservation("12345", 1234L, "abc", 10L, null,
			License.Mode.CAPACITY, License.Type.FLOATING_POOL, null, null);
	private static final Reservation RESERVATION1_POOL = reservation("12345", 1234L, "abc", 10L, null,
			License.Mode.CAPACITY, License.Type.POOL, null, null);
	private static final Reservation RESERVATION2_FOLATING = reservation("12345", 1234L, "def", 20L, null,
			License.Mode.CAPACITY, License.Type.FLOATING_POOL, null, null);
	private static final Reservation RESERVATION2_POOL = reservation("12345", 1234L, "def", 20L, null,
			License.Mode.CAPACITY, License.Type.POOL, null, null);
	private static final ReservationConditionsMapper MAPPER = mapper();

	private static QueryReservationsStub QUERY_RESERVATIONS_STUB;
	
	private static ReservationConditionsMapper mapper() {
		final LicenseModeValueConverter modeConverter = new LicenseModeValueConverter();
		final LicenseTypeValueConverter typeConverter = new LicenseTypeValueConverter();

		final ReservationConditionsMapper mapper = new ReservationConditionsMapper();
		setInternalState(mapper, "modeConverter", modeConverter);
		setInternalState(mapper, "typeConverter", typeConverter);
		mapper.start();
		return mapper;
	}

	static {
		try {
			QUERY_RESERVATIONS_STUB = new QueryReservationsStub();
		} catch (final ConditionProcessingException e) {
		}
	}

	private Conditions capturedConditions;

	@Override
	protected QueryReservations createQueryReservations(final Conditions conditions)
			throws ConditionProcessingException {
		this.capturedConditions = conditions;
		super.createQueryReservations(conditions);
		if (QUERY_RESERVATIONS_STUB.isException()) {
			throw new ConditionProcessingException("message", null, null, new Exception("mmm"));
		} else {
			return QUERY_RESERVATIONS_STUB;
		}
	}

	private void reset(final Mode mode) {
		resetAll();
		this.capturedConditions = null;
		QUERY_RESERVATIONS_STUB.setMode(mode);
	}

	@Test
	public void testReleaseAll() throws Exception {
		testReleaseAll(false);
		testReleaseAll(true);
	}

	private void testReleaseAll(final boolean force) throws Exception {
		reset(null);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<License.Type, Integer> converterMock = createMock(LicenseType2IntegerConverter.class);

		final Update updateMock;
		if (force) {
			updateMock = createMockAndExpectNew(DeleteReservations.class, CLIENT);
		} else {
			updateMock = createMockAndExpectNew(DeleteReservationsByLicenseType.class, CLIENT,
					License.Type.FLOATING_POOL, converterMock);
		}
		updateExecutorMock.execute(updateMock);

		replayAll();
		setInternalState(this, "licenseType2IntegerConverter", converterMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		releaseAll(CLIENT, force);
		verifyAll();
	}

	@Test
	public void testReleaseAllWithExceptionDuringUpdate() throws Exception {
		testReleaseAllWithExceptionDuringUpdate(false, DeleteReservationsByLicenseType.class);
		testReleaseAllWithExceptionDuringUpdate(true, DeleteReservations.class);
	}

	private void testReleaseAllWithExceptionDuringUpdate(final boolean force,
			final Class<? extends Update> expectedUpdateClass) throws Exception {
		reset(null);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(expectedUpdateClass));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "updateExecutor", updateExecutorMock);
		try {
			releaseAll(CLIENT, force);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", null);
		}
		verifyAll();
	}

	@Test
	public void testReleaseWithNoLicenses() throws Exception {
		testReleaseWithNoLicenses(false);
		testReleaseWithNoLicenses(true);
	}

	private void testReleaseWithNoLicenses(final boolean force) throws Exception {
		reset(Mode.EMPTY);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final Converter<Reservation, String> converterMock = createMock(Reservation2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_RESERVATIONS_STUB));

		replayAll();
		setInternalState(this, MAPPER);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "reservation2StringConverter", converterMock);
		release(CLIENT, 1234L, force);
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	@Test
	public void testRelease() throws Exception {
		testRelease(Mode.ONE_FLOATING, false);
		testRelease(Mode.TWO_FLOATING, false);
		testReleaseAndExpectVerificationError(Mode.ONE_POOL, false, RESERVATION1_POOL, 10L);
		testReleaseAndExpectVerificationError(Mode.TWO_POOL, false, RESERVATION1_POOL, 30L);
		testReleaseAndExpectVerificationError(Mode.FLOATING_AND_POOL, false, RESERVATION2_POOL, 20L);

		testRelease(Mode.ONE_FLOATING, true);
		testRelease(Mode.TWO_FLOATING, true);
		testRelease(Mode.ONE_POOL, true);
		testRelease(Mode.TWO_POOL, true);
		testRelease(Mode.FLOATING_AND_POOL, true);
	}

	private void testRelease(final Mode mode, final boolean force) throws Exception {
		reset(mode);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Reservation, String> converterMock = createMock(Reservation2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_RESERVATIONS_STUB));
		QUERY_RESERVATIONS_STUB.getList().forEach(r -> expect(converterMock.convertTo(r)).andReturn("log"));
		final DeleteReservationsByFeatureCode updateMock = createMockAndExpectNew(
				DeleteReservationsByFeatureCode.class, CLIENT, 1234L);
		updateExecutorMock.execute(updateMock);

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "reservation2StringConverter", converterMock);
		setInternalState(this, MAPPER);
		release(CLIENT, 1234L, force);
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	private void testReleaseAndExpectVerificationError(final Mode mode, final boolean force,
			final Reservation expectedReservation, final long expectedCapacity) throws SQLException {
		reset(mode);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final Converter<Reservation, String> converterMock = createMock(Reservation2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_RESERVATIONS_STUB));
		QUERY_RESERVATIONS_STUB.getList().forEach(r -> expect(converterMock.convertTo(r)).andReturn("log"));
		expect(converterMock.convertTo(expectedReservation)).andReturn("log");

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "reservation2StringConverter", converterMock);
		setInternalState(this, MAPPER);
		try {
			release(CLIENT, 1234L, force);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, null, featureError(1234L, expectedCapacity));
		}
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	@Test
	public void testReleaseWithExceptionDuringUpdate() throws Exception {
		reset(Mode.TWO_FLOATING);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Reservation, String> converterMock = createMock(Reservation2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_RESERVATIONS_STUB));
		QUERY_RESERVATIONS_STUB.getList().forEach(r -> expect(converterMock.convertTo(r)).andReturn("log"));
		updateExecutorMock.execute(isA(DeleteReservationsByFeatureCode.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "reservation2StringConverter", converterMock);
		setInternalState(this, MAPPER);
		try {
			release(CLIENT, 1234L, false);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", featureError(1234L, 0L));
		}
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	@Test
	public void testReleaseWithExceptionDuringQuery() throws Exception {
		reset(Mode.TWO_FLOATING);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_RESERVATIONS_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, MAPPER);
		try {
			release(CLIENT, 1234L, true);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", featureError(1234L, 0L));
		}
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	@Test
	public void testReleaseWithExceptionDuringQueryCreation() throws Exception {
		reset(Mode.EXCEPTION);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, MAPPER);
		try {
			release(CLIENT, 1234L, true);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", featureError(1234L, 0L));
		}
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", CLIENT.getClientId())
				.equalFilter("featureCode", "1234").build(), this.capturedConditions);
	}

	private void assertReleaseException(final ReleaseException e, final String expectedMessage,
			final FeatureError expectedFeatureError) {
		assertEquals(expectedMessage, e.getMessage());
		assertEquals(expectedFeatureError, e.getError());
	}

	@Test
	public void testReleaseForExpiredClients() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);
		final Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);

		final DeleteReservationsForExpiredClients updateMock = createMockAndExpectNew(
				DeleteReservationsForExpiredClients.class, DATE, converterMock, licenseType2IntegerConverter);
		updateExecutorMock.execute(updateMock);

		replayAll();
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "timestamp2DateTimeConverter", converterMock);
		setInternalState(this, "licenseType2IntegerConverter", licenseType2IntegerConverter);
		releaseForExpiredClients(DATE);
		verifyAll();
	}

	@Test
	public void testReleaseForExpiredClientsWithExceptionDuringUpdate() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(DeleteReservationsForExpiredClients.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "updateExecutor", updateExecutorMock);
		try {
			releaseForExpiredClients(DATE);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", null);
		}
		verifyAll();
	}

	@Test
	public void testReleaseForExpiredLicenses() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		final DeleteReservationsForExpiredLicenses updateMock = createMockAndExpectNew(
				DeleteReservationsForExpiredLicenses.class, DATE, converterMock);
		updateExecutorMock.execute(updateMock);

		replayAll();
		setInternalState(this, "updateExecutor", updateExecutorMock);
		setInternalState(this, "timestamp2DateTimeConverter", converterMock);
		releaseForExpiredLicenses(DATE);
		verifyAll();
	}

	@Test
	public void testReleaseForExpiredLicensesWithExceptionDuringUpdate() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(DeleteReservationsForExpiredLicenses.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "updateExecutor", updateExecutorMock);
		try {
			releaseForExpiredLicenses(DATE);
			fail();
		} catch (final ReleaseException e) {
			assertReleaseException(e, "message", null);
		}
		verifyAll();
	}

	private static enum Mode {
		EMPTY, ONE_FLOATING, ONE_POOL, TWO_FLOATING, TWO_POOL, FLOATING_AND_POOL, EXCEPTION
	}

	private static class QueryReservationsStub extends QueryReservations {
		private Mode mode;

		public QueryReservationsStub() throws ConditionProcessingException {
			super(ConditionsBuilder.createAndSkipMetaData().build(), new ConditionsMapper(), null);
		}

		public void setMode(final Mode mode) {
			this.mode = mode;
		}

		public boolean isException() {
			return this.mode == Mode.EXCEPTION;
		}

		@Override
		public List<Reservation> getList() {
			switch (this.mode) {
				case EMPTY:
					return new ArrayList<>();
				case ONE_FLOATING:
					return Arrays.asList(RESERVATION1_FLOATING);
				case ONE_POOL:
					return Arrays.asList(RESERVATION1_POOL);
				case TWO_FLOATING:
					return Arrays.asList(RESERVATION1_FLOATING, RESERVATION2_FOLATING);
				case TWO_POOL:
					return Arrays.asList(RESERVATION1_POOL, RESERVATION2_POOL);
				case FLOATING_AND_POOL:
					return Arrays.asList(RESERVATION1_FLOATING, RESERVATION2_POOL);
				default:
					break;
			}
			return null;
		}
	}
}
