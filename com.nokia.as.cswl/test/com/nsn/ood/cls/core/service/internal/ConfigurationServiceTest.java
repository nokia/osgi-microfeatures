/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static com.nsn.ood.cls.util.test.AnnotationTestUtil.assertAnnotation;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.audit.AuditLog;
import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.operation.SettingRetrieveOperation;
import com.nsn.ood.cls.core.operation.SettingUpdateOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 * 
 */
public class ConfigurationServiceTest {
	private static final SettingKey SETTING_KEY = SettingKey.FLOATING_RELEASE_TIME;

	private SettingRetrieveOperation settingRetrieveOperationMock;
	private SettingUpdateOperation settingUpdateOperationMock;
	private PlatformPreferences platformPreferencesMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private ConfigurationService service;

	@Before
	public void setUp() throws Exception {
		this.settingRetrieveOperationMock = createMock(SettingRetrieveOperation.class);
		this.settingUpdateOperationMock = createMock(SettingUpdateOperation.class);
		this.platformPreferencesMock = createMock(PlatformPreferences.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new ConfigurationService();
		setInternalState(this.service, this.settingRetrieveOperationMock, this.settingUpdateOperationMock,
				this.platformPreferencesMock, this.serviceExceptionFactoryMock);
	}

	@Test
	public void testGetTargetId() throws Exception {
		expect(this.platformPreferencesMock.getTargetId()).andReturn("target");

		replayAll();
		assertEquals("target", this.service.getTargetId());
		verifyAll();
	}

	@Test
	public void testGetSetting() throws Exception {
		final Conditions conditions = createMock(Conditions.class);
		final MetaDataList<Setting> metaDataList = new MetaDataList<Setting>();

		expect(this.settingRetrieveOperationMock.getList(conditions)).andReturn(metaDataList);

		replayAll();
		assertEquals(metaDataList, this.service.getSettings(conditions));
		verifyAll();
	}

	@Test
	public void testGetSettingAndExpectError() throws Exception {
		final Conditions conditions = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.settingRetrieveOperationMock.getList(conditions)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("message"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("message"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getSettings(conditions);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testSetSetting() throws Exception {
		this.settingUpdateOperationMock.updateSettings(settingsList(setting(SETTING_KEY, "test")));

		replayAll();
		this.service.setSettings(settingsList(setting(SETTING_KEY, "test")));
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectError() throws Exception {
		final UpdateException exceptionMock = createMock(UpdateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		this.settingUpdateOperationMock.updateSettings(settingsList(setting(SETTING_KEY, "test")));
		expectLastCall().andThrow(exceptionMock);
		expect(exceptionMock.getIndex()).andReturn(0);
		expect(
				this.serviceExceptionFactoryMock.setting(ErrorCode.CONFIGURATION_UPDATE_FAIL, exceptionMock,
						setting(SETTING_KEY, "test"))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.setSettings(settingsList(setting(SETTING_KEY, "test")));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testAnnotation() throws Exception {
		assertAnnotation(ConfigurationService.class.getMethod("setSettings", List.class), AuditLog.class);
	}
}
