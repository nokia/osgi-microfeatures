/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.assertStoredLicense;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class StoredLicenseTest {
	private static final DateTime TIME = new DateTime(2015, 6, 30, 15, 52);

	@Test
	public void testEmptyStoredLicense() throws Exception {
		assertStoredLicense(new StoredLicense(), null, null, null, null, null, null);
	}

	@Test
	public void testStoredLicense() throws Exception {
		assertStoredLicense(new StoredLicense().withCustomerName("name"), "name", null, null, null, null, null);
		assertStoredLicense(new StoredLicense().withCustomerId("id"), null, "id", null, null, null, null);
		assertStoredLicense(new StoredLicense().withOrderId("order"), null, null, "order", null, null, null);
		assertStoredLicense(new StoredLicense().withUser("user"), null, null, null, "user", null, null);
		assertStoredLicense(new StoredLicense().withImportDate(TIME), null, null, null, null, TIME, null);
		assertStoredLicense(new StoredLicense().withRemainingCapacity(13L), null, null, null, null, null, 13L);

		assertStoredLicense(new StoredLicense().withCustomerName("nnn").withCustomerId("iii").withOrderId("ooo")
				.withUser("uuu").withImportDate(TIME).withRemainingCapacity(17L), "nnn", "iii", "ooo", "uuu", TIME, 17L);
	}

	@Test
	public void testStoredLicenseSetters() throws Exception {
		final StoredLicense storedLicense = new StoredLicense();
		storedLicense.setCustomerName("aaa");
		storedLicense.setCustomerId("bbb");
		storedLicense.setOrderId("ccc");
		storedLicense.setUser("ddd");
		storedLicense.setImportDate(TIME);
		storedLicense.setRemainingCapacity(32L);

		assertStoredLicense(storedLicense, "aaa", "bbb", "ccc", "ddd", TIME, 32L);
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(StoredLicense.class, "customerName", "customerId", "orderId", "user", "importDate",
				"remainingCapacity");
		assertJsonProperty(StoredLicense.class, "customerName", "customerName");
		assertJsonProperty(StoredLicense.class, "customerId", "customerId");
		assertJsonProperty(StoredLicense.class, "orderId", "orderId");
		assertJsonProperty(StoredLicense.class, "user", "user");
		assertJsonProperty(StoredLicense.class, "importDate", "importDate");
		assertJsonProperty(StoredLicense.class, "remainingCapacity", "remainingCapacity");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new StoredLicense().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final StoredLicense license = (StoredLicense) new StoredLicense().withSerialNumber("123");

		assertFalse(license.equals(null));
		assertFalse(license.equals("test"));
		assertEquals(license, license);

		assertFalse(license.equals(new StoredLicense().withSerialNumber("234")));
		assertNotEquals(license.hashCode(), new StoredLicense().withSerialNumber("234").hashCode());

		final StoredLicense license2 = (StoredLicense) new StoredLicense().withSerialNumber("123");
		assertEquals(license, license2);
		assertEquals(license.hashCode(), license2.hashCode());
	}
}
