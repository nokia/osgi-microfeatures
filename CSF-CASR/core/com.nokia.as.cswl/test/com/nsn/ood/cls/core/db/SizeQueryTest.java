/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;

import com.nsn.ood.cls.core.db.SizeQuery.QueryPrepare;


/**
 * @author marynows
 * 
 */
public class SizeQueryTest {

	@Test
	public void testQuery() throws Exception {
		final SizeQuery query = new SizeQuery(null, null, null);
		assertEquals(0L, query.getSize());
		assertNull(query.next());
	}

	@Test
	public void testSql() throws Exception {
		assertEquals("select 0 as count", new SizeQuery(null, null, null).sql());
		assertEquals("select 0 as count", new SizeQuery("", null, null).sql());
		assertEquals("select count(*) as count from (test) t", new SizeQuery("test", null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final QueryPrepare queryPrepareMock = createMock(QueryPrepare.class);

		queryPrepareMock.prepare(statementMock);

		replayAll();
		new SizeQuery(null, queryPrepareMock, null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testPrepareWithNullQueryPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		new SizeQuery(null, null, null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testNext() throws Exception {
		assertNull(new SizeQuery(null, null, null).next());

		final Query nextMock = createMock(Query.class);
		replayAll();
		assertEquals(nextMock, new SizeQuery(null, null, nextMock).next());
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getLong("count")).andReturn(77L);

		replayAll();
		final SizeQuery query = new SizeQuery(null, null, null);
		query.handle(resultSetMock);
		assertEquals(77L, query.getSize());
		verifyAll();
	}

	@Test
	public void testHandleNoResult() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SizeQuery query = new SizeQuery(null, null, null);
		query.handle(resultSetMock);
		assertEquals(0L, query.getSize());
		verifyAll();
	}
}
