package com.nokia.as.jaxrs.jersey.sless;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.internal.LocalizationMessages;

public class AuthorizationInflector implements Inflector<ContainerRequestContext, Void> {

    @Inject
    private javax.inject.Provider<AsyncResponse> responseProvider;
    private final List<String> roles;
    private final String rolePolicy;
    private final HttpSlessInflector delegate;

    protected AuthorizationInflector(List<String> roles,
				     String rolePolicy,
				     HttpSlessInflector methodHandler){
	this.roles = roles;
	this.rolePolicy = rolePolicy;
	this.delegate = methodHandler;
    }

    @Override
    public Void apply(ContainerRequestContext context) {
	applyAuthorization(context);
	return this.delegate.apply(context, responseProvider.get ());
    }

    private void applyAuthorization(ContainerRequestContext requestContext) {
	if (roles.size() > 0 && !isAuthenticated(requestContext)) {
	    throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED());
	}
	int ok = 0;
	for (final String role : roles) {
	    if (requestContext.getSecurityContext().isUserInRole(role)) {
		if (rolePolicy == HttpSlessRuntime.ROLE_POLICY_ANY)
		    return;
		ok++;
	    }
	}
	if (ok == roles.size ()) return; // ROLE_POLICY_ALL
	throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED());
    }

    private static boolean isAuthenticated(final ContainerRequestContext requestContext) {
	return requestContext.getSecurityContext().getUserPrincipal() != null;
    }
}
