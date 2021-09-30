/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class StringsTest {

	@Test
	public void testIsNullOrEmpty() throws Exception {
		assertTrue(Strings.isNullOrEmpty(null));
		assertTrue(Strings.isNullOrEmpty(""));
		assertFalse(Strings.isNullOrEmpty("test"));
	}

	@Test
	public void testNullToEmpty() throws Exception {
		assertEquals("", Strings.nullToEmpty(null));
		assertEquals("", Strings.nullToEmpty(""));
		assertEquals("test", Strings.nullToEmpty("test"));
	}

	@Test
	public void testEmptyToNull() throws Exception {
		assertNull(Strings.emptyToNull(null));
		assertNull(Strings.emptyToNull(""));
		assertEquals("test", Strings.emptyToNull("test"));
	}
}
