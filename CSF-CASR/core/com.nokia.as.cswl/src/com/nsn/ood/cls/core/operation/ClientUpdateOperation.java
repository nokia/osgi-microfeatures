/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.client.UpdateClient;
import com.nsn.ood.cls.core.db.client.UpdateClientExpirationTime;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.operation.util.ClientUtils;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ClientUpdateOperation.class)
@Loggable
public class ClientUpdateOperation {
	private static final Logger LOG = LoggerFactory.getLogger(ClientUpdateOperation.class);

	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency
	private ClientUtils clientUtils;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public ClientTag update(final Client client) throws UpdateException {
		if (client.getKeepAliveTime() == null) {
			client.setKeepAliveTime(this.clientUtils.getDefaultKeepAliveTime());
		}

		final ClientTag clientTag = new ClientTag()//
				.withETag(this.clientUtils.generateETag())//
				.withExpires(this.clientUtils.calculateExpiresTime(client.getKeepAliveTime()));

		execute(createUpdateClient(client, clientTag));

		return clientTag;
	}

	protected UpdateClient createUpdateClient(final Client client, final ClientTag clientTag) {
		return new UpdateClient(client, clientTag, timestamp2DateTimeConverter);
	}

	public ClientTag updateExpirationTime(final ClientWithTag clientWithTag) throws UpdateException {
		final Client client = clientWithTag.getObject();
		final ClientTag clientTag = clientWithTag.getClientTag()//
				.withETag(this.clientUtils.generateETag())//
				.withExpires(this.clientUtils.calculateExpiresTime(client.getKeepAliveTime()));

		execute(createUpdateExpirationTime(client, clientTag));

		return clientTag;
	}

	protected UpdateClientExpirationTime createUpdateExpirationTime(final Client client, final ClientTag clientTag) {
		return new UpdateClientExpirationTime(client, clientTag, timestamp2DateTimeConverter);
	}

	private <T extends Update> void execute(final T update) throws UpdateException {
		try {
			this.updateExecutor.execute(update);
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new UpdateException(e);
		}
	}
}
