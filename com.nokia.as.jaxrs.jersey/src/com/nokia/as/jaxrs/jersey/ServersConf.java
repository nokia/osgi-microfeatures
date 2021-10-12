// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

/**
 * Allows configuration of multiple JAX-RS Servers
 *
 */
@Config(section = "jax-rs server")
public interface ServersConf {

	@FileDataProperty(title = "Servers Configuration", dynamic = true, required = true, fileData = "defJaxRsServer.txt", help = "Configure JAX-RS Servers")
	String getServersConf();

	@StringProperty(help = "root path for all jax-rs servers", title = "global alias", defval = "/services")
	String getGlobalAlias();

	@FileDataProperty(title = "Keycloak Configuration file", dynamic = true, required = true, fileData = "keycloak.json", help = "enable keycloak authorization filter")
	String getKeycloakJson();
	
	@StringProperty(help = "Indicates the threadpool type to be used when invoking rest resources. Valid values are \"cpu\" or \"io\"", title = "threadpool type", defval = "io")
	String getExecutorType();
}
