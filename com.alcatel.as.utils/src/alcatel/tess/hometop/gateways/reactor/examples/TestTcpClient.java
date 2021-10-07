package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;

/**
 * Open a tcp socket to the TestServer program (port 9999),
 * and send him "Hello".
 */
public class TestTcpClient implements TcpClientChannelListener {
  static Logger tracer = Logger.getLogger("test");
  static Logger tracerReactor = Logger.getLogger("reactor");
  private static Reactor reactor;
  
  public static void main(final String args[]) throws Exception {
    final boolean secure = (args.length) > 0 ? args[args.length - 1].equals("secure") : false;
    final TestTcpClient me = new TestTcpClient();
    final ReactorProvider factory = ReactorProvider.provider();
    reactor = factory.create("Client");
    reactor.start();
    
    reactor.schedule(new Runnable() {
      @Override
      public void run() {
        InetSocketAddress local = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        InetSocketAddress addr = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

        factory.newTcpClientChannel(local, addr, me, reactor, new Integer(9999), 5000L, 0, tracerReactor,
                                    secure);
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
    tracer.warn("Could not connect on addr " + cnx.getRemoteAddress() + "(attachment=" + cnx.attachment()
        + ")", err);
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
