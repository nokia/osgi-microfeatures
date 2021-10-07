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
import com.nsn.ood.cls.model.internal.StoredLicense;


/**
 * @author marynows
 * 
 */
public class StoredLicenseTestUtil {

	public static List<StoredLicense> storedLicensesList(final StoredLicense... storedLicenses) {
		return Arrays.asList(storedLicenses);
	}

	public static StoredLicense storedLicense() {
		return new StoredLicense();
	}

	public static StoredLicense storedLicense(final String serialNumber) {
		return (StoredLicense) storedLicense().withSerialNumber(serialNumber);
	}

	public static StoredLicense storedLicense(final String customerName, final String customerId, final String orderId,
			final String user, final DateTime importDate, final Long remainingCapacity) {
		return storedLicense().withCustomerName(customerName).withCustomerId(customerId).withOrderId(orderId)
				.withUser(user).withImportDate(importDate).withRemainingCapacity(remainingCapacity);
	}

	public static StoredLicense storedLicense(final String customerName, final String customerId, final String orderId,
			final String user, final DateTime importDate, final Long remainingCapacity, final String serialNumber,
			final Type type, final Mode mode, final DateTime startDate, final DateTime endDate,
			final Long totalCapacity, final Long usedCapacity, final String capacityUnit, final String code,
			final String name, final String fileName, final String targetType) {
		return (StoredLicense) storedLicense(customerName, customerId, orderId, user, importDate, remainingCapacity)
				.withSerialNumber(serialNumber).withType(type).withMode(mode).withStartDate(startDate)
				.withEndDate(endDate).withTotalCapacity(totalCapacity).withUsedCapacity(usedCapacity)
				.withCapacityUnit(capacityUnit).withCode(code).withName(name).withFileName(fileName)
				.withTargetType(targetType);
	}

	public static void assertStoredLicense(final StoredLicense storedLicense, final String expectedCustomerName,
			final String expectedCustomerId, final String expectedOrderId, final String expectedUser,
			final DateTime expectedImportDate, final Long expectedRemainingCapacity) {
		assertEquals(expectedCustomerName, storedLicense.getCustomerName());
		assertEquals(expectedCustomerId, storedLicense.getCustomerId());
		assertEquals(expectedOrderId, storedLicense.getOrderId());
		assertEquals(expectedUser, storedLicense.getUser());
		assertEquals(expectedImportDate, storedLicense.getImportDate());
		assertEquals(expectedRemainingCapacity, storedLicense.getRemainingCapacity());
	}
}
