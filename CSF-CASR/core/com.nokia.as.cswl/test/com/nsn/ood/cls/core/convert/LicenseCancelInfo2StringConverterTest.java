/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class LicenseCancelInfo2StringConverterTest {
	private LicenseCancelInfo2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new LicenseCancelInfo2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(info(null, null, null)));

		assertEquals("[fileName=file]", this.converter.convertTo(info("file", null, null)));
		assertEquals("[serialNumber=123]", this.converter.convertTo(info(null, "123", null)));
		assertEquals("[user=uuu]", this.converter.convertTo(info(null, null, "uuu")));

		assertEquals("[fileName=file, serialNumber=123, user=uuu]",
				this.converter.convertTo(info("file", "123", "uuu")));
	}

	private LicenseCancelInfo info(final String fileName, final String serialNbr, final String userName) {
		final LicenseCancelInfo info = new LicenseCancelInfo();
		info.setLicenseFileName(fileName);
		info.setSerialNbr(serialNbr);
		info.setUserName(userName);
		return info;
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
