/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.Activity.Result;


/**
 * @author marynows
 * 
 */
public class ActivityResult2StringConverterTest {
	private ActivityResult2StringConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new ActivityResult2StringConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertEquals("failure", this.converter.convertTo(Result.FAILURE));
		assertEquals("partial", this.converter.convertTo(Result.PARTIAL));
		assertEquals("success", this.converter.convertTo(Result.SUCCESS));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(Result.SUCCESS, this.converter.convertFrom("success"));
		assertEquals(Result.PARTIAL, this.converter.convertFrom("partial"));
		assertEquals(Result.FAILURE, this.converter.convertFrom("failure"));
		assertNull(this.converter.convertFrom(null));
		assertNull(this.converter.convertFrom(""));
		assertNull(this.converter.convertFrom("test"));
	}
}
