/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.service.ClientsService;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.errors.ETagError;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.rest.ValidateETag;
import com.nsn.ood.cls.rest.util.ErrorBuilderFactory;
import com.nsn.ood.cls.rest.util.RestUtil;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component
@Provider
@Produces(CLSMediaType.APPLICATION_JSON)
@Loggable
@ValidateETag
public class ValidateETagFilter implements ContainerRequestFilter {
	private static final Logger LOG = LoggerFactory.getLogger(ValidateETagFilter.class);

	@ServiceDependency
	private ClientsService clientsService;
	@ServiceDependency
	private ErrorBuilderFactory errorFactory;
	@ServiceDependency
	private RestUtil restUtil;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		final String clientId = getClientId(requestContext);
		final String providedETag = getProvidedETag(requestContext);
		if ((clientId != null) && (providedETag != null)) {
			try {
				final String currentETag = getCurrentETag(clientId);
				if (!eTagsEquals(providedETag, currentETag)) {
					LOG.info(
							"AUDIT: Conflicting client IDs incident detected: clientId: {}, sent ETag: {}, expected ETag: {}",
							clientId, providedETag, currentETag);
					abortRequest(requestContext, providedETag, currentETag, clientId);
				}
			} catch (final ServiceException e) {
				throw new IOException("Error ocurred during ETag retrieval", e);
			}
		}
	}

	private String getClientId(final ContainerRequestContext requestContext) {
		return requestContext.getUriInfo().getPathParameters().getFirst("clientId");
	}

	private String getProvidedETag(final ContainerRequestContext requestContext) {
		return requestContext.getHeaderString(HttpHeaders.IF_MATCH);
	}

	private String getCurrentETag(final String clientId) throws ServiceException {
		return this.clientsService.getETag(clientId);
	}

	private boolean eTagsEquals(final String providedETag, final String currentETag) {
		return StringUtils.equals(providedETag, currentETag);
	}

	private void abortRequest(final ContainerRequestContext requestContext, final String providedETag,
			final String currentETag, final String clientId) {
		LOG.warn("Etags do not match: client={}, provided={}, current={}", clientId, providedETag, currentETag);

		final Error error = this.errorFactory.code(ErrorCode.DUPLICATED_CLIENT_ID.getCode(), null)
				.embedded("etags", new ETagError().withProvidedETag(providedETag).withCurrentETag(currentETag)).build();
		requestContext.abortWith(this.restUtil.errorResponse(Status.PRECONDITION_FAILED, error));
	}
}
