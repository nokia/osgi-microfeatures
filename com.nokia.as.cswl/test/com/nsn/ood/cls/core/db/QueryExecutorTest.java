/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public class QueryExecutorTest extends StatementExecutorTest<Query> {

	@Override
	protected StatementExecutor<Query> createExecutor() {
		return new QueryExecutor();
	}

	@Override
	protected Query createStatementMock() {
		return createStrictMock(Query.class);
	}

	@Override
	protected void executeAndHandle(final PreparedStatement preparedStatementMock, final Query statementMock)
			throws SQLException {
		final ResultSet resultSetMock = createMock(ResultSet.class);
		expect(preparedStatementMock.executeQuery()).andReturn(resultSetMock);
		statementMock.handle(resultSetMock);
		resultSetMock.close();
	}
}
