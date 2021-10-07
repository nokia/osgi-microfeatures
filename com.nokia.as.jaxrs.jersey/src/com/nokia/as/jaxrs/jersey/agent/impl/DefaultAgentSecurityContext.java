package com.nokia.as.jaxrs.jersey.agent.impl;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class DefaultAgentSecurityContext implements SecurityContext {

	@Override
	public String getAuthenticationScheme() {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return false;
	}

}