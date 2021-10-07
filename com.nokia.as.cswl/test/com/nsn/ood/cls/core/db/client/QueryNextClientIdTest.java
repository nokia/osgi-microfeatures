/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.ResultSet;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class QueryNextClientIdTest {

	@Test
	public void testQuery() throws Exception {
		final QueryNextClientId query = new QueryNextClientId();
		assertEquals("select UUID_SHORT() as id", query.sql());
		assertEquals(Long.valueOf(0L), query.getValue());
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getLong("id")).andReturn(13L);

		replayAll();
		assertEquals(Long.valueOf(13L), new QueryNextClientId().handleValue(resultSetMock));
		verifyAll();
	}
}
