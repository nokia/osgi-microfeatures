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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SimpleDistinctQueryTest {

	@Test
	public void testSimpleDistinctQuery() throws Exception {
		final SimpleDistinctQuery<?> query = new SimpleDistinctQuery<Object>("sql", null) {
		};
		assertEquals("sql", query.sql());
		assertNull(query.next());
		assertTrue(query.getValues().isEmpty());
	}

	@Test
	public void testHandleNoValues() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleDistinctQuery<?> query = new SimpleDistinctQuery<Object>(null, null) {
		};
		query.handle(resultSetMock);
		assertTrue(query.getValues().isEmpty());
		verifyAll();
	}

	@Test
	public void testHandleIntegerValues() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getInt(1)).andReturn(13);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleDistinctQuery<Integer> query = new SimpleDistinctQuery<Integer>(null, Integer.class) {
		};
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(13), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleLongValues() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getLong(1)).andReturn(13L);
		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getLong(1)).andReturn(17L);
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleDistinctQuery<Long> query = new SimpleDistinctQuery<Long>(null, Long.class) {
		};
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(13L, 17L), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleTimestampValues() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getTimestamp(1)).andReturn(new Timestamp(666L));
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleDistinctQuery<Timestamp> query = new SimpleDistinctQuery<Timestamp>(null, Timestamp.class) {
		};
		query.handle(resultSetMock);
		assertEquals(Arrays.asList(new Timestamp(666L)), query.getValues());
		verifyAll();
	}

	@Test
	public void testHandleStringValues() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.next()).andReturn(true);
		expect(resultSetMock.getString(1)).andReturn("sss");
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final SimpleDistinctQuery<String> query = new SimpleDistinctQuery<String>(null, String.class) {
		};
		query.handle(resultSetMock);
		assertEquals(Arrays.asList("sss"), query.getValues());
		verifyAll();
	}
}
