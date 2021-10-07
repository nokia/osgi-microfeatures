/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ResourceIdTest {

	@Test
	public void testClientID() {
		final ResourceId clientID = new ResourceId();
		assertNull(clientID.getResourceId());

		clientID.setResourceId("ABC123");
		assertEquals("ABC123", clientID.getResourceId());
	}
}
