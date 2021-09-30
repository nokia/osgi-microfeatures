/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.assertActivityDetail;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class ErrorException2ActivityDetailConverterTest {
	private ErrorException2ActivityDetailConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new ErrorException2ActivityDetailConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().error(ErrorCode.NOT_ENOUGH_CAPACITY,
				new Exception())), "100", Status.FAILURE, null, null, null, null, null);

		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().license(
				ErrorCode.CANNOT_RELEASE_CAPACITY, new Exception(), "CLJL", license().withFileName("file"))),
				"101,CLJL", Status.FAILURE, "file", null, null, null, null);
		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().license(
				ErrorCode.CANNOT_RELEASE_CAPACITY, new Exception(), "CLJL", license().withFileName(null))), "101,CLJL",
				Status.FAILURE, null, null, null, null, null);

		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().setting(
				ErrorCode.CONFIGURATION_UPDATE_FAIL, new Exception(), setting(SettingKey.EMAIL_SENDER, "value"))),
				"122", Status.FAILURE, null, null, null, "emailSender", "value");
		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().setting(
				ErrorCode.CONFIGURATION_UPDATE_FAIL, new Exception(), setting(null, null))), "122", Status.FAILURE,
				null, null, null, null, null);
		assertActivityDetail(this.converter.convertTo(new ErrorExceptionFactory().setting(
				ErrorCode.CONFIGURATION_UPDATE_FAIL, new Exception(), setting(SettingKey.EMAIL_SERVER, ""))), "122",
				Status.FAILURE, null, null, null, "emailServer", null);
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
