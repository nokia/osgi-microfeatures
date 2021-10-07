/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertFeature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertFeatureAllocations;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.convert.Feature2StringConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.license.QueryLicensesForFeature;
import com.nsn.ood.cls.core.db.mapper.LicenseModeValueConverter;
import com.nsn.ood.cls.core.db.mapper.LicenseTypeValueConverter;
import com.nsn.ood.cls.core.db.mapper.ReservationConditionsMapper;
import com.nsn.ood.cls.core.db.reservation.QueryReservations;
import com.nsn.ood.cls.core.db.reservation.UpdateFeatureUsage;
import com.nsn.ood.cls.core.db.reservation.UpdateReservations;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;
import com.nsn.ood.cls.core.operation.util.FeatureUtils;
import com.nsn.ood.cls.core.operation.util.ReservationErrorType;
import com.nsn.ood.cls.core.operation.util.ReservationsUtils;
import com.nsn.ood.cls.core.operation.util.ReservationsUtils.CalculationException;
import com.nsn.ood.cls.core.operation.util.UsageCalculator;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Feature.Type;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		UpdateFeatureUsage.class, CalculationException.class })
public class FeatureReservationOperationTest extends FeatureReservationOperation {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ReservationConditionsMapper MAPPER = mapper();
	private static final Client CLIENT = client("12345");

	private static final List<License> LICENSES = licensesList(license("1"));
	private static final QueryLicensesForFeature QUERY_LICENSES_STUB = new QueryLicensesForFeature(null, null, null,
			null, null) {
		@Override
		public List<License> getList() {
			return LICENSES;
		}
	};
	
	private static ReservationConditionsMapper mapper() {
		final LicenseModeValueConverter modeConverter = new LicenseModeValueConverter();
		final LicenseTypeValueConverter typeConverter = new LicenseTypeValueConverter();

		final ReservationConditionsMapper mapper = new ReservationConditionsMapper();
		setInternalState(mapper, "modeConverter", modeConverter);
		setInternalState(mapper, "typeConverter", typeConverter);
		mapper.start();
		return mapper;
	}

	private static final Reservation RESERVATION1 = reservation("12345");
	private static final Reservation RESERVATION2 = reservation("23456");
	private static final List<Reservation> RESERVATIONS = reservationsList(RESERVATION1);
	private static final List<Reservation> NEW_RESERVATIONS = reservationsList(RESERVATION1, RESERVATION2);

	private final List<Feature> capturedFeatures = new ArrayList<>();
	private final List<Client> capturedClients = new ArrayList<>();
	private List<Reservation> capturedReservations;
	private Conditions capturedConditions;
	private QueryReservations queryReservationsStub;
	private ConditionProcessingException conditionProcessingExceptionMock;
	private boolean exceptionInCreateQueryReservations;

	@Override
	protected QueryLicensesForFeature createLicensesQuery(final Client client, final Feature feature) {
		this.capturedClients.add(client);
		this.capturedFeatures.add(feature);
		super.createLicensesQuery(client, feature);
		return QUERY_LICENSES_STUB;
	}

	@Override
	protected QueryReservations createQueryReservations(final Conditions conditions)
			throws ConditionProcessingException {
		this.capturedConditions = conditions;
		super.createQueryReservations(CONDITIONS);
		if (this.exceptionInCreateQueryReservations) {
			throw this.conditionProcessingExceptionMock;
		}
		return this.queryReservationsStub;
	}

	@Override
	protected UpdateReservations createReservationsUpdate(final Client client, final Feature feature,
			final List<Reservation> reservations) {
		this.capturedClients.add(client);
		this.capturedFeatures.add(feature);
		this.capturedReservations = reservations;
		return super.createReservationsUpdate(client, feature, reservations);
	}

	@Before
	public void setUp() throws Exception {
		resetAll();
		this.queryReservationsStub = new QueryReservations(CONDITIONS, MAPPER, null) {
			@Override
			public List<Reservation> getList() {
				return RESERVATIONS;
			};
		};
		this.conditionProcessingExceptionMock = createMock(ConditionProcessingException.class);
		this.exceptionInCreateQueryReservations = false;
	}

	@Test
	public void testReserveCapacity() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final FeatureUtils featureUtilsMock = createMock(FeatureUtils.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 50L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 50L), RESERVATIONS)).andReturn(true);
		queryExecutorMock.execute(same(QUERY_LICENSES_STUB));
		expect(reservationsUtilsMock.calculate(CLIENT, featureCapacity(1234L, 50L), LICENSES, RESERVATIONS))
				.andReturn(NEW_RESERVATIONS);
		updateExecutorMock.execute(isA(UpdateReservations.class));
		expect(featureUtilsMock.createFeatureWithAllocations(featureCapacity(1234L, 50L), NEW_RESERVATIONS))
				.andReturn(featureCapacity(1234L, 50L, allocation(50L)));

		mockUsageCalculator(RESERVATIONS, NEW_RESERVATIONS, featureCapacity(1234L, 50L), usageCalculator,
				updateExecutorMock);

		replayAll();
		setInternalState(this, reservationsUtilsMock, featureUtilsMock, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		final ReservationResult result = reserveCapacity(CLIENT, featureCapacity(1234L, 50L));
		verifyAll();

		assertTrue(result.isUpdated());
		assertFeature(result.getFeature(), 1234L, Type.CAPACITY, 50L);
		assertFeatureAllocations(result.getFeature(), allocation(50L));
		assertCaptured(Arrays.asList(CLIENT, CLIENT), //
				Arrays.asList(featureCapacity(1234L, 50L), featureCapacity(1234L, 50L)), //
				NEW_RESERVATIONS, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWhenCalculationsNotNeeded() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final FeatureUtils featureUtilsMock = createMock(FeatureUtils.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 50L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 50L), RESERVATIONS)).andReturn(false);
		expect(featureUtilsMock.createFeatureWithAllocations(featureCapacity(1234L, 50L), RESERVATIONS))
				.andReturn(featureCapacity(1234L, 50L, allocation(50L)));

		replayAll();
		setInternalState(this, reservationsUtilsMock, featureUtilsMock, MAPPER,	usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		final ReservationResult result = reserveCapacity(CLIENT, featureCapacity(1234L, 50L));
		verifyAll();

		assertFalse(result.isUpdated());
		assertFeature(result.getFeature(), 1234L, Type.CAPACITY, 50L);
		assertFeatureAllocations(result.getFeature(), allocation(50L));
		assertCaptured(Collections.<Client> emptyList(), Collections.<Feature> emptyList(), //
				null, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveZeroCapacity() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final FeatureUtils featureUtilsMock = createMock(FeatureUtils.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 0L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 0L), RESERVATIONS)).andReturn(true);
		queryExecutorMock.execute(same(QUERY_LICENSES_STUB));
		expect(reservationsUtilsMock.calculate(CLIENT, featureCapacity(1234L, 0L), LICENSES, RESERVATIONS))
				.andReturn(NEW_RESERVATIONS);
		updateExecutorMock.execute(isA(UpdateReservations.class));
		expect(featureUtilsMock.createFeatureWithAllocations(featureCapacity(1234L, 0L), NEW_RESERVATIONS))
				.andReturn(featureCapacity(1234L, 0L));

		mockUsageCalculator(RESERVATIONS, NEW_RESERVATIONS, featureCapacity(1234L, 0L), usageCalculator,
				updateExecutorMock);

		replayAll();
		setInternalState(this, reservationsUtilsMock, featureUtilsMock, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		final ReservationResult result = reserveCapacity(CLIENT, featureCapacity(1234L, 0L));
		verifyAll();

		assertTrue(result.isUpdated());
		assertFeature(result.getFeature(), 1234L, Type.CAPACITY, 0L);
		assertFeatureAllocations(result.getFeature());
		assertCaptured(Arrays.asList(CLIENT, CLIENT), //
				Arrays.asList(featureCapacity(1234L, 0L), featureCapacity(1234L, 0L)), //
				NEW_RESERVATIONS, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWithExceptionDuringReservationUpdate() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 50L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 50L), RESERVATIONS)).andReturn(true);
		queryExecutorMock.execute(same(QUERY_LICENSES_STUB));
		expect(reservationsUtilsMock.calculate(CLIENT, featureCapacity(1234L, 50L), LICENSES, RESERVATIONS))
				.andReturn(NEW_RESERVATIONS);
		updateExecutorMock.execute(isA(UpdateReservations.class));
		expectLastCall().andThrow(new SQLException("message"));

		mockUsageCalculator(RESERVATIONS, usageCalculator);

		replayAll();
		setInternalState(this, reservationsUtilsMock, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		try {
			reserveCapacity(CLIENT, featureCapacity(1234L, 50L));
			fail();
		} catch (final ReservationException e) {
			assertReservationException(e, "message", featureError(1234L, 50L, null), ReservationErrorType.CAPACITY);
		}
		verifyAll();

		assertCaptured(Arrays.asList(CLIENT, CLIENT), //
				Arrays.asList(featureCapacity(1234L, 50L), featureCapacity(1234L, 50L)), //
				NEW_RESERVATIONS, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWithExceptionDuringAllocationsCalculation() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final CalculationException exceptionMock = createMock(CalculationException.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 50L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 50L), RESERVATIONS)).andReturn(true);
		queryExecutorMock.execute(same(QUERY_LICENSES_STUB));
		expect(reservationsUtilsMock.calculate(CLIENT, featureCapacity(1234L, 50L), LICENSES, RESERVATIONS))
				.andThrow(exceptionMock);
		expect(exceptionMock.getMessage()).andReturn("message");
		expect(exceptionMock.getError()).andReturn(featureError(1234L, 50L, 30L));
		expect(exceptionMock.getErrorType()).andReturn(ReservationErrorType.RELEASE);

		mockUsageCalculator(RESERVATIONS, usageCalculator);

		replayAll();
		setInternalState(this, reservationsUtilsMock, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			reserveCapacity(CLIENT, featureCapacity(1234L, 50L));
			fail();
		} catch (final ReservationException e) {
			assertReservationException(e, "message", featureError(1234L, 50L, 30L), ReservationErrorType.RELEASE);
		}
		verifyAll();

		assertCaptured(Arrays.asList(CLIENT), //
				Arrays.asList(featureCapacity(1234L, 50L)), //
				null, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWithExceptionDuringLicensesQuery() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final ReservationsUtils reservationsUtilsMock = createMock(ReservationsUtils.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureCapacity(1234L, 50L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expect(reservationsUtilsMock.isCalculationsNeeded(featureCapacity(1234L, 50L), RESERVATIONS)).andReturn(true);
		queryExecutorMock.execute(same(QUERY_LICENSES_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, reservationsUtilsMock, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			reserveCapacity(CLIENT, featureCapacity(1234L, 50L));
			fail();
		} catch (final ReservationException e) {
			assertReservationException(e, "message", featureError(1234L, 50L, null), ReservationErrorType.CAPACITY);
		}
		verifyAll();

		assertCaptured(Arrays.asList(CLIENT), //
				Arrays.asList(featureCapacity(1234L, 50L)), //
				null, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWithExceptionDuringReservationsQuery() throws Exception {
		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureOnOff(1234L))).andReturn("log");
		queryExecutorMock.execute(same(this.queryReservationsStub));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			reserveCapacity(CLIENT, featureOnOff(1234L));
			fail();
		} catch (final ReservationException e) {
			assertReservationException(e, "message", featureError(1234L, null, null), ReservationErrorType.ON_OFF);
		}
		verifyAll();

		assertCaptured(Collections.<Client> emptyList(), Collections.<Feature> emptyList(), //
				null, expectedConditions(CLIENT.getClientId(), "1234"));
	}

	@Test
	public void testReserveCapacityWithExceptionDuringReservationsQueryCreation() throws Exception {
		this.exceptionInCreateQueryReservations = true;

		final Converter<Feature, String> converterMock = createMock(Feature2StringConverter.class);
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UsageCalculator usageCalculator = createMock(UsageCalculator.class);

		expect(converterMock.convertTo(featureOnOff(2345L))).andReturn("log");
		expect(this.conditionProcessingExceptionMock.getMessage()).andReturn("message");

		replayAll();
		setInternalState(this, MAPPER, usageCalculator);
		setInternalState(this, "feature2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			reserveCapacity(CLIENT, featureOnOff(2345L));
			fail();
		} catch (final ReservationException e) {
			assertReservationException(e, "message", featureError(2345L, null, null), ReservationErrorType.ON_OFF);
		}
		verifyAll();

		assertCaptured(Collections.<Client> emptyList(), Collections.<Feature> emptyList(), //
				null, expectedConditions(CLIENT.getClientId(), "2345"));
	}

	private void mockUsageCalculator(final List<Reservation> oldReservations, final UsageCalculator usageCalculator) {
		final List<Reservation> clonedReservations = new ArrayList<>();
		expect(usageCalculator.cloneReservations(oldReservations)).andReturn(clonedReservations);
	}

	private FeatureLicenseUsageDelta mockUsageCalculator(final List<Reservation> oldReservations,
			final List<Reservation> newReservations, final Feature feature, final UsageCalculator usageCalculator,
			final UpdateExecutor updateExecutor) throws Exception {
		final List<Reservation> clonedReservations = new ArrayList<>();
		final FeatureLicenseUsageDelta featureUsageDelta = createMock(FeatureLicenseUsageDelta.class);

		expect(usageCalculator.cloneReservations(oldReservations)).andReturn(clonedReservations);
		expect(usageCalculator.calculateUsage(feature, clonedReservations, newReservations, LICENSES))
				.andReturn(featureUsageDelta);
		final UpdateFeatureUsage updateFeatureUsageMock = createMockAndExpectNew(UpdateFeatureUsage.class,
				featureUsageDelta);
		updateExecutor.execute(updateFeatureUsageMock);

		return featureUsageDelta;

	}

	private Conditions expectedConditions(final String clientId, final String featureCode) {
		return ConditionsBuilder.createAndSkipMetaData().equalFilter("clientId", clientId)
				.equalFilter("featureCode", featureCode).build();
	}

	private void assertReservationException(final ReservationException e, final String expectedMessage,
			final FeatureError expectedFeatureError, final ReservationErrorType expectedErrorType) {
		assertEquals(expectedMessage, e.getMessage());
		assertEquals(expectedFeatureError, e.getError());
		assertEquals(expectedErrorType, e.getErrorType());
	}

	private void assertCaptured(final List<Client> expectedClients, final List<Feature> expectedFeatures,
			final List<Reservation> expectedReservations, final Conditions expectedConditions) {
		assertEquals(expectedClients, this.capturedClients);
		assertEquals(expectedFeatures, this.capturedFeatures);
		assertEquals(expectedReservations, this.capturedReservations);
		assertEquals(expectedConditions, this.capturedConditions);
	}
}
