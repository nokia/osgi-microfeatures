/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;

import com.nsn.ood.cls.core.db.creator.ClientWithTagCreator;


/**
 * @author marynows
 * 
 */
public class QueryClientTest {

	@Test
	public void testQuery() throws Exception {
		final QueryClient query = new QueryClient(null, null);
		assertEquals("select * from cls.clients where clientid = ?", query.sql());
		assertNull(query.getValue());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "12345");

		replayAll();
		new QueryClient("12345", null).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);
		final ClientWithTagCreator clientWithTagCreatorMock = createMock(ClientWithTagCreator.class);

		expect(clientWithTagCreatorMock.createClient(resultSetMock)).andReturn(clientWithTag(null, null));

		replayAll();
		assertEquals(clientWithTag(null, null),
				new QueryClient(null, clientWithTagCreatorMock).handleValue(resultSetMock));
		verifyAll();
	}
}
