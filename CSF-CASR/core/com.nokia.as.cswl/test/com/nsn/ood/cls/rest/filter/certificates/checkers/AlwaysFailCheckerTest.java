/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates.checkers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class AlwaysFailCheckerTest {

	@Test
	public void testCheckCondition() throws Exception {
		assertFalse(new AlwaysFailChecker().checkCondition(null));
	}

	@Test
	public void testGetErrorMessaage() throws Exception {
		assertEquals("Error occured during SSLFilter initialization", new AlwaysFailChecker().getErrorMessaage());
	}
}
