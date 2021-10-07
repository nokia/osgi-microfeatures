/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
public class License2LicenseCancelInfoConverterTest {
	private License2LicenseCancelInfoConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new License2LicenseCancelInfoConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertLicenseCancelInfo(this.converter.convertTo(license()), null, null);
		assertLicenseCancelInfo(this.converter.convertTo(license("123")), "123", null);
		assertLicenseCancelInfo(this.converter.convertTo(license("123").withFileName("fileName")), "123", "fileName");
		assertLicenseCancelInfo(this.converter.convertTo(license().withFileName("fileName")), null, "fileName");
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

	private void assertLicenseCancelInfo(final LicenseCancelInfo license, final String expectedSerialNumber,
			final String expectedFileName) {
		assertEquals(expectedSerialNumber, license.getSerialNbr());
		assertEquals(expectedFileName, license.getLicenseFileName());
		assertEquals(0L, license.getFeaturecode());
		assertNull(license.getUserName());
		assertNull(license.getCancelReason());
		assertNull(license.getCancelDate());
		assertEquals(0, license.getId());
		assertNull(license.getCancelListSignature());
	}
}
