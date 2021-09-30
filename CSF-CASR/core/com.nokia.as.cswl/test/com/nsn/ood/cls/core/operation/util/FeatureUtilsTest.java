/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertCapacityFeature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertFeatureAllocations;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.assertOnOffFeature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Allocation.Usage;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.model.internal.Reservation;


/**
 * @author marynows
 * 
 */
public class FeatureUtilsTest {
	private static final Feature CAPACITY_FEATURE = featureCapacity(1234L, 100L);
	private static final Feature ONOFF_FEATURE = featureOnOff(2345L);
	private static final DateTime END_DATE = new DateTime(2015, 2, 22, 12, 12);

	private FeatureUtils utils;

	@Before
	public void setUp() throws Exception {
		this.utils = new FeatureUtils();
	}

	@Test
	public void testCreateFeatureWithAllocations_NoReservations() throws Exception {
		testCreateCapacityFeatureWithAllocations(reservationsList());

		testCreateOnOffFeatureWithAllocations(reservationsList());
	}

	@Test
	public void testCreateFeatureWithAllocations_OneReservation() throws Exception {
		testCreateCapacityFeatureWithAllocations(reservationsList(reservation("abc123", 100L, null, null)),
				allocation(100L, "abc123", null, null));
		testCreateCapacityFeatureWithAllocations(
				reservationsList(reservation("abc123", 100L, Type.FLOATING_POOL, null)),
				allocation(100L, "abc123", Usage.FLOATING_POOL, null));
		testCreateCapacityFeatureWithAllocations(
				reservationsList(reservation("abc123", 100L, Type.FLOATING_POOL, END_DATE)),
				allocation(100L, "abc123", Usage.FLOATING_POOL, END_DATE));
		testCreateCapacityFeatureWithAllocations(reservationsList(reservation("abc123", 100L, null, null)),
				allocation(100L, "abc123", null, null));
		testCreateCapacityFeatureWithAllocations(reservationsList(reservation("abc123", 100L, Type.POOL, END_DATE)),
				allocation(100L, "abc123", Usage.POOL, END_DATE));
		testCreateCapacityFeatureWithAllocations(reservationsList(reservation("abc123", 100L, null, END_DATE)),
				allocation(100L, "abc123", null, END_DATE));

		testCreateOnOffFeatureWithAllocations(reservationsList(reservation("abc123", 1L, Type.FLOATING_POOL, null)),
				allocation(null, "abc123", Usage.FLOATING_POOL, null));
		testCreateOnOffFeatureWithAllocations(
				reservationsList(reservation("abc123", 1L, Type.FLOATING_POOL, END_DATE)),
				allocation(null, "abc123", Usage.FLOATING_POOL, END_DATE));
	}

	@Test
	public void testCreateFeatureWithAllocations_MoreReservations() throws Exception {
		testCreateCapacityFeatureWithAllocations(reservationsList(//
				reservation("abc123", 40L, Type.FLOATING_POOL, END_DATE),//
				reservation("cba321", 60L, Type.POOL, END_DATE.plusHours(1))),//
				allocation(40L, "abc123", Usage.FLOATING_POOL, END_DATE),//
				allocation(60L, "cba321", Usage.POOL, END_DATE.plusHours(1)));
		testCreateCapacityFeatureWithAllocations(reservationsList(//
				reservation("abc123", 40L, Type.FLOATING_POOL, END_DATE.plusHours(1)),//
				reservation("cba321", 60L, Type.POOL, END_DATE.plusHours(2))),//
				allocation(40L, "abc123", Usage.FLOATING_POOL, END_DATE.plusHours(1)),//
				allocation(60L, "cba321", Usage.POOL, END_DATE.plusHours(2)));
		testCreateCapacityFeatureWithAllocations(reservationsList(//
				reservation("abc123", 40L, Type.FLOATING_POOL, END_DATE.plusHours(1)),//
				reservation("cba321", 60L, null, null)),//
				allocation(40L, "abc123", Usage.FLOATING_POOL, END_DATE.plusHours(1)),//
				allocation(60L, "cba321", null, null));

		testCreateOnOffFeatureWithAllocations(reservationsList(//
				reservation("abc123", 1L, Type.FLOATING_POOL, END_DATE.plusHours(1)),//
				reservation("cba321", 1L, Type.POOL, END_DATE.plusHours(2))),//
				allocation(null, "abc123", Usage.FLOATING_POOL, END_DATE.plusHours(1)),//
				allocation(null, "cba321", Usage.POOL, END_DATE.plusHours(2)));

		testCreateCapacityFeatureWithAllocations(reservationsList(//
				reservation("abc123", 20L, Type.FLOATING_POOL, END_DATE),//
				reservation("cba321", 30L, Type.POOL, END_DATE.plusHours(2)),//
				reservation("def456", 50L, Type.POOL, END_DATE.plusHours(1))),//
				allocation(20L, "abc123", Usage.FLOATING_POOL, END_DATE),//
				allocation(30L, "cba321", Usage.POOL, END_DATE.plusHours(2)),//
				allocation(50L, "def456", Usage.POOL, END_DATE.plusHours(1)));
		testCreateCapacityFeatureWithAllocations(reservationsList(//
				reservation("abc123", 20L, Type.FLOATING_POOL, null),//
				reservation("cba321", 30L, Type.POOL, null),//
				reservation("def456", 50L, Type.POOL, END_DATE)),//
				allocation(20L, "abc123", Usage.FLOATING_POOL, null),//
				allocation(30L, "cba321", Usage.POOL, null),//
				allocation(50L, "def456", Usage.POOL, END_DATE));
	}

	private void testCreateCapacityFeatureWithAllocations(final List<Reservation> reservations,
			final Allocation... allocations) {
		final Feature result = this.utils.createFeatureWithAllocations(CAPACITY_FEATURE, reservations);
		assertCapacityFeature(result, 1234L, 100L);
		assertFeatureAllocations(result, allocations);
	}

	private void testCreateOnOffFeatureWithAllocations(final List<Reservation> reservations,
			final Allocation... allocations) {
		final Feature result = this.utils.createFeatureWithAllocations(ONOFF_FEATURE, reservations);
		assertOnOffFeature(result, 2345L);
		assertFeatureAllocations(result, allocations);
	}
}
