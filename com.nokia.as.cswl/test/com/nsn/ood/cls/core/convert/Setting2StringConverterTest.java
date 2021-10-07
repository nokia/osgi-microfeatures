/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Setting2StringConverterTest {
	private Setting2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Setting2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("[]", this.converter.convertTo(setting()));

		assertEquals("[key=emailSubject]", this.converter.convertTo(setting(SettingKey.EMAIL_SUBJECT, null)));
		assertEquals("[value=test]", this.converter.convertTo(setting(null, "test")));

		assertEquals("[key=emailSender, value=SSS]", this.converter.convertTo(setting(SettingKey.EMAIL_SENDER, "SSS")));
	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
