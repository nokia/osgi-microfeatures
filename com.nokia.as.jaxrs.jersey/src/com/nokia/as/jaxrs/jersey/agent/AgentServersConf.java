// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.agent;

import java.util.Map;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

/**
 * Allows configuration of multiple JAX-RS Agent Servers
 *
 */
@Config(section = "jax-rs server", name = "com.nokia.as.jaxrs.jersey.AgentServersConf")
public interface AgentServersConf {

	@StringProperty(help = "root path for all jax-rs servers", title = "global alias", defval = "/services")
	String getGlobalAlias();

	@FileDataProperty(title = "Servers Configuration", dynamic = true, required = true, fileData = "agentServersConf.txt", help = "Configure JAX-RS Servers")
	String getServersConf();
	
	@StringProperty(help = "Indicates the threadpool type to be used when invoking rest resources. Valid values are \"cpu\" or \"io\"", title = "threadpool type", defval = "io")
	String getExecutorType();	

}
