/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import java.util.List;
import java.util.regex.Pattern;

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
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.service.LicensesService;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.licenses.DBLicense;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


/**
 * @author marynows
 * 
 */
@Component(provides = LicensesResource.class)
@Path(CLSApplication.API + CLSApplication.VERSION + "/licenses")
@Produces({
		CLSMediaType.APPLICATION_LICENSE_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class LicensesResource {
	private static final Pattern SERIAL_NUMBER_PATTERN = Pattern.compile("\\d{1,15}");

	@ServiceDependency
	private LicensesService licensesService;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;
	
	@ServiceDependency
	private BaseResource baseResource;
	@ServiceDependency(filter = "(&(from=uriInfo)(to=conditions))")
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;

	@Start
	public void start() {
		baseResource.init(null, "licenses");
	}

	@GET
	@Operation(
			summary = "Query License Keys with filtering criteria",
			description = "It's possible to get filtered licenses, for example with GET /licenses?featureCode=2220, Feature model has to be used for filters",
			tags={"Licenses"},
			responses = {
					@ApiResponse(responseCode = "200"),
					@ApiResponse(responseCode = "400")
			}
	)
	public Response getLicenses(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<License> licenses = this.licensesService.getLicenses(conditions.clone());

			return getLicensesResponse(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, licenses.getMetaData().getFiltered()), licenses.getMetaData())//
					.licenses(licenses.getList()).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("/db")
	@Operation(
			summary = "Query License Keys with filtering criteria",
			description = "It's possible to get filtered db licenses, for example with GET /licenses/db",
			tags={"DBLicenses"},
			responses = {
					@ApiResponse(responseCode = "200"),
					@ApiResponse(responseCode = "400")
			}
	)
	public Response getDBLicenses(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<DBLicense> licenses = this.licensesService.getDBLicenses(conditions.clone());

			return getLicensesResponse(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, licenses.getMetaData().getFiltered()), licenses.getMetaData())//
					.dblicenses(licenses.getList()).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("{serialNumber}")
	@Operation(
			summary = "Query License Key details",
			tags={"Licenses"},
			responses = {
					@ApiResponse(responseCode = "200"),
					@ApiResponse(responseCode = "400")
			}
	)
	public Response getLicense(@PathParam("serialNumber") final String serialNumber) {
		verifySerialNumber(serialNumber);

		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData()
				.equalFilter("serialNumber", serialNumber).build();

		try {
			final MetaDataList<License> licenses = this.licensesService.getLicenses(conditions);
			verifyIfNotEmpty(licenses.getList(), serialNumber);

			return getLicensesResponse(baseResource.getResourceFactory()//
					.selfLink(baseResource.link(serialNumber))//
					.licenses(licenses.getList()).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	private Response getLicensesResponse(final Resource resource) {
		return baseResource.getResponseFactory().ok(resource).build();
	}

	private void verifySerialNumber(final String serialNumber) {
		if (!SERIAL_NUMBER_PATTERN.matcher(serialNumber).matches()) {
			throw baseResource.getViolationFactory().valueException("licenses.serialNumber", serialNumber);
		}
	}

	private void verifyIfNotEmpty(final List<License> licenses, final String serialNumber) throws ServiceException {
		if (CollectionUtils.isEmpty(licenses)) {
			throw this.serviceExceptionFactory.licenseNotFound(new License().withSerialNumber(serialNumber)
					.withTargets(null).withFeatures(null));
		}
	}
}
