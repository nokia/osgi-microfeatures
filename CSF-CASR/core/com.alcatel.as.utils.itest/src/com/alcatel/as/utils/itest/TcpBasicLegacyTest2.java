package com.alcatel.as.utils.itest;

import static org.junit.Assert.fail;

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
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

/**
 * Tests basic server connection callbacks
 */
@RunWith(MockitoJUnitRunner.class)
public class TcpBasicLegacyTest2 extends IntegrationTestBase {

	private static final Ensure _ensureServer = new Ensure();

	@Before
	public void before() {

		//Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		//ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getLogger("as.service.reactor").setLevel(org.apache.log4j.Level.WARN);
		org.apache.log4j.Logger.getLogger("reactor.Server1").setLevel(org.apache.log4j.Level.WARN);
	}

	@Test
	public void testTcpServer() {
		try {
			Server s = new Server();
			component(comp -> comp.impl(s).withSvc(ReactorProvider.class, "(type=nio)", true));
			_ensureServer.waitForStep(1); // server opened
			
			// create client			
			Client c = new Client();
			component(comp -> comp.impl(c).withSvc(ReactorProvider.class, "(type=nio)", true));
			
			// wait for client/server exchange
			_ensureServer.waitForStep(2);

			// stopServer
			s.stopServer();
			_ensureServer.waitForStep(3); // server closed
		} catch(Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	// test server connection opened callbacks
	public static class Server implements TcpServerChannelListener {
		ReactorProvider _provider;
		Reactor _reactor;
		Logger _log = Logger.getLogger("reactor.server");
		volatile TcpServerChannel _serverChannel;
		
		void start() {
			_reactor = _provider.create("server");
			_reactor.start();
		    InetSocketAddress from = new InetSocketAddress(8888);
		    Map<ReactorProvider.TcpServerOption, Object> o = new HashMap<>();
		    o.put(ReactorProvider.TcpServerOption.ENABLE_READ, false);
			try {
				_serverChannel = _provider.tcpAccept(_reactor, from, this, o);
				_serverChannel.enableReading();
				_ensureServer.step(1);
			} catch (Exception e) {
				_log.warn("could not setup tcp server", e);
			}
		}
		
		void stop() {
			_log.warn("stopping reactor");
			_reactor.stop();
		}
		
		void stopServer() {
			_log.warn("stopping server channel");
			_serverChannel.close();
		}
		
		@Override
		public void receiveTimeout(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			byte[] b = new byte[msg.remaining()];
			msg.get(b);
			String s = new String(b);
			if (s.equals("hello")) {
				_ensureServer.step(2);
			}
			return 0;
		}

		@Override
		public void writeBlocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void writeUnblocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void connectionClosed(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void serverConnectionOpened(TcpServerChannel server) {
		}

		@Override
		public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
			// TODO Auto-generated method stub
		}

		@Override
		public void serverConnectionClosed(TcpServerChannel server) {
			_log.warn("server connection closed");
			_ensureServer.step(3);
		}

		@Override
		public void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
			// TODO Auto-generated method stub
			
		}
	}	
	
	class Client implements TcpClientChannelListener {
		ReactorProvider _provider;
		Reactor _reactor;
		Logger _log = Logger.getLogger("reactor.client");
		volatile TcpChannel _channel;
		
		void start() {
			_reactor = _provider.create("client");
			_reactor.start();
			_provider.newTcpClientChannel(new InetSocketAddress(8888), this, _reactor, null, 0, _log);
		}
		
		void stop() {
			_log.warn("stopping reactor client");
			_reactor.stop();
		}

		@Override
		public void receiveTimeout(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			byte[] b = new byte[msg.remaining()];
			String s = new String(b);
			if ("s".equals("hello")) {
				cnx.send("hi".getBytes());
			}
			return 0;
		}

		@Override
		public void writeBlocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void writeUnblocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void connectionClosed(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void connectionEstablished(TcpChannel cnx) {
			cnx.send("hello".getBytes());
		}

		@Override
		public void connectionFailed(TcpChannel cnx, Throwable error) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
