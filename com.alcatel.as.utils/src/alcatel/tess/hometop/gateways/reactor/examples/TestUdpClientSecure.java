package alcatel.tess.hometop.gateways.reactor.examples;

import java.io.FileInputStream;
// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.UdpOption;

/**
 * Send an Udp message on port 9998.
 */
public class TestUdpClientSecure implements UdpChannelListener, Runnable {
  final static Logger tracer = Logger.getLogger("test");
  private static UdpChannel _channel;
  private static int _counter;
  
  public static void main(String args[]) throws Exception {
    tracer.setLevel(Level.DEBUG);
    
    ReactorProvider factory = ReactorProvider.provider();
    Reactor reactor = factory.create("reactor");
    reactor.start();
    
    String NEEDED_CIPHERS[] = { 
    		".*", "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
    		"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"
    };
    
    Security security = new Security()
    		.addProtocol("DTLSv1.2")
    		.addCipher(NEEDED_CIPHERS)
    		.keyStore(new FileInputStream("tls/client.ks"))
    		.keyStorePassword("password")
    		.authenticateClients(true)
    		.build(); 

    TestUdpClientSecure me = new TestUdpClientSecure();
    InetSocketAddress local = new InetSocketAddress("localhost", 9999);
    Map<ReactorProvider.UdpOption, Object> o = new HashMap<> ();
    o.put(UdpOption.SECURITY, security);
    o.put(UdpOption.IS_CLIENT, true);
    _channel = factory.udpBind(reactor, local, me, o);
    
    //reactor.scheduleAtFixedRate(me, 0, 1, TimeUnit.SECONDS);
    me.run();
    
    Thread.sleep(Integer.MAX_VALUE);
  }
  
  public void run() {
    tracer.warn("UdpClient: sending message to server:" + _channel);
    ByteBuffer buf = ByteBuffer.wrap(("hello" + (++_counter)).getBytes());
    _channel.send(new InetSocketAddress("localhost", 9998), buf, false);
  }
  
  // --- ChannelListener interface ---
  
  public void connectionOpened(UdpChannel cnx) {
  }
  
  public void connectionClosed(UdpChannel c) {
    tracer.warn("UdpConnection closed" + c);
  }
  
  public void connectionFailed(UdpChannel cnx, Throwable err) {
    tracer.warn("UdpConnection failed on addr" + cnx.getLocalAddress(), err);
  }
  
  // Handle an incoming message. 
  public void messageReceived(UdpChannel cnx, ByteBuffer msg, InetSocketAddress from) {
    StringBuffer sb = new StringBuffer();
    while (msg.hasRemaining()) {
      sb.append((char) msg.get());
    }
    tracer.debug("Received message from " + from + ": " + sb.toString());
  }
  
  // Invoked when an IO exception occurs. 
  public void exceptionCaught(UdpChannel cnx, Throwable cause) {
    tracer.warn("Got exception", cause);
  }
  
  // Called if we are using timers. (see Channel.setSoTimeout()). 
  public void receiveTimeout(UdpChannel cnx) {
    tracer.warn("Messaage timeout");
  }
  
  // When invoked, this method tells that the socket is blocked on writes.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling Channel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // Channel.enableReading().
  public void writeBlocked(UdpChannel cnx) {
    tracer.debug("Write blocked");
  }
  
  // When invoked, this method tells that all pending data has been sent out.
  // Actually, this method may be usefull for flow control: for example,
  // You can stop reading a socket by calling Channel.disableReading()
  // method. Re-enabling read operations may be done by calling 
  // Channel.enableReading().
  public void writeUnblocked(UdpChannel cnx) {
    tracer.debug("Write unblocked");
  }
}
