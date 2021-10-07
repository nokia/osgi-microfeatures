/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class CollectionUtilsTest {

	@Test
	public void testIsEmpty() throws Exception {
		assertTrue(CollectionUtils.isEmpty(null));
		assertTrue(CollectionUtils.isEmpty(Collections.emptyList()));
		assertFalse(CollectionUtils.isEmpty(Arrays.asList("test")));
	}

	@Test
	public void testIsNotEmpty() throws Exception {
		assertFalse(CollectionUtils.isNotEmpty(null));
		assertFalse(CollectionUtils.isNotEmpty(Collections.emptySet()));
		assertTrue(CollectionUtils.isNotEmpty(Arrays.asList("test")));
	}
}
