package com.alcatel_lucent.as.agent.web.itest;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel_lucent.as.agent.web.itest.servlets.DumpRequestBodyServlet;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

/**
 * Open many clients to jetty. Each client sends a Post http request using chunks body parts.
 * Each parts are sent seperately, in different writes.
 */
@RunWith(MockitoJUnitRunner.class)
public class PostChunkServletTest extends IntegrationTestBase {
	
	static Logger _logger = Logger.getLogger(PostChunkServletTest.class);
	final static int CLIENTS = 10;	
	final static int REQUESTS_PER_CLIENT = 10;
	final static int CHUNK_SIZE = 100;
	final static CountDownLatch _testLatch = new CountDownLatch(CLIENTS);
	byte[] _body = IntStream.range(0, 1024).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString().getBytes();							
	
	@Test
	public void testPOSTWithChunks() throws InterruptedException {
		Utils.initLoggers();
		component(comp -> comp.impl(new DumpRequestBodyServlet()).provides(Servlet.class, "alias", "/post/chunk"));
		Utils.request("http://127.0.0.1:8080/post/chunk", "POST", "foo", "bar"); // just ensure the servlet is loaded before starting real test.
		_logger.warn("Starting clients using post/chunk requests ...");
		Thread[] threads = new Thread[CLIENTS];
		IntStream.range(0, CLIENTS).forEach(i -> threads[i] = new Thread(new Client(String.valueOf(i))));
		Stream.of(threads).forEach(t -> t.start());
		assertEquals("test timeout", true, _testLatch.await(30000, TimeUnit.MILLISECONDS));
	}
	
	class Client implements Runnable {
		private final String _id;

		Client(String id) {
			_id = id;
		}

		@Override
		public void run() {
			try {
				_logger.warn("client #" + _id + ": starting");
				for (int i = 0; i < REQUESTS_PER_CLIENT; i++) {
					_logger.debug("client #" + _id + ": sending request #" + i);
					URL url = new URL("http://127.0.0.1:8080/post/chunk");
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod("POST");
					con.setRequestProperty("Content-Type", "text/plain");
					// con.setRequestProperty("Content-Encoding", "gzip");
					// con.setRequestProperty("Connection", "close");
					con.setDoOutput(true);
					con.setChunkedStreamingMode(CHUNK_SIZE);

					// send
					boolean suspend = true;
					try (OutputStream os = con.getOutputStream()) {
						ByteArrayInputStream in = new ByteArrayInputStream(_body);
						byte[] buf = new byte[CHUNK_SIZE];
						int size;
						while ((size = in.read(buf, 0, buf.length)) != -1) {
							_logger.debug("sending chunk: size=" + size);
							os.write(buf, 0, size);
							Thread.sleep(ThreadLocalRandom.current().nextInt(10));
						}
						os.flush();
					}

					// receive

					try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
						StringBuilder response = new StringBuilder();
						String responseLine = null;
						while ((responseLine = br.readLine()) != null) {
							response.append(responseLine.trim());
						}
						_logger.debug("response: " + response);
					}
					
					_logger.debug("client #" + _id + ": received response #" + i);
					// con.disconnect();
				}
				_logger.warn("client #" + _id + " done.");
				_testLatch.countDown();
			} catch (Exception e) {
				_logger.warn("exception", e);
			}
		}
	}

}