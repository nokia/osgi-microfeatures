/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class DescriptionBuilderTest {

	@Test
	public void testCreate() throws Exception {
		assertEquals("[]", new DescriptionBuilder().build());
	}

	@Test
	public void testAppend() throws Exception {
		assertEquals("[name=value]", new DescriptionBuilder().append("name", "value").build());
		assertEquals("[name=23]", new DescriptionBuilder().append("name", Long.valueOf(23L)).build());
		assertEquals("[]", new DescriptionBuilder().append("name", null).build());
		assertEquals("[n1=v1, n2=v2]", new DescriptionBuilder().append("n1", "v1").append("n2", "v2").build());
		assertEquals("[n1=2.4, n2=32]", new DescriptionBuilder().append("n1", 2.4).append("n2", 32).append("n3", null)
				.build());
		assertEquals("[n1=v1, n3=v3]", new DescriptionBuilder().append("n1", "v1").append("n2", null)
				.append("n3", "v3").append("n3", null).build());
	}

	@Test
	public void testAppendList() throws Exception {
		assertEquals("[name=[]]", new DescriptionBuilder().append("name", Collections.emptyList()).build());
		assertEquals("[name=[v1]]", new DescriptionBuilder().append("name", Arrays.asList("v1")).build());
		assertEquals("[name=[v1, v2]]", new DescriptionBuilder().append("name", Arrays.asList("v1", "v2")).build());
	}
}
