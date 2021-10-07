/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.nsn.ood.cls.core.model.ClientTag;


/**
 * @author marynows
 * 
 */
public class ClientTagTestUtil {
	public static final String ETAG = "eTag";
	public static final DateTime EXPIRES = new DateTime(2015, 1, 26, 14, 18, 32, DateTimeZone.forOffsetHours(1));
	public static final String EXPIRES_AS_STRING = "Mon, 26 Jan 2015 13:18:32 GMT";

	public static ClientTag clientTag() {
		return new ClientTag().withETag(ETAG).withExpires(EXPIRES);
	}

	public static ClientTag clientTag(final String eTag, final DateTime expires) {
		return new ClientTag().withETag(eTag).withExpires(expires);
	}

	public static void assertClientTag(final ClientTag clientTag) {
		assertEquals(ETAG, clientTag.getETag());
		assertTrue(EXPIRES.isEqual(clientTag.getExpires()));
	}

	public static void assertClientTag(final ClientTag clientTag, final String expectedETag,
			final DateTime expectedExpires) {
		assertEquals(expectedETag, clientTag.getETag());
		assertTrue(expectedExpires.isEqual(clientTag.getExpires()));
	}
}
