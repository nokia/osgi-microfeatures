/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.model.LicenseState;


/**
 * @author marynows
 * 
 */
public class LicenseState2StringConverterTest {
	private LicenseState2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicenseState2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("active", this.converter.convertTo(LicenseState.ACTIVE));
		assertEquals("expired", this.converter.convertTo(LicenseState.EXPIRED));
		assertEquals("inactive", this.converter.convertTo(LicenseState.INACTIVE));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(LicenseState.ACTIVE, this.converter.convertFrom("active"));
		assertEquals(LicenseState.EXPIRED, this.converter.convertFrom("expired"));
		assertEquals(LicenseState.INACTIVE, this.converter.convertFrom("inactive"));
		assertNull(this.converter.convertFrom(null));
		assertNull(this.converter.convertFrom(""));
		assertNull(this.converter.convertFrom("test"));
	}
}
