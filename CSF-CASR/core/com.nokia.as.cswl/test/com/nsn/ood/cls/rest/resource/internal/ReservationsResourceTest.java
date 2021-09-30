/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ReservationsService;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.resource.AbstractResourceTest;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;


/**
 * @author marynows
 * 
 */
public class ReservationsResourceTest extends AbstractResourceTest {
	private ReservationsResource resource;
	private ReservationsService reservationsServiceMock;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.reservationsServiceMock = createMock(ReservationsService.class);
		this.base = createMock(BaseResource.class);
		init(base);

		this.resource = new ReservationsResource();
		setInternalState(this.resource, this.reservationsServiceMock, base, this.uriInfo2ConditionsConverter);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("reservations"));
	}

	@Test
	public void testGetReservations() throws Exception {
		final Resource resourceMock = createMock(Resource.class);
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final Capture<Links> capturedLinks = new Capture<>();
		final List<Reservation> reservations = reservationsList(reservation("1"), reservation("2"));
		final MetaData metaData = metaData(20L, 10L);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.reservationsServiceMock.getReservations(conditions)).andReturn(
				new MetaDataList<>(reservations, metaData));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.reservations(reservations)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/reservations?query")), null, null));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getReservations(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/reservations?query");
	}

	@Test
	public void testGetReservationsAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.reservationsServiceMock.getReservations(conditions)).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getReservations(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetFilters() throws Exception {
		final Capture<Link> capturedLink = new Capture<>();
		final List<String> filterValues = Arrays.asList("filter1", "filter2");

		expect(this.reservationsServiceMock.getReservationFilterValues("fff")).andReturn(filterValues);
		final Resource resourceMock = createMock(Resource.class);
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.filterValues(filterValues)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("filters", "fff")).andReturn(new Link().withHref("/reservations/filters/fff"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/reservations/filters/fff");
	}

	@Test
	public void testGetFiltersAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.reservationsServiceMock.getReservationFilterValues("fff")).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();
	}
}
