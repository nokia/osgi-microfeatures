// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.gogo.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.log4j.Logger;
 
@Component(provides = GogoRestBridge.class)
@Path("gogo")
public class GogoRestBridge {

    final static Logger LOG = Logger.getLogger(GogoRestBridge.class.getName());

	@ServiceDependency
	private CommandProcessor cp;
	
	@POST
	@Produces("text/plain")
    @Consumes("application/x-www-form-urlencoded")
    @Path("/execCommand")
    public byte[] execCommand(@FormParam("command") String command) {
		if(command == null || command.isEmpty()) {
			return "error".getBytes();		
		}
		ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
		PrintStream psStdOut = new PrintStream(stdOut);
		ByteArrayInputStream stdIn = new ByteArrayInputStream(command.getBytes());
		
		CommandSession cs = cp.createSession(stdIn, psStdOut, psStdOut);
		byte[] result;
		try {
			Object commandResult = cs.execute(command);
			if (commandResult != null) {
				CharSequence formattedCommandResult = cs.format(commandResult, Converter.INSPECT);
				psStdOut.println(formattedCommandResult);
			}
		} catch (Exception e) {
			result = e.getMessage().getBytes();
			cs.close();
			return result;
		}
		result = stdOut.toString().getBytes();
		cs.close();
		return result;
    }
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/execCommand")
    public GogoResponse execCommand(GogoRequest command) {
		if(command.getCommand() == null) {
			return null;
		}
		ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
		PrintStream psStdOut = new PrintStream(stdOut);
		ByteArrayInputStream stdIn = new ByteArrayInputStream(command.getCommand().getBytes());
		
		CommandSession cs = cp.createSession(stdIn, psStdOut, psStdOut);
		String result;
		try {
			cs.execute(command.getCommand());
		} catch (Exception e) {
			result = e.getMessage();
			cs.close();
			return new GogoResponse("", result);
		}
		result = stdOut.toString();
		cs.close();
		return new GogoResponse(result, "");
    }
	
	@GET
	@Produces(MediaType.TEXT_HTML)
    public InputStream browser() {
		return GogoRestBridge.class.getResourceAsStream("/META-INF/gogo-browser.htm");
	}
	
	@GET
	@Path("res/{path : .*}")
	public InputStream resources(@PathParam("path") String path) {
		return GogoRestBridge.class.getResourceAsStream("/META-INF/" + path);
	}
}