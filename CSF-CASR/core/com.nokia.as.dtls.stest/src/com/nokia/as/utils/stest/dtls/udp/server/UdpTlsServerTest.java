package com.nokia.as.utils.stest.dtls.udp.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;
import com.nokia.as.util.test.osgi.Ensure;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
public class UdpTlsServerTest {

	@ServiceDependency
	private volatile ReactorProvider provider;

	@ServiceDependency
	PlatformExecutors _execs;

	@ServiceDependency
	private LogServiceFactory logFactory;
	private LogService log;

	private final String keyStore = "instance/server.ks";
	private final String keyStorePassword = "password";
	private final String expected = "Hello from the other side";
	private int _received;

	private final Ensure ensure = new Ensure();

	@Before
	public void initLog() {
		log = logFactory.getLogger(UdpTlsServerTest.class);
	}

	@Test
	public void testDtls() {
		log.warn("Testing DTLSServer");
		try {
			new DTLSServer();
			ensure.waitForStep(3, 20);
			ensure.ensure();
			Thread.sleep(1000);
		} catch (Throwable e) {
			log.warn("error!", e);
			fail();
		}
//		try {
//			Thread.currentThread().sleep(Integer.MAX_VALUE);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	public class DTLSServer implements UdpChannelListener {

		public DTLSServer() throws Exception {
			Reactor reactor = provider.create("reactorServer");
			reactor.start();

			String NEEDED_CIPHERS[] = { ".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA" };

			Security security = new Security()
                    .addCipher(NEEDED_CIPHERS)
					.keyStore(new FileInputStream(keyStore))
					.keyStorePassword(keyStorePassword)
					.authenticateClients(true)
					.build();

			InetSocketAddress local = new InetSocketAddress("127.0.0.1", 9999);

			Map<ReactorProvider.UdpOption, Object> opts = new HashMap<>();
			opts.put(ReactorProvider.UdpOption.SECURITY, security);
			opts.put(ReactorProvider.UdpOption.SESSION_TIMEOUT, new Long(2000));
			//opts.put(ReactorProvider.UdpOption.INPUT_EXECUTOR, _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor()));

			UdpChannel channel = provider.udpBind(reactor, local, DTLSServer.this, opts);
			log.warn("Creating SCTP/DTLS server with params:");
			log.warn("Local address: " + local);
			log.warn("Keystore path: " + keyStore);
			log.warn("Keystore passwork: " + keyStorePassword);
			log.warn("Channel: " + channel);
			log.warn("Step 1");
			ensure.step(1);
		}

		public void messageReceived(UdpChannel cnx, ByteBuffer buf, InetSocketAddress addr) {
			try {
				byte[] b = new byte[buf.remaining()];
				buf.get(b);
				assertEquals(expected, new String(b));
				log.warn("Received from client: " + new String(b));

				String message = "Goodbye from the other side";
				log.warn("Sending to client: " + message);
				for (int i = 0; i < 10; i++) {
					cnx.send(addr, ByteBuffer.wrap(message.getBytes()), false);
				}

				_received++;

				if (_received == 1) {
					ensure.step(2); // received first message
				} else if (_received == 2) {
					ensure.step(3); // received second message
				}
			} catch (Exception e) {
				log.warn("exception", e);
				ensure.throwable(e);
			}
		}

		public void receiveTimeout(UdpChannel cnx) {
		}

		public void connectionOpened(UdpChannel cnx) {
		}

		public void connectionFailed(UdpChannel cnx, Throwable err) {
		}

		public void writeBlocked(UdpChannel cnx) {
		}

		public void writeUnblocked(UdpChannel cnx) {
		}

		public void connectionClosed(UdpChannel arg0) {
		}

	}
}
