/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser;


/**
 * @author marynows
 * 
 */
public class DateTimeDeserializerTest {
	private final DateTimeDeserializer deserializer = new DateTimeDeserializer();

	@Test
	public void testDeserialize() throws Exception {
		assertDateTime("2014-02-28T01:01:01.001+02:00",
				new DateTime(2014, 2, 28, 1, 1, 1, 1, DateTimeZone.forOffsetHours(2)));
		assertDateTime("2014-08-30T23:59:59+01:00",
				new DateTime(2014, 8, 30, 23, 59, 59, 0, DateTimeZone.forOffsetHours(1)));
		assertDateTime("2014-02-28T01:01:01", new DateTime(2014, 2, 28, 1, 1, 1, 0, DateTimeZone.getDefault()));
		assertDateTime("2014-12-31", new DateTime(2014, 12, 31, 0, 0, 0, 0, DateTimeZone.getDefault()));

		try {
			assertDateTime("test", new DateTime());
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	private void assertDateTime(final String actual, final DateTime expected) throws Exception {
		final JsonParser jpMock = createMock(JsonParser.class);
		expect(jpMock.getText()).andReturn(actual);

		replayAll();
		final DateTime dateTime = this.deserializer.deserialize(jpMock, null);
		assertTrue(expected.toString() + " <> " + dateTime.toString(), dateTime.isEqual(expected));
		verifyAll();
	}
}
