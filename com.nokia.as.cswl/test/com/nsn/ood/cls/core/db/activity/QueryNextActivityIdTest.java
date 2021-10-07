/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.activity;

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
public class QueryNextActivityIdTest {

	@Test
	public void testQuery() throws Exception {
		final QueryNextActivityId query = new QueryNextActivityId();
		assertEquals("select nextval('cls.activities_id_seq') as id", query.sql());
		assertEquals(Long.valueOf(0L), query.getValue());
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getLong("id")).andReturn(77L);

		replayAll();
		assertEquals(Long.valueOf(77L), new QueryNextActivityId().handleValue(resultSetMock));
		verifyAll();
	}
}
