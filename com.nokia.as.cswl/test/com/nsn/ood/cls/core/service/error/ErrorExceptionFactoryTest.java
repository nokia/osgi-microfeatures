/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import static com.nsn.ood.cls.core.service.error.ErrorExceptionTest.assertErrorException;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 * 
 */
public class ErrorExceptionFactoryTest {

	private ErrorExceptionFactory factory;

	@Before
	public void setUp() throws Exception {
		this.factory = new ErrorExceptionFactory();
	}

	@Test
	public void testViolation() throws Exception {
		final Throwable cause = new Exception("message1");
		final ErrorException exception = this.factory.violation(cause, violationError("vvv"));
		assertErrorException(exception, "message1", cause, ErrorCode.CONDITIONS_FAIL, "violation",
				violationError("vvv"), null);
	}

	@Test
	public void testLicense() throws Exception {
		final Throwable cause = new Exception("message2");
		final ErrorException exception = this.factory.license(ErrorCode.CLJL_LICENSE_CANCEL_FAIL, cause, "cljlCode",
				license("123"));
		assertErrorException(exception, "message2", cause, ErrorCode.CLJL_LICENSE_CANCEL_FAIL, "license",
				license("123"), "cljlCode");
	}

	@Test
	public void testLicenseNotFound() throws Exception {
		final ErrorException exception = this.factory.licenseNotFound(license("234"));
		assertErrorException(exception, "License does not exist", null, ErrorCode.RESOURCE_NOT_FOUND, "license",
				license("234"), null);
	}

	@Test
	public void testClient() throws Exception {
		final Throwable cause = new Exception("message3");
		final ErrorException exception = this.factory.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, cause, client("11"));
		assertErrorException(exception, "message3", cause, ErrorCode.CANNOT_RESERVE_CLIENT_ID, "client", client("11"),
				null);
	}

	@Test
	public void testClientNotFound() throws Exception {
		final ErrorException exception = this.factory.clientNotFound(client("22"));
		assertErrorException(exception, "Client does not exist", null, ErrorCode.RESOURCE_NOT_FOUND, "client",
				client("22"), null);
	}

	@Test
	public void testFeature() throws Exception {
		final Throwable cause = new Exception("message4");
		final ErrorException exception = this.factory.feature(ErrorCode.NOT_ENOUGH_CAPACITY, cause, featureError(33L));
		assertErrorException(exception, "message4", cause, ErrorCode.NOT_ENOUGH_CAPACITY, "feature", featureError(33L),
				null);
	}

	@Test
	public void testFeatureNotFound() throws Exception {
		final ErrorException exception = this.factory.featureNotFound(featureError(44L));
		assertErrorException(exception, "Feature does not exist", null, ErrorCode.RESOURCE_NOT_FOUND, "feature",
				featureError(44L), null);
	}

	@Test
	public void testSetting() throws Exception {
		final Throwable cause = new Exception("message5");
		final ErrorException exception = this.factory.setting(ErrorCode.CONFIGURATION_UPDATE_FAIL, cause,
				setting(SettingKey.EMAIL_SUBJECT, "value"));
		assertErrorException(exception, "message5", cause, ErrorCode.CONFIGURATION_UPDATE_FAIL, "setting",
				setting(SettingKey.EMAIL_SUBJECT, "value"), null);
	}

	@Test
	public void testError() throws Exception {
		final Throwable cause = new Exception("message6");
		final ErrorException exception = this.factory.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, cause);
		assertErrorException(exception, "message6", cause, ErrorCode.CONCURRENT_ACTIONS_FAIL, null, null, null);
	}
}
