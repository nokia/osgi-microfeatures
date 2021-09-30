package com.nokia.casr.samples.jaxrs.lib.server;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.io.*;
import org.apache.log4j.*;

@Path("valid")
public class MyResource {
        final static Logger _log = Logger.getLogger(MyResource.class);

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public User getValidUser(@Valid User u) {
	    _log.warn("getValidUser:" + u);
	    return u;
	}

}
