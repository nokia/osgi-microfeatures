/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class LicenseType2IntegerConverterTest {
	private LicenseType2IntegerConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicenseType2IntegerConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals(Integer.valueOf(2), this.converter.convertTo(Type.POOL));
		assertEquals(Integer.valueOf(4), this.converter.convertTo(Type.FLOATING_POOL));

		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(Type.POOL, this.converter.convertFrom(2));
		assertEquals(Type.FLOATING_POOL, this.converter.convertFrom(4));

		try {
			this.converter.convertFrom(0);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
