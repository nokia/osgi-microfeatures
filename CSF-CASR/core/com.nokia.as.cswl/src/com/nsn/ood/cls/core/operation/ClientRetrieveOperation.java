/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.client.QueryClient;
import com.nsn.ood.cls.core.db.creator.ClientWithTagCreator;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ClientRetrieveOperation.class)
@Loggable
public class ClientRetrieveOperation {
	private static final Logger LOG = LoggerFactory.getLogger(ClientRetrieveOperation.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;
	@ServiceDependency
	private ClientWithTagCreator clientWithTagCreator;

	public ClientWithTag getClient(final String clientId) throws UnknownErrorException {
		final QueryClient query = createQueryClient(clientId);
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			throw new UnknownErrorException("Eror occured during client retrieval", e);
		}
		return query.getValue();
	}

	protected QueryClient createQueryClient(final String clientId) {
		return new QueryClient(clientId, this.clientWithTagCreator);
	}
}
