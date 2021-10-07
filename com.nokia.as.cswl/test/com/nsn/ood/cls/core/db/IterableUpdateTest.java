/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class IterableUpdateTest {

	@Test
	public void testIterableUpdate() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final List<String> capturedValues = new ArrayList<>();

		replayAll();
		final IterableUpdate<String> update = new IterableUpdate<String>(null, Arrays.asList("test1", "test2")) {
			@Override
			protected void prepareRow(final PreparedStatement statement, final String value) throws SQLException {
				assertEquals(statementMock, statement);
				capturedValues.add(value);
			}
		};

		assertEquals(0, update.getIndex());

		update.prepare(statementMock);
		assertEquals(0, update.getIndex());
		assertSame(update, update.next());

		update.prepare(statementMock);
		assertEquals(1, update.getIndex());
		assertNull(update.next());
		verifyAll();

		assertEquals(Arrays.asList("test1", "test2"), capturedValues);
	}
}
