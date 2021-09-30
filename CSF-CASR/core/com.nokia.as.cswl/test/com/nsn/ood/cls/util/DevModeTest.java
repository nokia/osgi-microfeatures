/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class DevModeTest {

	@Before
	public void setUp() throws Exception {
		setProperty("");
	}

	@Test
	public void testIsEnable() throws Exception {
		assertFalse(DevMode.isEnable());

		setProperty("true");
		assertTrue(DevMode.isEnable());

		setProperty("false");
		assertFalse(DevMode.isEnable());
	}

	@After
	public void tearDown() throws Exception {
		setProperty("");
	}

	private void setProperty(final String value) {
		System.setProperty("com.nsn.ood.cls.devMode", value);
	}
}
