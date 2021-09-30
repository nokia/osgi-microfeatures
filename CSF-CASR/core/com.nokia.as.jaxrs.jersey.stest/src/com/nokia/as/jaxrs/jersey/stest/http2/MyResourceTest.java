package com.nokia.as.jaxrs.jersey.stest.http2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nokia.as.jaxrs.jersey.stest.MyResource;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class MyResourceTest {
	private static final int RETRY = 100;
	Logger _log = Logger.getLogger(MyResourceTest.class);
	Client client = ClientBuilder.newClient();

	@ServiceDependency
	MyResource _myResource;

	volatile static boolean _doWait = true;
	private String _url = "http://localhost:8080/services/info";

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
	public void should_support_http2_only() throws Exception {
		_log.warn("should_support_http2");

		OkHttpClient client_http2 = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE))
				.build();
		okhttp3.Response response;
		for (int i = 0; i < RETRY; i++) {
			Request request = new Request.Builder().url(_url).build();
			try {
				response = client_http2.newCall(request).execute();
				if (response != null) {
					assertEquals("H2_PRIOR_KNOWLEDGE", response.protocol().name());
					assertEquals("info",
							new String(response.body().bytes(), java.nio.charset.Charset.forName("utf-8")));
					return;
				}
				_log.warn("should_support_http2_only");
			} catch (Exception e) {
				// connect exception
				_log.warn("should_support_http2_only", e);
			}
			Thread.sleep(100);
		}
		fail("should_support_http2_only : test failed");
	}

	@Test(expected = IOException.class)
	public void should_not_support_http1() throws Exception {
		_log.warn("should_not_support_http1");

		OkHttpClient client_http1 = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();
		client_http1.newCall(new Request.Builder().url(_url).build()).execute();
	}

}
