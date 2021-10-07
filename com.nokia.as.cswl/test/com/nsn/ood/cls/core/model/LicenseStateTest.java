/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class LicenseStateTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("inactive", LicenseState.INACTIVE.toString());
		assertEquals("active", LicenseState.ACTIVE.toString());
		assertEquals("expired", LicenseState.EXPIRED.toString());
	}

	@Test
	public void testFromValue() throws Exception {
		assertEquals(LicenseState.INACTIVE, LicenseState.fromValue("inactive"));
		assertEquals(LicenseState.ACTIVE, LicenseState.fromValue("active"));
		assertEquals(LicenseState.EXPIRED, LicenseState.fromValue("expired"));
		try {
			LicenseState.fromValue("test");
			fail();
		} catch (final IllegalArgumentException e) {
		}
		try {
			LicenseState.fromValue(null);
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}
}
