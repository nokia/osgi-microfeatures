/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ClientTagTest {
	private static final String TIME = "2014-12-17T14:42:23+01:00";
	private static final ClientTag CLIENT_TAG = new ClientTag().withETag("eTag").withExpires(DateTime.parse(TIME));

	@Test
	public void testClientTag() throws Exception {
		final ClientTag tag = new ClientTag();
		assertNull(tag.getETag());
		assertNull(tag.getExpires());

		tag.setETag("eTag");
		assertEquals("eTag", tag.getETag());

		tag.setExpires(DateTime.parse(TIME));
		assertTrue(DateTime.parse(TIME).isEqual(tag.getExpires()));
	}

	@Test
	public void testBuilderMethods() throws Exception {
		assertEquals("eTag", CLIENT_TAG.getETag());
		assertTrue(DateTime.parse(TIME).isEqual(CLIENT_TAG.getExpires()));
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(CLIENT_TAG.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertFalse(CLIENT_TAG.equals(null));
		assertFalse(CLIENT_TAG.equals("test"));
		assertEquals(CLIENT_TAG, CLIENT_TAG);

		assertFalse(CLIENT_TAG.equals(new ClientTag()));
		assertNotEquals(CLIENT_TAG.hashCode(), new ClientTag().hashCode());

		final ClientTag clientTag = new ClientTag().withETag("eTag").withExpires(DateTime.parse(TIME));
		assertEquals(CLIENT_TAG, clientTag);
		assertEquals(CLIENT_TAG.hashCode(), clientTag.hashCode());
	}
}
