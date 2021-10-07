package com.nokia.as.thirdparty.resteasy.stest;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.*;
import org.apache.felix.dm.annotation.api.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class Client {
    
    @Path("/hello")
    public interface TestRequest {
        @GET
        @Produces("text/plain")
        @Path("/getsync")
        public String getsync(@QueryParam("size") int size);
    }


    volatile static boolean _doWait = true;

    @Before
    public void before() {
	if (_doWait) {
	    try {
		Thread.sleep(3000);
	    } catch (InterruptedException e) {
	    }
	    _doWait = false;
	}
    }

    @Test
    public void testRestEasyClient() {
        Thread.currentThread().setContextClassLoader(ResteasyClient.class.getClassLoader());
        String path = "http://localhost:8080/services";
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(path));
        TestRequest proxy = target.proxy(TestRequest.class);
        String resp = proxy.getsync(10);
        System.out.println("Response: " + resp);
        client.close();
        Thread.currentThread().setContextClassLoader(null);
	assertEquals("XXXXXXXXXX", resp);
    }
    
}
