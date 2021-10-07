/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.assertClientTag;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.client.UpdateClient;
import com.nsn.ood.cls.core.db.client.UpdateClientExpirationTime;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.operation.util.ClientUtilsMock;
import com.nsn.ood.cls.model.gen.clients.Client;


/**
 * @author marynows
 * 
 */
public class ClientUpdateOperationTest extends ClientUpdateOperation {
	private Client capturedClient;
	private ClientTag capturedClientTag;

	@Override
	protected UpdateClient createUpdateClient(final Client client, final ClientTag clientTag) {
		this.capturedClient = client;
		this.capturedClientTag = clientTag;
		return super.createUpdateClient(client, clientTag);
	}

	@Override
	protected UpdateClientExpirationTime createUpdateExpirationTime(final Client client, final ClientTag clientTag) {
		this.capturedClient = client;
		this.capturedClientTag = clientTag;
		return super.createUpdateExpirationTime(client, clientTag);
	}

	@Before
	public void setUp() throws Exception {
		resetAll();
		setInternalState(this, new ClientUtilsMock());
	}

	@Test
	public void testUpdate() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateClient.class));

		replayAll();
		setInternalState(this, updateExecutorMock);
		final ClientTag result = update(client("12345", 600L, "TYPE"));
		verifyAll();

		assertEquals(this.capturedClient, client("12345", 600L, "TYPE"));
		assertEquals(this.capturedClientTag, result);
		assertClientTag(this.capturedClientTag, ClientUtilsMock.ETAG, ClientUtilsMock.EXPIRES_TIME.plusSeconds(600));
	}

	@Test
	public void testUpdateWithNullKeepAliveTime() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateClient.class));

		replayAll();
		setInternalState(this, updateExecutorMock);
		final ClientTag result = update(client("12345", null, "TYPE"));
		verifyAll();

		assertEquals(this.capturedClient, client("12345", ClientUtilsMock.KEEP_ALIVE_TIME, "TYPE"));
		assertEquals(this.capturedClientTag, result);
		assertClientTag(this.capturedClientTag, ClientUtilsMock.ETAG,
				ClientUtilsMock.EXPIRES_TIME.plusSeconds((int) ClientUtilsMock.KEEP_ALIVE_TIME));
	}

	@Test
	public void testUpdateWithSQLException() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateClient.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, updateExecutorMock);
		try {
			update(client("12345", 600L, "TYPE"));
			fail();
		} catch (final UpdateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testUpdateExpirationTime() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateClientExpirationTime.class));

		replayAll();
		setInternalState(this, updateExecutorMock);
		final ClientTag result = updateExpirationTime(clientWithTag(client("12345", 600L, "TYPE"), clientTag()));
		verifyAll();

		assertEquals(this.capturedClient, client("12345", 600L, "TYPE"));
		assertEquals(this.capturedClientTag, result);
		assertClientTag(this.capturedClientTag, ClientUtilsMock.ETAG, ClientUtilsMock.EXPIRES_TIME.plusSeconds(600));
	}

	@Test
	public void testUpdateExpirationTimeWithSQLException() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(isA(UpdateClientExpirationTime.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, updateExecutorMock);
		try {
			updateExpirationTime(clientWithTag(client("12345", 600L, "TYPE"), clientTag()));
			fail();
		} catch (final UpdateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}
}
