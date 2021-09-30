package alcatel.tess.hometop.gateways.reactor.examples;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;

/**
 * Open a tcp socket to the TestServer program (port 9999), and send him
 * "Hello".
 */
public class TestTcpClientSecure implements TcpClientChannelListener {
	static Logger tracer = Logger.getLogger("test");
	static Logger tracerReactor = Logger.getLogger("reactor");
	private static Reactor reactor;

	public static void main(final String args[]) throws Exception {
		final TestTcpClientSecure me = new TestTcpClientSecure();
		final ReactorProvider reactorProvider = ReactorProvider.provider();
		reactor = reactorProvider.create("Client");
		reactor.start();

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
	    		.setSNI("localhost")
	    		.build();    		

		reactor.schedule(new Runnable() {
			@Override
			public void run() {
				InetSocketAddress addr = new InetSocketAddress("localhost", 9999);
				Map<ReactorProvider.TcpClientOption, Object> options = new HashMap<>();
				options.put(ReactorProvider.TcpClientOption.SECURITY, security);
				reactorProvider.tcpConnect(reactor, addr, me, options);
			}
		});
		Thread.sleep(Integer.MAX_VALUE);
	}

	// --- TcpClientChannelListener interface ---
	static int _counter = 0;

	// Invokded when we are actually connected to our peer
	public void connectionEstablished(final TcpChannel cnx) {
		tracer.warn("client connected: sending ping");
		cnx.send("ping".getBytes());
	}

	// Invoked when the connection has failed.
	public void connectionFailed(TcpChannel cnx, Throwable err) {
		tracer.warn("Could not connect on addr " + cnx.getRemoteAddress() + "(attachment=" + cnx.attachment() + ")",
				err);
	}

	// Invoked when the connection is closed.
	public void connectionClosed(TcpChannel cnx) {
		tracer.warn("TcpChannel closed: " + cnx);
	}

	// Handle an incoming message.
	public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
		StringBuffer sb = new StringBuffer();
		while (msg.hasRemaining()) {
			sb.append((char) msg.get());
		}
		tracer.warn("Received: " + sb.toString());
		//cnx.send("Ping".getBytes());
		//cnx.close();
		return 0;
	}

	// Called if we are using timers.
	public void receiveTimeout(TcpChannel cnx) {
		tracer.warn("Message timeout");
	}

	// When invoked, this method tells that the socket is blocked on writes.
	// Actually, this method may be usefull for flow control: for example,
	// You can stop reading a socket by calling TcpChannel.disableReading()
	// method. Re-enabling read operations may be done by calling
	// TcpChannel.enableReading().
	public void writeBlocked(TcpChannel cnx) {
		tracer.warn("Write blocked");
	}

	// When invoked, this method tells that all pending data has been sent out.
	// Actually, this method may be usefull for flow control: for example,
	// You can stop reading a socket by calling TcpChannel.disableReading()
	// method. Re-enabling read operations may be done by calling
	// TcpChannel.enableReading().
	public void writeUnblocked(TcpChannel cnx) {
		tracer.warn("Write unblocked");
	}
}
