/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class License2StringConverterTest {
	private static final DateTime END_DATE = new DateTime();

	private License2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new License2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(license()));

		assertEquals("[fileName=file]", this.converter.convertTo(license().withFileName("file")));
		assertEquals("[serialNumber=123]", this.converter.convertTo(license("123")));
		assertEquals("[mode=capacity]", this.converter.convertTo(license().withMode(Mode.CAPACITY)));
		assertEquals("[type=pool]", this.converter.convertTo(license().withType(Type.POOL)));
		assertEquals("[endDate=" + END_DATE + "]", this.converter.convertTo(license().withEndDate(END_DATE)));
		assertEquals("[targetType=type]", this.converter.convertTo(license().withTargetType("type")));
		assertEquals("[usedCapacity=20]", this.converter.convertTo(license(null, 20L)));
		assertEquals("[totalCapacity=100]", this.converter.convertTo(license(100L, null)));

		assertEquals("[fileName=file, serialNumber=123, mode=capacity, type=pool, endDate=" + END_DATE
				+ ", targetType=type, usedCapacity=20, totalCapacity=100]",
				this.converter.convertTo(license("123", Type.POOL, Mode.CAPACITY, END_DATE, 100L, 20L, "file", "type")));

	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
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
