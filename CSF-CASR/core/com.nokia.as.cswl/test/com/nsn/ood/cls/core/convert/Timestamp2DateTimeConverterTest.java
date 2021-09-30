/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class Timestamp2DateTimeConverterTest {
	private Timestamp2DateTimeConverter converter;

	@Before
	public void setUp() throws Exception {
		this.converter = new Timestamp2DateTimeConverter();
	}

	@Test
	public void testConvertTo() throws Exception {
		assertTrue(new DateTime(1234567890L).isEqual(this.converter.convertTo(new Timestamp(1234567890L))));
		assertNull(this.converter.convertTo(null));
	}

	@Test
	public void testConvertFrom() throws Exception {
		assertEquals(new Timestamp(1234567890L), this.converter.convertFrom(new DateTime(1234567890L)));
		assertNull(this.converter.convertFrom(null));
	}
}
