// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.mux.reactor.stest.tls;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpServerOption;
import alcatel.tess.hometop.gateways.reactor.Security;
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

	private Reactor serverReactor;
	

	@Start
	public void start() throws Exception {
		log = logFactory.getLogger(MuxProviderTest.class);
		serverReactor = reactorProvider.create("my-server-reactor");
		
		log.warn("starting TLS server");
		serverReactor.start();
		
		final Security security = new Security()
	    		.addProtocol("TLSv1.1", "TLSv1.2")
	    		.keyStore(new FileInputStream("/tmp/server-keystore.ks"))
	    		.keyStorePassword("password")
	    		.build();
		
		Map<TcpServerOption, Object> opts = new HashMap<ReactorProvider.TcpServerOption, Object>();
		opts.put(TcpServerOption.SECURITY, security);
		
		reactorProvider.tcpAccept(serverReactor, new InetSocketAddress("127.0.0.1", PORT),
				serverListener, 
				opts);
		
		log.warn("starting client mux reactor");
		
		Map<TcpClientOption, Object> clientOpts = new HashMap<ReactorProvider.TcpClientOption, Object>();
		clientOpts.put(TcpClientOption.SECURITY, new Security());
		
		clientReactor = muxProvider.create("my-client-reactor");
		clientReactor.start();
		muxProvider.tcpConnect(clientReactor, new InetSocketAddress("127.0.0.1", PORT), 
				clientListener, 
				clientOpts);
		
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
			log.info(cnx + " (client2) received " + ByteBufferUtils.toUTF8String(false, msg));
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
	public void test2_Disconnect() {
		ensure.waitForStep(3, 50);
		log.warn("starting testDisconnect");
		serverToClientChan.close();
		ensure.waitForStep(4);
		Map<TcpClientOption, Object> clientOpts = new HashMap<ReactorProvider.TcpClientOption, Object>();
		clientOpts.put(TcpClientOption.SECURITY, new Security());

		muxProvider.tcpConnect(clientReactor, new InetSocketAddress("127.0.0.1", PORT),
				clientListener2,
				clientOpts);
		ensure.waitForStep(5);
	}
	
	
	private TcpServerChannelListener secureUpgradeServerListener = new TcpServerChannelListener() {
		boolean secureUpgraded = false;

		@Override
		public void writeUnblocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void writeBlocked(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void receiveTimeout(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			String message = ByteBufferUtils.toUTF8String(false, msg);
			log.info(cnx + " (secureUpgradeChannelListener) received " + message);
			if(!secureUpgraded) {
				if(!"first message: clear".equals(message)) {
					log.warn("no the expected message!");
					fail();
					return 0;
				}
				cnx.send(ByteBufferUtils.getUTF8("first reply: upgrade now!"), false);
				try {
					cnx.upgradeToSecure();
					secureUpgraded = true;
				} catch (Exception e) {
					log.warn("server upgrade failed!", e);
				}
			} else {
				log.warn("second message");
				ensure.inc();
			}
			return 0;
		}
		
		@Override
		public void connectionClosed(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void serverConnectionOpened(TcpServerChannel server) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void serverConnectionClosed(TcpServerChannel server) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private TcpClientChannelListener secureUpgradeListener = new TcpClientChannelListener() {

		boolean clientUpgrade = false;
		@Override
		public void receiveTimeout(TcpChannel cnx) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
			String message =  ByteBufferUtils.toUTF8String(false, msg);
			log.info(cnx + " (secureUpgradeListener) received " + message);
			if(!clientUpgrade) {
				if(!"first reply: upgrade now!".equals(message)) {
					log.warn("unexpected message!");
					return 0;
				}
				log.info("client upgrade now!");
				try {
					cnx.upgradeToSecure();
					clientUpgrade = false;
				} catch (Exception e) {
					log.warn("client upgrade failed!", e);
					fail();
				}
				
				cnx.send(ByteBufferUtils.getUTF8("second message: encrypted"), false);				
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
			log.warn("connectionClosed");
			
		}

		@Override
		public void connectionEstablished(TcpChannel cnx) {
			log.warn("secureUpgradeListener: connectionEstablished");
			cnx.send(ByteBufferUtils.getUTF8("first message: clear"), false);
		}

		@Override
		public void connectionFailed(TcpChannel cnx, Throwable error) {
			log.warn("connectionFailed", error);
			
		}
		
	};
	
	@Test
	public void test3_TLSUpgrade() throws Exception {
		ensure.waitForStep(6, 50);
		log.warn("starting testTLSUpgrade");

		final Security security = new Security()
	    		.addProtocol("TLSv1.1", "TLSv1.2")
	    		.keyStore(new FileInputStream("/tmp/server-keystore.ks"))
	    		.keyStorePassword("password")
	    		.delayed()
	    		.build();
		
		Map<TcpServerOption, Object> opts = new HashMap<ReactorProvider.TcpServerOption, Object>();
		opts.put(TcpServerOption.SECURITY, security);
		
		reactorProvider.tcpAccept(serverReactor, new InetSocketAddress("127.0.0.1", PORT + 1),
				secureUpgradeServerListener, 
				opts);
		
		
		
		muxProvider.tcpConnect(clientReactor,  new InetSocketAddress("127.0.0.1", PORT + 1),
				secureUpgradeListener,
				Collections.singletonMap(TcpClientOption.SECURITY, 
				new Security().delayed()));
		
		ensure.waitForStep(7);

	}
}
