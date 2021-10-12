// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;

@Component(provides = Filter.class)
@Property(name = "pattern", value = "/.*")
public class KeycloakFilter extends KeycloakOIDCFilter {
	private String _keycloakJson;
	private KeycloakFilterConf _keycloakFilterConf;
	private final static Logger _log = Logger.getLogger("com.nokia.as.keycloak");

	@ConfigurationDependency
	void updated(KeycloakFilterConf keycloakFilterConf) {
		_log.info("KeycloakFilterConf : " + keycloakFilterConf);
		_keycloakFilterConf = keycloakFilterConf;
	}

	@Start
	public void setService() throws Exception {
		_keycloakJson = _keycloakFilterConf.getKeycloakJson();
	}

	private boolean confNotProvided() {
		return _keycloakJson == null || _keycloakJson.isEmpty();
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		InputStream is = loadKeycloakConfigFile();
		deploymentContext = new AdapterDeploymentContext(createKeycloakDeploymentFrom(is));
		_log.info("Keycloak is using a per-deployment configuration.");
		filterConfig.getServletContext().setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
		nodesRegistrationManagement = new NodesRegistrationManagement();
	}

	private KeycloakDeployment createKeycloakDeploymentFrom(InputStream is) {
		if (is == null || confNotProvided()) {
			_log.info("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
			return new KeycloakDeployment();
		}
		return KeycloakDeploymentBuilder.build(is);
	}

	protected InputStream loadKeycloakConfigFile() {
		return new ByteArrayInputStream(_keycloakJson.getBytes(StandardCharsets.UTF_8));
	}

}
