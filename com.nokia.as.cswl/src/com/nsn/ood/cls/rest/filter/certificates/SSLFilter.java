/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.rest.exception.CLSCertificateException;
import com.nsn.ood.cls.rest.filter.certificates.checkers.AlwaysFailChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.ConditionChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.CorrectPatchChecker;
import com.nsn.ood.cls.rest.filter.certificates.checkers.SSLConditionChecker;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component
@Provider
@Produces(CLSMediaType.APPLICATION_JSON)
@Loggable
public class SSLFilter implements ContainerRequestFilter {
	private static final Logger LOG = LoggerFactory.getLogger(SSLFilter.class);

	@ServiceDependency
	private CertificateInfoProviderThankYouPowerMock certificateInfoProvider;
	@ServiceDependency
	private CLSCertificateLoader certificateLoader;
	
	private final List<ConditionChecker> conditionCheckers = new ArrayList<>();

	@Start
	private void initSSLFilter() {
		initSSLChecker();
		initCertificatesChecker();
	}

	private void initSSLChecker() {
		this.conditionCheckers.add(new SSLConditionChecker());
	}

	private void initCertificatesChecker() {
		try {
			final List<X509Certificate> clsCertificates = this.certificateLoader
					.loadCertificates(this.certificateInfoProvider);
			this.conditionCheckers.add(new CorrectPatchChecker(clsCertificates));
		} catch (final CLSCertificateException e) {
			LOG.warn("Error occured during SSL filter initialization, access to all resources will be denied", e);
			this.conditionCheckers.add(new AlwaysFailChecker());
		}
	}

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		
		SecurityContext sc = new SecurityContext() {
			public boolean isUserInRole(String arg0) { return false; }
			public boolean isSecure() { return requestContext.getUriInfo().getBaseUri().getScheme().equals("https"); }
			public Principal getUserPrincipal() { return null; }
			public String getAuthenticationScheme() { return SecurityContext.BASIC_AUTH; }
		};
		requestContext.setSecurityContext(sc);
		
		for (final ConditionChecker conditionChecker : this.conditionCheckers) {
			if (!conditionChecker.checkCondition(requestContext)) {
				LOG.debug("Condition checker {} aborts certificate validation process",
						conditionChecker.getClass().getCanonicalName());
				abortRequest(conditionChecker.getErrorMessaage(), requestContext);
				return;
			}
		}
	}

	private void abortRequest(final String message, final ContainerRequestContext requestContext) {
		LOG.debug(message);
		requestContext.abortWith(Response.status(Status.NOT_FOUND).build());
	}
}
