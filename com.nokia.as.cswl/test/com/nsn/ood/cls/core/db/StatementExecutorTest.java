/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.transaction.control.TransactionControl;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.util.osgi.transaction.TransactionService;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	StatementExecutor.class })
public abstract class StatementExecutorTest<S extends Statement<S>> {
	private PreparedStatement preparedStatementMock;
	private Connection connectionMock;
	private TransactionService txServiceMock;
	private TransactionControl txControlMock;
	private Capture<PreparedStatement> capturedPreparedStatement;
	private StatementExecutor<S> executor;

	protected abstract StatementExecutor<S> createExecutor();

	protected abstract S createStatementMock();

	protected abstract void executeAndHandle(PreparedStatement preparedStatementMock, S statementMock)
			throws SQLException;

	@Before
	public void setUp() {
		this.preparedStatementMock = createMock(PreparedStatement.class);
		this.connectionMock = createMock(Connection.class);
		this.txServiceMock = createMock(TransactionService.class);
		this.capturedPreparedStatement = new Capture<>(CaptureType.ALL);
		this.txControlMock = createMock(TransactionControl.class);

		this.executor = createExecutor();
		setInternalState(this.executor, this.txServiceMock);
	}

	@Test
	public void testExecuteSimpleQuery() throws Exception {
		final S statementMock = createStatementMock();
		expect(txServiceMock.txControl()).andReturn(txControlMock);
		executeMock(statementMock, "sql", null);
		replay(txServiceMock, connectionMock, statementMock, preparedStatementMock);
		
		expect(txControlMock.notSupported(EasyMock.anyObject())).andReturn(doTx(statementMock));
		replayAll();
		
		this.executor.execute(statementMock);
		assertPreparedStatement(1);
	}

	@Test
	public void testExecuteMultipleQuery() throws Exception {
		expect(txServiceMock.txControl()).andReturn(txControlMock);

		final S firstStatement = createStatementMock();
		S statementMock = firstStatement;
		for (int i = 0; i < 5; i++) {
			final S nextQueryMock = createStatementMock();
			executeMock(statementMock, "sql" + i, nextQueryMock);
			replay(statementMock);
			statementMock = nextQueryMock;
		}
		executeMock(statementMock, "sqlx", null);
		replay(txServiceMock, connectionMock, statementMock, preparedStatementMock);

		expect(txControlMock.notSupported(EasyMock.anyObject())).andReturn(doTx(firstStatement));
		replayAll();
		this.executor.execute(firstStatement);

		assertPreparedStatement(6);
	}

	private void assertPreparedStatement(final int size) {
		final List<PreparedStatement> values = this.capturedPreparedStatement.getValues();
		assertEquals(size, values.size());
		for (final PreparedStatement preparedStatement : values) {
			assertEquals(this.preparedStatementMock, preparedStatement);
		}
	}
	
	private void executeMock(S statementMock, String sql, S nextQueryMock) throws SQLException {
		expect(txServiceMock.getConnection()).andReturn(connectionMock);
		expect(statementMock.sql()).andReturn(sql);
		expect(connectionMock.prepareStatement(sql)).andReturn(preparedStatementMock);
		statementMock.prepare(capture(capturedPreparedStatement));
		executeAndHandle(preparedStatementMock, statementMock);
		preparedStatementMock.close();
		expect(statementMock.next()).andReturn(nextQueryMock);
		connectionMock.close();
	}
	
	private Object doTx(S statement) throws SQLException {
		try (Connection connection = txServiceMock.getConnection()) {
			S s = statement;
			do {
				try (PreparedStatement preparedStatement = connection.prepareStatement(s.sql())) {
					s.prepare(preparedStatement);
					executor.executeAndHandle(preparedStatement, s);
				}
				s = s.next();
			} while (s != null);
			return null;
		}
	}
}
