/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.assertFeatureError;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;
import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.operation.util.ReservationsCalculator.CapacityException;
import com.nsn.ood.cls.core.operation.util.ReservationsUtils.CalculationException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		CapacityException.class, CalculationException.class })
public class ReservationsUtilsTest extends ReservationsUtils {
	private ReservationsCalculator calculatorMock;
	private List<License> captuedLicenses;
	private List<Reservation> captuedReservations;
	private long captuedRequestedCapacity;
	private DateTime captuedReservationTime;

	@Before
	public void setUp() throws Exception {
		this.calculatorMock = createMock(ReservationsCalculator.class);
	}

	@Override
	protected ReservationsCalculator createCalculator(final List<License> licenses,
			final List<Reservation> reservations, final long requestedCapacity, final DateTime reservationTime) {
		this.captuedLicenses = licenses;
		this.captuedReservations = reservations;
		this.captuedRequestedCapacity = requestedCapacity;
		this.captuedReservationTime = reservationTime;
		super.createCalculator(licenses, reservations, requestedCapacity, reservationTime);
		return this.calculatorMock;
	}

	@Test
	public void testCapacityFeature() throws Exception {
		testFeature(featureCapacity(1234L, 50L), licensesList(license()), 50L);
		testFeature(featureCapacity(1234L, 0L), licensesList(license()), 0L);
	}

	@Test
	public void testCapacityFeatureWhenNoLicenses() throws Exception {
		testFeature(featureCapacity(1234L, 0L), licensesList(), 0L);
		testException(featureCapacity(1234L, 50L), licensesList(), ReservationErrorType.CAPACITY, 50L, 0L, null);
	}

	@Test
	public void testOnOffFeature() throws Exception {
		testFeature(featureOnOff(1234L), licensesList(license()), 1L);
	}

	@Test
	public void testOnOffFeatureWhenNoLicenses() throws Exception {
		testException(featureOnOff(1234L), licensesList(), ReservationErrorType.ON_OFF, null, null, null);
	}

	@Test
	public void testReleaseException() throws Exception {
		final CapacityException exceptionMock = createMock(CapacityException.class);

		expect(this.calculatorMock.calculate()).andThrow(exceptionMock);
		expect(exceptionMock.getCapacity()).andReturn(7L);
		expect(exceptionMock.isRelease()).andReturn(true);

		replayAll();
		testException(featureCapacity(1234L, 50L), licensesList(license()), ReservationErrorType.RELEASE, null, null,
				7L);
		verifyAll();
	}

	@Test
	public void testCapacityException() throws Exception {
		final CapacityException exceptionMock = createMock(CapacityException.class);

		expect(this.calculatorMock.calculate()).andThrow(exceptionMock);
		expect(exceptionMock.getCapacity()).andReturn(7L);
		expect(exceptionMock.isRelease()).andReturn(false);

		replayAll();
		testException(featureCapacity(1234L, 50L), licensesList(license()), ReservationErrorType.CAPACITY, 50L, 7L,
				null);
		verifyAll();
	}

	@Test
	public void testOnOffException() throws Exception {
		final CapacityException exceptionMock = createMock(CapacityException.class);

		expect(this.calculatorMock.calculate()).andThrow(exceptionMock);
		expect(exceptionMock.getCapacity()).andReturn(1L);
		expect(exceptionMock.isRelease()).andReturn(false);

		replayAll();
		testException(featureOnOff(1234L), licensesList(license()), ReservationErrorType.ON_OFF, null, null, null);
		verifyAll();
	}

	private void testFeature(final Feature feature, final List<License> licensesList,
			final long expectedRequestedCapacity) throws CapacityException, CalculationException {
		resetAll();

		final List<Reservation> reservationsList = reservationsList();

		expect(this.calculatorMock.calculate()).andReturn(reservationsList(reservation()));

		replayAll();
		final List<Reservation> result = calculate(client("ab12"), feature, licensesList, reservationsList);
		verifyAll();

		assertEquals(
				reservationsList(
						reservation("ab12", feature.getFeatureCode(), null, null, null, null, null, null, null)),
				result);

		assertSame(licensesList, this.captuedLicenses);
		assertSame(reservationsList, this.captuedReservations);
		assertEquals(expectedRequestedCapacity, this.captuedRequestedCapacity);
		assertNow(this.captuedReservationTime);
	}

	private void testException(final Feature feature, final List<License> licenses,
			final ReservationErrorType expectedErrorType, final Long expectedRequestedCapacity,
			final Long expectedRemainingCapacity, final Long expectedCapacity) {
		try {
			calculate(client("ab12"), feature, licenses, reservationsList());
			fail();
		} catch (final CalculationException e) {
			assertEquals(expectedErrorType, e.getErrorType());
			assertFeatureError(e.getError(), feature.getFeatureCode(), expectedRequestedCapacity,
					expectedRemainingCapacity, expectedCapacity);
		}
	}

	@Test
	public void testEmptyParameters() throws Exception {
		assertCalculateInputParameters(null, feature(), licensesList(), reservationsList());
		assertCalculateInputParameters(client(), null, licensesList(), reservationsList());
		assertCalculateInputParameters(client(), feature(), null, reservationsList());
		assertCalculateInputParameters(client(), feature(), licensesList(), null);
		assertIsCalculationsNeededInputParameters(null, reservationsList());
		assertIsCalculationsNeededInputParameters(feature(), null);
	}

	private void assertCalculateInputParameters(final Client client, final Feature feature,
			final List<License> licenses, final List<Reservation> reservations) throws CalculationException {
		try {
			calculate(client, feature, licenses, reservations);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	private void assertIsCalculationsNeededInputParameters(final Feature feature, final List<Reservation> reservations)
			throws CalculationException {
		try {
			isCalculationsNeeded(feature, reservations);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testIsCalculationsNeeded() throws Exception {
		final Feature capacityFeature = featureCapacity(null, 10L);
		final Feature onOffFeature = featureOnOff(null);
		final Reservation r1 = reservation().withCapacity(1L);
		final Reservation r2 = reservation().withCapacity(2L);
		final Reservation r9 = reservation().withCapacity(9L);
		final Reservation r10 = reservation().withCapacity(10L);
		final Reservation r11 = reservation().withCapacity(11L);

		assertTrue(isCalculationsNeeded(capacityFeature, reservationsList(r9)));
		assertFalse(isCalculationsNeeded(capacityFeature, reservationsList(r10)));
		assertTrue(isCalculationsNeeded(capacityFeature, reservationsList(r11)));
		assertFalse(isCalculationsNeeded(capacityFeature, reservationsList(r9, r1)));
		assertTrue(isCalculationsNeeded(capacityFeature, reservationsList(r9, r2)));

		assertFalse(isCalculationsNeeded(onOffFeature, reservationsList(r1)));
		assertTrue(isCalculationsNeeded(onOffFeature, reservationsList(r2)));
	}
}
