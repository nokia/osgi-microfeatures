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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ListQueryTest {

	@Test
	public void testQuery() throws Exception {
		final ListQuery<Long> query = new ListQuery<Long>("sql") {
			@Override
			protected Long handleRow(final ResultSet resultSet) throws SQLException {
				return null;
			}
		};
		assertEquals("sql", query.sql());
		assertNull(query.next());
		assertTrue(query.getList().isEmpty());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		replayAll();
		final ListQuery<Integer> query = new ListQuery<Integer>(null) {
			@Override
			protected Integer handleRow(final ResultSet resultSet) throws SQLException {
				return null;
			}
		};
		query.prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		testHandleMultipleResults(0);
		testHandleMultipleResults(1);
		testHandleMultipleResults(2);
		testHandleMultipleResults(10);
	}

	public void testHandleMultipleResults(final int n) throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		final List<String> expectedList = new ArrayList<>();
		for (long i = 0; i < n; i++) {
			expectedList.add("test" + i);
			expect(resultSetMock.next()).andReturn(true);
		}
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final ListQuery<String> query = new ListQuery<String>(null) {
			private int i = 0;

			@Override
			protected String handleRow(final ResultSet resultSet) throws SQLException {
				assertEquals(resultSetMock, resultSet);
				return "test" + (this.i++);
			}
		};
		query.handle(resultSetMock);
		assertEquals(expectedList, query.getList());
		verifyAll();
	}
}
