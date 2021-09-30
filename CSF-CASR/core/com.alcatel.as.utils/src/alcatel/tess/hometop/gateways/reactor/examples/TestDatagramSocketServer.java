package alcatel.tess.hometop.gateways.reactor.examples;

// Jdk
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * Receive a datagram using nio.
 */
public class TestDatagramSocketServer {
  public static void main(String args[]) throws Exception {
    Selector sel = Selector.open();
    
    DatagramChannel channel = DatagramChannel.open();
    channel.socket().bind(new InetSocketAddress(9999));
    channel.configureBlocking(false);
    channel.register(sel, SelectionKey.OP_READ);
    
    while (true) {
      int n = sel.select();
      if (n == 0)
        continue;
      
      Iterator it = sel.selectedKeys().iterator();
      while (it.hasNext()) {
        SelectionKey key = (SelectionKey) it.next();
        if (key.isReadable()) {
          DatagramChannel dc = (DatagramChannel) key.channel();
          ByteBuffer b = ByteBuffer.allocate(1000);
          InetSocketAddress addr;
          while ((addr = (InetSocketAddress) dc.receive(b)) != null) {
            b.flip();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (b.hasRemaining()) {
              out.write(b.get());
            }
            System.out.println(new String(out.toByteArray()));
            b.clear();
          }
        }
        
        it.remove();
      }
    }
  }
}
