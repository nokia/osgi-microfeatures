// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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