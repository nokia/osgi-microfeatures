// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.stest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;

@Component(provides = MyResource.class)
@Path("/")
public class MyResource {
	final static Logger _log = Logger.getLogger(MyResource.class);

	@ServiceDependency
	ExampleClassForInjection e;

	@GET
	@Path("info")
	public String getInfoFromDependency() {
		return e.getInfo();
	}

	@POST
	@Path("json")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public User getUser(User u) {
		_log.warn("getUser:" + u);
		return u;
	}

	@POST
	@Path("validation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public User getValidUser(@Valid User u) {
		_log.warn("getValidUser:" + u);
		return u;
	}

	@GET
	@Path("receiveJson")
	public Response swagger() {
		return Response.ok(new File("/tmp/swagger.json"), MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Path("async")
	public void asyncGet(@Suspended final AsyncResponse asyncResponse) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				veryExpensiveOperation();
			}

			private void veryExpensiveOperation() {
				try {
					Thread.sleep(10000);
					System.out.println("hello world");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		asyncResponse.resume(Response.accepted().location(URI.create("uniqueId")).build());
	}
	
	@POST
	@Path("chunk")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String checkChunk(String chunk) throws IOException {
		_log.warn("checkChunk: received message: " + chunk);
		BufferedReader reader = new BufferedReader(new StringReader(chunk));
		String line;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			if (! line.equals("XXXXXXXXXX" + String.valueOf(index))) {
				throw new IllegalArgumentException("received invalid chunk message: " + chunk);
			}
			index ++;
		}
		return "Ok";
	}

}
