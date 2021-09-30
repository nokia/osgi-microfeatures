/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;

import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.model.internal.Reservation;


/**
 * @author marynows
 * 
 */
public class ReservationTestUtil {

	public static List<Reservation> reservationsList(final Reservation... reservations) {
		return Arrays.asList(reservations);
	}

	public static Reservation reservation() {
		return new Reservation();
	}

	public static Reservation reservation(final String clientId) {
		return reservation().withClientId(clientId);
	}

	public static Reservation reservation(final String clientId, final Long featureCode, final String serialNumber,
			final Long capacity, final DateTime reservationTime, final Mode mode, final Type type,
			final DateTime endDate, final String fileName) {
		return reservation(clientId).withFeatureCode(featureCode).withSerialNumber(serialNumber).withCapacity(capacity)
				.withReservationTime(reservationTime).withMode(mode).withType(type).withEndDate(endDate)
				.withFileName(fileName);
	}

	public static Reservation reservation(final String serialNumber, final Long capacity, final Type type,
			final DateTime endDate) {
		return reservation().withSerialNumber(serialNumber).withCapacity(capacity).withType(type).withEndDate(endDate);
	}

	public static void assertReservation(final Reservation reservation, final String expectedClientId,
			final Long expectedFeatureCode, final String expectedSerialNumber, final Long expectedCapacity,
			final DateTime expectedReservationTime, final Mode expectedMode, final Type expectedType,
			final DateTime expectedEndDate, final String expectedFileName) {
		assertEquals(expectedClientId, reservation.getClientId());
		assertEquals(expectedFeatureCode, reservation.getFeatureCode());
		assertEquals(expectedSerialNumber, reservation.getSerialNumber());
		assertEquals(expectedCapacity, reservation.getCapacity());
		assertEquals(expectedReservationTime, reservation.getReservationTime());
		assertEquals(expectedMode, reservation.getMode());
		assertEquals(expectedType, reservation.getType());
		assertEquals(expectedEndDate, reservation.getEndDate());
		assertEquals(expectedFileName, reservation.getFileName());
	}
}
