/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.Test;

import com.nsn.ood.cls.core.db.DistinctQuery.QueryPrepare;


/**
 * @author marynows
 * 
 */
public class DistinctQueryTest {

	@Test
	public void testQuery() throws Exception {
		final DistinctQuery query = new DistinctQuery(null, null, null, null);
		assertTrue(query.getValues().isEmpty());
		assertNull(query.next());
	}

	@Test
	public void testSql() throws Exception {
		assertEquals("select null where 1=0", new DistinctQuery(null, null, "name", null).sql());
		assertEquals("select null where 1=0", new DistinctQuery("", null, "name", null).sql());
		assertEquals("select null where 1=0", new DistinctQuery("sql", null, null, null).sql());
		assertEquals("select null where 1=0", new DistinctQuery("sql", null, "", null).sql());
		assertEquals("select distinct name from (sql) t order by 1", new DistinctQuery("sql", null, "name", null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final QueryPrepare queryPrepareMock = createMock(QueryPrepare.class);

		queryPrepareMock.prepare(statementMock);

		replayAll();
		new DistinctQuery(null, queryPrepareMock, null, null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testPrepareWithNullQueryPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		new DistinctQuery(null, null, null, null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleIntegers() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getInt(1)).andReturn(5);
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getInt(1)).andReturn(7);
		expect(resultSetMock.wasNull()).andReturn(true);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final DistinctQuery query = new DistinctQuery(null, null, null, Integer.class);
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(5), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleLongs() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getLong(1)).andReturn(5L);
		expect(resultSetMock.wasNull()).andReturn(true);
		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getLong(1)).andReturn(7L);
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final DistinctQuery query = new DistinctQuery(null, null, null, Long.class);
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(7L), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleTimestamps() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getTimestamp(1)).andReturn(new Timestamp(77777L));
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getTimestamp(1)).andReturn(new Timestamp(88888L));
		expect(resultSetMock.wasNull()).andReturn(true);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final DistinctQuery query = new DistinctQuery(null, null, null, Timestamp.class);
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(new Timestamp(77777L)), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleStrings() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getString(1)).andReturn("aaa");
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getString(1)).andReturn("bbb");
		expect(resultSetMock.wasNull()).andReturn(false);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final DistinctQuery query = new DistinctQuery(null, null, null, String.class);
		query.handle(resultSetMock);
		assertEquals(Arrays.asList("aaa", "bbb"), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleNoResult() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final DistinctQuery query = new DistinctQuery(null, null, null, null);
		query.handle(resultSetMock);
		assertTrue(query.getValues().isEmpty());
		verifyAll();
	}
}
