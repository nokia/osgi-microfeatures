/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsTest {

	@Test
	public void testSql() throws Exception {
		assertEquals("delete from cls.reservations where clientid = ?", new DeleteReservations(null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "12345");

		replayAll();
		new DeleteReservations(client("12345")).prepare(statementMock);
		verifyAll();
	}
}
