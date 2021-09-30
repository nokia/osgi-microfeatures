package alcatel.tess.hometop.gateways.reactor.examples;

import java.io.FileInputStream;
// Jdk
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimerTask;

/**
 * Listen to incoming tcp connection request on port 9999.
 */
public class TestTcpServerSecure implements TcpServerChannelListener {
  final static Logger tracer = Logger.getLogger(TestTcpServerSecure.class);
  static PlatformExecutors _execs;
  
  public static void main(String args[]) throws Exception {
    ReactorProvider factory = ReactorProvider.provider();
    Reactor reactor = factory.create("server");
    reactor.start();
    InetSocketAddress from = new InetSocketAddress("localhost", 9999);
    _execs = PlatformExecutors.getInstance();

    String NEEDED_CIPHERS[] = { 
    		".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
    		"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
    };
    
    Security security = new Security()
    		.addProtocol("TLSv1.2")
    		.addCipher(NEEDED_CIPHERS)
    		.keyStore(new FileInputStream("tls/server.ks"))
    		.keyStorePassword("password")
    		.authenticateClients(true)
    		.setSNIMatcher("localhost")
    		.build();    		
    
    TestTcpServerSecure me = new TestTcpServerSecure();
    Map<ReactorProvider.TcpServerOption, Object> o = new HashMap<>();
    o.put(ReactorProvider.TcpServerOption.ENABLE_READ, true);
    o.put(ReactorProvider.TcpServerOption.SECURITY, security);
    final TcpServerChannel s = factory.tcpAccept(reactor, from, me, o);
    
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  // --- TcpServerChannelListener interface ---
  
  static TcpServerChannel tsc = null;
  
  public void serverConnectionOpened(final TcpServerChannel server) {
    tracer.warn("serverSocket ready to accept from local addr=" + server.getLocalAddress());
    tsc = server;
  }
  
  public void serverConnectionClosed(TcpServerChannel server) {
    tracer.warn("ServerSocket closed");
  }
  
  public void serverConnectionFailed(TcpServerChannel cnx, Throwable err) {
    tracer.warn("Could not setup our server socket on addr " + cnx.getLocalAddress() + " (attachment="
                    + cnx.attachment() + ")", err);
  }
  
  public void connectionAccepted(TcpServerChannel tsc, TcpChannel cnx) {
    tracer.warn("Accepted new client: " + cnx);
    //cnx.setInputExecutor(_execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor()));
    //cnx.close();
  }
  
  public void connectionFailed(TcpServerChannel tsc, Throwable err) {
    tracer.warn("Failed to accept a new tcp connection", err);
  }
  
  // Invoked when the connection is closed. 
  public void connectionClosed(TcpChannel cnx) {
    tracer.warn("TcpChannel closed: " + cnx);
  }
  
  final static byte[] RESP = "HTTP1.0 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\nContent-Length: 5\r\n\r\nHello".getBytes();
  
  // Handle an incoming message. 
  public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
    try {
      tracer.warn("client requested server names:" + cnx.getClientRequestedServerNames());
      for (SNIHostName sni : cnx.getClientRequestedServerNames()) {
    	  tracer.warn("XX: " + sni.getAsciiName());    	  
      }

      StringBuffer sb = new StringBuffer();
      while (msg.hasRemaining()) {
        sb.append((char) msg.get());
      }
      tracer.warn("Received: " + sb.toString());
      for (int i = 0; i < 1; i ++)
    	  cnx.send(("Hello-" + i).getBytes(), false);
      cnx.close();
    }
    
    catch (Throwable e) {
      tracer.warn("Error while sending", e);
    }
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
