/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class UpdateClientExpirationTimeTest {
	private static final DateTime EXPIRES = DateTime.parse("2015-01-28T15:41:23+01:00");
	private static final Timestamp EXPIRES_TS = new Timestamp(EXPIRES.getMillis());

	@Test
	public void testSql() throws Exception {
		assertEquals("update cls.clients set etag = ?, expires = ? where clientid = ?", new UpdateClientExpirationTime(
				null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		statementMock.setString(1, "eTag");
		expect(converterMock.convertFrom(EXPIRES)).andReturn(EXPIRES_TS);
		statementMock.setTimestamp(2, EXPIRES_TS);
		statementMock.setString(3, "12345");

		replayAll();
		new UpdateClientExpirationTime(client("12345", 1800L, "TYPE"), clientTag("eTag", EXPIRES), converterMock)
				.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		new UpdateClientExpirationTime(null, null, null).handle(1);

		try {
			new UpdateClientExpirationTime(null, null, null).handle(0);
			fail();
		} catch (final SQLException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
