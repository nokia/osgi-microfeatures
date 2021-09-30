package com.nokia.as.utils.stest.dtls.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.felix.dm.annotation.api.Component;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;
import com.nokia.as.util.test.osgi.Ensure;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class DTLSClientTest {

	@ServiceDependency
	private volatile ReactorProvider provider;

	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;

	private final String keyStore = "instance/client.ks";
	private final String keyStorePassword = "password";
	private final String expected = "Goodbye from the other side";

	private final Ensure ensure = new Ensure();

	@Before
	public void initLog() {
		log = logFactory.getLogger(DTLSClient.class);
	}

	@Test
	public void testDtls() {
		log.warn("Testing DTLSClient");
		try {
			new DTLSClient();
			ensure.waitForStep(3, 60);
		} catch(Exception e) {
			fail();
		}
	}

	public class DTLSClient implements SctpClientChannelListener {

		public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, 
				int bytes, boolean isComplete,
				boolean isUnordered, int ploadPID, int streamNumber) {

			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			assertEquals(expected, new String(b));
			log.warn("Received from server: " + new String(b));
			ensure.step(3);
			log.warn("Step 3");
			cnx.close();
		}

		public void receiveTimeout(SctpChannel cnx) { }

		public void connectionClosed(SctpChannel cnx, Throwable err) { 
			log.warn("connectionClosed");
		}

		public void writeBlocked(SctpChannel cnx) { }

		public void writeUnblocked(SctpChannel cnx) { }

		public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, 
				int errcode, int streamNumber) { }

		public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) { }

		public void connectionEstablished(SctpChannel cnx) { 
			String message = "Hello from the other side";
			log.warn("Sending to server: " + message);
			cnx.send(false, null, 0, ByteBuffer.wrap(message.getBytes()));
			log.warn("Step 2");
			ensure.step(2);
		}

		public void connectionFailed(SctpChannel cnx, Throwable err) {
			log.warn("connectionFailed", err);
			fail();
		}

		public DTLSClient() throws Exception {
			Reactor reactor = provider.create("reactorClient");
			reactor.start();

			Security security = new Security()
					.addProtocol("DTLSv1.2")
					.keyStore(new FileInputStream(keyStore))
					.keyStorePassword(keyStorePassword)
					.build();

			SocketAddress local = new InetSocketAddress("127.0.0.1", 5001);
			SocketAddress remote = new InetSocketAddress("127.0.0.1", 5000);

			Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
			opts.put(SctpClientOption.SECURITY, security);
			opts.put(SctpClientOption.LOCAL_ADDR, local);

			provider.sctpConnect(reactor, remote, DTLSClient.this, opts);
			log.warn("Creating SCTP/DTLS client with params:");
			log.warn("Local address: " + local);
			log.warn("Remote address: " + remote);
			log.warn("Keystore path: " + keyStore);
			log.warn("Keystore passwork: " + keyStorePassword);
			log.warn("Step 1");
			ensure.step(1);
		}
	}
}
