/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.error;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
public class ServiceExceptionFactoryTest {
	private ErrorExceptionFactory errorExceptionFactoryMock;
	private ServiceExceptionFactory factory;

	@Before
	public void setUp() throws Exception {
		this.errorExceptionFactoryMock = createMock(ErrorExceptionFactory.class);
		this.factory = new ServiceExceptionFactory();
		setInternalState(factory, errorExceptionFactoryMock);
	}

	@Test
	public void testViolation() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(this.errorExceptionFactoryMock.violation(cause, violationError("vv"))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.violation(cause, violationError("vv")), false, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testLicense() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(
				this.errorExceptionFactoryMock.license(ErrorCode.CLJL_LICENSE_INSTALL_FAIL, cause, "cljlCode",
						license("22"))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(
				this.factory.license(ErrorCode.CLJL_LICENSE_INSTALL_FAIL, cause, "cljlCode", license("22")), false,
				errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testLicenseNotFound() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		expect(this.errorExceptionFactoryMock.licenseNotFound(license("33"))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.licenseNotFound(license("33")), true, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testClient() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(this.errorExceptionFactoryMock.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, cause, client("44")))
				.andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, cause, client("44")), false,
				errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testClientNotFound() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		expect(this.errorExceptionFactoryMock.clientNotFound(client("55"))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.clientNotFound(client("55")), true, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testFeature() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(this.errorExceptionFactoryMock.feature(ErrorCode.NOT_ENOUGH_CAPACITY, cause, featureError(66L)))
				.andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.feature(ErrorCode.NOT_ENOUGH_CAPACITY, cause, featureError(66L)), false,
				errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testFeatureNotFound() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		expect(this.errorExceptionFactoryMock.featureNotFound(featureError(77L))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.featureNotFound(featureError(77L)), true, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testSetting() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(
				this.errorExceptionFactoryMock.setting(ErrorCode.CONFIGURATION_UPDATE_FAIL, cause,
						setting(SettingKey.EMAIL_SENDER, "value"))).andReturn(errorExceptionMock);

		replayAll();
		assertServiceException(
				this.factory.setting(ErrorCode.CONFIGURATION_UPDATE_FAIL, cause,
						setting(SettingKey.EMAIL_SENDER, "value")), false, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testError() throws Exception {
		final ErrorException errorExceptionMock = createMock(ErrorException.class);
		final Throwable cause = new Exception();

		expect(this.errorExceptionFactoryMock.error(ErrorCode.DUPLICATED_CLIENT_ID, cause)).andReturn(
				errorExceptionMock);

		replayAll();
		assertServiceException(this.factory.error(ErrorCode.DUPLICATED_CLIENT_ID, cause), false, errorExceptionMock);
		verifyAll();
	}

	@Test
	public void testExceptions() throws Exception {
		final ErrorException errorExceptionMock1 = createMock(ErrorException.class);
		final ErrorException errorExceptionMock2 = createMock(ErrorException.class);

		replayAll();
		assertServiceException(this.factory.exceptions(Arrays.asList(errorExceptionMock1, errorExceptionMock2)), false,
				errorExceptionMock1, errorExceptionMock2);
		verifyAll();
	}

	@Test
	public void testExceptionsWithEmptyErrorsList() throws Exception {
		replayAll();
		try {
			this.factory.exceptions(Collections.<ErrorException> emptyList());
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
		verifyAll();
	}

	private void assertServiceException(final ServiceException serviceException, final boolean notFound,
			final ErrorException... errorExceptions) {
		assertEquals(Arrays.asList(errorExceptions), serviceException.getExceptions());
		assertEquals(notFound, serviceException.isNotFound());
	}
}
