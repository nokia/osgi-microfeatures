/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class UpdateReservationsTest {

	@Test
	public void testNext() throws Exception {
		assertNull(new UpdateReservations(null, 0, null, null, null, null).next());
		assertNull(new UpdateReservations(null, 0, reservationsList(), null, null, null).next());
		assertTrue(new UpdateReservations(null, 0, reservationsList(reservation()), null, null, null).next() instanceof InsertReservations);
	}
}
