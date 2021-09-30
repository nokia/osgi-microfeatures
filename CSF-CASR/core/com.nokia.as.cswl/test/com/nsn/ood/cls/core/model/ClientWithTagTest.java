/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.clients.Client;


/**
 * @author marynows
 * 
 */
public class ClientWithTagTest {
	private static final Client CLIENT = new Client();
	private static final ClientTag CLIENT_TAG = new ClientTag();
	private static final ClientWithTag CLIENT_WITH_TAG = new ClientWithTag().withClientTag(CLIENT_TAG).withObject(
			CLIENT);

	@Test
	public void testClientWithTag() throws Exception {
		final ClientWithTag clientWithTag = new ClientWithTag();
		assertNull(clientWithTag.getClientTag());
		assertNull(clientWithTag.getObject());

		clientWithTag.setClientTag(CLIENT_TAG);
		assertSame(CLIENT_TAG, clientWithTag.getClientTag());

		clientWithTag.setObject(CLIENT);
		assertSame(CLIENT, clientWithTag.getObject());
	}

	@Test
	public void testBuilders() throws Exception {
		assertSame(CLIENT_TAG, CLIENT_WITH_TAG.getClientTag());
		assertSame(CLIENT, CLIENT_WITH_TAG.getObject());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(CLIENT_WITH_TAG.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertFalse(CLIENT_WITH_TAG.equals(null));
		assertFalse(CLIENT_WITH_TAG.equals("test"));
		assertEquals(CLIENT_WITH_TAG, CLIENT_WITH_TAG);

		assertFalse(CLIENT_WITH_TAG.equals(new ClientWithTag()));
		assertNotEquals(CLIENT_WITH_TAG.hashCode(), new ClientWithTag().hashCode());

		final ClientWithTag clientWithTag = new ClientWithTag().withClientTag(CLIENT_TAG).withObject(CLIENT);
		assertEquals(CLIENT_WITH_TAG, clientWithTag);
		assertEquals(CLIENT_WITH_TAG.hashCode(), clientWithTag.hashCode());
	}
}
