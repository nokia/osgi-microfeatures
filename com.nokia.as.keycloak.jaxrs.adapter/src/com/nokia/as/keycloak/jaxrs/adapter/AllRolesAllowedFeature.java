// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.jaxrs.adapter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;
import org.osgi.service.component.annotations.Component;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.internal.LocalizationMessages;

@Component(service = AllRolesAllowedFeature.class)
@Provider
public class AllRolesAllowedFeature implements DynamicFeature {
	private final static Logger _log = Logger.getLogger("" + JaxrsBearerTokenFilterImpl.class);

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {

		final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
		if (am.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed annotation = am.getAnnotation(RolesAllowed.class);
			String rolesAllowed = annotation.value()[0];
			if (rolesAllowed.equals("*")) {
				if (_log.isDebugEnabled()) {
					_log.debug("Token is required for " + resourceInfo.getResourceMethod());
				}
				context.register(SecuredRequestFilter.class);
			}
			return;
		}
	}

	@Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
	private static class SecuredRequestFilter implements ContainerRequestFilter {

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			if (noToken(requestContext))
				throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED());
		}

		public boolean noToken(ContainerRequestContext request) {
			String authHeader = request.getHeaderString("Authorization");
			String tokenString = null;
			if (authHeader == null)
				return true;
			String[] split = authHeader.trim().split("\\s+");
			if (split.length > 1)
				tokenString = split[1];
			return tokenString == null;
		}
	}

}