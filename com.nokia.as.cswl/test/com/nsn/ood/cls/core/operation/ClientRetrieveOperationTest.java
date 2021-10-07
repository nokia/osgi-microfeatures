/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;

import org.junit.Test;

import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.client.QueryClient;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;


/**
 * @author marynows
 *
 */
public class ClientRetrieveOperationTest extends ClientRetrieveOperation {
	private static final String CLIENT_ID = "12345";

	private static final ClientWithTag CLIENT_WITH_TAG = clientWithTag(client(), clientTag());
	private static final QueryClient QUERY_CLIENT_STUB = new QueryClient(null, null) {
		@Override
		public ClientWithTag getValue() {
			return CLIENT_WITH_TAG;
		}
	};

	private String capturedClientId;

	@Override
	protected QueryClient createQueryClient(final String clientId) {
		this.capturedClientId = clientId;
		super.createQueryClient(clientId);
		return QUERY_CLIENT_STUB;
	}

	@Test
	public void testGetClient() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_CLIENT_STUB));

		replayAll();
		setInternalState(this, queryExecutorMock);
		final ClientWithTag result = getClient(CLIENT_ID);
		verifyAll();

		assertSame(CLIENT_WITH_TAG, result);
		assertEquals(CLIENT_ID, this.capturedClientId);
	}

	@Test
	public void testGetClientWithSQLException() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_CLIENT_STUB));
		expectLastCall().andThrow(new SQLException());

		replayAll();
		setInternalState(this, queryExecutorMock);
		try {
			getClient(CLIENT_ID);
			fail("No exception thrown!");
		} catch (final UnknownErrorException e) {
			assertEquals(SQLException.class, e.getCause().getClass());
			verifyAll();
			assertEquals(CLIENT_ID, this.capturedClientId);

		}
	}
}
