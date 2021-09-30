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

import com.nokia.licensing.dtos.AddnColumns.LicenseType;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class StoredLicenseType2LicenseTypeConverterTest {
	private StoredLicenseType2LicenseTypeConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new StoredLicenseType2LicenseTypeConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals(Type.FLOATING_POOL, this.converter.convertTo(LicenseType.FLOATING_POOL));
		assertEquals(Type.POOL, this.converter.convertTo(LicenseType.POOL));
		assertNull(this.converter.convertTo(LicenseType.FLOATING_NMS));
		assertNull(this.converter.convertTo(LicenseType.NE));
		assertNull(this.converter.convertTo(LicenseType.NMS));
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
