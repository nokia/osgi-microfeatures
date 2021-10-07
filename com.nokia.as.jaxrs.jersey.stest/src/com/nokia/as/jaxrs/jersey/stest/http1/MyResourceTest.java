package com.nokia.as.jaxrs.jersey.stest.http1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nokia.as.jaxrs.jersey.stest.MyResource;
import com.nokia.as.jaxrs.jersey.stest.User;
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

	// Make sure the resource is registered
	@ServiceDependency
	MyResource _myResource;

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
	public void should_inject_dependencies() throws Exception {
		_log.warn("should_inject_dependencies");

		String url = "http://localhost:8080/services/info";
		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			try {
				response = doRequest(url).request().get();
				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					String body = response.readEntity(String.class);
					assertEquals(body, "info");
					return;
				}
				_log.warn("should_inject_dependencies_retry");
			} catch (Exception e) {
				// connect exception
				_log.warn("should_inject_dependencies_retry", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + response.getStatus());
	}

	@Test
	public void should_consume_and_produce_json() throws Exception {
		_log.warn("should_consume_and_produce_json");

		String url = "http://localhost:8080/services/json";
		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			Builder b = jsonTargetRequest(url);

			try {
				response = b.buildPost(Entity.json("{\"name\":\"toto\"}")).submit(Response.class).get();
				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					User user = response.readEntity(User.class);
					assertEquals(user.getName(), "toto");
					return;
				}
				_log.warn("should_consume_and_produce_json_retry");
			} catch (Exception e) {
				_log.warn("should_consume_and_produce_json_retry", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + (response == null ? "null response"  : response.getStatus()));
	}

	@Test
	public void should_validate_beans_invalid_bean() throws Exception {
		_log.warn("should_validate_beans_invalid_bean");

		String url = "http://localhost:8080/services/validation";

		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			Builder b = jsonTargetRequest(url);

			try {
				response = b.buildPost(Entity.json("{\"name\":null}")).submit(Response.class).get();
				if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
					return;
				}
				_log.warn("should_validate_beans_invalid_bean_retry");
			} catch (Exception e) {
				_log.warn("should_validate_beans_invalid_bean_retry", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + (response == null ? "null response"  : response.getStatus()));
	}

	@Test
	public void should_validate_beans_valid_bean() throws Exception {
		_log.warn("should_validate_beans_invalid_bean");

		String url = "http://localhost:8080/services/validation";

		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			Builder b = jsonTargetRequest(url);

			try {
				response = b.buildPost(Entity.json("{\"name\":\"toto\"}")).submit(Response.class).get();
				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					return;
				}
				_log.warn("should_validate_beans_valid_bean");
			} catch (Exception e) {
				_log.warn("should_validate_beans_valid_bean", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + (response == null ? "null response"  : response.getStatus()));
	}

	@Test
	public void should_receive_json() throws Exception {
		_log.warn("should_receive_json");

		String url = "http://localhost:8080/services/receiveJson";
		Response response = null;
		for (int i = 0; i < RETRY; i++) {
			try {
				response = doRequest(url).request().get();

				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					String json = response.readEntity(String.class);
					assertTrue(json.startsWith("{\n  \"swagger\" : \"2.0\","));
					assertTrue(json.endsWith(
							"    \"Providers\" : {\n" + "      \"type\" : \"object\"\n" + "    }\n" + "  }\n" + "}"));

					return;
				}
				_log.warn("should_receive_json_retry ");
			} catch (Exception e) {
				_log.warn("should_receive_json_retry", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + (response == null ? "null response"  : response.getStatus()));
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

	private Builder jsonTargetRequest(String url) {
		return doRequest(url).request(MediaType.APPLICATION_JSON);
	}

	@Test
	public void should_support_http2() throws Exception {
		_log.warn("should_support_http2");

		String url = "http://localhost:8080/services/info";


		okhttp3.Response response = null;
		for (int i = 0; i < RETRY; i++) {
			OkHttpClient client = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE)).build();
			Request request = new Request.Builder().url(url).build();
			try {
				response = client.newCall(request).execute();
				if (response != null) {
					assertEquals("H2_PRIOR_KNOWLEDGE", response.protocol().name());
					assertEquals("info",
							new String(response.body().bytes(), java.nio.charset.Charset.forName("utf-8")));
					return;
				}
				_log.warn("should_support_http2_retry");
			} catch (Exception e) {
				// connect exception
				_log.warn("should_support_http2_retry", e);
			}
			Thread.sleep(100);
		}
		fail("should_support_http2 : test failed");
	}
	@Test
	public void should_support_asyncresponse() throws Exception {
		_log.warn("should_support_asyncresponse");

		String url = "http://localhost:8080/services/async";
		Response response = null;
		for (int i = 0; i < 1; i++) {
			try {
				response = doRequest(url).request().get();
				if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
					return; // success
				}
				_log.warn("should_support_asyncresponse_retry");
			} catch (Exception e) {
				// connect exception
				_log.warn("should_support_asyncresponse_retry", e);
			}
			Thread.sleep(100);
		}
		fail("Status code: " + (response == null ? "null response"  : response.getStatus()));
	}
	
	@Test
	public void checkChunk() throws Exception {
		_log.warn("checkChunk");
		String url = "http://localhost:8080/services/chunk";
		URL request_url = new URL(url);
		HttpURLConnection http_conn = (HttpURLConnection)request_url.openConnection();
		http_conn.setChunkedStreamingMode(10);
		http_conn.setConnectTimeout(100000);
		http_conn.setReadTimeout(100000);
		http_conn.setRequestMethod("POST");
		http_conn.setRequestProperty("Transfer-Encoding", "chunked");
		http_conn.setRequestProperty("Content-Type", "text/plain");
		http_conn.setInstanceFollowRedirects(true);
		http_conn.setDoOutput(true);
		PrintWriter out = new PrintWriter(http_conn.getOutputStream());
		for (int i = 0;i < 10; i ++) {
			out.println("XXXXXXXXXX" + i);
			out.flush();
		}
		out.close();
		out = null;
		
		_log.warn("Response : " + String.valueOf(http_conn.getResponseCode()));
	}
}
