/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SimpleQueryTest {

	@Test
	public void testCreate() throws Exception {
		final SimpleQuery<String> query1 = new TestSimpleQuery(null, null);
		assertEquals("sql", query1.sql());
		assertNull(query1.next());
		assertNull(query1.getValue());

		final SimpleQuery<String> query2 = new TestSimpleQuery("test", null);
		assertEquals("sql", query1.sql());
		assertNull(query2.next());
		assertEquals("test", query2.getValue());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		new TestSimpleQuery(null, null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);

		replayAll();
		final SimpleQuery<String> query = new TestSimpleQuery(null, resultSetMock);
		query.handle(resultSetMock);
		assertEquals("test", query.getValue());
		verifyAll();
	}

	@Test
	public void testHandleNoResult() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleQuery<String> query = new TestSimpleQuery(null, null);
		query.handle(resultSetMock);
		assertNull(query.getValue());
		verifyAll();
	}

	private static class TestSimpleQuery extends SimpleQuery<String> {
		private final ResultSet expectedResultSet;

		private TestSimpleQuery(final String value, final ResultSet expectedResultSet) {
			super("sql", value);
			this.expectedResultSet = expectedResultSet;
		}

		@Override
		protected String handleValue(final ResultSet resultSet) throws SQLException {
			if (this.expectedResultSet == null) {
				fail();
			} else {
				assertEquals(this.expectedResultSet, resultSet);
			}
			return "test";
		}
	}
}
