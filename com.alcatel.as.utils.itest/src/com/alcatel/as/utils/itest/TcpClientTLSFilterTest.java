// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.utils.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpServerOption;
import alcatel.tess.hometop.gateways.reactor.spi.ChannelListenerFactory;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

@RunWith(MockitoJUnitRunner.class)
public class TcpClientTLSFilterTest extends IntegrationTestBase {
	private static final Ensure ensureClient = new Ensure();
	private static final Ensure ensureServer = new Ensure();

	@Before
	public void before() {

		// Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);

		component(comp -> comp.impl(TcpServer.class)
				.withSvc(LogServiceFactory.class, true)
				.withSvc(ReactorProvider.class, "(type=nio)", true));

		ensureServer.waitForStep(1);
		component(comp -> comp.impl(TcpClient.class)
				.withSvc(LogServiceFactory.class, true)
				.withSvc(ReactorProvider.class, "(type=nio)", true)
				.withSvc(ChannelListenerFactory.class, "(type=tls)", true));
		ensureClient.waitForStep(1);
	}

	@Test
	public void testTcpClientTLSFilter() {
		try {
			ensureClient.waitForStep(4, 1000);
			ensureServer.waitForStep(4, 1000);
			ensureClient.ensure();
			ensureServer.ensure();
		} catch (Throwable e) {
			e.printStackTrace();
			fail();
		}
	}

	public static class TcpClient implements TcpClientChannelListener {

		private volatile ChannelListenerFactory _filterProvider;
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService _log;
		private final String _expected = "Goodbye from the other side";
		private TcpChannel _channel;
		private Reactor _reactor;

		public void start() throws Exception {
			_log = logFactory.getLogger(TcpClient.class);
			_reactor = provider.create("tlscodec-reactorclient");
			_reactor.start();

			InetSocketAddress local = new InetSocketAddress("127.0.0.1", 5005);
			InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 5004);

			Map<TcpClientOption, Object> opts = new HashMap<ReactorProvider.TcpClientOption, Object>();
			opts.put(TcpClientOption.FROM_ADDR, local);
			
			TcpClientChannelListener tcpClientTLSListener = createTcpClientTLSFilter(provider);
			provider.tcpConnect(_reactor, remote, tcpClientTLSListener, opts);
			_log.warn("Creating TCP client with params:");
			ensureClient.step(1);
		}
		
		public void stop() {
			_log.warn("stop");
			_reactor.stop();
		}

		private TcpClientChannelListener createTcpClientTLSFilter(ReactorProvider provider) throws FileNotFoundException, Exception {
			String NEEDED_CIPHERS[] = { ".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA" };

			final Security security = new Security()
					.addProtocol("TLSv1.1", "TLSv1.2")
					.addCipher(NEEDED_CIPHERS)
					.keyStore(new FileInputStream("tls/client.ks"))
					.keyStorePassword("password")
					.delayed()
					.build();
			
			Map<String, Object> params = new HashMap<>();
			params.put("security", security);
			return _filterProvider.createListener(TcpClient.this, params);
		}

		public void connectionEstablished(TcpChannel cnx) {
			_log.warn("Connection established");
			_channel = cnx;
			String message = "Hello from the other side";
			_log.warn("Sending to server: " + message);
			
			_channel.upgradeToSecure();
			_channel.send(message.getBytes(), false);
			ensureClient.step(2);
		}

		public int messageReceived(TcpChannel cnx, ByteBuffer buf) {
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			_log.warn("Received from server: " + new String(b));
			assertEquals(_expected, new String(b));
			try {
				ensureClient.step(3);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
			
			_channel.close();
			return 0;
		}

		public void receiveTimeout(TcpChannel cnx) {
		}

		public void connectionClosed(TcpChannel cnx) {
			try {
				ensureClient.step(4);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		}

		public void writeBlocked(TcpChannel cnx) {
		}

		public void writeUnblocked(TcpChannel cnx) {
		}

		public void connectionFailed(TcpChannel cnx, Throwable error) {
		}		
	}

	public static class TcpServer implements TcpServerChannelListener {

		private volatile ReactorProvider _provider;
		private volatile LogServiceFactory _logFactory;
		private LogService _log;
		private Reactor _reactor;
		private final String expected = "Hello from the other side";

		public void start() throws Exception {
			_log = _logFactory.getLogger(TcpServer.class);
			_reactor = _provider.create("tlscodec-reactorserver");
			_reactor.start();

			InetSocketAddress local = new InetSocketAddress("127.0.0.1", 5004);

			String NEEDED_CIPHERS[] = { ".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA" };

			final Security security = new Security().addProtocol("TLSv1.1", "TLSv1.2").addCipher(NEEDED_CIPHERS)
					.keyStore(new FileInputStream("tls/server.ks")).keyStorePassword("password").build();

			Map<TcpServerOption, Object> opts = new HashMap<ReactorProvider.TcpServerOption, Object>();
			opts.put(TcpServerOption.SECURITY, security);
			
			try {
				_provider.tcpAccept(_reactor, local, TcpServer.this, opts);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
			_log.warn("Creating TCP server with params:");
			_log.warn("Local address: " + local);
			_log.warn("Step 1");
			ensureServer.step(1);
		}

		public void stop() {
			_log.warn("server stop");
			_reactor.stop();
		}

		public void connectionAccepted(TcpServerChannel ssc, TcpChannel client) {
			_log.warn("Accepted connection from" + client.getLocalAddress());
			_log.warn("Step 2");
			ensureServer.step(2);
		}

		public int messageReceived(TcpChannel cnx, ByteBuffer buf) {

			byte[] b = new byte[buf.remaining()];
			buf.get(b);

			assertEquals(expected, new String(b));
			_log.warn("Received from client: " + new String(b));

			String message = "Goodbye from the other side";
			_log.warn("Sending to client: " + message);
			cnx.send(message.getBytes(), false);
			ensureServer.step(3);
			return 0;
		}

		public void receiveTimeout(TcpChannel cnx) {
		}

		public void connectionClosed(TcpChannel cnx) {
			_log.warn("Connection closed");
			ensureServer.step(4);
		}

		public void writeBlocked(TcpChannel cnx) {
		}

		public void writeUnblocked(TcpChannel cnx) {
		}

		@Override
		public void serverConnectionOpened(TcpServerChannel server) {
		}

		@Override
		public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
		}

		@Override
		public void serverConnectionClosed(TcpServerChannel server) {
		}

		@Override
		public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
		}
	}
	
}
