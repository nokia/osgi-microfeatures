/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 * 
 */
public class SettingCreatorTest {
	private SettingCreator creator;

	@Before
	public void setUp() throws Exception {
		this.creator = new SettingCreator();
	}

	@Test
	public void testCreateSetting() throws Exception {
		testCreateSetting(SettingKey.FLOATING_RELEASE_TIME, "77", 77L);
		testCreateSetting(SettingKey.FLOATING_RELEASE_TIME, "test", SettingKey.FLOATING_RELEASE_TIME.defaultValue());

		testCreateSetting(SettingKey.EXPIRING_LICENSES_THRESHOLD, "55", 55L);
		testCreateSetting(SettingKey.EXPIRING_LICENSES_THRESHOLD, null,
				SettingKey.EXPIRING_LICENSES_THRESHOLD.defaultValue());

		testCreateSetting(SettingKey.EMAIL_NOTIFICATIONS, "true", true);
		testCreateSetting(SettingKey.EMAIL_NOTIFICATIONS, "FALSE", false);
		testCreateSetting(SettingKey.EMAIL_NOTIFICATIONS, "test", SettingKey.EMAIL_NOTIFICATIONS.defaultValue());

		testCreateSetting(SettingKey.EMAIL_SUBJECT, "test", "test");
		testCreateSetting(SettingKey.EMAIL_SUBJECT, null, "");
		testCreateSetting(SettingKey.EMAIL_SUBJECT, "", "");
	}

	private void testCreateSetting(final SettingKey key, final String dbValue, final Object expectedValue)
			throws SQLException {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("settingskey")).andReturn(key.name());
		expect(resultSetMock.getString("value")).andReturn(dbValue);

		replayAll();
		final Setting setting = this.creator.createSetting(resultSetMock);
		verifyAll();

		assertEquals(setting(key, expectedValue), setting);
	}

	@Test
	public void testCreateSettingWithIllegalKey() throws Exception {
		testCreateSettingAndExpectSQLException(null);
		testCreateSettingAndExpectSQLException("test");
	}

	private void testCreateSettingAndExpectSQLException(final String dbKey) throws SQLException {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("settingskey")).andReturn(dbKey);

		replayAll();
		try {
			this.creator.createSetting(resultSetMock);
			fail();
		} catch (final SQLException e) {
			assertFalse(e.getMessage().isEmpty());
		}
		verifyAll();
	}

	@Test
	public void testCreateSettingWithCustomType() throws Exception {
		testCreateSettingWithCustomType("default", "value");

		final Object defaultValue = new Object();
		testCreateSettingWithCustomType(defaultValue, defaultValue);
	}

	private void testCreateSettingWithCustomType(final Object defaultValue, final Object expectedValue)
			throws SQLException {
		final ResultSet resultSetMock = createMock(ResultSet.class);
		final SettingKey settingKeyMock = createMock(SettingKey.class);

		expect(resultSetMock.getString("settingskey")).andReturn("key");
		expect(resultSetMock.getString("value")).andReturn("value");
		expect(settingKeyMock.defaultValue()).andReturn(defaultValue);

		replayAll();
		final Setting setting = new SettingCreator() {
			@Override
			protected SettingKey getSettingKey(final String key) throws SQLException {
				assertEquals("key", key);
				return settingKeyMock;
			}
		}.createSetting(resultSetMock);
		verifyAll();

		assertEquals(setting(settingKeyMock, expectedValue), setting);
	}
}
