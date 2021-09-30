package com.alcatel.as.utils.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
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
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

@RunWith(MockitoJUnitRunner.class)
public class TcpUpgradeTlsTest extends IntegrationTestBase {
	private static final Ensure ensureClient = new Ensure();
	private static final Ensure ensureServer = new Ensure();

	@Before
	public void before() {

		//Set up console loggers
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);

		component(comp -> comp.impl(TcpServer.class).withSvc(ReactorProvider.class, "(type=nio)", true).withSvc(LogServiceFactory.class, true));
		ensureServer.waitForStep(1);
		component(comp -> comp.impl(TcpClient.class).withSvc(ReactorProvider.class, "(type=nio)", true).withSvc(LogServiceFactory.class, true));
		ensureClient.waitForStep(1);
	}

	@Test
	public void testSctpOptions() {
		try {
			ensureClient.waitForStep(4);
			ensureServer.waitForStep(5);
			ensureClient.ensure();
			ensureServer.ensure();
		} catch(Throwable e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public static class TcpClient implements TcpClientChannelListener {
		
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;

		private final String expected = "Goodbye from the other side";
		private final String expectedTLS = "Goodbye from the other side TLS";
		private boolean expectTLS = false;

		public int messageReceived(TcpChannel cnx, ByteBuffer buf) {
			System.out.println(cnx.getClass());
					
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			
			if(!expectTLS) {		
				assertEquals(expected, new String(b));
				log.warn("Received from server: " + new String(b));
				
				try {
					log.warn("Upgrading client to TLS");
					cnx.upgradeToSecure();
					String message = "Hello from the other side TLS";
					log.warn("Sending to server (secure): " + message);
					cnx.send(message.getBytes(), false);
					ensureClient.step(3);
					log.warn("Step 3");
					expectTLS = true;
				} catch(Exception e) {
					e.printStackTrace();
					fail();
				}
			} else {
				assertEquals(expectedTLS, new String(b));
				log.warn("Received from server (secure): " + new String(b));
				ensureClient.step(4);
				log.warn("Step 4");
				cnx.close();
			}
			
			return 0;
		}

		public void receiveTimeout(TcpChannel cnx) { }

		public void connectionClosed(TcpChannel cnx) { }

		public void writeBlocked(TcpChannel cnx) { }

		public void writeUnblocked(TcpChannel cnx) { }

		public void connectionEstablished(TcpChannel cnx) {
			log.warn("Connection established");
					
			String message = "Hello from the other side";
			log.warn("Sending to server: " + message);
			cnx.send(message.getBytes(), false);
			log.warn("Step 2");
			ensureClient.step(2);
		}

		public void connectionFailed(TcpChannel cnx, Throwable error) { }
		
		public void start() throws Exception {
			log = logFactory.getLogger(TcpClient.class);
			Reactor reactor = provider.create("TcpUpgradeTlsTest-reactorClient");
			reactor.start();

			InetSocketAddress local = new InetSocketAddress("127.0.0.1", 5007);
			InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 5006);
			
			String NEEDED_CIPHERS[] = {
		    		".*",
		    		"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
		    		"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
		    };
			
			final Security security = new Security()
		    		.addProtocol("TLSv1.1", "TLSv1.2")
		    		.addCipher(NEEDED_CIPHERS)
		    		.keyStore(new FileInputStream("tls/client.ks"))
		    		.keyStorePassword("password")
		    		.delayed()
		    		.build();
			
			Map<TcpClientOption, Object> opts = new HashMap<ReactorProvider.TcpClientOption, Object>();
			opts.put(TcpClientOption.FROM_ADDR, local);
			opts.put(TcpClientOption.SECURITY, security);

			provider.tcpConnect(reactor, remote, TcpClient.this, opts);
			log.warn("Creating TCP client with params:");
			log.warn("Local address: " + local);
			log.warn("Remote address: " + remote);
			log.warn("Socket options: " + opts);
			log.warn("Step 1");
			ensureClient.step(1);
		}
		
	}
	
	public static class TcpServer implements TcpServerChannelListener {
		
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;

		private final String expected = "Hello from the other side";
		private final String expectedTLS = "Hello from the other side TLS";
		private boolean expectTLS = false;
		
		public int messageReceived(TcpChannel cnx, ByteBuffer buf) {
			
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			
			if(!expectTLS) {
				assertEquals(expected, new String(b));
				log.warn("Received from client: " + new String(b));

				String message = "Goodbye from the other side";
				log.warn("Sending to client: " + message);
				cnx.send(message.getBytes(), false);
				
				try {
					log.warn("Upgrading server to TLS");
					cnx.upgradeToSecure();
				} catch(Exception e) {
					e.printStackTrace();
					fail();
				}
				
				expectTLS = true;
				log.warn("Step 3");
				ensureServer.step(3);
			} else {
				assertEquals(expectedTLS, new String(b));
				log.warn("Received from client: " + new String(b));
				String message = "Goodbye from the other side TLS";
				log.warn("Sending to client: " + message);
				cnx.send(message.getBytes(), false);
				log.warn("Step 4");
				ensureServer.step(4);
			}
			
			return 0;
		}

		public void receiveTimeout(TcpChannel cnx) { }

		public void connectionClosed(TcpChannel cnx) { 
			log.warn("Connection closed");
			log.warn("Step 5");
			ensureServer.step(5);
		}

		public void writeBlocked(TcpChannel cnx) { }

		public void writeUnblocked(TcpChannel cnx) { }

		public void connectionAccepted(TcpServerChannel ssc, TcpChannel client) {
			log.warn("Accepted connection from" + client.getLocalAddress());
			log.warn("Step 2");
			ensureServer.step(2);
		}

		@Override
		public void serverConnectionOpened(TcpServerChannel server) { }

		@Override
		public void serverConnectionFailed(TcpServerChannel server, Throwable err) { }

		@Override
		public void serverConnectionClosed(TcpServerChannel server) { }

		@Override
		public void connectionFailed(TcpServerChannel serverChannel, Throwable err) { }
		
		public void start() throws Exception {
			log = logFactory.getLogger(TcpServer.class);
			Reactor reactor = provider.create("TcpUpgradeTlsTest-reactorServer");
			reactor.start();

			InetSocketAddress local = new InetSocketAddress("127.0.0.1", 5006);

			String NEEDED_CIPHERS[] = {
		    		".*",
		    		"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
		    		"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
		    };
			
			final Security security = new Security()
		    		.addProtocol("TLSv1.1", "TLSv1.2")
		    		.addCipher(NEEDED_CIPHERS)
		    		.keyStore(new FileInputStream("tls/server.ks"))
		    		.keyStorePassword("password")
		    		.delayed()
		    		.build();
			
			Map<TcpServerOption, Object> opts = new HashMap<ReactorProvider.TcpServerOption, Object>();
			opts.put(TcpServerOption.SECURITY, security);
			
			try {
				provider.tcpAccept(reactor, local, TcpServer.this, opts);
			} catch(Exception e) { e.printStackTrace(); fail(); }
			log.warn("Creating TCP server with params:");
			log.warn("Local address: " + local);
			log.warn("Step 1");
			ensureServer.step(1);
		}
	}
}
