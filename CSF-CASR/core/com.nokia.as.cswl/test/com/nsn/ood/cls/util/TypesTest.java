/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.Test;


/**
 * @author marynows
 *
 */
public class TypesTest {

	@Test
	public void testNewParameterizedType() throws Exception {
		final ParameterizedType type = Types.newParameterizedType(List.class, String.class);

		assertEquals(List.class, type.getRawType());
		assertNull(type.getOwnerType());
		assertArrayEquals(new Type[] {
				String.class }, type.getActualTypeArguments());
		assertEquals("java.util.List<java.lang.String>", type.toString());
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("null", Types.newParameterizedType(null).toString());
		assertEquals("null<java.lang.String>", Types.newParameterizedType(null, String.class).toString());
		assertEquals("null<TestType>", Types.newParameterizedType(null, new TestType()).toString());
		assertEquals("java.lang.Integer", Types.newParameterizedType(Integer.class).toString());
		assertEquals("java.lang.Integer<null>", Types.newParameterizedType(Integer.class, (Type) null).toString());
	}

	private static final class TestType implements Type {
		@Override
		public String toString() {
			return "TestType";
		}
	}
}
