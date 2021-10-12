// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.utils.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpServerOption;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;

/**
 * This test validates the issue from CSFS-31337. The use case tested is: - an
 * sctp server accepts an sctp association - sctp server closes the sctp server
 * endpoint (client association remained opened) - the sctp server listen again
 * on the sctp server port.
 * 
 * The expected behavior is that the above scenario works only when using sctp
 * REUSE ADDR socket option.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CSFS31337_SctpReuseAddrTest extends IntegrationTestBase {

	private static final Ensure ensureClient = new Ensure();
	private static final Ensure ensureServer = new Ensure();
	
	private SctpServer server;

	@Before
	public void before() {

		// Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);

		server = new SctpServer();
		component(comp -> comp.impl(server).withSvc(ReactorProvider.class, "(type=nio)", true)
				.withSvc(LogServiceFactory.class, true));
		ensureServer.waitForStep(1);
		component(comp -> comp.impl(SctpClient.class).withSvc(ReactorProvider.class, "(type=nio)", true)
				.withSvc(LogServiceFactory.class, true));
		ensureClient.waitForStep(1);
	}

	@Test
	public void testSctpSocketReuseAddr() {
		try {
			ensureClient.waitForStep(3);
			ensureServer.waitForStep(3);
			server.stopListening();
			ensureServer.waitForStep(4);
			server.accept();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public static class SctpClient implements SctpClientChannelListener {

		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;
		private Reactor reactor;

		private final String expected = "Goodbye from the other side";

		public void start() {
			log = logFactory.getLogger(SctpClient.class);
			reactor = provider.create("SctpOptionsTest-reactorClient");
			reactor.start();

			SocketAddress local = new InetSocketAddress("127.0.0.1", 5001);
			SocketAddress remote = new InetSocketAddress("127.0.0.1", 5000);

			Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
			opts.put(SctpClientOption.LOCAL_ADDR, local);
			provider.sctpConnect(reactor, remote, SctpClient.this, opts);
			ensureClient.step(1);
		}
		
		public void stop() {
			reactor.stop();
		}

		public void connectionEstablished(SctpChannel cnx) {
			log.warn("Connection established");
			String message = "Hello from the other side";
			log.warn("Sending to server: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			log.warn("Step 2");
			ensureClient.step(2);
		}

		public void connectionFailed(SctpChannel cnx, Throwable error) {
		}

		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {

			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from server: " + new String(b));
			ensureClient.step(3);
		}

		public void receiveTimeout(SctpChannel cnx) {
		}

		public void connectionClosed(SctpChannel cnx, Throwable err) {
		}

		public void writeBlocked(SctpChannel cnx) {
		}

		public void writeUnblocked(SctpChannel cnx) {
		}

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
		}

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
		}

	}

	public static class SctpServer implements SctpServerChannelListener {
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;
		private final String expected = "Hello from the other side";
		private Reactor reactor;
		private SctpServerChannel sctpServer;

		public void start() {
			log = logFactory.getLogger(SctpServer.class);
			reactor = provider.create("reactor.server");
			reactor.start();
			try {
				accept();
				log.warn("Sctp Server listening");
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
			ensureServer.step(1);
		}

		public void stop() {
			reactor.stop();
		}
		
		public void stopListening() {
			sctpServer.close();
		}

		public void accept() throws IOException {
			SocketAddress local = new InetSocketAddress("127.0.0.1", 5000);
			Map<SctpServerOption, Object> opts = new HashMap<ReactorProvider.SctpServerOption, Object>();
			// sctp server socket is now reusing addr by default.
			
//			Map<SctpSocketOption, SctpSocketParam> sctpOptions = new HashMap<>();
//			sctpOptions.put(SctpSocketOption.SCTP_SO_REUSEADDR, sctp_boolean.TRUE);
//			opts.put(SctpServerOption.SOCKET_OPTIONS, sctpOptions);
			sctpServer = provider.sctpAccept(reactor, local, SctpServer.this, opts);
		}
		
		public void connectionAccepted(SctpServerChannel ssc, SctpChannel client) {
			log.warn("Accepted connection from" + client.getLocalAddress());
			ensureServer.step(2);
		}
		
		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from client: " + new String(b));

			String message = "Goodbye from the other side";
			log.warn("Sending to client: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			ensureServer.step(3);
		}

		public void connectionClosed(SctpChannel cnx, Throwable err) {
		}

		public void receiveTimeout(SctpChannel cnx) {
		}

		public void writeBlocked(SctpChannel cnx) {
		}

		public void writeUnblocked(SctpChannel cnx) {
		}

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
		}

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
		}

		public void serverConnectionClosed(SctpServerChannel ssc, Throwable err) {
		    ensureServer.step(4);
		}
	}

}
