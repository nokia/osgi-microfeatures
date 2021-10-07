package alcatel.tess.hometop.gateways.reactor;

/**
 * Base interface for all reactor channels.
 */
public interface Channel {
  /**
   * Closes this connection gracefully and ensure that all pending data are sent
   * out.<BR>
   * The connectionClosed() method will be called once the socket is really
   * closed.
   */
  void close();
  
  /**
   * Aborts this channel. The Listener.connectionClosed method is called when
   * the shutdown is performed.
   */
  void shutdown();
  
  /**
   * Returns the context attached to this channel.
   * @return the context attached to this channel
   */
  <T> T attachment();
  
  /**
   * Attaches a context to this channel.
   * @param attached the context to be attached
   */
  void attach(Object attached);
  
  /**
   * Return the reactor managing this channel.
   * @return the reactor managing this channel.
   */
  Reactor getReactor();
}
