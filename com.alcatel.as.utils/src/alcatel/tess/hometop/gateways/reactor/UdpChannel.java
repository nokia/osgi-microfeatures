package alcatel.tess.hometop.gateways.reactor;

// Jdk
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a Datagram socket connection.
 */
@ProviderType
public interface UdpChannel extends AsyncChannel {
  /**
   * Writes some data into this connection. The data is actually buffered and
   * won't be sent out until you call the flush() method.
   * 
   * @param addr the address where the udp packet must be sent
   * @param msg The message to be sent.
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full). You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=true).
   */
  void send(InetSocketAddress addr, ByteBuffer msg, boolean copy);
  
  /**
   * Writes some data into this connection. The data is actually buffered and
   * won't be sent out until you call the flush() method.
   * 
   * @param addr the address where the udp packet must be sent
   * @param msg The message to be sent.
   * @param copy true if the buffer must be copied when it is buffered (because
   *          the socket is full). You are strongly encouraged to "give" the
   *          buffer to this method (that is: provide copy=true).
   * @param bufs the messages to be sent
   */
  void send(InetSocketAddress addr, boolean copy, ByteBuffer ... bufs);
  
  /**
   * Ensure that all buffered message are sent out to the specified address.
   * @param addr the address to send the message to
   * @throws IOException on any errors
   * @deprecated use {@link #send(InetSocketAddress, ByteBuffer, boolean)}
   */
  @Deprecated
  void flush(InetSocketAddress addr) throws IOException;
  
  /**
   * @return true is this channel uses DTLS
   */
  public boolean isSecure();
  
  /**
   * Updates security parameters for this channel. The new parameters will be applied only for new UDP secured sessions.
   * @param security the security parameters to set
   * @throws IllegalStateException if the channel is unsecured.
   */
  void updateSecurity(Security security);

}
