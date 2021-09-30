/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.audit;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		AuditLogInterceptor.class, LoggerFactory.class })
public class AuditLogInterceptorTest {
	private static final String USER = "UUU";

	private AuditLogInterceptor interceptor;
	private BasicPrincipal basicPrincipal;

	@Before
	public void setUp() throws Exception {
		this.basicPrincipal = new BasicPrincipal();
		this.basicPrincipal.setUser(USER);

		this.interceptor = new AuditLogInterceptor();
		setInternalState(this.interceptor, this.basicPrincipal);

		mockStatic(LoggerFactory.class);
	}

	@Test
	public void testUndefined() throws Throwable {
		final JoinPoint ctxMock = createMock(JoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(TestAuditLog.class.getMethod("undefined"));
		expect(LoggerFactory.getLogger(TestAuditLog.class)).andReturn(loggerMock);

		replayAll();
		try {
			this.interceptor.auditLog(ctxMock);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
		verifyAll();
	}

	@Test
	public void testDefault() throws Exception {
		testDefault("licenseInstall", AuditLogType.LICENSE_INSTALLATION);
		testDefault("licenseTerminate", AuditLogType.LICENSE_TERMINATION);
	}

	private void testDefault(final String methodName, final AuditLogType auditLogType) throws Exception {
		resetAll();

		final JoinPoint ctxMock = createMock(JoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(TestAuditLog.class.getMethod(methodName));
		expect(LoggerFactory.getLogger(TestAuditLog.class)).andReturn(loggerMock);
		loggerMock.info("AUDIT: user {}, operation {}", USER, auditLogType.toString());

		replayAll();
		this.interceptor.auditLog(ctxMock);
		verifyAll();
	}

	@Test
	public void testSetting() throws Exception {
		final JoinPoint ctxMock = createMock(JoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(TestAuditLog.class.getMethod("settings"));
		expect(LoggerFactory.getLogger(TestAuditLog.class)).andReturn(loggerMock);
		expect(ctxMock.getArgs()).andReturn(new Object[] {
			settingsList(setting(SettingKey.FLOATING_RELEASE_TIME, 123L)) });
		loggerMock.info("AUDIT: user {}, operation {}, setting {}, value {}", USER,
				AuditLogType.CHANGE_SETTING.toString(), SettingKey.FLOATING_RELEASE_TIME.name(), 123L);

		replayAll();
		this.interceptor.auditLog(ctxMock);
		verifyAll();
	}

	@Test
	public void testSettingWithWrongParameters() throws Exception {
		testSettingWithWrongParameters(new Object[] {});
		testSettingWithWrongParameters(new Object[] {
			"test" });
	}

	private void testSettingWithWrongParameters(final Object[] parameters) throws Exception {
		resetAll();

		final JoinPoint ctxMock = createMock(JoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(TestAuditLog.class.getMethod("settings"));
		expect(LoggerFactory.getLogger(TestAuditLog.class)).andReturn(loggerMock);
		expect(ctxMock.getArgs()).andReturn(parameters);

		replayAll();
		this.interceptor.auditLog(ctxMock);
		verifyAll();
	}
}

class TestAuditLog {

	@AuditLog
	public void undefined() {
	}

	@AuditLog(AuditLogType.CHANGE_SETTING)
	public void settings() {
	}

	@AuditLog(AuditLogType.LICENSE_INSTALLATION)
	public void licenseInstall() {
	}

	@AuditLog(AuditLogType.LICENSE_TERMINATION)
	public void licenseTerminate() {
	}
}
