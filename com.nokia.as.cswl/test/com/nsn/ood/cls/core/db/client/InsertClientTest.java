/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertClientTest {
	private static final DateTime EXPIRES = DateTime.parse("2014-12-17T15:25:22+01:00");
	private static final Timestamp EXPIRES_TS = new Timestamp(EXPIRES.getMillis());

	@Test
	public void testSql() throws Exception {
		assertEquals(
				"insert into cls.clients (clientid, targettype, keepalivetime, expires, etag) values (?, ?, ?, ?, ?)",
				new InsertClient(null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		statementMock.setString(1, "12345");
		statementMock.setString(2, "TYPE");
		statementMock.setLong(3, 1800L);
		expect(converterMock.convertFrom(EXPIRES)).andReturn(EXPIRES_TS);
		statementMock.setTimestamp(4, EXPIRES_TS);
		statementMock.setString(5, "eTag");

		replayAll();
		new InsertClient(client("12345", 1800L, "TYPE"), clientTag("eTag", EXPIRES), converterMock)
				.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testPrepareWithEmptyTargetType() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		statementMock.setString(1, "12345");
		statementMock.setString(2, null);
		statementMock.setLong(3, 1800L);
		expect(converterMock.convertFrom(EXPIRES)).andReturn(EXPIRES_TS);
		statementMock.setTimestamp(4, EXPIRES_TS);
		statementMock.setString(5, "eTag");

		replayAll();
		new InsertClient(client("12345", 1800L, ""), clientTag("eTag", EXPIRES), converterMock).prepare(statementMock);
		verifyAll();
	}
}
