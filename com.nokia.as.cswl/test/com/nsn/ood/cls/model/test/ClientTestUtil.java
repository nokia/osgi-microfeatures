/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.clients.Clients;


/**
 * @author marynows
 * 
 */
public class ClientTestUtil {

	public static List<Client> clientsList(final Client... clients) {
		return Arrays.asList(clients);
	}

	public static Client client() {
		return new Client();
	}

	public static Client client(final String id) {
		return client().withClientId(id);
	}

	public static Client client(final String id, final Long keepAliveTime) {
		return client(id).withKeepAliveTime(keepAliveTime);
	}

	public static Client client(final String id, final String targetType) {
		return client(id).withTargetType(targetType);
	}

	public static Client client(final String id, final Long keepAliveTime, final String targetType) {
		return client(id, keepAliveTime).withTargetType(targetType);
	}

	public static void assertClient(final Client client, final String expectedClientId,
			final Long expectedKeepAliveTime, final String expectedTargetType) {
		assertEquals(expectedClientId, client.getClientId());
		assertEquals(expectedKeepAliveTime, client.getKeepAliveTime());
		assertEquals(expectedTargetType, client.getTargetType());
	}

	public static Clients clients(final Client... clients) {
		return new Clients().withClients(Arrays.asList(clients));
	}

	public static Clients clients(final List<Client> clients) {
		return new Clients().withClients(clients);
	}
}
