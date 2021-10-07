/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

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
public class QueryReservationsFeatureCodesTest {

	@Test
	public void testInitialization() throws Exception {
		final QueryReservationsFeatureCodes query = new TestQueryReservationsFeatureCodes();

		assertNull(query.next());
		assertTrue(query.getValues().isEmpty());
	}

	@Test
	public void testSql() throws Exception {
		assertEquals("select distinct featurecode from cls.reservations where 1=1",
				new TestQueryReservationsFeatureCodes().sql());
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

		final List<Long> expectedFeatureCodes = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			expectedFeatureCodes.add(1000L + i);

			expect(resultSetMock.next()).andReturn(true);
			expect(resultSetMock.getLong(1)).andReturn(1000L + i);
		}
		expect(resultSetMock.next()).andReturn(false);

		replayAll();
		final QueryReservationsFeatureCodes query = new TestQueryReservationsFeatureCodes();
		query.handle(resultSetMock);
		assertEquals(expectedFeatureCodes, query.getValues());
		verifyAll();
	}

	private static class TestQueryReservationsFeatureCodes extends QueryReservationsFeatureCodes {

		private TestQueryReservationsFeatureCodes() {
			super("1=1");
		}

		@Override
		public void prepare(final PreparedStatement statement) throws SQLException {
		}
	}
}
