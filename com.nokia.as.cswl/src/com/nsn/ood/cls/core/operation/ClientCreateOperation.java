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
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.db.client.InsertClient;
import com.nsn.ood.cls.core.db.client.QueryNextClientId;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.util.ClientUtils;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ClientCreateOperation.class)
@Loggable
public class ClientCreateOperation {
	private static final Logger LOG = LoggerFactory.getLogger(ClientCreateOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency(filter = "(name=update)")
	private StatementExecutor<Update> updateExecutor;
	@ServiceDependency
	private ClientUtils clientUtils;
	@ServiceDependency
	private ClientRetrieveOperation clientRetrieveOperation;
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	@ServiceDependency(filter = "(&(from=client)(to=string))")
	private Converter<Client, String> client2StringConverter;

	public ClientWithTag createNew(final Long keepAliveTime, final String targetType) throws CreateException {
		final String clientId = this.clientUtils.createNewId(retrieveNextId());
		return create(clientId, keepAliveTime, targetType);
	}

	private long retrieveNextId() throws CreateException {
		try {
			final QueryNextClientId query = createQueryNextClientId();
			this.queryExecutor.execute(query);
			return query.getValue();
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new CreateException(e);
		}
	}

	protected QueryNextClientId createQueryNextClientId() {
		return new QueryNextClientId();
	}

	public ClientWithTag createIfNotExist(final String clientId) throws CreateException, UnknownErrorException {
		ClientWithTag clientWithTag = this.clientRetrieveOperation.getClient(clientId);
		if (clientWithTag == null) {
			clientWithTag = create(clientId, null, null);
		}
		return clientWithTag;
	}

	private ClientWithTag create(final String clientId, final Long keepAliveTime, final String targetType)
			throws CreateException {
		final Client client = new Client()//
				.withClientId(clientId)//
				.withKeepAliveTime(getKeepAliveTime(keepAliveTime))//
				.withTargetType(targetType);
		LOG.debug("Creating new client: {}", client2StringConverter.convertTo(client));

		final ClientTag clientTag = new ClientTag()//
				.withETag(this.clientUtils.generateETag())//
				.withExpires(this.clientUtils.calculateDefaultExpiresTime());

		try {
			this.updateExecutor.execute(createInsertClient(client, clientTag));
		} catch (final SQLException e) {
			LOG.error(LogMessage.UPDATE_FAIL, e);
			throw new CreateException(e);
		}

		return new ClientWithTag().withObject(client).withClientTag(clientTag);
	}

	private long getKeepAliveTime(final Long keepAliveTime) {
		if (keepAliveTime == null) {
			return this.clientUtils.getDefaultKeepAliveTime();
		}
		return keepAliveTime;
	}

	protected InsertClient createInsertClient(final Client client, final ClientTag clientTag) {
		return new InsertClient(client, clientTag, timestamp2DateTimeConverter);
	}
}
