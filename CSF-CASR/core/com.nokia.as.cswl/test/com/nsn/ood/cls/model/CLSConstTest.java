/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class CLSConstTest {

	@Test
	public void testConst() throws Exception {
		assertEquals("UTF-8", CLSConst.CHARSET);
		assertEquals("yyyy-MM-dd'T'HH:mm:ssZZ", CLSConst.DATE_TIME_FORMAT);
		assertEquals("yyyy-MM-dd", CLSConst.DATE_FORMAT);
	}
}
