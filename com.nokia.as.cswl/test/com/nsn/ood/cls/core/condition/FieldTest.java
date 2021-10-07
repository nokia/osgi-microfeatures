/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Field.Order;


/**
 * @author marynows
 * 
 */
public class FieldTest {
	private static final Field FIELD = new Field("name", Order.ASC);

	@Test
	public void testField() throws Exception {
		final Field field = FIELD;
		assertEquals("name", field.name());
		assertEquals(Order.ASC, field.order());
	}

	@Test
	public void testClone() throws Exception {
		final Field field2 = FIELD.clone();

		assertNotSame(FIELD, field2);
		assertEquals(FIELD, field2);
		assertEquals(FIELD.hashCode(), field2.hashCode());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(FIELD.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertEquals(FIELD, FIELD);
		assertFalse(FIELD.equals(null));
		assertFalse(FIELD.equals("test"));
	}
}
