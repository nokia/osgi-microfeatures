package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Send a datagram using nio.
 */
public class TestDatagramSocketClient {
  public static void main(String args[]) throws Exception {
    DatagramChannel dc = DatagramChannel.open();
    ByteBuffer b = ByteBuffer.allocate(1000);
    b.put("Hello".getBytes());
    b.flip();
    dc.send(b, new InetSocketAddress("localhost", 9999));
    Thread.sleep(Integer.MAX_VALUE);
  }
}
