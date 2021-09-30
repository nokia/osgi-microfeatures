/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceScope;
import org.slf4j.Logger;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;
import com.nsn.ood.cls.rest.util.RestUtil;
import com.nsn.ood.cls.rest.util.ViolationExceptionBuilderFactory;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
@Component(provides = BaseResource.class, scope = ServiceScope.PROTOTYPE)
public class BaseResource {
	public static final String LOG_ADD_ACTIVITY = "Add activity";

	private static final String SEPARATOR = "/";

	@Context
	private Application application;
	@ServiceDependency
	private RestUtil restUtil;
	@ServiceDependency(filter = "(&(from=conditions)(to=queryString))")
	private Converter<Conditions, String> conditions2QueryStringConverter;
	@ServiceDependency(filter = "(&(from=errorException)(to=error))")
	private Converter<ErrorException, Error> errorException2ErrorConverter;
	@ServiceDependency
	private ResourceBuilderFactory resourceFactory;
	@ServiceDependency
	private ResponseBuilderFactory responseFactory;
	@ServiceDependency
	private ViolationExceptionBuilderFactory violationFactory;

	private Logger logger;
	private String selfHref;

	public void init() {
		init(null);
	}

	public void init(final Logger logger) {
		this.logger = logger;
		this.selfHref = "";
	}

	public void init(final Logger logger, final String... path) {
		this.logger = logger;
		this.selfHref = join(path);
	}

	public Link link(final String... path) {
		return new Link().withHref(this.selfHref + join(path));
	}

	public Links links(final Conditions conditions, final long total) {
		final Link selfLink = link(conditions);
		Link nextLink = null;
		Link prevLink = null;

		if (conditions.pagination().isLimited()) {
			final int offset = conditions.pagination().offset();
			final int nextOffset = offset + conditions.pagination().limit();
			final int prevOffset = offset - conditions.pagination().limit();

			if (nextOffset < total) {
				conditions.setPaginationOffset(nextOffset);
				nextLink = link(conditions);
			}

			if (prevOffset >= 0) {
				conditions.setPaginationOffset(prevOffset);
				prevLink = link(conditions);
			} else if (offset > 0) {
				conditions.setPaginationOffset(0);
				prevLink = link(conditions);
			}
		}

		return new Links(selfLink, nextLink, prevLink);
	}

	private Link link(final Conditions conditions) {
		final String query = conditions2QueryStringConverter.convertTo(conditions);
		return new Link().withHref(this.selfHref + query);
	}

	private String join(final String... parts) {
		final StringBuilder builder = new StringBuilder();
		for (final String part : parts) {
			builder.append(SEPARATOR).append(part);
		}
		return builder.toString();
	}

	public CLSApplication application() {
		if (this.application instanceof CLSApplication) {
			return (CLSApplication) this.application;
		}
		return null;
	}

	public Response exceptionResponse(final ServiceException exception) {
		final Status status = getStatus(exception);
		final List<Error> errors = exception.getExceptions().stream().map(errorException2ErrorConverter::convertTo).collect(Collectors.toList());
		return this.restUtil.errorResponse(status, errors);
	}

	private Status getStatus(final ServiceException exception) {
		if (containsSQLException(exception)) {
			return Status.INTERNAL_SERVER_ERROR;
		} else
			return exception.isNotFound() ? Status.NOT_FOUND : Status.BAD_REQUEST;
	}

	private boolean containsSQLException(final ServiceException exception) {
		return hasOneElement(exception) && isCausedBySQLException(exception);
	}

	private boolean isCausedBySQLException(final ServiceException exception) {
		final Throwable firstCause = exception.getExceptions().get(0).getCause();
		return (firstCause != null) && (firstCause.getCause() instanceof SQLException);
	}

	private boolean hasOneElement(final ServiceException exception) {
		return exception.getExceptions().size() == 1;
	}

	public void logInit(final String action, final String details) {
		this.logger.info("{}: {}", action, details);
	}

	public void logSuccess(final String action) {
		this.logger.info("{}: Success", action);
	}

	public void logSuccess(final String action, final String details) {
		this.logger.info("{}: Success: {}", action, details);
	}

	public void logFailure(final String action, final ServiceException exception) {
		this.logger.error("{}: Failure: {}", action,
				exception.getExceptions().stream().map(errorException2ErrorConverter::convertTo).collect(Collectors.toList()).toString());
	}

	public void logFailure(final String action, final String details) {
		this.logger.error("{}: Failure: {}", action, details);
	}

	public RestUtil getRestUtil() {
		return restUtil;
	}

	public ResourceBuilderFactory getResourceFactory() {
		return resourceFactory;
	}

	public ResponseBuilderFactory getResponseFactory() {
		return responseFactory;
	}

	public ViolationExceptionBuilderFactory getViolationFactory() {
		return violationFactory;
	}
}
