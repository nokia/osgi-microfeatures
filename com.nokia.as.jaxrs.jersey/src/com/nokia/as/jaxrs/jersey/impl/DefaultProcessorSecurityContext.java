// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.impl;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.osgi.framework.BundleContext;

import com.nokia.as.jaxrs.jersey.ServersConf;

import alcatel.tess.hometop.gateways.utils.HashMap;

@Component
public class DefaultProcessorSecurityContext implements SecurityContext {

	private ServersConf _serversConf;

	@Inject
	BundleContext _bc;

	@ConfigurationDependency
	void updated(ServersConf serversConf) {
		_serversConf = serversConf;
		if (_serversConf.getKeycloakJson() != null) {
			HashMap properties = new HashMap();
			properties.put("keycloak.json", _serversConf.getKeycloakJson());
			_bc.registerService(Object.class, this, properties);
		}
	}

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