// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.http.swagger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.Operation;

@Component(provides=Object.class)
@Path("/openapi.{type:json|yaml}")
public class OpenApiResourceComponent extends OpenApiResource {
	
	private final static Logger _log = Logger.getLogger(OpenApiResourceComponent.class);
	
    @Context
    Application app;

	private SwaggerConfig _cnf;

	@ConfigurationDependency
    void updated(SwaggerConfig cnf) {
		_cnf = cnf;
    }
    
    @Start
    void start() {
    	String swaggerConfig = _cnf.getSwaggerConfig();
		if (swaggerConfig != null) {
    		_log.info("starting swagger with configuraiton file " + swaggerConfig);  	
    		super.setConfigLocation(swaggerConfig);
    	}
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {                                                              
        return getOpenApi(headers, null, app, uriInfo, type);
    }

}