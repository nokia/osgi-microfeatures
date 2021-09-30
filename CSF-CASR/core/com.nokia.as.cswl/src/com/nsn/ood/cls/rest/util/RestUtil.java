/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = RestUtil.class)
@Loggable
public class RestUtil {
	
	@ServiceDependency
	private ResourceBuilderFactory resourceFactory;
	
	@ServiceDependency
	private ResponseBuilderFactory responseFactory;
	
	@ServiceDependency
	private ErrorBuilderFactory errorFactory;

	public Response errorResponse(final StatusType status, final String message) {
		final Error error = this.errorFactory.status(status, message).build();
		return errorResponse(status, Arrays.asList(error));
	}

	public Response errorResponse(final StatusType status, final Error error) {
		return errorResponse(status, Arrays.asList(error));
	}

	public Response errorResponse(final StatusType status, final List<Error> errors) {
		final Resource resource = this.resourceFactory.errors(errors).build();
		return this.responseFactory.error(status, resource).build();
	}

	public StatusType getStatus(final WebApplicationException exception) {
		return (exception.getResponse() == null ? Status.BAD_REQUEST : exception.getResponse().getStatusInfo());
	}
}
