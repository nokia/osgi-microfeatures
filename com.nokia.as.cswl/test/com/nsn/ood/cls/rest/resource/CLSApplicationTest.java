/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.ApplicationPath;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class CLSApplicationTest {

	@Test
	public void testConstants() throws Exception {
		assertEquals("/api", CLSApplication.API);
		assertEquals("/CLS", CLSApplication.CONTEXT_ROOT);
		assertEquals("/internal", CLSApplication.INTERNAL);
		assertEquals("/v1", CLSApplication.VERSION);
	}

	@Test
	public void testAnnotation() {
		final ApplicationPath annotation = CLSApplication.class.getAnnotation(ApplicationPath.class);
		assertNotNull(annotation);
		assertEquals("/", annotation.value());
	}
}
