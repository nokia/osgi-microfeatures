/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class CLSMediaTypeTest implements CLSMediaType {

	@Test
	public void testConstants() {
		assertEquals("application/vnd.nokia-cls-client.text+json", CLSMediaType.APPLICATION_CLIENT_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-cls-client.text+json"),
				CLSMediaType.APPLICATION_CLIENT_JSON_TYPE);

		assertEquals("application/vnd.nokia-error-response.text+json", CLSMediaType.APPLICATION_ERROR_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-error-response.text+json"),
				CLSMediaType.APPLICATION_ERROR_JSON_TYPE);

		assertEquals("application/vnd.nokia-cls-feature.text+json", CLSMediaType.APPLICATION_FEATURE_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-cls-feature.text+json"),
				CLSMediaType.APPLICATION_FEATURE_JSON_TYPE);

		assertEquals("application/vnd.nokia-cls-license.text+json", CLSMediaType.APPLICATION_LICENSE_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-cls-license.text+json"),
				CLSMediaType.APPLICATION_LICENSE_JSON_TYPE);

		assertEquals("application/vnd.nokia-cls.text+json", CLSMediaType.APPLICATION_CLS_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-cls.text+json"), CLSMediaType.APPLICATION_CLS_JSON_TYPE);

		assertEquals("application/vnd.nokia-versioning.text+json", CLSMediaType.APPLICATION_VERSIONING_JSON);
		assertEquals(new MediaType("application", "vnd.nokia-versioning.text+json"),
				CLSMediaType.APPLICATION_VERSIONING_JSON_TYPE);

		assertEquals("application/*+json", CLSMediaType.APPLICATION_JSON);
		assertEquals(new MediaType("application", "*+json"), CLSMediaType.APPLICATION_JSON_TYPE);
	}
}
