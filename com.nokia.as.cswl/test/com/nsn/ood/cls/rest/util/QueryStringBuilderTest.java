/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nsn.ood.cls.util.MapBuilder;


/**
 * @author marynows
 * 
 */
public class QueryStringBuilderTest {

	@Test
	public void testCreate() throws Exception {
		assertEquals("", QueryStringBuilder.create().build());
		assertEquals("?name=value", QueryStringBuilder.create().add("name", "value").build());
		assertEquals("?n1=a%20b&n2%25=32%26%5E*?", QueryStringBuilder.create().add("n1", "a b").add("n2%", "32&^*?")
				.build());
	}

	@Test
	public void testFromMap() throws Exception {
		assertEquals("", QueryStringBuilder.fromMap(null).build());
		assertEquals("?key=value", QueryStringBuilder.fromMap(MapBuilder.linkedMap("key", "value").build()).build());
		assertEquals("?k1=v1&k2=v2",
				QueryStringBuilder.fromMap(MapBuilder.linkedMap("k1", "v1").put("k2", "v2").build()).build());
	}
}
