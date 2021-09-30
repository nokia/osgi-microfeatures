package alcatel.tess.hometop.gateways.reactor;

// Jdk

/**
 * A Tcp Server Connection listener.
 */
public interface TcpServerChannelListener extends TcpChannelListener {
  /**
   * The server connection is ready to accept incoming connection requests.
   * @param server the tcp server
   * 
   * @deprecated This callback is only invoked when using the following deprecated
   * factory methods: <p>
   * <ul><li>{@link ReactorProviderCompatibility#newTcpServerChannel(java.net.InetSocketAddress, TcpServerChannelListener, Reactor, Object, org.apache.log4j.Logger)}
   * <li>{@link ReactorProviderCompatibility#newTcpServerChannel(java.net.InetSocketAddress, TcpServerChannelListener, Reactor, Object, org.apache.log4j.Logger, boolean)}
   * </ul>.<p> 
   * These factory methods are deprecated and are now replaced by the {@link ReactorProvider#tcpAccept(Reactor, java.net.InetSocketAddress, TcpServerChannelListener, java.util.Map)}
   * method, which does not invoke this callback anymore (The tcpAccept method is now returning the TcpServerChannel synchronously).
   */
  @Deprecated
  void serverConnectionOpened(TcpServerChannel server);
  
  /**
   * The server connection could not be initialized, mainly because of an
   * already bound port number.
   * @param server the tcp server
   * @param err the error cause
   * 
   * @deprecated This callback is only invoked when using the following deprecated
   * factory methods: <p>
   * <ul><li>{@link ReactorProviderCompatibility#newTcpServerChannel(java.net.InetSocketAddress, TcpServerChannelListener, Reactor, Object, org.apache.log4j.Logger)}
   * <li>{@link ReactorProviderCompatibility#newTcpServerChannel(java.net.InetSocketAddress, TcpServerChannelListener, Reactor, Object, org.apache.log4j.Logger, boolean)}
   * </ul>.<p> 
   * These factory methods are deprecated and are now replaced by the {@link ReactorProvider#tcpAccept(Reactor, java.net.InetSocketAddress, TcpServerChannelListener, java.util.Map)}
   * method, which does not invoke anymore this callback (an IOException is thrown instead of invoking this callback).
   */
  @Deprecated
  void serverConnectionFailed(TcpServerChannel server, Throwable err);
  
  /**
   * The server connection is closed.
   * @param server the tcp channel
   */
  void serverConnectionClosed(TcpServerChannel server);
  
  /**
   * We are accepting a new client. 
   * @param serverChannel the tcp server
   * @param acceptedChannel the accepted tcp channel
   */
  void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel);
  
  /**
   * We could not accept an incoming connection request. This kind of situation may happens
   * for example when the max number of simultaneous opened sockets is exhausted.
   * @param serverChannel the server channel
   * @param err the error cause
   */
  void connectionFailed(TcpServerChannel serverChannel, Throwable err);
}
