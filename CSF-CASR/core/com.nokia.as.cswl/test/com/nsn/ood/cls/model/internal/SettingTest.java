/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.assertSetting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.bigString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SettingTest {

	@Test
	public void testValueMaxLength() throws Exception {
		assertEquals(10000, Setting.VALUE_MAX_LENGTH);
	}

	@Test
	public void testEmptyActivity() throws Exception {
		assertSetting(new Setting(), null, null);
	}

	@Test
	public void testSetting() throws Exception {
		assertSetting(new Setting().withKey(SettingKey.FLOATING_RELEASE_TIME), SettingKey.FLOATING_RELEASE_TIME, null);
		assertSetting(new Setting().withValue("test"), null, "test");

		assertSetting(new Setting().withKey(SettingKey.EXPIRING_LICENSES_THRESHOLD).withValue(13L),
				SettingKey.EXPIRING_LICENSES_THRESHOLD, 13L);
	}

	@Test
	public void testSettingSetters() throws Exception {
		final Setting setting = new Setting();
		setting.setKey(SettingKey.FLOATING_RELEASE_TIME);
		setting.setValue(2);

		assertSetting(setting, SettingKey.FLOATING_RELEASE_TIME, 2);
	}

	@Test
	public void testValidateNullKey() throws Exception {
		try {
			new Setting().validate();
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testValidate() throws Exception {
		final SettingKey keyMock = createMock(SettingKey.class);

		keyMock.validate("test");

		replayAll();
		new Setting().withKey(keyMock).withValue("test").validate();
		verifyAll();
	}

//	@Test
//	public void testAnnotations() throws Exception {
//		assertJsonPropertyOrder(Setting.class, "settingskey", "value");
//		assertJsonProperty(Setting.class, "settingskey", "settingskey");
//		assertJsonProperty(Setting.class, "value", "value");
//	}

	@Test
	public void testToString() throws Exception {
		assertFalse(new Setting().toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		final Setting setting = new Setting().withKey(SettingKey.EMAIL_SERVER).withValue("value");

		assertFalse(setting.equals(null));
		assertFalse(setting.equals("test"));
		assertEquals(setting, setting);

		assertFalse(setting.equals(new Setting()));
		assertNotEquals(setting.hashCode(), new Setting().hashCode());

		final Setting setting2 = new Setting().withKey(SettingKey.EMAIL_SERVER).withValue("value");
		assertEquals(setting, setting2);
		assertEquals(setting.hashCode(), setting2.hashCode());
	}

	@Test
	public void testGetValueAsString() throws Exception {
		assertNull(new Setting().withValue(null).getValueAsString());
		assertNull(new Setting().withValue("").getValueAsString());
		assertEquals("13", new Setting().withValue(13).getValueAsString());
		assertEquals("test", new Setting().withValue("test").getValueAsString());
		assertEquals(bigString(Setting.VALUE_MAX_LENGTH), new Setting().withValue(bigString(Setting.VALUE_MAX_LENGTH))
				.getValueAsString());
		assertEquals(bigString(Setting.VALUE_MAX_LENGTH - 3) + "...",
				new Setting().withValue(bigString(Setting.VALUE_MAX_LENGTH + 1)).getValueAsString());
	}

}
