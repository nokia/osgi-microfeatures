/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.platform;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.ResultSet;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class QueryTargetIdTest {

	@Test
	public void testQuery() throws Exception {
		final QueryTargetId query = new QueryTargetId();
		assertEquals("select target_id from ood_system.system_info", query.sql());
		assertNull(query.getValue());
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("target_id")).andReturn("11223344");

		replayAll();
		assertEquals("11223344", new QueryTargetId().handleValue(resultSetMock));
		verifyAll();
	}
}
