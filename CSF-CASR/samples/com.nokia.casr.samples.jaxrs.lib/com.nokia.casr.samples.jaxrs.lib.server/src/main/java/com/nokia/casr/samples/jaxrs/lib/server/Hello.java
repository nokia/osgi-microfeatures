package com.nokia.casr.samples.jaxrs.lib.server;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.io.*;

/**
 * Our simple SpellChecker web service.
 */
@Path("hello")
public class Hello {

    static volatile String RESP;

    @GET
    @Produces("text/plain")
    public String hello(@QueryParam("size") int size) {
	if (RESP == null) {
	    RESP = generateResp(size);
	}

	return RESP;
    }

    private String generateResp(int size) {
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < size; i ++) {
	    sb.append('X');
	}
	return sb.toString();
    }

}
