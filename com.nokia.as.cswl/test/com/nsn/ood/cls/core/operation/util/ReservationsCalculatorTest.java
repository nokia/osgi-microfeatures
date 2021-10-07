/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ReservationsCalculatorTest {

	@Test
	public void testNoLicenses() throws Exception {
		assertEquals(3, testCalculate("no_licenses.test"));
	}

	@Test
	public void testCapacity_NoReservations_OneLicense() throws Exception {
		assertEquals(12, testCalculate("capacity_no_reservations_one.test"));
	}

	@Test
	public void testCapacity_NoReservations_Mixed() throws Exception {
		assertEquals(12, testCalculate("capacity_no_reservations_mixed.test"));
	}

	@Test
	public void testCapacity_WithReservations_OnePoolLicense() throws Exception {
		assertEquals(13, testCalculate("capacity_with_reservations_one_pool.test"));
	}

	@Test
	public void testCapacity_WithReservations_OneFloatingPoolLicense() throws Exception {
		assertEquals(14, testCalculate("capacity_with_reservations_one_floating.test"));
	}

	@Test
	public void testCapacity_WithReservations_TwoPoolLicenses() throws Exception {
		assertEquals(29, testCalculate("capacity_with_reservations_two_pool.test"));
	}

	@Test
	public void testCapacity_WithReservations_TwoFloatingPoolLicenses() throws Exception {
		assertEquals(29, testCalculate("capacity_with_reservations_two_floating.test"));
	}

	@Test
	public void testCapacity_WithReservations_TwoMixedLicenses() throws Exception {
		assertEquals(43, testCalculate("capacity_with_reservations_two_mixed.test"));
	}

	@Test
	public void testCapacity_WithReservations_MoreMixedLicenses() throws Exception {
		assertEquals(10, testCalculate("capacity_with_reservations_more_mixed.test"));
	}

	@Test
	public void testOnOff_NoReservations() throws Exception {
		assertEquals(11, testCalculate("on_off_no_reservations.test"));
	}

	@Test
	public void testOnOff_WithReservations() throws Exception {
		assertEquals(24, testCalculate("on_off_with_reservations.test"));
	}

	private int testCalculate(final String resourceName) {
		return new ReservationsCalculatorTester(resourceName).test().size();
	}
}
