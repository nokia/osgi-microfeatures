/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.util.osgi.transaction.TransactionService;


/**
 * @author marynows
 * 
 */
@Component
@Property(name = "name", value = "query")
public class QueryExecutor implements StatementExecutor<Query> {
	
	private static final Logger LOG = LoggerFactory.getLogger(StatementExecutor.class);
	
	@ServiceDependency
	private TransactionService txService;

	@Override
	public void executeAndHandle(final PreparedStatement preparedStatement, final Query statement)
			throws SQLException {
		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			statement.handle(resultSet);
		}
	}
	
	@Override
	public void execute(final Query statement) throws SQLException {
		txService.txControl().notSupported(() -> {
			try (Connection connection = txService.getConnection()) {
					Query s = statement;
					do {
						s = execute(connection, s);
					} while (s != null);
					return null;
			}
		});
	}

	private Query execute(final Connection connection, final Query statement) throws SQLException {
		try (PreparedStatement preparedStatement = createPreparedStatement(connection, statement)) {
			statement.prepare(preparedStatement);
			log(statement, preparedStatement);
			executeAndHandle(preparedStatement, statement);
		}
		return statement.next();
	}

	private PreparedStatement createPreparedStatement(final Connection connection, final Query statement) throws SQLException {
		final PreparedStatement prepareStatement = connection.prepareStatement(statement.sql());
		if (LOG.isTraceEnabled()) {
			return new LoggingPreparedStatement(prepareStatement);
		} else {
			return prepareStatement;
		}
	}

	private void log(final Query statement, final PreparedStatement preparedStatement) {
		if (preparedStatement instanceof LoggingPreparedStatement) {
			final List<Object> parameters = ((LoggingPreparedStatement) preparedStatement).getParameters();
			String sql = statement.sql();
			for (final Object parameter : parameters) {
				String value = String.valueOf(parameter);
				if ((parameter instanceof Date//
				) || (parameter instanceof Time//
				) || (parameter instanceof Timestamp//
				) || (parameter instanceof String)) {
					value = "\"" + value + "\"";
				}
				sql = sql.replaceFirst("\\?", value);
			}
			LOG.trace("{}: {}", statement.getClass().getSimpleName(), sql);
		}
	}
}
