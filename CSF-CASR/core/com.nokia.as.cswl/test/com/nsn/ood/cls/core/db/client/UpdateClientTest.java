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
public class UpdateClientTest {
	private static final DateTime EXPIRES = DateTime.parse("2015-01-09T10:31:13+01:00");
	private static final Timestamp EXPIRES_TS = new Timestamp(EXPIRES.getMillis());

	@Test
	public void testSql() throws Exception {
		assertEquals("update cls.clients set keepalivetime = ?, etag = ?, expires = ? where clientid = ?",
				new UpdateClient(null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		statementMock.setLong(1, 1800L);
		statementMock.setString(2, "eTag");
		expect(converterMock.convertFrom(EXPIRES)).andReturn(EXPIRES_TS);
		statementMock.setTimestamp(3, EXPIRES_TS);
		statementMock.setString(4, "12345");

		replayAll();
		new UpdateClient(client("12345", 1800L, "TYPE"), clientTag("eTag", EXPIRES), converterMock)
				.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		new UpdateClient(null, null, null).handle(1);

		try {
			new UpdateClient(null, null, null).handle(0);
			fail();
		} catch (final SQLException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
