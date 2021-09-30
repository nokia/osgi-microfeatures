/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.Test;

import com.nsn.ood.cls.core.db.Query;


/**
 * @author marynows
 *
 */
public class LockReservationsTest {

	@Test
	public void testSql() throws Exception {
		System.clearProperty("com.nsn.ood.cls.lockingDisabled");
		assertEquals("select featurecode from cls.features where featurecode = ? for update",
				new LockReservations(Arrays.asList(1L)).sql());

		System.setProperty("com.nsn.ood.cls.lockingDisabled", "false");
		assertEquals("select featurecode from cls.features where featurecode = ? for update",
				new LockReservations(Arrays.asList(1L)).sql());

	}

	@Test
	public void testSqlLockingDisabled() throws Exception {
		System.setProperty("com.nsn.ood.cls.lockingDisabled", "true");
		assertEquals("select featurecode from cls.features where featurecode = ?",
				new LockReservations(Arrays.asList(1L)).sql());

		System.setProperty("com.nsn.ood.cls.lockingDisabled", "True");
		assertEquals("select featurecode from cls.features where featurecode = ?",
				new LockReservations(Arrays.asList(1L)).sql());

	}

	@Test
	public void testPrepareAndNext() throws Exception {
		testLoop(3L);
		testLoop(1234L, 2345L);
		testLoop(6L, 5L, 4L, 3L, 2L, 1L);
	}

	private void testLoop(final Long... featureCodes) throws SQLException {
		final PreparedStatement statementMock = createStrictMock(PreparedStatement.class);

		Arrays.sort(featureCodes);
		for (int i = 0; i < featureCodes.length; i++) {
			statementMock.setLong(1, featureCodes[i]);
		}

		replayAll();
		Query query = new LockReservations(Arrays.asList(featureCodes));
		do {
			query.prepare(statementMock);
		} while ((query = query.next()) != null);
		verifyAll();
	}

	@Test
	public void testHandle() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		replayAll();
		new LockReservations(Arrays.asList(1L)).handle(resultSetMock);
		verifyAll();
	}
}
