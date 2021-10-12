// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.operation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;
import com.nsn.ood.cls.core.model.LicenseUpdate;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Reservation;


public class UsageCalculatorTest {
	final List<Reservation> oldReservations = new ArrayList<>();
	final List<Reservation> newReservations = new ArrayList<>();
	final List<License> licenses = new ArrayList<>();

	private UsageCalculator bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new UsageCalculator();
	}

	@Test
	public void testNoReservationsBefore() throws Exception {
		final long featureCode = 997L;
		final String serial1 = "123";

		final License lic1 = createLicense(serial1, 110L, 0L);
		this.licenses.add(lic1);

		final Feature feature = createFeature(60L, featureCode);

		final Reservation resNew1 = createReservation(60L, serial1, featureCode);
		this.newReservations.add(resNew1);

		final FeatureLicenseUsageDelta calculateUsage = this.bean.calculateUsage(feature, this.oldReservations,
				this.newReservations, this.licenses);

		assertEquals(60, calculateUsage.getFeature().getUsageDelta());
		assertEquals(featureCode, calculateUsage.getFeature().getFeatureCode());

		final LicenseUpdate retLic1 = getLicense(serial1, calculateUsage.getLicense());
		assertEquals(60, retLic1.getUsageDelta());
		assertEquals(serial1, retLic1.getSerialNumber());
		assertEquals(60L, (long) lic1.getUsedCapacity());

	}

	@Test
	public void testNewCapacityAddedFromAnotherLicense() throws Exception {
		final long featureCode = 997L;
		final String serial1 = "123";
		final String serial2 = "1234";

		final Reservation resOld = createReservation(100L, serial1, featureCode);
		this.oldReservations.add(resOld);

		final Reservation resNew1 = createReservation(110L, serial1, featureCode);
		final Reservation resNew2 = createReservation(15L, serial2, featureCode);

		this.newReservations.add(resNew1);
		this.newReservations.add(resNew2);

		final License lic1 = createLicense(serial1, 110L, 100L);
		final License lic2 = createLicense(serial2, 50L, 0L);

		this.licenses.add(lic1);
		this.licenses.add(lic2);

		final Feature feature = createFeature(125L, featureCode);

		final FeatureLicenseUsageDelta calculateUsage = this.bean.calculateUsage(feature, this.oldReservations,
				this.newReservations, this.licenses);

		assertEquals(25, calculateUsage.getFeature().getUsageDelta());
		assertEquals(featureCode, calculateUsage.getFeature().getFeatureCode());

		final LicenseUpdate retLic1 = getLicense(serial1, calculateUsage.getLicense());
		assertEquals(10, retLic1.getUsageDelta());
		assertEquals(serial1, retLic1.getSerialNumber());
		assertEquals(110, (long) lic1.getUsedCapacity());

		final LicenseUpdate retLic2 = getLicense(serial2, calculateUsage.getLicense());
		assertEquals(15, retLic2.getUsageDelta());
		assertEquals(serial2, retLic2.getSerialNumber());
		assertEquals(15, (long) lic2.getUsedCapacity());

	}

	@Test
	public void testNewCapacityAddedFromSameLicense() throws Exception {
		final long featureCode = 997L;
		final String serial = "123";
		final long totalCapcaity = 200L;
		final long usedCapacityBefore = 100L;

		final Reservation resOld = createReservation(usedCapacityBefore, serial, featureCode);
		this.oldReservations.add(resOld);

		final Reservation resNew = createReservation(120L, serial, featureCode);
		this.newReservations.add(resNew);

		final Feature feature = createFeature(120L, featureCode);

		final License lic = createLicense(serial, totalCapcaity, usedCapacityBefore);

		this.licenses.add(lic);

		final FeatureLicenseUsageDelta calculateUsage = this.bean.calculateUsage(feature, this.oldReservations,
				this.newReservations, this.licenses);

		assertEquals(20, calculateUsage.getFeature().getUsageDelta());

		final LicenseUpdate retLic = calculateUsage.getLicense().get(0);
		assertEquals(20, retLic.getUsageDelta());
		assertEquals(120L, (long) lic.getUsedCapacity());
	}

	@Test
	@Ignore("Multiple executions are not allowed now, because of license state modification during usage claclulations")
	public void testNewCapacityAddedFromSameLicenseMultipleExecution() throws Exception {
		final long featureCode = 997L;
		final String serial = "123";
		final long totalCapcaity = 200L;
		final long usedCapacityBefore = 100L;

		final Reservation resOld = createReservation(usedCapacityBefore, serial, featureCode);
		this.oldReservations.add(resOld);

		final Reservation resNew = createReservation(120L, serial, featureCode);
		this.newReservations.add(resNew);

		final Feature feature = createFeature(120L, featureCode);

		final License lic = createLicense(serial, totalCapcaity, usedCapacityBefore);

		this.licenses.add(lic);

		this.bean.calculateUsage(feature, this.oldReservations, this.newReservations, this.licenses);
		final FeatureLicenseUsageDelta calculateUsage = this.bean.calculateUsage(feature, this.oldReservations,
				this.newReservations, this.licenses);

		assertEquals(20, calculateUsage.getFeature().getUsageDelta());

		final LicenseUpdate retLic = calculateUsage.getLicense().get(0);
		assertEquals(20, retLic.getUsageDelta());
		assertEquals(120, (long) lic.getUsedCapacity());
	}

	@Test
	public void testFeatureIncreaseLicenseDecrease() throws Exception {
		final long featureCode = 997L;
		final String serial1 = "s1";
		final String serial2 = "s2";

		final License lic1 = createLicense(serial1, 100L, 10L);
		final License lic2 = createLicense(serial2, 100L, 0L);

		final Reservation resOld = createReservation(10L, serial1, featureCode);
		this.oldReservations.add(resOld);

		final Reservation resNew = createReservation(11L, serial2, featureCode);
		this.newReservations.add(resNew);

		final Feature feature = createFeature(11L, featureCode);

		this.licenses.add(lic1);
		this.licenses.add(lic2);

		final FeatureLicenseUsageDelta calculateUsage = this.bean.calculateUsage(feature, this.oldReservations,
				this.newReservations, this.licenses);

		assertEquals(featureCode, calculateUsage.getFeature().getFeatureCode());
		assertEquals(1L, calculateUsage.getFeature().getUsageDelta());

		final LicenseUpdate licUpdate1 = getLicenseBySerial(calculateUsage.getLicense(), serial1);
		final LicenseUpdate licUpdate2 = getLicenseBySerial(calculateUsage.getLicense(), serial2);

		assertNotNull(licUpdate1);
		assertNotNull(licUpdate2);

		assertEquals(-10L, licUpdate1.getUsageDelta());
		assertEquals(11L, licUpdate2.getUsageDelta());

		assertEquals(0L, (long) lic1.getUsedCapacity());
		assertEquals(11L, (long) lic2.getUsedCapacity());

	}

	private LicenseUpdate getLicense(final String serial, final List<LicenseUpdate> license) {
		for (final LicenseUpdate licenseUpdate : license) {
			if (licenseUpdate.getSerialNumber().equals(serial)) {
				return licenseUpdate;
			}
		}
		return null;
	}

	private License createLicense(final String serial, final Long total, final Long used) {
		final License lic = new License();
		lic.setSerialNumber(serial);
		lic.setTotalCapacity(total);
		lic.setUsedCapacity(used);
		return lic;
	}

	private Feature createFeature(final Long cpacity, final Long featureCode) {
		final Feature feature = new Feature();
		feature.setCapacity(cpacity);
		feature.setFeatureCode(featureCode);
		return feature;
	}

	private Reservation createReservation(final Long capacity, final String serial, final Long featureCode) {
		final Reservation resOld = new Reservation();
		resOld.setCapacity(capacity);
		resOld.setClientId("1");
		resOld.setSerialNumber(serial);
		resOld.setFeatureCode(featureCode);
		return resOld;
	}

	private LicenseUpdate getLicenseBySerial(final List<LicenseUpdate> licenses, final String serial) {
		for (final LicenseUpdate license : licenses) {
			if (license.getSerialNumber().equals(serial)) {
				return license;
			}
		}
		return null;
	}

}
