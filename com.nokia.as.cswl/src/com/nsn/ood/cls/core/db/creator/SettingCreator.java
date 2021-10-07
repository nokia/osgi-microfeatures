/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.Strings;


/**
 * @author marynows
 *
 */
@Component(provides = SettingCreator.class)
public class SettingCreator {

	public Setting createSetting(final ResultSet resultSet) throws SQLException {
		final Setting setting = createSetting(resultSet.getString("settingskey"));
		setValue(setting, resultSet.getString("value"));
		return setting;
	}

	private Setting createSetting(final String key) throws SQLException {
		final SettingKey settingKey = getSettingKey(key);
		return new Setting().withKey(settingKey).withValue(settingKey.defaultValue());
	}

	protected SettingKey getSettingKey(final String key) throws SQLException {
		try {
			return SettingKey.valueOf(key);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new SQLException("Illegal key value: " + key, e);
		}
	}

	private void setValue(final Setting setting, final String value) {
		if (setting.getValue() instanceof Long) {
			try {
				setting.setValue(Long.valueOf(value));
			} catch (final NumberFormatException e) {
			}
		} else if (setting.getValue() instanceof String) {
			setting.setValue(Strings.nullToEmpty(value));
		} else if (setting.getValue() instanceof Boolean) {
			if ("true".equalsIgnoreCase(value)) {
				setting.setValue(true);
			} else if ("false".equalsIgnoreCase(value)) {
				setting.setValue(false);
			}
		}
	}
}
