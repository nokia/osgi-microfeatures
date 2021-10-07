/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.assertActivityDetail;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.ActivityDetail.Status;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Setting2ActivityDetailConverterTest {
	private Setting2ActivityDetailConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Setting2ActivityDetailConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertActivityDetail(this.converter.convertTo(setting()), null, Status.SUCCESS, null, null, null, null, null);
		assertActivityDetail(this.converter.convertTo(setting(SettingKey.EMAIL_SENDER, 33L)), null, Status.SUCCESS,
				null, null, null, "emailSender", "33");
		assertActivityDetail(this.converter.convertTo(setting(SettingKey.EMAIL_SERVER, null)), null, Status.SUCCESS,
				null, null, null, "emailServer", null);
		assertActivityDetail(this.converter.convertTo(setting(SettingKey.EMAIL_SUBJECT, "")), null, Status.SUCCESS,
				null, null, null, "emailSubject", null);
		assertActivityDetail(this.converter.convertTo(setting(SettingKey.EMAIL_SUBJECT, "test")), null, Status.SUCCESS,
				null, null, null, "emailSubject", "test");
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
