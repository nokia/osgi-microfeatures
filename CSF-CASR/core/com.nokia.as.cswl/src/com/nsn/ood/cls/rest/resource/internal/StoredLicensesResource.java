/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.model.LicenseFile;
import com.nsn.ood.cls.core.service.LicensesService;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.Licenses;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.BasicSecure;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.rest.util.HttpUtils;
import com.nsn.ood.cls.rest.util.MultipartOutputBuilder;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;


/**
 * @author marynows
 * 
 */
@Component(provides = StoredLicensesResource.class)
@Path(CLSApplication.INTERNAL + "/licenses")
@Produces({
		CLSMediaType.APPLICATION_LICENSE_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class StoredLicensesResource {
	private static final Logger LOG = LoggerFactory.getLogger(StoredLicensesResource.class);
	private static final String LOG_INSTALL_LICENSE = "Install license";
	private static final String LOG_CANCEL_LICENSE = "Cancel license";
	private static final String LOG_EXPORT_LICENSE = "Export license";

	@ServiceDependency
	private LicensesService licensesService;
	@ServiceDependency
	private ActivityService activityService;
	@ServiceDependency
	private HttpUtils httpUtils;
	@ServiceDependency
	private ErrorExceptionFactory errorExceptionFactory;
	@ServiceDependency
	private MultipartOutputBuilder multipartOutputBuilder;
	@ServiceDependency
	private BaseResource baseResource;
	
	@ServiceDependency(filter = "(&(from=uriInfo)(to=conditions))")
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	
	@ServiceDependency(filter = "(&(from=errorException)(to=error))")
	private Converter<ErrorException, Error> errorException2ErrorConverter;

	@Start
	public void start() {
		baseResource.init(LOG, "licenses");
	}

	@GET
	@Produces({
			CLSMediaType.APPLICATION_CLS_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
	@Operation(hidden = true)
	public Response getStoredLicenses(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<StoredLicense> licenses = this.licensesService.getStoredLicenses(conditions.clone());

			return baseResource.getResponseFactory().ok(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, licenses.getMetaData().getFiltered()), licenses.getMetaData())//
					.storedLicenses(licenses.getList()).build()).build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("filters/{filterName}")
	@Produces({
			CLSMediaType.APPLICATION_CLS_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
	@Operation(hidden = true)
	public Response getFilters(@PathParam("filterName") final String filterName) {
		try {
			final List<String> filterValues = this.licensesService.getStoredLicenseFilterValues(filterName);

			return baseResource.getResponseFactory().ok(baseResource.getResourceFactory()//
					.selfLink(baseResource.link("filters", filterName)).filterValues(filterValues).build()).build();
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@BasicSecure
	@Operation(hidden = true)
	public Response upload(final MultiPart input) {
		final List<License> licenses = new ArrayList<>();
		final List<ErrorException> errorExceptions = new ArrayList<>();

		for (final BodyPart part : input.getBodyParts()) {
			if ("xml".equals(part.getMediaType().getSubtype())) {
				final String fileName = this.httpUtils.extractFileName(part);
				baseResource.logInit(LOG_INSTALL_LICENSE, new DescriptionBuilder().append("fileName", fileName).build());
				try {
					final LicenseFile licenseFile = new LicenseFile().withFileName(fileName)//
							.withContent(part.getEntityAs(String.class));
					licenses.add(this.licensesService.install(licenseFile));
					baseResource.logSuccess(LOG_INSTALL_LICENSE);
				} catch (final ServiceException e) {
					baseResource.logFailure(LOG_INSTALL_LICENSE, e);
					errorExceptions.addAll(e.getExceptions());
				}
			}
		}

		baseResource.logInit(BaseResource.LOG_ADD_ACTIVITY, LOG_INSTALL_LICENSE);
		try {
			this.activityService.addLicenseInstallActivity(licenses, errorExceptions);
			baseResource.logSuccess(BaseResource.LOG_ADD_ACTIVITY);
		} catch (final ServiceException e) {
			baseResource.logFailure(BaseResource.LOG_ADD_ACTIVITY, e);
		}

		final Resource resource = createResource(baseResource.link("upload"), licenses, errorExceptions);
		return baseResource.getResponseFactory().created(resource).build();
	}

	@POST
	@Path("cancel")
	@Consumes(CLSMediaType.APPLICATION_LICENSE_JSON)
	@BasicSecure
	@Operation(hidden = true)
	public Response cancel(final Licenses licenses) {
		final List<License> canceledLicenses = new ArrayList<>();
		final List<ErrorException> errorExceptions = new ArrayList<>();

		for (final License license : licenses.getLicenses()) {
			baseResource.logInit(LOG_CANCEL_LICENSE, new DescriptionBuilder().append("fileName", license.getFileName()).build());
			try {
				canceledLicenses.add(this.licensesService.cancel(license));
				baseResource.logSuccess(LOG_CANCEL_LICENSE);
			} catch (final ServiceException e) {
				baseResource.logFailure(LOG_CANCEL_LICENSE, e);
				errorExceptions.addAll(e.getExceptions());
			}
		}

		baseResource.logInit(BaseResource.LOG_ADD_ACTIVITY, LOG_CANCEL_LICENSE);
		try {
			this.activityService.addLicenseCancelActivity(canceledLicenses, errorExceptions);
			baseResource.logSuccess(BaseResource.LOG_ADD_ACTIVITY);
		} catch (final ServiceException e) {
			baseResource.logFailure(BaseResource.LOG_ADD_ACTIVITY, e);
		}

		final Resource resource = createResource(baseResource.link("cancel"), canceledLicenses, errorExceptions);
		return baseResource.getResponseFactory().accepted(resource).build();
	}

	private Resource createResource(final Link link, final List<License> licenses,
			final List<ErrorException> errorExceptions) {
		return baseResource.getResourceFactory().selfLink(link)//
				.licenses(licenses)//
				.errors(errorExceptions.stream().map(errorException2ErrorConverter::convertTo).collect(Collectors.toList()))//
				.build();
	}

	@POST
	@Path("export")
	@Consumes(CLSMediaType.APPLICATION_LICENSE_JSON)
	@Produces("multipart/mixed")
	@Operation(hidden = true)
	public Response export(final Licenses licenses) {
		for (final License license : licenses.getLicenses()) {
			baseResource.logInit(LOG_EXPORT_LICENSE, new DescriptionBuilder().append("serialNumber", license.getSerialNumber())
					.build());
			try {
				final LicenseFile licenseFile = this.licensesService.export(license);
				this.multipartOutputBuilder.addFilePart(licenseFile.getContent(), MediaType.TEXT_XML_TYPE,
						licenseFile.getFileName());
				baseResource.logSuccess(LOG_EXPORT_LICENSE);
			} catch (final ServiceException e) {
				baseResource.logFailure(LOG_EXPORT_LICENSE, e);
				final List<Error> errors = e.getExceptions().stream().map(errorException2ErrorConverter::convertTo).collect(Collectors.toList());
				this.multipartOutputBuilder.addJsonPart(errors, CLSMediaType.APPLICATION_ERROR_JSON_TYPE);
			}
		}

		return Response.ok().entity(this.multipartOutputBuilder.build()).build();
	}
}
