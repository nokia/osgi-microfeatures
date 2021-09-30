/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.sql.ResultSet;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class QueryReservationsTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryReservations query = new QueryReservations(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.reservations", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final ReservationCreator reservationCreatorMock = createMock(ReservationCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(reservationCreatorMock.createReservation(resultSetMock)).andReturn(reservation("id"));

		replayAll();
		final QueryReservations query = new QueryReservations(CONDITIONS, MAPPER, reservationCreatorMock);
		assertEquals(reservation("id"), query.handleRow(resultSetMock));
		verifyAll();
	}
}
