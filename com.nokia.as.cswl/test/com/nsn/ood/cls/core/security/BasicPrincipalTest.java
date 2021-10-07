/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class BasicPrincipalTest {

	@Test
	public void testInit() throws Exception {
		assertEquals("anonymous", new BasicPrincipal().getUser());
	}

	@Test
	public void testSettingUser() throws Exception {
		final BasicPrincipal principal = new BasicPrincipal();

		principal.setUser("user");
		assertEquals("user", principal.getUser());
	}
}
