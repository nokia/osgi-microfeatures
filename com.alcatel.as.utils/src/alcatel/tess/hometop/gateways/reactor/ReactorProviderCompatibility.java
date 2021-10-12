// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * Factory for creating Reactor services, as well as related classes.
 * @deprecated use {@link ReactorProvider}
 */
@Deprecated
@SuppressWarnings("javadoc")
public abstract class ReactorProviderCompatibility {
  /**
   * Return the reactor provider singleton.
   * @deprecated Get the reactor using an OSGi service dependency
   */
  @Deprecated
  public final static ReactorProvider provider() {
    return ServiceLoader.getService(ReactorProvider.class);
  }
  
  /**
   * Returns the default reactor. This reactor is already active and a dedicated thread is
   * running it. You should not stop this default reactor, which may be used by other
   * components.
   * 
   * @deprecated any default reactor should be created using {@link ReactorProvider#create(String)} 
   * method.
   * @returns The default active reactor, which must not be stopped.
   */
  @Deprecated
  public abstract Reactor getDefaultReactor() throws IOException;
  
  /**
   * Creates a new reactor.
   * @deprecated use {@link ReactorProvider#create(String)}
   * 
   * @param name the Reactor name.
   * @param start true if this reactor must be started right away, false if not. If false, you
   *          will have to invoke the start method.
   * @param logger The logger to be used by that reactor. If null, then the default logger name
   *          is "reactor".
   */
  @Deprecated
  public abstract Reactor newReactor(String name, boolean start, Logger logger) throws IOException;
  
  /**
   * Creates a new reactor. The calling thread is supposed to call itself the Reactor.run()
   * method. In other words, no threads are created by this method and the
   * Reactor.getReactorThread() will return the calling thread.
   * 
   * @deprecated Use {@link ReactorProvider#create(String)} method.
   */
  @Deprecated
  public final Reactor newReactor() throws IOException {
    return newReactor((Logger) null);
  }
  
  /**
   * Creates a new reactor, with a dedicated thread. Unlike the {@link #newReactor()} method,
   * this method will fire a thread which will invoke the reactor's run method.
   * 
   * @param threadName The name of the thread that will be created by this method. The
   *          Reactor.getReactorThread() method will return the created thread.
   * @deprecated Use {@link ReactorProvider#create(String)} method.
   */
  @Deprecated
  public final Reactor newReactor(String threadName) throws IOException {
    return newReactor((Logger) null, threadName);
  }
  
  /**
   * Creates a new reactor. The calling thread is supposed to call itself the Reactor.run()
   * method. In other words, no threads are created by this method and the
   * Reactor.getReactorThread() will return the calling thread.
   * 
   * @param tr the tracer used by the reactor.
   * @deprecated Use newReactor(String name, boolean start, Logger logger) instead.
   */
  @Deprecated
  public abstract Reactor newReactor(Logger tr) throws IOException;
  
  /**
   * Creates a new reactor, with a dedicated thread.
   * 
   * @param tr The tracer used.
   * @param threadName The name of the thread that will be created by this method. The
   *          Reactor.getReactorThread() method will return the created thread.
   * @deprecated Use newReactor(String name, boolean start, Logger logger) instead.
   */
  @Deprecated
  public abstract Reactor newReactor(Logger tr, String threadName) throws IOException;
  
  /**
   * Open a Tcp channel asynchronously. Once connected, the listener.connectionEstablished
   * method will be called. On any erros, the listener is called in its
   * connectionNotEstablished() method.
   * 
   * @deprecated use {@link ReactorProvider#tcpConnect(Reactor, InetSocketAddress, TcpClientChannelListener, java.util.Map)}
   * 
   * @param to The remote address to connect to
   * @param listener the listener that will be called back once we are connected.
   * @param reactor the event dispatcher used to perform the asynchronous connection.
   * @param attachment the data that will be attached to the TcpChannel.
   * @param timeout the max duration in millis until we wait for the connection, or 0L if no
   *          timeout has to be used.
   * @param tr the traced used.
   */
  @Deprecated
  public abstract void newTcpClientChannel(InetSocketAddress to, TcpClientChannelListener listener,
                                           Reactor reactor, Object attachment, long timeout, Logger tr);
  
  /**
   * Open a Tcp channel asynchronously. Once connected, the listener.connectionEstablished
   * method will be called. On any erros, the listener is called in its
   * connectionNotEstablished() method.
   * @deprecated use {@link ReactorProvider#tcpConnect(Reactor, InetSocketAddress, TcpClientChannelListener, java.util.Map)}
   * 
   * @param to The remote address to connect to
   * @param listener the listener that will be called back once we are connected.
   * @param reactor the event dispatcher used to perform the asynchronous connection.
   * @param attachment the data that will be attached to the TcpChannel.
   * @param timeout the max duration in millis until we wait for the connection, or 0L if no
   *          timeout has to be used.
   * @param priority The channel priority. See AsyncChannel.MIN_PRIORITY, and
   *          AsyncChannel.Max_Priority.
   * @param tr the traced used.
   */
  @Deprecated
  public final void newTcpClientChannel(InetSocketAddress to, TcpClientChannelListener listener,
                                        Reactor reactor, Object attachment, long timeout, int priority,
                                        Logger tr) {
    newTcpClientChannel(null, to, listener, reactor, attachment, timeout, priority, tr);
  }
  
  /**
   * Open a Tcp channel asynchronously. Once connected, the listener.connectionEstablished
   * method will be called. On any erros, the listener is called in its
   * connectionNotEstablished() method.
   * @deprecated use {@link ReactorProvider#tcpConnect(Reactor, InetSocketAddress, TcpClientChannelListener, java.util.Map)}
   *
   * @param from The local address to which this connection will bind to
   * @param to The remote address to connect to
   * @param listener the listener that will be called back once we are connected.
   * @param reactor the event dispatcher used to perform the asynchronous connection.
   * @param attachment the data that will be attached to the TcpChannel.
   * @param timeout the max duration in millis until we wait for the connection, or 0L if no
   *          timeout has to be used.
   * @param priority The channel priority. See AsyncChannel.MIN_PRIORITY, and
   *          AsyncChannel.Max_Priority.
   * @param tr the traced used.
   */
  @Deprecated
  public final void newTcpClientChannel(InetSocketAddress from, InetSocketAddress to,
                                        TcpClientChannelListener listener, Reactor reactor,
                                        Object attachment, long timeout, int priority, Logger tr) {
    newTcpClientChannel(from, to, listener, reactor, attachment, timeout, priority, tr, false);
  }
  
  /**
   * Open a Tcp channel asynchronously. Once connected, the listener.connectionEstablished
   * method will be called. On any erros, the listener is called in its
   * connectionNotEstablished() method.
   * @deprecated use {@link ReactorProvider#tcpConnect(Reactor, InetSocketAddress, TcpClientChannelListener, java.util.Map)}
   * 
   * @param from The local address to which this connection will bind to
   * @param to The remote address to connect to
   * @param listener the listener that will be called back once we are connected.
   * @param reactor the event dispatcher used to perform the asynchronous connection.
   * @param attachment the data that will be attached to the TcpChannel.
   * @param timeout the max duration in millis until we wait for the connection, or 0L if no
   *          timeout has to be used.
   * @param priority The channel priority. See AsyncChannel.MIN_PRIORITY, and
   *          AsyncChannel.Max_Priority.
   * @param tr the traced used.
   * @param secure true for TLS sockets, false otherwise.
   */
  @Deprecated
  public abstract void newTcpClientChannel(InetSocketAddress from, InetSocketAddress to,
                                           TcpClientChannelListener listener, Reactor reactor,
                                           Object attachment, long timeout, int priority, Logger tr,
                                           boolean secure);
  
  /**
   * Create a new Server Channel.
   * @deprecated use {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, java.util.Map)}
   * 
   * @param attachment The data attached to this server socket.
   * @return the associated ServerChannel object.
   * @throws IOException if we can not bind to the provided address.
   */
  @Deprecated
  public final void newTcpServerChannel(InetSocketAddress listenedAddr, TcpServerChannelListener listener,
                                        Reactor reactor, Object attachment, Logger tr) {
    newTcpServerChannel(listenedAddr, listener, reactor, attachment, tr, false);
  }
  
  /**
   * Create a new Server Channel.
   * @deprecated use {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, java.util.Map)}
   *
   * @return the associated ServerChannel object.
   * @param attachment The data attached to this server socket.
   * @throws IOException if we can not bind to the provided address.
   * @deprecated use
   *             {@link #newTcpServerChannel(Reactor, TcpServerChannelListener, InetSocketAddress, Object, boolean)}
   */
  @Deprecated
  public abstract void newTcpServerChannel(InetSocketAddress listenedAddr, TcpServerChannelListener listener,
                                           Reactor reactor, Object attachment, Logger tr, boolean secure);
  
  /**
   * Creates a new Server Channel.
   * @deprecated use {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, java.util.Map)}
   *
   * @param reactor The reactor to be used when openning scokets
   * @param listener the tcp server channel listener for socket callbacks
   * @param listenedAddr The server address used when listening.
   * @param attachment any context attached to the tcp server channel
   * @param secure true for secure/TLS server channel, false if not.
   * @return the tcp server channel. you can invoke TcpServerChannel.getLocalAddress() if you
   *         wan't to retrive the actual listened address.
   * @throws IOException if we can not bind to the provided address.
   */
  @Deprecated
  public abstract TcpServerChannel newTcpServerChannel(Reactor reactor, TcpServerChannelListener listener,
                                                       InetSocketAddress listenedAddr, Object attachment,
                                                       boolean secure, Logger tr) throws IOException;
  
  /**
   * Binds a Datagram Channel
   * @deprecated use {@link ReactorProvider#udpBind(Reactor, InetSocketAddress, UdpChannelListener, java.util.Map)}
   * @param local The local addr to bind to
   * @param listener The callback for incomming udp packet
   * @param reactor The event dispatcher
   * @param attachment the data that will be attached to the UdpChannel.
   * @param tr the tracer to be used.
   */
  @Deprecated
  public abstract void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor,
                                     Object attachment, Logger tr);
  
  /**
   * Binds a Datagram Channel
   * @deprecated use {@link ReactorProvider#udpBind(Reactor, InetSocketAddress, UdpChannelListener, java.util.Map)}
   *
   * @param local The local addr to bind to
   * @param listener The callback for incomming udp packet
   * @param reactor The event dispatcher
   * @param priority The channel priority. See AsyncChannel.MIN_PRIORITY, and
   *          AsyncChannel.Max_Priority.
   * @param attachment the data that will be attached to the UdpChannel.
   * @param tr the tracer to be used.
   */
  @Deprecated
  public abstract void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor,
                                     int priority, Object attachment, Logger tr);
  
  /**
   * Creates a new SCTP client channel.
   * @deprecated use {@link ReactorProvider#sctpConnect(Reactor, SocketAddress, SctpClientChannelListener, java.util.Map)}
   *
   * @param local the primary client address
   * @param secondaryLocals optional client addresses, or null
   * @param maxOutStreams the maximum number of streams that the 
   *        application wishes to be able to send.
   * @param maxInStreams the maximum number of streams that the 
   *        application wishes to be able to receive
   * @param to the destination address
   * @param attachment a context to be attached to the SctpChannel
   * @param timeout max time in millis to connect
   * @param priority channel priority (see AsyncChannel.MAX_PRIORITY, adn
   *        AsyncChannel.MIN_PRIORITY)
   * @param logger the logger to be used by the channel
   * @param listener the channel listener
   * @param reactor the reactor to use
   */
  @Deprecated
  public abstract void newSctpClientChannel(SocketAddress local, InetAddress[] secondaryLocals,
                                            int maxOutStreams, int maxInStreams, SocketAddress to,
                                            Object attachment, long timeout, int priority, Logger logger,
                                            SctpClientChannelListener listener, Reactor reactor);
  
  /**
   * Creates a new SCTP server channel.
   * @deprecated use {@link ReactorProvider#sctpAccept(Reactor, SocketAddress, SctpServerChannelListener, java.util.Map)}
   *
   * @param local The local server address
   * @param secondaryLocals secondaryLocals optional server addresses, or null
   * @param maxOutStreams the maximum number of outbound streams
   * @param maxInStreams the maximum number of inbound streams
   * @param priority channel priority (see AsyncChannel.MAX_PRIORITY, adn
   *        AsyncChannel.MIN_PRIORITY)
   * @param logger the logger to be used by the channel
   * @param listener the channel listener
   * @param attachment a context to be attached to the SctpServerChannel
   * @param reactor the reactor to use
   * @return the bound SCTP Server channel 
   * @throws IOException if an I/O exception occurs.
   */
  @Deprecated
  public abstract SctpServerChannel newSctpServerChannel(SocketAddress local, InetAddress[] secondaryLocals,
                                                         int maxOutStreams, int maxInStreams, int priority,
                                                         Logger logger, SctpServerChannelListener listener,
                                                         Object attachment, Reactor reactor)
      throws IOException;
  
}
