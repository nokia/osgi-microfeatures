/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.ClientCreateOperation;
import com.nsn.ood.cls.core.operation.ClientRetrieveOperation;
import com.nsn.ood.cls.core.operation.ClientUpdateOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ClientsService.class)
@Loggable
public class ClientsService {
	@ServiceDependency
	private ClientCreateOperation clientCreateOperation;
	@ServiceDependency
	private ClientRetrieveOperation clientRetrieveOperation;
	@ServiceDependency
	private ClientUpdateOperation clientUpdateOperation;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;

	public ClientWithTag reserveClientId(final Client client) throws ServiceException {
		try {
			return this.clientCreateOperation.createNew(client.getKeepAliveTime(), client.getTargetType());
		} catch (final CreateException e) {
			throw this.serviceExceptionFactory.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, e, client);
		}
	}

	public ClientTag keepReservationAlive(final Client client) throws ServiceException {
		final Client existingClient = retrieveClient(client);
		existingClient.setKeepAliveTime(client.getKeepAliveTime());
		return updateClient(existingClient);
	}

	private Client retrieveClient(final Client client) throws ServiceException {
		try {
			final ClientWithTag existingClient = this.clientRetrieveOperation.getClient(client.getClientId());
			if (existingClient == null) {
				throw this.serviceExceptionFactory.clientNotFound(client);
			}
			return existingClient.getObject();
		} catch (final UnknownErrorException e) {
			throw this.serviceExceptionFactory.internalError(e);
		}
	}

	private ClientTag updateClient(final Client client) throws ServiceException {
		try {
			return this.clientUpdateOperation.update(client);
		} catch (final UpdateException e) {
			throw this.serviceExceptionFactory.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, e, client);
		}
	}

	public String getETag(final String clientId) throws ServiceException {
		try {
			final ClientWithTag clientWithTag = this.clientRetrieveOperation.getClient(clientId);
			return (clientWithTag != null ? clientWithTag.getClientTag().getETag() : null);
		} catch (final UnknownErrorException e) {
			throw this.serviceExceptionFactory.internalError(e);
		}
	}
}
