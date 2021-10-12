// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.thirdparty.prometheus.jmxexporter.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nokia.as.thirdparty.prometheus.jmxexporter.Activator;

@Component(provides = MetricsJaxRsBean.class)
@Path("jmxmetrics")
public class MetricsJaxRsBean {
	
	@ServiceDependency(required = true)
	private Activator activator;
	
	@GET
	@Produces("text/plain")
	public String serveMetrics() {
		return activator.getMetrics();
	}
}
