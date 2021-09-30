package com.nokia.as.thirdparty.resteasy.stest;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import java.util.*;
import java.io.*;
import org.apache.felix.dm.annotation.api.*;
import java.util.concurrent.*;

/**
 * Our simple SpellChecker web service.
 */
@Component(provides=MyResource.class)
@Path("hello")
public class MyResource {

    @GET
    @Produces("text/plain")
    @Path("/getsync")
    public String getsync(@QueryParam("size") int size) {
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < size; i ++) {
	    sb.append("X");
	}

	return sb.toString();
    }

}
