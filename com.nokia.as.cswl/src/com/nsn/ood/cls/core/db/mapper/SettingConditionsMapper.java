/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 *
 */
@Component(provides = ConditionsMapper.class)
@Property(name = "name", value = "setting")
public class SettingConditionsMapper extends ConditionsMapper {

	@Start
	public void start() {
		map("key", "settingskey", String.class, new KeyConverter());
	}

	private static class KeyConverter implements ValueConverter<String> {
		@Override
		public String prepareConvert(final String value) {
			return SettingKey.fromValue(value).name();
		}

		@Override
		public String handleConvert(final String value) {
			return SettingKey.valueOf(value).toString();
		}
	}
}
