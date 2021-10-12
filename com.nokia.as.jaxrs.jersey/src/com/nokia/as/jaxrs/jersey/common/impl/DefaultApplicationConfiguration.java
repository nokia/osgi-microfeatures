// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.common.impl;

import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.server.ServerProperties;

import com.nokia.as.jaxrs.jersey.common.ApplicationConfiguration;

public class DefaultApplicationConfiguration implements ApplicationConfiguration {

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		// don't look for implementations described by META-INF/services/*
		properties.put(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, false);
		// disable auto discovery on server, as it's handled via OSGI
		properties.put(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		return properties;
	}

}
