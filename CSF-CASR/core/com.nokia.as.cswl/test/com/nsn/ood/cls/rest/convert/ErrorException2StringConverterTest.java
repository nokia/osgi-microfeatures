/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.convert;

import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class ErrorException2StringConverterTest {
	private ErrorException2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new ErrorException2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[errorCode=4]", this.converter.convertTo(new ErrorExceptionFactory().error(
				ErrorCode.RESOURCE_NOT_FOUND, new Exception())));
		assertEquals("[errorCode=4, message=mmm]", this.converter.convertTo(new ErrorExceptionFactory().error(
				ErrorCode.RESOURCE_NOT_FOUND, new Exception("mmm"))));
		assertEquals("[errorCode=4, message=mmm;aaa]", this.converter.convertTo(new ErrorExceptionFactory().error(
				ErrorCode.RESOURCE_NOT_FOUND, new Exception("mmm\naaa"))));

		assertEquals("[errorCode=102, featureCode=12]", this.converter.convertTo(new ErrorExceptionFactory().feature(
				ErrorCode.ON_OFF_LICENSE_MISSING, new Exception(), featureError(12L))));
		assertEquals("[errorCode=101, featureCode=12, capacity=23]",
				this.converter.convertTo(new ErrorExceptionFactory().feature(ErrorCode.CANNOT_RELEASE_CAPACITY,
						new Exception(), featureError(12L, 23L))));
		assertEquals("[errorCode=100, featureCode=12, requestedCapacity=23, remainingCapacity=34]",
				this.converter.convertTo(new ErrorExceptionFactory().feature(ErrorCode.NOT_ENOUGH_CAPACITY,
						new Exception(), featureError(12L, 23L, 34L))));

		assertEquals("[errorCode=151]", this.converter.convertTo(new ErrorExceptionFactory().license(
				ErrorCode.LICENSE_VERIFICATION_FAIL, new Exception(), "cljlCode", license())));
		assertEquals("[errorCode=150, fileName=file]", this.converter.convertTo(new ErrorExceptionFactory().license(
				ErrorCode.CLJL_LICENSE_INSTALL_FAIL, new Exception(), "cljlCode", license().withFileName("file"))));
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
