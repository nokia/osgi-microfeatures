/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.service.internal.TestService;
import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.internal.Tasks;
import com.nsn.ood.cls.rest.resource.CLSApplication;
import com.nsn.ood.cls.util.log.Loggable;
import com.nsn.ood.cls.util.log.Loggable.Level;

import io.swagger.v3.oas.annotations.Operation;


/**
 * @author marynows
 *
 */
@Component(provides = TestResource.class)
@Path(CLSApplication.INTERNAL + "/test")
@Produces(CLSMediaType.APPLICATION_CLS_JSON)
@Loggable(value = Level.WARNING, duration = true)
public class TestResource {
	public static final String UPLOAD_URI = CLSApplication.INTERNAL + "/test/upload";

	@ServiceDependency
	private TestService testService;

	@GET
	@Path("upload")
	@Produces(MediaType.TEXT_HTML)
	@Operation(hidden = true)
	public String upload() {
		final InputStream is = TestResource.class.getResourceAsStream("/test/upload.html");
		try {
			return IOUtils.toString(is);
		} catch (final IOException e) {
			return e.getMessage();
		}
	}

	@PUT
	@Path("reloadTasks")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(hidden = true)
	public Response reloadTasks(final Tasks tasks) {
		this.testService.reloadTasks(tasks);
		return Response.noContent().build();
	}

	@PUT
	@Path("reloadTargetId")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(hidden = true)
	public Response reloadTargetId() {
		final String targetId = this.testService.reloadTargetId();
		return Response.ok().entity(targetId).build();
	}

	@PUT
	@Path("runTask")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(hidden = true)
	public Response runTask(final String taskName) {
		this.testService.runTask(taskName);
		return Response.noContent().build();
	}

	@PUT
	@Path("setAPIVersion")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(hidden = true)
	public Response setAPIVersion(final API_VERSION apiVersion) {
		this.testService.setApiVersion(apiVersion);
		return Response.noContent().build();
	}

}
