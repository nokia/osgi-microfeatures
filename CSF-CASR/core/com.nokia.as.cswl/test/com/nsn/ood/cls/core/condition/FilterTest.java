/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Filter.Type;


/**
 * @author marynows
 * 
 */
public class FilterTest {
	private static final Filter FILTER = new Filter(Type.EQUAL, "name", "value");

	@Test
	public void testFilter() throws Exception {
		assertEquals(Type.EQUAL, FILTER.type());
		assertEquals("name", FILTER.name());
		assertEquals("value", FILTER.value());
	}

	@Test
	public void testClone() throws Exception {
		final Filter filter2 = FILTER.clone();

		assertNotSame(FILTER, filter2);
		assertEquals(FILTER, filter2);
		assertEquals(FILTER.hashCode(), filter2.hashCode());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(FILTER.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertEquals(FILTER, FILTER);
		assertFalse(FILTER.equals(null));
		assertFalse(FILTER.equals("test"));
	}
}
