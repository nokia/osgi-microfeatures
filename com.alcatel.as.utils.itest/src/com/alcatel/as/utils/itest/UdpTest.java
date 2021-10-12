// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.utils.itest;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nokia.as.util.test.osgi.Ensure;
import com.nokia.as.util.test.osgi.IntegrationTestBase;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import org.junit.Assert;

/**
 * Tests basic server connection legacy callbacks
 */
@RunWith(MockitoJUnitRunner.class)
public class UdpTest extends IntegrationTestBase {

	private static final Ensure _ensureServer = new Ensure();
	private static final Ensure _ensureClient = new Ensure();

	@Before
	public void before() {

		// Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		// ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getLogger("as.service.reactor").setLevel(org.apache.log4j.Level.WARN);
		org.apache.log4j.Logger.getLogger("reactor.Server1").setLevel(org.apache.log4j.Level.WARN);
	}

	@Test
	public void testUdpServer() {
		try {
			Server s = new Server();
			component(comp -> comp.impl(s).withSvc(ReactorProvider.class, "(type=nio)", true));
			_ensureServer.waitForStep(1); // server opened

			// create client
			Client c = new Client();
			component(comp -> comp.impl(c).withSvc(ReactorProvider.class, "(type=nio)", true));

			// wait for client/server started
			_ensureServer.waitForStep(1);
			_ensureClient.waitForStep(1);

			// send a message to the server
			c.send();
			
			// check if server has received the message
			_ensureServer.waitForStep(2);
			
			// stop client/Server
			s.stopServer();
			c.stopClient();
			
			_ensureServer.waitForStep(3); // server closed
			_ensureClient.waitForStep(2);; // client closed
			
			// check if client send buffer is empty
			Assert.assertEquals(0, c.getChannel().getSendBufferSize());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	// test server connection opened callbacks
	public static class Server implements UdpChannelListener {
		ReactorProvider _provider;
		Reactor _reactor;
		Logger _log = Logger.getLogger("reactor.server");
		volatile UdpChannel _channel;

		void start() {
			_reactor = _provider.create("server");
			_reactor.start();
			InetSocketAddress local = new InetSocketAddress(9998);
			Map<ReactorProvider.UdpOption, Object> o = new HashMap<>();
			try {
				_channel = _provider.udpBind(_reactor, local, this, o);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_ensureServer.step(1);
		}

		void stop() {
			_log.warn("stopping reactor");
			_reactor.stop();
		}

		void stopServer() {
			_log.warn("stopping server channel");
			_channel.close();
		}

		@Override
		public void connectionOpened(UdpChannel cnx) {
		}

		@Override
		public void connectionFailed(UdpChannel cnx, Throwable err) {
		}

		@Override
		public void messageReceived(UdpChannel cnx, ByteBuffer msg, InetSocketAddress addr) {
			byte[] b = new byte[msg.remaining()];
			msg.get(b);
			String s = new String(b);
			if (s.equals("hello")) {
				_ensureServer.step(2);
			}
		}

		@Override
		public void connectionClosed(UdpChannel cnx) {
			_ensureServer.step(3);
		}

		@Override
		public void receiveTimeout(UdpChannel cnx) {
		}

		@Override
		public void writeBlocked(UdpChannel cnx) {
		}

		@Override
		public void writeUnblocked(UdpChannel cnx) {
		}
	}

	class Client implements UdpChannelListener {
		ReactorProvider _provider;
		Reactor _reactor;
		Logger _log = Logger.getLogger("reactor.client");
		volatile UdpChannel _channel;

		void start() {
			_reactor = _provider.create("client");
			_reactor.start();
			InetSocketAddress local = new InetSocketAddress(9999);
			Map<ReactorProvider.UdpOption, Object> o = new HashMap<>();
			try {
				_channel = _provider.udpBind(_reactor, local, this, o);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_ensureClient.step(1);
		}
		
		void stopClient() {
			_channel.close();
		}
		
		UdpChannel getChannel() {
			return _channel;
		}
		
		void send() {
		    ByteBuffer buf = ByteBuffer.wrap("hello".getBytes());
		    _channel.send(new InetSocketAddress("localhost", 9998), buf, false);
		}

		void stop() {
			_log.warn("stopping reactor");
			_reactor.stop();
		}

		@Override
		public void connectionOpened(UdpChannel cnx) {
			// TODO Auto-generated method stub

		}

		@Override
		public void connectionFailed(UdpChannel cnx, Throwable err) {
			// TODO Auto-generated method stub

		}

		@Override
		public void connectionClosed(UdpChannel cnx) {
			_ensureClient.step(2);
		}

		@Override
		public void messageReceived(UdpChannel cnx, ByteBuffer msg, InetSocketAddress addr) {
			// TODO Auto-generated method stub

		}

		@Override
		public void receiveTimeout(UdpChannel cnx) {
			// TODO Auto-generated method stub

		}

		@Override
		public void writeBlocked(UdpChannel cnx) {
			// TODO Auto-generated method stub

		}

		@Override
		public void writeUnblocked(UdpChannel cnx) {
			// TODO Auto-generated method stub

		}
	}
}
