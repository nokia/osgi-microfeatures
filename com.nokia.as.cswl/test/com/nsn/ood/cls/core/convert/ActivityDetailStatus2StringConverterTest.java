/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.ActivityDetail.Status;


/**
 * @author marynows
 * 
 */
public class ActivityDetailStatus2StringConverterTest {
	private ActivityDetailStatus2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new ActivityDetailStatus2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("success", this.converter.convertTo(Status.SUCCESS));
		assertEquals("failure", this.converter.convertTo(Status.FAILURE));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(Status.SUCCESS, this.converter.convertFrom("success"));
		assertEquals(Status.FAILURE, this.converter.convertFrom("failure"));
		assertNull(this.converter.convertFrom(null));
		assertNull(this.converter.convertFrom(""));
		assertNull(this.converter.convertFrom("test"));
	}
}
