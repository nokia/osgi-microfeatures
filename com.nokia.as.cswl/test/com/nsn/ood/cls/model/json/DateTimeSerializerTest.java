/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.json;

import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;


/**
 * @author marynows
 * 
 */
public class DateTimeSerializerTest {
	private final DateTimeSerializer serializer = new DateTimeSerializer();

	@Test
	public void testSerialize() throws Exception {
		assertDateTime(new DateTime(2014, 2, 28, 1, 1, 1, 1, DateTimeZone.forOffsetHours(1)),
				"2014-02-28T01:01:01+01:00");
		assertDateTime(new DateTime(2014, 12, 31, 23, 59, 59, DateTimeZone.forOffsetHours(2)),
				"2014-12-31T23:59:59+02:00");
		assertDateTime(new DateTime(2015, 8, 8, 0, 0, DateTimeZone.forOffsetHours(0)), "2015-08-08T00:00:00+00:00");
	}

	private void assertDateTime(final DateTime actual, final String expected) throws Exception {
		final JsonGenerator jgen = createMock(JsonGenerator.class);
		jgen.writeString(expected);

		replayAll();
		this.serializer.serialize(actual, jgen, null);
		verifyAll();
	}
}
