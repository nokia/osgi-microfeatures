/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.AddnColumns.LicenseMode;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class StoredLicenseMode2LicenseModeConverterTest {
	private StoredLicenseMode2LicenseModeConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new StoredLicenseMode2LicenseModeConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals(Mode.CAPACITY, this.converter.convertTo(LicenseMode.CAPACITY));
		assertEquals(Mode.ON_OFF, this.converter.convertTo(LicenseMode.ONOFF));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
