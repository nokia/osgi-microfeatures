package com.nokia.as.jaxrs.jersey.sless.stest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nokia.as.k8s.sless.Function;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class MyResourceTest {
	private static final int RETRY = 100;
	Logger _log = Logger.getLogger(MyResourceTest.class);
	Client client = ClientBuilder.newClient();

	// Make sure the resource is registered
	@ServiceDependency
	Function _myFunction;
	
	volatile static boolean _doWait = true;

	@Before
	public void before() {
		if (_doWait) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			_doWait = false;
		}
	}

	@Test
	public void secure_route_answer_403() throws Exception {
		String url = "http://localhost:8665/services/secure";
		_log.warn("secure_route_answer_403");
		int status = 0;
		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			try {
				response = doRequest(url).request().get();
				status = response.getStatus();
				if (status == Response.Status.FORBIDDEN.getStatusCode()) {
					assertEquals(status, 403);
					return;
				}
				_log.warn("secure_route_answer_403: current status = " + status);
			} catch (Exception e) {
				// connect exception
				_log.warn("secure_route_answer_403: exception=" + e.toString());
			}
			Thread.sleep(100);
		}
		fail("Status code: " + status);
	}
	
	@Test
	public void unsecure_route_answer_200() throws Exception {
		String url = "http://localhost:8665/services/unsecure";
		_log.warn("unsecure_route_answer_200");
		int status = 0;
		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			try {
				response = doRequest(url).request().get();
				status = response.getStatus();
				if (status == Response.Status.OK.getStatusCode()) {
					assertEquals(status, 200);
					return;
				}
				_log.warn("unsecure_route_answer_200: current status = " + status);
			} catch (Exception e) {
				// connect exception
				_log.warn("unsecure_route_answer_200: exception=" + e.toString());
			}
			Thread.sleep(100);
		}
		fail("Status code: " + status);
	}
	
	public WebTarget doRequest(String url) {
        ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JerseyClient.class.getClassLoader());
        	return client.target(url);
        } catch (Throwable e) {
    		_log.error("client request failed", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentThread);
        }
		return null;
	}
}
