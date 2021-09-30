package com.nokia.as.utils.stest.dtls.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;
import com.nokia.as.util.test.osgi.Ensure;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpServerOption;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class DTLSServerTest {

	@ServiceDependency
	private volatile ReactorProvider provider;

	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;

	private final String keyStore = "instance/server.ks";
	private final String keyStorePassword = "password";
	private final String expected = "Hello from the other side";

	private final Ensure ensure = new Ensure();

	@Before
	public void initLog() {
		log = logFactory.getLogger(DTLSServerTest.class);
	}

	@Test
	public void testDtls() {
		log.warn("Testing DTLSServer");
		try {
			new DTLSServer();
			ensure.waitForStep(4, 60);
		} catch(Exception e) {
			log.warn("error!", e);
			fail();
		}
	}

	public class DTLSServer implements SctpServerChannelListener { 

		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, 
				int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {

			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from client: " + new String(b));

			String message = "Goodbye from the other side";
			log.warn("Sending to client: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			log.warn("Step 3");
			ensure.step(3);
		}

		public void receiveTimeout(SctpChannel cnx) { }

		public void connectionClosed(SctpChannel cnx, Throwable err) {
			log.warn("Connection closed");
			log.warn("Step 4");
			ensure.step(4);
		}

		public void writeBlocked(SctpChannel cnx) { }

		public void writeUnblocked(SctpChannel cnx) { }

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) { }

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) { }

		public void connectionAccepted(SctpServerChannel ssc, SctpChannel client) {
			log.warn("Accepted connection from" + client.getLocalAddress());
			log.warn("Step 2");
			ensure.step(2);
		}

		public void serverConnectionClosed(SctpServerChannel ssc, Throwable err) { }

		public DTLSServer() throws Exception {
			Reactor reactor = provider.create("reactorServer");
			reactor.start();

			Security security = new Security()
					.addProtocol("DTLSv1.2")
					.keyStore(new FileInputStream(keyStore))
					.keyStorePassword(keyStorePassword)
					.authenticateClients(true)
					.build();

			SocketAddress local = new InetSocketAddress("127.0.0.1", 5000);

			Map<SctpServerOption, Object> opts = new HashMap<ReactorProvider.SctpServerOption, Object>();
			opts.put(SctpServerOption.SECURITY, security);

			provider.sctpAccept(reactor, local, DTLSServer.this, opts);
			log.warn("Creating SCTP/DTLS server with params:");
			log.warn("Local address: " + local);
			log.warn("Keystore path: " + keyStore);
			log.warn("Keystore passwork: " + keyStorePassword);
			log.warn("Step 1");
			ensure.step(1);
		}
	}
}
