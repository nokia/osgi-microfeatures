/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.Activity.OperationType;


/**
 * @author marynows
 * 
 */
public class ActivityOperationType2StringConverterTest {
	private ActivityOperationType2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new ActivityOperationType2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("license_cancel", this.converter.convertTo(OperationType.LICENSE_CANCEL));
		assertEquals("license_install", this.converter.convertTo(OperationType.LICENSE_INSTALL));
		assertEquals("setting_update", this.converter.convertTo(OperationType.SETTING_UPDATE));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(OperationType.LICENSE_CANCEL, this.converter.convertFrom("license_cancel"));
		assertEquals(OperationType.LICENSE_INSTALL, this.converter.convertFrom("license_install"));
		assertEquals(OperationType.SETTING_UPDATE, this.converter.convertFrom("setting_update"));
		assertNull(this.converter.convertFrom(null));
		assertNull(this.converter.convertFrom(""));
		assertNull(this.converter.convertFrom("test"));
	}
}
