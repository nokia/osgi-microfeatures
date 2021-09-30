package com.nokia.as.utils.stest.dtls.udp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
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
public class UdpTlsClientTest {

	@ServiceDependency
	private volatile ReactorProvider _provider;
	
	@ServiceDependency
	PlatformExecutors _execs;
	
	@ServiceDependency
	MeteringService _metering;

	@ServiceDependency
	private LogServiceFactory _logFactory;
	private LogService _log;

	private final String _keyStore = "instance/client.ks";
	private final String _keyStorePassword = "password";
	private final String _expected = "Goodbye from the other side";

	private final Ensure _ensure = new Ensure();

	@Before
	public void initLog() {
		_log = _logFactory.getLogger(DTLSClient.class);
	}

	@Test
	public void testUdpDtls() {
		_log.warn("Testing DTLSClient");
		try {
			new DTLSClient();
			_ensure.waitForStep(4, 10);
			_ensure.ensure();
		} catch (Throwable e) {
			fail();
		}
	}
	
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {}
	}

	public class DTLSClient implements UdpChannelListener {
		int _received = 0; // expected to receive 100 messages
		
		public DTLSClient() throws Exception {
			Reactor reactor = _provider.create("reactorClient");
			reactor.start();
			
		    String NEEDED_CIPHERS[] = {
	                ".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
	                "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
		    };

			Security security = new Security()
	                .addCipher(NEEDED_CIPHERS)
					.keyStore(new FileInputStream(_keyStore))
					.keyStorePassword(_keyStorePassword)
					.build();

			InetSocketAddress local = new InetSocketAddress(5001);

			Map<ReactorProvider.UdpOption, Object> opts = new HashMap<>();
			opts.put(ReactorProvider.UdpOption.SECURITY, security);
			opts.put(ReactorProvider.UdpOption.IS_CLIENT, true);
			opts.put(ReactorProvider.UdpOption.INPUT_EXECUTOR, _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor()));
			opts.put(ReactorProvider.UdpOption.SESSION_TIMEOUT, 3000L);

			UdpChannel channel = _provider.udpBind(reactor, local, DTLSClient.this, opts);
			_log.warn("Creating SCTP/DTLS udp client with params:");
			_log.warn("Local address: " + local);
			_log.warn("Keystore path: " + _keyStore);
			_log.warn("Keystore passwork: " + _keyStorePassword);
			_log.warn("Channel:"  + channel);
			_log.warn("Step 1");
			_ensure.step(1);
			
			sendToServer(channel);
			_log.warn("Step 2");
			_ensure.step(2);
		}

		private void sendToServer(UdpChannel cnx) {
			String message = "Hello from the other side";
			_log.warn("Sending to server: " + message);
			cnx.send(new InetSocketAddress("localhost", 9999), ByteBuffer.wrap(message.getBytes()), false);
		}
		
		public void messageReceived(UdpChannel cnx, ByteBuffer buf, InetSocketAddress addr) {
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			_log.warn("Received from server: " + new String(b));
			assertEquals(_expected, new String(b));
			
			_received ++;
			if (_received == 10) {
				_ensure.step(3);
				
				try {
					// at this point, ensure the secured session count is still set to 1
					Meter sessionCount = getReactorMeterSessionCount();
					assertEquals(1L, sessionCount.getValue());

					_log.warn("Waiting 5 seconds, checking if udp session will auto expired after 6 seconds");
					_execs.getCurrentThreadContext().getCurrentExecutor().schedule(() -> checkSessionExpired1(cnx), 5000, TimeUnit.MILLISECONDS);					
				} catch (Exception e) {
					_log.warn("exception", e);
					sleep(10000);
					_ensure.throwable(e);
				}
			} else if (_received == 20) {
				cnx.close();
				// at this point, the whole test is done, and we closed the channel: ensure the session has expired
				_execs.getCurrentThreadContext().getCurrentExecutor().schedule(() -> checkSessionExpired2(cnx), 1000, TimeUnit.MILLISECONDS);					
			}
		}
		
		private void checkSessionExpired1(UdpChannel cnx) {
			// now the session should have expired
			Meter sessionCount = getReactorMeterSessionCount();
			assertEquals(0L, sessionCount.getValue());
			
			// Send a last message: it will re create a secured session, and we'll receive 10 more responses
			sendToServer(cnx);
		}
		
		private void checkSessionExpired2(UdpChannel cnx) {
			// now the session should have expired
			try {
				Meter sessionCount = getReactorMeterSessionCount();
				assertEquals(0L, sessionCount.getValue());
				_ensure.step(4);
			} catch (Exception e) {
				_log.warn("exception", e);
				_ensure.throwable(e);
			}
		}
		
		private Meter getReactorMeterSessionCount() {
			Monitorable reactorMon = _metering.getMonitorable("as.service.reactor");
			assertNotNull("can't get \"as.service.reactor\" reactor monitorable service", reactorMon);
			Meter sessionCount = reactorMon.getMeters().get("reactorClient:udp.session.count");
			assertNotNull("can't get \"reactor:udp.session.count\" reactor meter", sessionCount);
			return sessionCount;
		}

		public void receiveTimeout(UdpChannel cnx) {
		}

		public void connectionClosed(UdpChannel cnx) {
			_log.warn("connectionClosed");
		}

		public void writeBlocked(UdpChannel cnx) {
		}

		public void writeUnblocked(UdpChannel cnx) {
		}

		public void connectionOpened(UdpChannel cnx) {
		}

		public void connectionFailed(UdpChannel cnx, Throwable err) {
			_log.warn("connectionFailed", err);
			fail();
		}
	}
}
