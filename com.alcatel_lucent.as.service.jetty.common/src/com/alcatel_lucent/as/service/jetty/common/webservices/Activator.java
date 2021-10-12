// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.common.webservices;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.apache.felix.dm.annotation.api.*;
import org.apache.log4j.Logger;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.osgi.framework.BundleContext;

/**
 * Register the jackson json body provider so it will be possible to convert jax rs web services to json.
 */
@Component
public class Activator {

        @Inject
	BundleContext _bc;

	@Start
	void start() {
		_bc.registerService(JacksonJsonProvider.class.getName(), new JacksonJsonProvider(), null);
	}

}
