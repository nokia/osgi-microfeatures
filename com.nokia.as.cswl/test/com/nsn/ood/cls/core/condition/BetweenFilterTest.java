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
public class BetweenFilterTest {
	private static final BetweenFilter FILTER = new BetweenFilter("name", "from", "to");

	@Test
	public void testBetweenFilterImpl() throws Exception {
		final BetweenFilter filter = FILTER;

		assertEquals(Type.BETWEEN, filter.type());
		assertEquals("name", filter.name());
		assertEquals("from", filter.from());
		assertEquals("to", filter.to());
		assertEquals("from,to", filter.value());
	}

	@Test
	public void testClone() throws Exception {
		final BetweenFilter filter2 = FILTER.clone();

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
