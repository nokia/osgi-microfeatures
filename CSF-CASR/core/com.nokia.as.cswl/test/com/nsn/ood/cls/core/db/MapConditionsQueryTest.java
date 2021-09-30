/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class MapConditionsQueryTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testQuery() throws Exception {
		final MapConditionsQuery<Long, String> query = createMapConditionsQuery();
		assertTrue(query.getList().isEmpty());
	}

	@Test
	public void testHandleNoResult() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final MapConditionsQuery<Long, String> query = createMapConditionsQuery();
		query.handle(resultSetMock);
		assertTrue(query.getList().isEmpty());
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
		final MapConditionsQuery<Long, String> query = new MapConditionsQuery<Long, String>(null, CONDITIONS, MAPPER,
				null) {
			private long i = 0;

			@Override
			protected void handleRow(final ResultSet resultSet) throws SQLException {
				assertEquals(resultSetMock, resultSet);
				put(this.i, "test" + (this.i++));
			}
		};
		query.handle(resultSetMock);
		assertEquals(expectedList, query.getList());
		verifyAll();
	}

	@Test
	public void testMap() throws Exception {
		final MapConditionsQuery<Long, String> query = createMapConditionsQuery();
		assertFalse(query.contains(1L));
		assertNull(query.get(1L));

		query.put(1L, "test");
		assertTrue(query.contains(1L));
		assertEquals("test", query.get(1L));
	}

	private MapConditionsQuery<Long, String> createMapConditionsQuery() throws ConditionProcessingException {
		return new MapConditionsQuery<Long, String>(null, CONDITIONS, MAPPER, null) {
			@Override
			protected void handleRow(final ResultSet resultSet) throws SQLException {
			}
		};
	}
}
