// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.loghistory.jaxrs;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.nokia.as.service.loghistory.LogBufferService;

@Component(provides = LoggerHttp.class)
@Path("logs")
public class LoggerHttp {


	@ServiceDependency(required=true)
	LogBufferService logBuffer;
	
	@Start
	void start() {
	}
	
	@GET
	@Produces("text/plain")
	public String getLogs(@DefaultValue("10") @QueryParam("nb") int nb,
						@QueryParam("filter") String filter) {
		return logBuffer.getLogs(nb, filter).toString();
	}
	
}