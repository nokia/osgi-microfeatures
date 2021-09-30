/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 * 
 */
public class SettingTestUtil {

	public static List<Setting> settingsList(final Setting... settings) {
		return Arrays.asList(settings);
	}

	public static Setting setting() {
		return new Setting();
	}

	public static Setting setting(final SettingKey key, final Object value) {
		return setting().withKey(key).withValue(value);
	}

	public static void assertSetting(final Setting setting, final SettingKey expectedKey, final Object expectedValue) {
		assertEquals(expectedKey, setting.getKey());
		assertEquals(expectedValue, setting.getValue());
	}

	public static String bigString(final int length) {
		final char[] chars = new char[length];
		Arrays.fill(chars, 'x');
		return new String(chars);
	}
}
