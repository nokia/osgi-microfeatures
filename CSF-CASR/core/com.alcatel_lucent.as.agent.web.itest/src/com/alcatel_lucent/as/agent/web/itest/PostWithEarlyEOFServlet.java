package com.alcatel_lucent.as.agent.web.itest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel_lucent.as.agent.web.itest.servlets.DumpRequestBodyServlet;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

/**
 * Open many clients to jetty. Each client sends a POST http request but socket
 * is closed before the request is fully sent. Then, at last, we send a normal
 * http request and check if it's OK.
 */
@RunWith(MockitoJUnitRunner.class)
public class PostWithEarlyEOFServlet extends IntegrationTestBase {

	final static Logger _logger = Logger.getLogger(PostWithEarlyEOFServlet.class);
	final static int CLIENTS = 10;
	final static int REQUEST_PER_CLIENTS = 10;
	final static CountDownLatch _latch1 = new CountDownLatch(CLIENTS * REQUEST_PER_CLIENTS);
	final static CountDownLatch _latch2 = new CountDownLatch(1);

	@Test
	public void testPOSTWithChunks() throws Exception {
		Utils.initLoggers();
		component(comp -> comp.impl(new DumpRequestBodyServlet()).provides(Servlet.class, "alias",
				"/post/earlyEOF"));
		// Ensure servlet is loaded before starting real test
		Utils.request("http://127.0.01:8080/post/earlyEOF", "POST", "foo", "bar");
		// Fire many clients, each one will send a too short http request, and will then close the socket.
		Thread[] threads = new Thread[CLIENTS];
		IntStream.range(0, CLIENTS).forEach(i -> threads[i] = new Thread(new Client1(String.valueOf(i))));
		Stream.of(threads).forEach(t -> t.start());
		assertEquals("test timeout", true, _latch1.await(30000, TimeUnit.MILLISECONDS));

		// finally, send a normal http request, and ensure we get a 200 OK.
		_logger.warn("expected 200 OK from last http request ...");
		Thread client2 = new Thread(new Client2());
		client2.start();
		if (! _latch2.await(10000, TimeUnit.MILLISECONDS)) {
			client2.interrupt();
			client2.join();
			throw new Exception("Could not get expected 200 OK");
		}
	}

	class Client1 implements Runnable {
		private final String _id;

		Client1(String id) {
			_id = id;
		}

		@Override
		public void run() {
			try {
				_logger.warn("client #" + _id + ": starting");
				for (int i = 0; i < REQUEST_PER_CLIENTS; i++) {
					_logger.debug("client #" + _id + ": sending request #" + i);
					try (Socket socket = new Socket("127.0.0.1", 8080)) {
						InputStream in = socket.getInputStream();
						PrintWriter writer = new PrintWriter(socket.getOutputStream());
						writer.print("POST /post/earlyEOF HTTP/1.1\r\n");
						writer.print("Host: localhost:8081\r\n");
						writer.print("User-Agent: curl/7.54.0\r\n");
						writer.print("Accept: */*\r\n");
						writer.print("Content-Length: 12\r\n"); // too large content length
						writer.print("Content-Type: application/x-www-form-urlencoded\r\n");
						writer.print("\r\n");
						writer.print("bar=123&c=1");
						writer.flush();
						writer.close();
						_latch1.countDown();
					} catch (IOException ioe) {
						_logger.debug("client #" + _id + " got expected IOE");
					}
				}
				_logger.warn("client #" + _id + " done.");
			} catch (Exception e) {
				_logger.warn("exception", e);
			}
		}
	}

	class Client2 implements Runnable {
		@Override
		public void run() {
			try {
				_logger.warn("sending normal http request ...");
				Map<String, String> params = new HashMap<>();
				params.put("number", "42");
				String result = Utils.request("http://127.0.0.1:8080/post/earlyEOF", params, "POST");
				assertEquals("Hello, world!", result.trim());
				_latch2.countDown();
			} catch (Exception e) {
				_logger.warn("Client2 got exception: ", e);
			}
		}
	}

}