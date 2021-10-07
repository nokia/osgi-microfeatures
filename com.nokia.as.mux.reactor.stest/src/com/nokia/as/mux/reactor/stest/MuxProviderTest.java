package com.nokia.as.mux.reactor.stest;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.mux.reactor.stest.utils.ByteBufferUtils;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;
import com.nokia.as.util.test.osgi.Ensure;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MuxProviderTest {
	@ServiceDependency
	private LogServiceFactory logFactory;
	
	@ServiceDependency(filter = "(type=mux)")
	private ReactorProvider muxProvider;
	
	@ServiceDependency(filter = "(type=nio)")
	private ReactorProvider reactorProvider;
	
	private LogService log;
	
	
	private Ensure ensure = new Ensure();
	
	TcpServerChannel serverChan;
	TcpChannel serverToClientChan;

	private Reactor clientReactor;
	static final int PORT = 31337;
	
	private TcpServerChannelListener serverListener = new TcpServerChannelListener() {
		
		@Override
		public void receiveTimeout(TcpChannel cnx) {
			log.warn("ReceiveTimeout");
		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			String message =  ByteBufferUtils.toUTF8String(false, msg);
			log.info(cnx + " (server) received " + message);
			if("hello".equals(message)) {
				ensure.inc();
				cnx.send(ByteBufferUtils.getUTF8("response!"), false);
			}
			return 0;
		}

		@Override
		public void writeBlocked(TcpChannel cnx) {
			
		}

		@Override
		public void writeUnblocked(TcpChannel cnx) {
			
		}

		@Override
		public void connectionClosed(TcpChannel cnx) {
			log.warn("Connection closed");
		}

		@Override
		public void serverConnectionOpened(TcpServerChannel server) {
			log.warn("serverConnectionOpened");
			serverChan = server;
		}

		@Override
		public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
			throw new RuntimeException("serverConnectionFailed", err);
			
		}

		@Override
		public void serverConnectionClosed(TcpServerChannel server) {
			log.warn("server Connection closed");
		}

		@Override
		public void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel) {
			log.warn("Connection accepted");
			serverToClientChan = acceptedChannel;
			ensure.inc();

		}

		@Override
		public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
			
		}
	};
	
	private TcpChannel clientChan;

	private TcpClientChannelListener clientListener = new TcpClientChannelListener() {

		
		@Override
		public void receiveTimeout(TcpChannel cnx) {
			log.warn("ReceiveTimeout");

		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			log.info(cnx + " (client) received " + ByteBufferUtils.toUTF8String(false, msg));
			ensure.inc();
			return 0;
		}

		@Override
		public void writeBlocked(TcpChannel cnx) {
			
		}

		@Override
		public void writeUnblocked(TcpChannel cnx) {
			
		}

		@Override
		public void connectionClosed(TcpChannel cnx) {
			log.warn("connection closed");
			ensure.inc();
		}

		@Override
		public void connectionEstablished(TcpChannel cnx) {
			clientChan = cnx;
			log.info("connectionEstablished");
		}

		@Override
		public void connectionFailed(TcpChannel cnx, Throwable error) {
			log.warn("connectionFailed", error);
		}

		
	};
	


	@Start
	public void start() throws Exception {
		log = logFactory.getLogger(MuxProviderTest.class);
		Reactor r = reactorProvider.create("my-server-reactor");
		
		log.warn("starting server reactor");
		r.start();
		
		reactorProvider.tcpAccept(r, new InetSocketAddress("127.0.0.1", PORT),
				serverListener, 
				Collections.emptyMap());
		
		log.warn("starting client mux reactor");
		
		clientReactor = muxProvider.create("my-client-reactor");
		clientReactor.start();
		muxProvider.tcpConnect(clientReactor, new InetSocketAddress("127.0.0.1", PORT), 
				clientListener, 
				Collections.emptyMap());
		
		ensure.waitForStep(1);
		log.warn("connected");
	}

	@Test
	public void test1_SimpleSend() {
		ensure.waitForStep(1,30);
		log.warn("starting test testSimpleSend");
		clientChan.send(ByteBufferUtils.getUTF8("hello"), false);
		ensure.waitForStep(2);
		log.warn("waiting for response");
		ensure.waitForStep(3);
	}
	private TcpClientChannelListener clientListener2 = new TcpClientChannelListener() {

		@Override
		public void receiveTimeout(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			// TODO Auto-generated method stub
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
			log.warn("listener 2 connect");
			ensure.inc();
		}

		@Override
		public void connectionFailed(TcpChannel cnx, Throwable error) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	@Test
	public void test2_tDisconnect() {
		ensure.waitForStep(3, 30);
		log.warn("starting testDisconnect");
		serverToClientChan.close();
		ensure.waitForStep(4);
		muxProvider.tcpConnect(clientReactor, new InetSocketAddress("127.0.0.1", PORT),
				clientListener2,
				Collections.emptyMap());
		ensure.waitForStep(5);
	}
}
