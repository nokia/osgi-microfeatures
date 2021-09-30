package com.nokia.as.agent.web.stest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;
import com.alcatel.as.service.coordinator.Coordination;

/**
 * Sends an http request to the "MyProxylet" through the http io handler.
 */
@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class MyHttpServletTest {
	@ServiceDependency
	LogServiceFactory _logFactory;

	@ServiceDependency(filter = "(name=ACTIVATION)")
	Coordination _ready; // injected

	LogService _log;

	@Before
	public void before() {
		_log = _logFactory.getLogger(MyHttpServletTest.class);
	}

	@Test
	public void testGET() throws Exception {
		_log.warn("testGET: %s", this);
		Throwable lastErr = null;

		for (int i = 0; i < 50; i++) {
			try {
				String result = download("http://127.0.0.1:8080/test");
				boolean ok = result.indexOf("HttpServiceServlet sessionId") != -1;
				_log.warn("got response: " + result);
				assertTrue(ok);
				return;
			} catch (Throwable t) {
				lastErr = t;
				Thread.sleep(100);
			}
		}

		throw new RuntimeException("test failed", lastErr);
	}

	@After
	public void after() {
	}

	private String download(String url) {
		try (Scanner in = new Scanner(new URL(url).openStream())) {
			StringBuilder builder = new StringBuilder();
			while (in.hasNextLine()) {
				builder.append(in.nextLine());
				builder.append("\n");
			}
			return builder.toString();
		} catch (IOException ex) {
			RuntimeException rex = new RuntimeException();
			rex.initCause(ex);
			throw rex;
		}
	}
}
