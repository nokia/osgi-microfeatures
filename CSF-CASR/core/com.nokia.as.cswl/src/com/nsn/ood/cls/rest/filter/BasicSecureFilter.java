/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.BasicSecure;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.rest.resource.internal.TestResource;
import com.nsn.ood.cls.rest.util.Credentials;
import com.nsn.ood.cls.rest.util.CredentialsUtils;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.rest.util.ResponseBuilderFactory;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component
@Provider
@Produces(CLSMediaType.APPLICATION_JSON)
@Loggable
@BasicSecure
public class BasicSecureFilter implements ContainerRequestFilter {
	private static final Logger LOG = LoggerFactory.getLogger(BasicSecureFilter.class);
	private static final String BASIC_REALM_CLS = "Basic realm=\"CLS\"";

	@ServiceDependency
	private BasicPrincipal basicPrincipal;
	@ServiceDependency
	private ResourceBuilderFactory resourceFactory;
	@ServiceDependency
	private ResponseBuilderFactory responseFactory;
	@ServiceDependency
	private ErrorBuilderFactory errorFactory;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		try {
			final Credentials credentials = getCredentials(requestContext);
			this.basicPrincipal.setUser(credentials.getUser());
		} catch (final CLSIllegalArgumentException e) {
			abortRequest(requestContext, e.getMessage());
		}
	}

	private Credentials getCredentials(final ContainerRequestContext context) {
		final String authHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (authHeader != null) {
			return CredentialsUtils.decodeFromHeader(authHeader);
		} else {
			throw new CLSIllegalArgumentException(null);
		}
	}

	private void abortRequest(final ContainerRequestContext requestContext, final String devMessage) {
		LOG.warn("Client unauthorized. {}", Strings.nullToEmpty(devMessage));

		final boolean internalTestPage = isInternalTestPage(requestContext);
		final Status status = internalTestPage ? Status.UNAUTHORIZED : Status.NOT_FOUND;

		final Error error = this.errorFactory.status(status, devMessage).build();
		final Resource resource = this.resourceFactory.errors(error).build();
		final ResponseBuilder responseBuilder = this.responseFactory.error(status, resource);
		if (internalTestPage) {
			responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_CLS);
		}
		final Response response = responseBuilder.build();

		requestContext.abortWith(response);
	}

	private boolean isInternalTestPage(final ContainerRequestContext requestContext) {
		return Strings.nullToEmpty(requestContext.getHeaderString("referer")).endsWith(
				CLSApplication.CONTEXT_ROOT + TestResource.UPLOAD_URI);
	}
}
