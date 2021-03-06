// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.metering.prometheus.pull.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.as.metering.prometheus.pull.AbstractPullMetersTracker;

@Component(provides = RestPullMetersTracker.class)
@Path("metrics")
public class RestPullMetersTracker extends AbstractPullMetersTracker {
	
	@GET
	@Produces("text/plain")
	public String getMetrics() {
		return super.getMetrics();
	}
}