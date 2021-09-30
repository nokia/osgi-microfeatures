package alcatel.tess.hometop.gateways.reactor;

// Jdk
import java.nio.ByteBuffer;

/**
 * This interface listens to Tcp Data received from a TcpChannel.
 */
public interface TcpChannelListener {
  /**
   * Invoked when data did not arrive timely on a udp connection.
   * Notice that the connection is not closed. It is up to you to implements the
   * required logic, when a message does not arrive timely on this connection.
   * @param cnx the tcp channel
   */
  void receiveTimeout(TcpChannel cnx);
  
  /**
   * Invoked when the reactor detects that a message is ready to be
   * read on this connection.
   * If there is not enough data available in the buffer for parsing a whole message,
   * then this method may return the missing bytes.
   * If you don't fully read the buffer, the remaining bytes stay until the next selection.
   * @param cnx the tcp channel
   * @param msg the received message
   *
   * @return the missing bytes required to handle a whole message, or 0.
   */
  int messageReceived(TcpChannel cnx, ByteBuffer msg);
  
  /**
   * When invoked, this method tells that the socket is blocked on writes.
   * Actually, this method may be useful for flow control.
   * @param cnx the tcp channel
   */
  void writeBlocked(TcpChannel cnx);
  
  /**
   * When invoked, this method tells that all pending data has been sent out.
   * Actually, this method may be useful for flow control.
   * @param cnx the tcp channel
   */
  void writeUnblocked(TcpChannel cnx);
  
  /**
   * Invoked when a client/server connection is closed.
   * @param cnx the tcp channel
   */
  void connectionClosed(TcpChannel cnx);
}
