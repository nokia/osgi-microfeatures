/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.ETAG;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.assertClientTag;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.ClientCreateOperation;
import com.nsn.ood.cls.core.operation.ClientRetrieveOperation;
import com.nsn.ood.cls.core.operation.ClientUpdateOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;


/**
 * @author marynows
 * 
 */
public class ClientsServiceTest {
	private static final String ID = "12345";

	private ClientCreateOperation createOperationMock;
	private ClientRetrieveOperation retrieveOperationMock;
	private ClientUpdateOperation updateOperationMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private ClientsService service;

	@Before
	public void setUp() throws Exception {
		this.createOperationMock = createMock(ClientCreateOperation.class);
		this.retrieveOperationMock = createMock(ClientRetrieveOperation.class);
		this.updateOperationMock = createMock(ClientUpdateOperation.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new ClientsService();
		setInternalState(this.service, this.createOperationMock, this.retrieveOperationMock, this.updateOperationMock,
				this.serviceExceptionFactoryMock);
	}

	@Test
	public void testReserveClientId() throws Exception {
		testReserveClientId(null, null);
		testReserveClientId(null, "TYPE");
		testReserveClientId(1800L, "TYPE");
		testReserveClientId(1800L, null);
	}

	private void testReserveClientId(final Long keepAliveTime, final String targetType) throws Exception {
		resetAll();

		expect(this.createOperationMock.createNew(keepAliveTime, targetType)).andReturn(
				clientWithTag(client(ID), clientTag()));

		replayAll();
		final ClientWithTag result = this.service.reserveClientId(client(null, keepAliveTime, targetType));
		verifyAll();

		assertEquals(clientWithTag(client(ID), clientTag()), result);
	}

	@Test
	public void testReserveClientIdWithCreateException() throws Exception {
		final CreateException exceptionMock = createMock(CreateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.createOperationMock.createNew(null, null)).andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, exceptionMock, client()))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveClientId(client());
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetETag() throws Exception {
		expect(this.retrieveOperationMock.getClient(ID)).andReturn(clientWithTag(client(ID), clientTag()));

		replayAll();
		assertEquals(ETAG, this.service.getETag(ID));
		verifyAll();
	}

	@Test
	public void testGetETagWhenClientTagIsNull() throws Exception {
		expect(this.retrieveOperationMock.getClient(ID)).andReturn(null);

		replayAll();
		assertNull(this.service.getETag(ID));
		verifyAll();
	}

	@Test
	public void testKeepReservationAlive() throws Exception {
		testKeepReservationAlive(1234L);
		testKeepReservationAlive(null);
	}

	private void testKeepReservationAlive(final Long keepAliveTime) throws Exception {
		resetAll();

		expect(this.retrieveOperationMock.getClient(ID)).andReturn(
				clientWithTag(client(ID, 1800L, "TYPE"), clientTag()));
		expect(this.updateOperationMock.update(eq(client(ID, keepAliveTime, "TYPE")))).andReturn(clientTag());

		replayAll();
		assertClientTag(this.service.keepReservationAlive(client(ID, keepAliveTime)));
		verifyAll();
	}

	@Test
	public void testKeepReservationAliveWithUpdateException() throws Exception {
		final UpdateException exceptionMock = createMock(UpdateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.retrieveOperationMock.getClient(ID)).andReturn(
				clientWithTag(client(ID, 1800L, "TYPE"), clientTag()));
		this.updateOperationMock.update(eq(client(ID, 1234L, "TYPE")));
		expectLastCall().andThrow(exceptionMock);
		expect(
				this.serviceExceptionFactoryMock.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, exceptionMock,
						client(ID, 1234L, "TYPE"))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.keepReservationAlive(client(ID, 1234L));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testKeepReservationAliveForNonExistingClient() throws Exception {
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.retrieveOperationMock.getClient(ID)).andReturn(null);
		expect(this.serviceExceptionFactoryMock.clientNotFound(client(ID))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.keepReservationAlive(client(ID));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

}
