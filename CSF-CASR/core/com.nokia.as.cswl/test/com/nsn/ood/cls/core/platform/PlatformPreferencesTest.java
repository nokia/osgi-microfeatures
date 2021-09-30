/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.platform;

import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;

import org.junit.Test;

import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.platform.QueryTargetId;


/**
 * @author marynows
 * 
 */
public class PlatformPreferencesTest extends PlatformPreferences {
	private static final String TARGET_ID = "11223344";
	private static final QueryTargetId QUERY_TARGET_ID_STUB = new QueryTargetId() {
		@Override
		public String getValue() {
			return TARGET_ID;
		}
	};

	@Override
	protected QueryTargetId createTargetIdQuery() {
		super.createTargetIdQuery();
		return QUERY_TARGET_ID_STUB;
	}

	@Test
	public void testGetTargetId() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_TARGET_ID_STUB));

		replayAll();
		setInternalState(this, queryExecutorMock);
		assertEquals(TARGET_ID, getTargetId());
		assertEquals(TARGET_ID, getTargetId());
		verifyAll();
	}

	@Test
	public void testGetTargetIdWithExceptionDuringQuery() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_TARGET_ID_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, queryExecutorMock);
		assertNull(getTargetId());
		verifyAll();
	}

	@Test
	public void testReloadTargetId() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_TARGET_ID_STUB));

		replayAll();
		setInternalState(this, queryExecutorMock);
		setInternalState(this, "4321");
		assertEquals(TARGET_ID, reloadTargetId());
		verifyAll();
	}

	@Test
	public void testReloadTargetIdWithExceptionDuringQuery() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(same(QUERY_TARGET_ID_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, queryExecutorMock);
		setInternalState(this, "4321");
		assertEquals("4321", reloadTargetId());
		verifyAll();
	}
}
