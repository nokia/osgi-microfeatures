/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ReservationsService;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;


/**
 * @author wro50095
 * 
 */
@Component(provides = ReservationsResource.class)
@Path(CLSApplication.INTERNAL + "/reservations")
@Produces({
		CLSMediaType.APPLICATION_CLS_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class ReservationsResource {
	@ServiceDependency
	private ReservationsService reservationsService;

	@ServiceDependency
	private BaseResource baseResource;
	
	@ServiceDependency(filter = "(&(from=uriInfo)(to=conditions))")
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	
	@Start
	public void start() {
		baseResource.init(null, "reservations");
	}

	@GET
	@Operation(hidden = true)
	public Response getReservations(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<Reservation> reservations = this.reservationsService.getReservations(conditions.clone());

			return baseResource.getResponseFactory().ok(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, reservations.getMetaData().getFiltered()), reservations.getMetaData())//
					.reservations(reservations.getList()).build()).build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("filters/{filterName}")
	@Operation(hidden = true)
	public Response getFilters(@PathParam("filterName") final String filterName) {
		try {
			final List<String> filterValues = this.reservationsService.getReservationFilterValues(filterName);

			return baseResource.getResponseFactory().ok(baseResource.getResourceFactory()//
					.selfLink(baseResource.link("filters", filterName)).filterValues(filterValues).build()).build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}
}
