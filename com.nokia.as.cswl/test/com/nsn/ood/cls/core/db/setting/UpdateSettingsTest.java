/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.setting;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;

import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 * 
 */
public class UpdateSettingsTest {
	private static final SettingKey SETTING_KEY = SettingKey.EXPIRING_LICENSES_THRESHOLD;

	@Test
	public void testSql() throws Exception {
		assertEquals("update cls.settings set value = ? where settingskey = ?", new UpdateSettings(null).sql());
	}

	@Test
	public void testPrepareRow() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "test");
		statementMock.setString(2, SETTING_KEY.name());

		replayAll();
		new UpdateSettings(null).prepareRow(statementMock, setting(SETTING_KEY, "test"));
		verifyAll();
	}

	@Test
	public void testPrepareRowWithNullValue() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, null);
		statementMock.setString(2, SETTING_KEY.name());

		replayAll();
		new UpdateSettings(null).prepareRow(statementMock, setting(SETTING_KEY, ""));
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		new UpdateSettings(null).handle(1);

		try {
			new UpdateSettings(null).handle(0);
			fail();
		} catch (final SQLException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
