/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class LicenseMode2IntegerConverterTest {
	private LicenseMode2IntegerConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicenseMode2IntegerConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals(Integer.valueOf(1), this.converter.convertTo(Mode.ON_OFF));
		assertEquals(Integer.valueOf(2), this.converter.convertTo(Mode.CAPACITY));

		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(Mode.ON_OFF, this.converter.convertFrom(1));
		assertEquals(Mode.CAPACITY, this.converter.convertFrom(2));

		try {
			this.converter.convertFrom(0);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
