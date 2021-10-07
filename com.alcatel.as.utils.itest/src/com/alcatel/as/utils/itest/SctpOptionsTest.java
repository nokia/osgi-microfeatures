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
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.SctpSocketParam;
import com.alcatel.as.util.sctp.sctp_boolean;
import com.alcatel.as.util.sctp.sctp_initmsg;
import com.alcatel.as.util.sctp.sctp_paddrparams;
import com.alcatel.as.util.sctp.sctp_rtoinfo;
import com.alcatel.as.util.sctp.sctp_spp_flags;
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

@RunWith(MockitoJUnitRunner.class)
public class SctpOptionsTest extends IntegrationTestBase {

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

		component(comp -> comp.impl(SctpOptionsServer.class).withSvc(ReactorProvider.class, "(type=nio)", true).withSvc(LogServiceFactory.class, true));
		ensureServer.waitForStep(1);
		component(comp -> comp.impl(SctpOptionsClient.class).withSvc(ReactorProvider.class, "(type=nio)", true).withSvc(LogServiceFactory.class, true));
		ensureClient.waitForStep(1);
	}

	@Test
	public void testSctpOptions() {
		try {
			ensureClient.waitForStep(3);
			ensureServer.waitForStep(4);
		} catch(Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public static class SctpOptionsClient implements SctpClientChannelListener {
		
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;

		private final String expected = "Goodbye from the other side";

		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {
					
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from server: " + new String(b));
			ensureClient.step(3);
			log.warn("Step 3");
			cnx.close();
		}

		public void receiveTimeout(SctpChannel cnx) { }

		public void connectionClosed(SctpChannel cnx, Throwable err) { }

		public void writeBlocked(SctpChannel cnx) { }

		public void writeUnblocked(SctpChannel cnx) { }

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) { }

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) { }

		public void connectionEstablished(SctpChannel cnx) {
			log.warn("Connection established");
			
			log.warn("Checking SctpOptions");
			try {
				sctp_paddrparams paddrparams = cnx.getSocketOption(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, null);
				log.debug("paddrparams => " + paddrparams);
				assertEquals(10000, paddrparams.spp_hbinterval);
				
				sctp_rtoinfo rtoinfo = cnx.getSocketOption(SctpSocketOption.SCTP_RTOINFO, null);
				log.debug("rtoinfo => " + rtoinfo);
				assertEquals(10000, rtoinfo.srto_initial);
				assertEquals(30000, rtoinfo.srto_max);
				assertEquals(5000, rtoinfo.srto_min);
				
				sctp_initmsg initmsg = cnx.getSocketOption(SctpSocketOption.SCTP_INITMSG, null);
				log.debug("initmsg => " + initmsg);
				assertEquals(10, initmsg.sinit_num_ostreams);
				assertEquals(10, initmsg.sinit_max_instreams);
				assertEquals(5, initmsg.sinit_max_attempts);
				assertEquals(30000, initmsg.sinit_max_init_timeo);
				
				boolean reuse = cnx.getSocketOption(SctpSocketOption.SCTP_SO_REUSEADDR, null);
				assertEquals(true, reuse);
			} catch(IOException io) {
				log.warn("Get socket options failed", io);
				fail();
			}
			
			String message = "Hello from the other side";
			log.warn("Sending to server: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			log.warn("Step 2");
			ensureClient.step(2);
		}

		public void connectionFailed(SctpChannel cnx, Throwable error) { }
		
		public void start() {
			log = logFactory.getLogger(SctpOptionsClient.class);
			Reactor reactor = provider.create("SctpOptionsTest-reactorClient");
			reactor.start();

			SocketAddress local = new InetSocketAddress("127.0.0.1", 5003);
			SocketAddress remote = new InetSocketAddress("127.0.0.1", 5002);
			
			Map<SctpSocketOption, SctpSocketParam> sctpOptions = new HashMap<>();
			
			sctp_spp_flags spp_flags = new sctp_spp_flags(true, false, true, true, false);
			sctp_paddrparams paddrparams = new sctp_paddrparams(new InetSocketAddress(5002), 10000, 0, 0, 0, spp_flags);
			sctpOptions.put(SctpSocketOption.SCTP_PEER_ADDR_PARAMS, paddrparams);
			
			sctp_rtoinfo rtoinfo = new sctp_rtoinfo(10000, 30000, 5000);
			sctpOptions.put(SctpSocketOption.SCTP_RTOINFO, rtoinfo);
			
			sctp_initmsg initmsg = new sctp_initmsg(10, 10, 5, 30000);
			sctpOptions.put(SctpSocketOption.SCTP_INITMSG, initmsg);
			
			sctpOptions.put(SctpSocketOption.SCTP_SO_REUSEADDR, sctp_boolean.TRUE);

			Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
			opts.put(SctpClientOption.LOCAL_ADDR, local);
			opts.put(SctpClientOption.SOCKET_OPTIONS, sctpOptions);

			provider.sctpConnect(reactor, remote, SctpOptionsClient.this, opts);
			log.warn("Creating SCTP client with params:");
			log.warn("Local address: " + local);
			log.warn("Remote address: " + remote);
			log.warn("Socket options: " + sctpOptions);
			log.warn("Step 1");
			ensureClient.step(1);
		}
		
	}
	
	public static class SctpOptionsServer implements SctpServerChannelListener {
		
		private volatile ReactorProvider provider;
		private volatile LogServiceFactory logFactory;
		private LogService log;

		private final String expected = "Hello from the other side";

		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from client: " + new String(b));

			String message = "Goodbye from the other side";
			log.warn("Sending to client: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			log.warn("Step 3");
			ensureServer.step(3);
		}

		public void receiveTimeout(SctpChannel cnx) { }

		public void connectionClosed(SctpChannel cnx, Throwable err) { 
			log.warn("Connection closed");
			log.warn("Step 4");
			ensureServer.step(4);
		}

		public void writeBlocked(SctpChannel cnx) { }

		public void writeUnblocked(SctpChannel cnx) { }

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) { }

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) { }

		public void connectionAccepted(SctpServerChannel ssc, SctpChannel client) {
			log.warn("Accepted connection from" + client.getLocalAddress());
			log.warn("Step 2");
			ensureServer.step(2);
		}

		public void serverConnectionClosed(SctpServerChannel ssc, Throwable err) { }
		
		public void start() {
			log = logFactory.getLogger(SctpOptionsServer.class);
			Reactor reactor = provider.create("SctpOptionsTest-reactorServer");
			reactor.start();

			SocketAddress local = new InetSocketAddress("127.0.0.1", 5002);

			Map<SctpServerOption, Object> opts = new HashMap<ReactorProvider.SctpServerOption, Object>();

			try {
				provider.sctpAccept(reactor, local, SctpOptionsServer.this, opts);
			} catch(Exception e) { e.printStackTrace(); fail(); }
			log.warn("Creating SCTP server with params:");
			log.warn("Local address: " + local);
			log.warn("Step 1");
			ensureServer.step(1);
		}
	}

}
