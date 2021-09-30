/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = ResponseBuilderFactory.class)
@Loggable
public class ResponseBuilderFactory {

	public ResponseBuilder ok(final Resource resource) {
		return createResponseBuilder(Status.OK).resource(resource);
	}

	public ResponseBuilder created(final Resource resource) {
		return createResponseBuilder(Status.CREATED).resource(resource);
	}

	public ResponseBuilder accepted(final Resource resource) {
		return createResponseBuilder(Status.ACCEPTED).resource(resource);
	}

	public ResponseBuilder noContent() {
		return createResponseBuilder(Status.NO_CONTENT);
	}

	public ResponseBuilder error(final StatusType status, final Resource resource) {
		return createResponseBuilder(status).type(CLSMediaType.APPLICATION_ERROR_JSON).resource(resource);
	}

	protected ResponseBuilder createResponseBuilder(final StatusType status) {
		return new ResponseBuilder(status);
	}
}
