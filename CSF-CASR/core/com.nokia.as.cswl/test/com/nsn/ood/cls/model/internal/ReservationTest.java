/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.assertReservation;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;


/**
 * @author marynows
 *
 */
public class ReservationTest {
	private static final DateTime RESERVATION_TIME = new DateTime(2015, 6, 1, 11, 28);
	private static final DateTime END_DATE = new DateTime(2015, 8, 19, 12, 51);

	@Test
	public void testEmptyReservation() throws Exception {
		assertReservation(new Reservation(), null, null, null, null, null, null, null, null, null);
	}

	@Test
	public void testReservation() throws Exception {
		assertReservation(new Reservation().withClientId("id"), "id", null, null, null, null, null, null, null, null);
		assertReservation(new Reservation().withFeatureCode(12L), null, 12L, null, null, null, null, null, null, null);
		assertReservation(new Reservation().withSerialNumber("sn"), null, null, "sn", null, null, null, null, null,
				null);
		assertReservation(new Reservation().withCapacity(23L), null, null, null, 23L, null, null, null, null, null);
		assertReservation(new Reservation().withReservationTime(RESERVATION_TIME), null, null, null, null,
				RESERVATION_TIME, null, null, null, null);
		assertReservation(new Reservation().withMode(Mode.CAPACITY), null, null, null, null, null, Mode.CAPACITY, null,
				null, null);
		assertReservation(new Reservation().withType(Type.POOL), null, null, null, null, null, null, Type.POOL, null,
				null);
		assertReservation(new Reservation().withEndDate(END_DATE), null, null, null, null, null, null, null, END_DATE,
				null);
		assertReservation(new Reservation().withFileName("fileName"), null, null, null, null, null, null, null, null,
				"fileName");

		assertReservation(
				new Reservation().withClientId("id").withFeatureCode(12L).withSerialNumber("sn").withCapacity(23L)
						.withReservationTime(RESERVATION_TIME).withMode(Mode.ON_OFF).withType(Type.FLOATING_POOL)
						.withEndDate(END_DATE).withFileName("file"),
				"id", 12L, "sn", 23L, RESERVATION_TIME, Mode.ON_OFF, Type.FLOATING_POOL, END_DATE, "file");
	}

	@Test
	public void testReservationSetters() throws Exception {
		final Reservation reservation = new Reservation();
		reservation.setClientId("id");
		reservation.setFeatureCode(12L);
		reservation.setSerialNumber("sn");
		reservation.setCapacity(23L);
		reservation.setReservationTime(RESERVATION_TIME);
		reservation.setMode(Mode.CAPACITY);
		reservation.setType(Type.FLOATING_POOL);
		reservation.setEndDate(END_DATE);
		reservation.setFileName("fff");

		assertReservation(reservation, "id", 12L, "sn", 23L, RESERVATION_TIME, Mode.CAPACITY, Type.FLOATING_POOL,
				END_DATE, "fff");
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(Reservation.class, "featureCode", "serialNumber", "clientId", "capacity",
				"reservationTime", "mode", "type", "endDate", "fileName");
		assertJsonProperty(Reservation.class, "featureCode", "featureCode");
		assertJsonProperty(Reservation.class, "serialNumber", "serialNumber");
		assertJsonProperty(Reservation.class, "clientId", "clientId");
		assertJsonProperty(Reservation.class, "capacity", "capacity");
		assertJsonProperty(Reservation.class, "reservationTime", "reservationTime");
		assertJsonProperty(Reservation.class, "mode", "mode");
		assertJsonProperty(Reservation.class, "type", "type");
		assertJsonProperty(Reservation.class, "endDate", "endDate");
		assertJsonProperty(Reservation.class, "fileName", "fileName");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new Reservation().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final Reservation reservation = new Reservation().withClientId("123");

		assertFalse(reservation.equals(null));
		assertFalse(reservation.equals("test"));
		assertEquals(reservation, reservation);

		assertFalse(reservation.equals(new Reservation()));
		assertNotEquals(reservation.hashCode(), new Reservation().hashCode());

		final Reservation reservation2 = new Reservation().withClientId("123");
		assertEquals(reservation, reservation2);
		assertEquals(reservation.hashCode(), reservation2.hashCode());
	}

	@Test
	public void testClone() throws Exception {
		final Reservation toClone = new Reservation();
		toClone.setCapacity(1L);
		toClone.setClientId("id");
		toClone.setEndDate(END_DATE);
		toClone.setFeatureCode(2L);
		toClone.setFileName("file");
		toClone.setMode(Mode.CAPACITY);
		toClone.setReservationTime(RESERVATION_TIME);
		toClone.setSerialNumber("serial");
		toClone.setType(Type.POOL);

		final Reservation cloned = new Reservation(toClone);

		assertEquals((Long) 1L, cloned.getCapacity());
		assertEquals("id", cloned.getClientId());
		assertEquals(END_DATE, cloned.getEndDate());
		assertEquals((Long) 2L, cloned.getFeatureCode());
		assertEquals("file", cloned.getFileName());
		assertEquals(Mode.CAPACITY, cloned.getMode());
		assertEquals(RESERVATION_TIME, cloned.getReservationTime());
		assertEquals("serial", cloned.getSerialNumber());
		assertEquals(Type.POOL, cloned.getType());

	}
}
