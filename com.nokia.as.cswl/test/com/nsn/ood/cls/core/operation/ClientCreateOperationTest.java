/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.assertClientTag;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.assertClient;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

import com.nsn.ood.cls.core.convert.Client2StringConverter;
import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.client.InsertClient;
import com.nsn.ood.cls.core.db.client.QueryNextClientId;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.util.ClientUtils;
import com.nsn.ood.cls.core.operation.util.ClientUtilsMock;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ClientCreateOperationTest extends ClientCreateOperation {
	private static final Long KEEP_ALIVE_TIME = 2345L;
	private static final String TARGET_TYPE = "TYPE";
	private static final String CLIENT_ID = "54321";

	private static final long NEXT_ID = 13L;
	private static final QueryNextClientId QUERY_NEXT_CLIENT_ID_STUB = new QueryNextClientId() {
		@Override
		public Long getValue() {
			return NEXT_ID;
		};
	};

	private ClientUtilsMock clientUtilsMock;
	private Client capturedClient;
	private ClientTag capturedClientTag;

	@Override
	protected InsertClient createInsertClient(final Client client, final ClientTag clientTag) {
		this.capturedClient = client;
		this.capturedClientTag = clientTag;
		return super.createInsertClient(client, clientTag);
	}

	@Override
	protected QueryNextClientId createQueryNextClientId() {
		super.createQueryNextClientId();
		return QUERY_NEXT_CLIENT_ID_STUB;
	}

	@Before
	public void setUp() throws Exception {
		resetAll();
		this.clientUtilsMock = new ClientUtilsMock();
		setInternalState(this, ClientUtils.class, this.clientUtilsMock);
	}

	@Test
	public void testCreateNew() throws Exception {
		testCreateNew(KEEP_ALIVE_TIME, TARGET_TYPE, KEEP_ALIVE_TIME, TARGET_TYPE);
		testCreateNew(null, TARGET_TYPE, ClientUtilsMock.KEEP_ALIVE_TIME, TARGET_TYPE);
		testCreateNew(KEEP_ALIVE_TIME, null, KEEP_ALIVE_TIME, null);
		testCreateNew(null, null, ClientUtilsMock.KEEP_ALIVE_TIME, null);
	}

	private void testCreateNew(final Long keepAliveTime, final String tragetType, final Long expectedKeepAliveTime,
			final String expectedTragetType) throws SQLException, CreateException {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Client, String> converterMock = createMock(Client2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_NEXT_CLIENT_ID_STUB));
		expect(converterMock.convertTo(anyObject(Client.class))).andReturn("log");
		updateExecutorMock.execute(isA(InsertClient.class));

		replayAll();
		setInternalState(this, "client2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		final ClientWithTag result = createNew(keepAliveTime, tragetType);
		verifyAll();

		assertTrue(this.clientUtilsMock.verifyCreateNewId(NEXT_ID));
		assertEquals(this.capturedClient, result.getObject());
		assertEquals(this.capturedClientTag, result.getClientTag());
		assertClient(this.capturedClient, ClientUtilsMock.CLIENT_ID, expectedKeepAliveTime, expectedTragetType);
		assertClientTag(this.capturedClientTag, ClientUtilsMock.ETAG, ClientUtilsMock.EXPIRES_TIME);
	}

	@Test
	public void testCreateNewWithSQLExceptionDuringUpdate() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Client, String> converterMock = createMock(Client2StringConverter.class);

		queryExecutorMock.execute(same(QUERY_NEXT_CLIENT_ID_STUB));
		expect(converterMock.convertTo(anyObject(Client.class))).andReturn("log");
		updateExecutorMock.execute(isA(InsertClient.class));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "client2StringConverter", converterMock);
		setInternalState(this, "queryExecutor", queryExecutorMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		try {
			createNew(null, null);
			fail();
		} catch (final CreateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testCreateNewWithSQLExceptionDuringQuery() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_NEXT_CLIENT_ID_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, "queryExecutor", queryExecutorMock);
		try {
			createNew(null, null);
			fail();
		} catch (final CreateException e) {
			assertEquals("message", e.getMessage());
		}
		verifyAll();
	}

	@Test
	public void testCreateIfNotExistWhenClientNotExist() throws Exception {
		final ClientRetrieveOperation retrieveOperationMock = createMock(ClientRetrieveOperation.class);
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);
		final Converter<Client, String> converterMock = createMock(Client2StringConverter.class);

		expect(retrieveOperationMock.getClient(CLIENT_ID)).andReturn(null);
		expect(converterMock.convertTo(anyObject(Client.class))).andReturn("log");
		updateExecutorMock.execute(isA(InsertClient.class));

		replayAll();
		setInternalState(this, retrieveOperationMock);
		setInternalState(this, "client2StringConverter", converterMock);
		setInternalState(this, "updateExecutor", updateExecutorMock);
		final ClientWithTag result = createIfNotExist(CLIENT_ID);
		verifyAll();

		assertEquals(this.capturedClient, result.getObject());
		assertEquals(this.capturedClientTag, result.getClientTag());
		assertClient(this.capturedClient, CLIENT_ID, ClientUtilsMock.KEEP_ALIVE_TIME, null);
		assertClientTag(this.capturedClientTag, ClientUtilsMock.ETAG, ClientUtilsMock.EXPIRES_TIME);
	}

	@Test
	public void testCreateIfNotExistWhenClientAlreadyExists() throws Exception {
		final ClientRetrieveOperation retrieveOperationMock = createMock(ClientRetrieveOperation.class);

		final ClientWithTag clientWithTag = clientWithTag(client(CLIENT_ID), clientTag());

		expect(retrieveOperationMock.getClient(CLIENT_ID)).andReturn(clientWithTag);

		replayAll();
		setInternalState(this, retrieveOperationMock);
		final ClientWithTag result = createIfNotExist(CLIENT_ID);
		verifyAll();

		assertEquals(clientWithTag, result);
		assertNull(this.capturedClient);
		assertNull(this.capturedClientTag);
	}
}
