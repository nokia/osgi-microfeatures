/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.ReservationRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 * 
 */
public class ReservationsServiceTest {
	private ReservationRetrieveOperation reservationRetrieveOperationMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private ReservationsService service;

	@Before
	public void setUp() throws Exception {
		this.reservationRetrieveOperationMock = createMock(ReservationRetrieveOperation.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new ReservationsService();
		setInternalState(this.service, this.reservationRetrieveOperationMock, this.serviceExceptionFactoryMock);
	}

	@Test
	public void testGetReservations() throws Exception {
		final MetaDataList<Reservation> reservations = new MetaDataList<>(reservationsList(reservation("1"),
				reservation("2")), metaData());
		final Conditions conditionsMock = createMock(Conditions.class);

		expect(this.reservationRetrieveOperationMock.getList(conditionsMock)).andReturn(reservations);

		replayAll();
		assertEquals(reservations, this.service.getReservations(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetReservationsWithRetrieveException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.reservationRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("message"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("message"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getReservations(conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetReservationFilterValues() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		expect(this.reservationRetrieveOperationMock.getFilterValues("filterName", conditions)).andReturn(
				Arrays.asList("1", "2"));

		replayAll();
		assertEquals(Arrays.asList("1", "2"), this.service.getReservationFilterValues("filterName"));
		verifyAll();
	}

	@Test
	public void testGetReservationFilterValuesWithException() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.reservationRetrieveOperationMock.getFilterValues("filterName", conditions)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getReservationFilterValues("filterName");
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

}
