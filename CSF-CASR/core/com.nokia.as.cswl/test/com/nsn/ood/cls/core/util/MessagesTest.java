/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class MessagesTest {

	@Test
	public void testGetSubject() throws Exception {
		assertEquals("License expirations", Messages.getSubject("licenseExpirationSubject"));
		assertEquals("Capacity limit", Messages.getSubject("capacityThresholdSubject"));
		assertEquals("!test!", Messages.getSubject("test"));
	}
}
