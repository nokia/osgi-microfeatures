/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.util.ApiVersionChooser;
import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


/**
 * @author marynows
 *
 */
@Component(provides = ApiResource.class)
@Path(CLSApplication.API)
@Loggable(duration = true)
public class ApiResource {
	@ServiceDependency
	private ResourceBuilderFactory resourceFactory;
	@ServiceDependency
	private ResponseBuilderFactory responseFactory;
	@ServiceDependency
	private ApiVersionChooser apiVersionChooser;

	@GET
	@Produces(CLSMediaType.APPLICATION_VERSIONING_JSON)
	@Operation(
			summary = "Give indexes of rest points from /api",
			tags={"api"},
			responses = {
					@ApiResponse(responseCode = "200", description = "OK")
			}
	)
	public Response getVersions() {
		final Resource resource = this.resourceFactory.create()//
				.links("versions", link(CLSApplication.VERSION, CLSMediaType.APPLICATION_CLS_JSON))//
				.build();
		final ResponseBuilder response = this.responseFactory.ok(resource);

		addVersionHeader(response);

		return response.build();
	}

	/**
	 * @param response
	 */
	private void addVersionHeader(final ResponseBuilder response) {
		final API_VERSION currentApiVersion = this.apiVersionChooser.getCurrentVersion();
		switch (currentApiVersion) {
			case VERSION_1_0:
				break;
			case VERSION_1_1:
				response.header(CLSApplication.FEATURE_LEVEL, CLSApplication.V1_1);
				break;
			default:
				break;
		}
	}

	@GET
	@Path(CLSApplication.VERSION)
	@Produces(CLSMediaType.APPLICATION_CLS_JSON)
	@Operation(
			summary = "Give indexes of rest points from "+ CLSApplication.API + CLSApplication.VERSION,
			tags={"api"},
			responses = {
					@ApiResponse(responseCode = "200", description = "OK")
			}
	)
	public Response getResources() {
		final Resource resource = this.resourceFactory.create()//
				.links("clients", link("/clients", CLSMediaType.APPLICATION_CLIENT_JSON))//
				.links("licenses", link("/licenses", CLSMediaType.APPLICATION_LICENSE_JSON))//
				.build();
		return this.responseFactory.ok(resource).build();
	}

	private Link link(final String href, final String type) {
		return new Link().withHref(href).withType(type);
	}
}
