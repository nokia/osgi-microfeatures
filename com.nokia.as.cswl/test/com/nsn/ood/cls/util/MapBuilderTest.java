/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class MapBuilderTest {

	@Test
	public void testNewLinkedMap() throws Exception {
		final Map<Long, String> map = MapBuilder.linkedMap(777L, "value").build();
		assertTrue(map instanceof LinkedHashMap);
		assertEquals(1, map.size());
		assertEquals("value", map.get(777L));
	}

	@Test
	public void testHashMap() throws Exception {
		final Map<String, Long> map = MapBuilder.hashMap("key", 666L).build();
		assertTrue(map instanceof HashMap);
		assertEquals(1, map.size());
		assertEquals(Long.valueOf(666L), map.get("key"));
	}

	@Test
	public void testPutOne() throws Exception {
		final Map<String, Long> result = MapBuilder.hashMap("test", 1L).build();
		assertEquals(1, result.size());
		assertEquals(Long.valueOf(1), result.get("test"));
	}

	@Test
	public void testPutMore() throws Exception {
		final Map<String, Long> result = MapBuilder.hashMap("test", 1L).put("aaa", 3L).put("vv", 77L).build();
		assertEquals(3, result.size());
		assertEquals(Long.valueOf(1), result.get("test"));
		assertEquals(Long.valueOf(3), result.get("aaa"));
		assertEquals(Long.valueOf(77), result.get("vv"));
		assertNull(result.get("ggg"));
	}
}
