/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.config;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.SettingRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 * 
 */
public class ConfigurationTest {
	private SettingRetrieveOperation settingRetrieveOperationMock;
	private Configuration configuration;

	@Before
	public void setUp() throws Exception {
		this.settingRetrieveOperationMock = createMock(SettingRetrieveOperation.class);

		this.configuration = new Configuration();
		setInternalState(this.configuration, this.settingRetrieveOperationMock);
	}

	@Test
	public void testGetDefaultFloatingReleaseTime() throws Exception {
		testSetting(SettingKey.FLOATING_RELEASE_TIME, 33L, new ConfigMethodCaller<Long>() {
			@Override
			public Long call(final Configuration configuration) {
				return configuration.getDefaultFloatingReleaseTime();
			}
		});
	}

	@Test
	public void testGetExpiringLicensesThreshold() throws Exception {
		testSetting(SettingKey.EXPIRING_LICENSES_THRESHOLD, 123L, new ConfigMethodCaller<Long>() {
			@Override
			public Long call(final Configuration configuration) {
				return configuration.getExpiringLicensesThreshold();
			}
		});
	}

	@Test
	public void testIsEmailNotificationsEnabled() throws Exception {
		testSetting(SettingKey.EMAIL_NOTIFICATIONS, true, new ConfigMethodCaller<Boolean>() {
			@Override
			public Boolean call(final Configuration configuration) {
				return configuration.isEmailNotificationsEnabled();
			}
		});
	}

	@Test
	public void testGetEmailServer() throws Exception {
		testSetting(SettingKey.EMAIL_SERVER, "server", new ConfigMethodCaller<String>() {
			@Override
			public String call(final Configuration configuration) {
				return configuration.getEmailServer();
			}
		});
	}

	@Test
	public void testGetEmailSubjectPrefix() throws Exception {
		testSetting(SettingKey.EMAIL_SUBJECT, "subject", new ConfigMethodCaller<String>() {
			@Override
			public String call(final Configuration configuration) {
				return configuration.getEmailSubjectPrefix();
			}
		});
	}

	@Test
	public void testGetEmailSender() throws Exception {
		testSetting(SettingKey.EMAIL_SENDER, "sender", new ConfigMethodCaller<String>() {
			@Override
			public String call(final Configuration configuration) {
				return configuration.getEmailSender();
			}
		});
	}

	@Test
	public void testGetEmailRecipients() throws Exception {
		testSetting(SettingKey.EMAIL_RECIPIENTS, "recipients", new ConfigMethodCaller<String>() {
			@Override
			public String call(final Configuration configuration) {
				return configuration.getEmailRecipients();
			}
		});
	}

	@Test
	public void testGetCapacityThreshold() throws Exception {
		testSetting(SettingKey.CAPACITY_THRESHOLD, 80L, new ConfigMethodCaller<Long>() {
			@Override
			public Long call(final Configuration configuration) {
				return configuration.getCapacityThreshold();
			}
		});
	}

	private interface ConfigMethodCaller<T> {
		T call(Configuration configuration);
	}

	@SuppressWarnings("unchecked")
	private <T> void testSetting(final SettingKey key, final T expectedResult, final ConfigMethodCaller<T> caller)
			throws RetrieveException {
		testGetSpecificSetting(key, settingsList(setting(null, expectedResult)), expectedResult, caller);
		testGetSpecificSetting(key, settingsList(), (T) key.defaultValue(), caller);
		testGetSpecificSetting(key, null, (T) key.defaultValue(), caller);
		testGetSpecificSettingAndExpectException(key, (T) key.defaultValue(), caller);
	}

	private <T> void testGetSpecificSetting(final SettingKey key, final List<Setting> settings, final T expectedResult,
			final ConfigMethodCaller<T> caller) throws RetrieveException {
		resetAll();

		final Capture<Conditions> capturedConditions = new Capture<>();

		expect(this.settingRetrieveOperationMock.getList(capture(capturedConditions))).andReturn(
				new MetaDataList<Setting>(settings, null));

		replayAll();
		assertEquals(expectedResult, caller.call(this.configuration));
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("key", key.toString()).build(),
				capturedConditions.getValue());
	}

	private <T> void testGetSpecificSettingAndExpectException(final SettingKey key, final T expectedResult,
			final ConfigMethodCaller<T> caller) throws RetrieveException {
		resetAll();

		final Capture<Conditions> capturedConditions = new Capture<>();
		final RetrieveException exceptionMock = createNiceMock(RetrieveException.class);

		expect(this.settingRetrieveOperationMock.getList(capture(capturedConditions))).andThrow(exceptionMock);

		replayAll();
		assertEquals(expectedResult, caller.call(this.configuration));
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("key", key.toString()).build(),
				capturedConditions.getValue());
	}
}
