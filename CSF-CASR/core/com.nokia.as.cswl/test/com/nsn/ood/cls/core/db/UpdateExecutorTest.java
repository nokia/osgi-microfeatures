/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createStrictMock;

import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public class UpdateExecutorTest extends StatementExecutorTest<Update> {

	@Override
	protected StatementExecutor<Update> createExecutor() {
		return new UpdateExecutor();
	}

	@Override
	protected Update createStatementMock() {
		return createStrictMock(Update.class);
	}

	@Override
	protected void executeAndHandle(final PreparedStatement preparedStatementMock, final Update statementMock)
			throws SQLException {
		expect(preparedStatementMock.executeUpdate()).andReturn(7);
		statementMock.handle(7);
	}
}
