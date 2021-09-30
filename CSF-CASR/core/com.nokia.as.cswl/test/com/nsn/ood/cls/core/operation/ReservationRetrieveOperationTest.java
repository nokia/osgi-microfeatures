/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.creator.ReservationCreator;
import com.nsn.ood.cls.core.db.mapper.ReservationConditionsMapper;
import com.nsn.ood.cls.core.db.reservation.QueryReservations;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	ReservationRetrieveOperation.class })
public class ReservationRetrieveOperationTest {

	@Test
	public void testOperation() throws Exception {
		final ReservationConditionsMapper mapperMock = createMock(ReservationConditionsMapper.class);
		final ReservationCreator creatorMock = createMock(ReservationCreator.class);
		final Conditions conditionsMock = createMock(Conditions.class);

		final QueryReservations queryReservationsMock = createMockAndExpectNew(QueryReservations.class, conditionsMock,
				mapperMock, creatorMock);

		replayAll();
		final ReservationRetrieveOperation operation = new ReservationRetrieveOperation();
		setInternalState(operation, mapperMock, creatorMock);
		assertEquals(queryReservationsMock, operation.createQuery(conditionsMock));
		assertEquals(mapperMock, operation.getMapper());
		verifyAll();
	}
}
