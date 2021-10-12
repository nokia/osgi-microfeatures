// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

// Log4j
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * Reactor channels are instantiated using this class. This class is an OSGi 
 * service and should be obtained from the OSGi bundle context, or using an
 * OSGi dependency injection framework, like declarative service.
 */
@SuppressWarnings("deprecation")
public abstract class ReactorProvider extends ReactorProviderCompatibility {
  /**
   * Options used when opening a tcp client channel.
   * @see {@link ReactorProvider#tcpConnect(Reactor, InetSocketAddress, TcpClientChannelListener, Map)}
   */
  public enum TcpClientOption {
    /**
     * Attachment to be set in the tcp channel (option value is an Object).
     */
    ATTACHMENT,
    
    /**
     * Max connection timeout in millisecond (Option value is a Long).
     */
    TIMEOUT,
    
    /**
     * Channel priority (Option value is an Integer).
     * @see AsyncChannel#MAX_PRIORITY
     * @see AsyncChannel#MIN_PRIORITY
     */
    PRIORITY,
    
    /**
     * Channel local address (Option value is an InetSocketAddress)
     */
    FROM_ADDR,
    
    /**
     * Channel secure mode (Option value is a Boolean).
     * @deprecated use SECURITY
     */
    SECURE,
    
    /**
     * SSL parameters used to establish secured tcp connections (Option value is an instance of {@link #Security}).
     */
    SECURITY,
        
    /**
     * Channel executor to be used when reading socket (Option value is an PlatformExecutor).
     */
    INPUT_EXECUTOR,
        
    /**
     * Activate Tcp No Delay (Option value is a Boolean, Boolean.TRUE by default)
     */
    TCP_NO_DELAY,
    
    /**
     * Use direct buffers when reading sockets. The direct buffer is passed to the Listener.
     * (Option value is a Boolean, Boolean.FALSE by default)
     */
    USE_DIRECT_BUFFER,
    
    /**
     * Auto flush sent messages. Option value is an Integer (default=0). 0 means all sent messages are autoflushed.
     * a positive value means messages are sent when send-buffer size exceeds the given value.
     * WARNING: if you specify a positive value, then you have to invoke the tcpChannel.flush() method when
     * you want to make sure all pending messages must be sent.
     */
    AUTO_FLUSH_SIZE,
    
    /**
     * The size of the socket send buffer. If not specified, the default system value is used.
     */
    SO_SNDBUF,
    
    /**
     * The size of the socket receive buffer. If not specified, the default system value is used.
     */
    SO_RCVBUF,
    
    /**
     * Set the ability to bind non-local addresses. (Boolean, Boolean.FALSE is default)
     */
    IP_TRANSPARENT,
    
    /**
     * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
     * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
     * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
     * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
     */
    LINGER
  }
  
  /**
   * Options used when opening a tcp server channel.
   * @see {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, Map))}
   */
  @SuppressWarnings("javadoc")
  public enum TcpServerOption {
    /**
     * Attached to be assigned to accepted channels (value=Object)
     */
    ATTACHMENT,
    
    /**
     * Server channel secure mode (value=Boolean).
     * @deprecated use SECURITY
     */
    SECURE,
    
    /**
     * SSL parameters used to establish secured tcp connections (Option value is an instance of {@link #Security}).
     */
    SECURITY,
    
    /**
     * The maximum number number (Integer) of pending connection requests (default=1024)
     */
    BACKLOG,
    
    /**
     * Activate Tcp No Delay (Option value is a Boolean, Boolean.TRUE by default)
     */
    TCP_NO_DELAY,
    
    /**
     * Automatically enable tcp server channel reading mode(true by default). If false, you have to enable server reading mode, using the
     * @link {@link TcpChannel#enableReading()} method.
     */
    ENABLE_READ, 
    
    /**
     * Use direct buffers when reading sockets. The direct buffer is passed to the Listener.
     */
    USE_DIRECT_BUFFER,
    
    /**
     * Auto flush sent messages. Option value is an Integer (default=0). 0 means all sent messages are autoflushed.
     * a positive value means messages are sent when send-buffer size exceeds the given value.
     * WARNING: if you specify a positive value, then you have to invoke the tcpChannel.flush() method when
     * you want to make sure all pending messages must be sent.
     */
    AUTO_FLUSH_SIZE,
    
    /**
     * The size of the socket send buffer. If not specified, the default system value is used.
     */
    SO_SNDBUF,
    
    /**
     * The size of the socket receive buffer. If not specified, the default system value is used.
     */
    SO_RCVBUF,
    
    /**
     * Timeout in millis (Long) used when disable server socket accepts when there is not enough resources available. 
     * For instance, if the system has too many open files, the server socket won't accept incoming connection requests 
     * during the specified timeout. The property value is a Long object (1000 by default). 0 value means the default value must be
     * used (1 second by default).
     */
    DISABLE_ACCEPT_TIMEOUT,
    
    /**
     * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
     * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
     * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
     * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
     */
    LINGER
  }
  
  /**
   * Options used when opening an udp channel.
   * {@link ReactorProvider#udpBind(Reactor, InetSocketAddress, UdpChannelListener, Map)}
   */
  public enum UdpOption {
    /**
     * Attached to be assigned to udp channel (value=Object)
     */
    ATTACHMENT,
    
    /**
     * Channel priority (Option value is an Integer).
     * @see AsyncChannel#MAX_PRIORITY
     * @see AsyncChannel#MIN_PRIORITY
     */
    PRIORITY,
    
    /**
     * Channel Platform Executor to be used when reading to socket (Option value is a PlatformExecutor).
     */
    INPUT_EXECUTOR,
    
    /**
     * Automatically enable udp channel reading mode (true by default). If false, you have to enable the udp channel reading mode, using the
     * @link {@link UdpChannel#enableReading()} method.
     */
    ENABLE_READ,
    
    /**
     * The size of the socket send buffer. If not specified, the default system value is used.
     */
    SO_SNDBUF,
    
    /**
     * The size of the socket receive buffer. If not specified, the default system value is used.
     */
    SO_RCVBUF,
    
    /**
     * Use direct buffers when reading sockets. The direct buffer is passed to the Listener.
     * (Option value is a Boolean, Boolean.FALSE by default)
     */
    USE_DIRECT_BUFFER,
    
    /**
     * If the channel uses DTLS, then this option in set. (Option value is a {@link Security})
     */
    SECURITY,
    
    /**
     * Session timeout, used to invalidate udp client sessions. Option value is a long (millis). Default value: 5000 millis.
     */
    SESSION_TIMEOUT,
    
    /**
     * Whether the channel is a client or a server (used only for secure UDP channels)
     * (Options value is a Boolean, Boolean.FALSE by default)
     */
    IS_CLIENT,
    
    /**
     * Set the ability to bind non-local addresses. (Boolean, Boolean.FALSE is default)
     */
    IP_TRANSPARENT,
  }
  
  /**
   * Options used when opening an sctp channel.
   * @see ReactorProvider#sctpConnect(Reactor, SocketAddress, SctpClientChannelListener, Map)
   */
  public enum SctpClientOption {
    /**
     * Local address (value is a SocketAddress)
     */
    LOCAL_ADDR,
    
    /**
     * Secondary local address (value = InetAddress[])
     */
    SECONDARY_LOCAL_ADDRS,
    
    /**
     * Max output streams (value = Integer)
     */
    MAX_OUT_STREAMS,
    
    /**
     * Max input streams (value = Integer)
     */
    MAX_IN_STREAMS,
    
    /**
     * Context attached to the sctp client channel.
     */
    ATTACHMENT,
    
    /**
     * Connection timeout in millis (value = Long)
     */
    TIMEOUT,
    
    /**
     * Channel priority (Option value is an Integer).
     * @see AsyncChannel#MAX_PRIORITY
     * @see AsyncChannel#MIN_PRIORITY
     */
    PRIORITY,
    
    /**
     * Channel executor to be used when reading to socket (Option value is an PlatformExecutor).
     */
    INPUT_EXECUTOR,
        
    /**
     * The size of the socket receive buffer.
     */
    SO_RCVBUF,
    
    /**
     * The size of the socket send buffer.
     */
    SO_SNDBUF,
    
    /**
     * Use direct buffers when reading sockets. The direct buffer is passed to the Listener.
     * (Option value is a Boolean, Boolean.FALSE by default)
     */
    USE_DIRECT_BUFFER,
    
    SOCKET_OPTIONS,
    
    SECURITY,
    
    NO_DELAY,
    
    FRAGMENT_INTERLEAVE,
    
    DISABLE_FRAGMENTS,
    
    CLOSE_TIM,
    
    /**
     * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
     * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
     * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
     * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
     */
    LINGER,
  }
  
  /**
   * Options used when opening an sctp server channel.
   * @see ReactorProvider #tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, Map)
   */
  public enum SctpServerOption {
    /**
     * Secondary sctp server address (value = InetAddress[])
     */
    SECONDARY_LOCAL_ADDRS,
    
    /**
     * Max output streams (value = Integer)
     */
    MAX_OUT_STREAMS,
    
    /**
     * Max input streams (value = Integer)
     */
    MAX_IN_STREAMS,
    
    /**
     * Channel priority (Option value is an Integer).
     * @see AsyncChannel#MAX_PRIORITY
     * @see AsyncChannel#MIN_PRIORITY
     */
    PRIORITY,
    
    /**
     * Context attached to the sctp server channel.
     */
    ATTACHMENT,
    
    /**
     * The maximum number number (Integer) of pending associations (default=1024)
     */
    BACKLOG,
    
    /**
     * The size of the socket receive buffer.
     */
    SO_RCVBUF,
    
    /**
     * The size of the socket send buffer.
     */
    SO_SNDBUF,
    
    /**
     * Automatically enable sctp server channel reading mode (true by default). If false, you have to enable the udp channel reading mode, using the
     * @link {@link SctpServerChannel#enableReading} method.
     */
    ENABLE_READ,
    
    /**
     * Use direct buffers when reading sockets. The direct buffer is passed to the Listener.
     * (Option value is a Boolean, Boolean.FALSE by default)
     */
    USE_DIRECT_BUFFER,
    
    /**
     * Timeout in millis (Long) used when disable server socket accepts when there is not enough resources available. 
     * For instance, if the system has too many open files, the server socket won't accept incoming connection requests 
     * during the specified timeout. The property value is a Long object (1000 by default). 0 value means the default value must be
     * used (1 second by default).
     */
    DISABLE_ACCEPT_TIMEOUT,
    
    SOCKET_OPTIONS,
    
    SECURITY,
    
    FRAGMENT_INTERLEAVE,
    
    DISABLE_FRAGMENTS,
    
    /**
     * Controls the action taken when unsent data is queued on the socket and a method to close the socket is invoked. 
     * it represents a Long timeout value, in milliseconds, known as the linger interval. The linger interval is the timeout for the close method to complete 
     * while the operating system attempts to transmit the unsent data. Socket will be forcibly closed after the timeout expires or when all unsent data is flushed.
     * 0 means no linger interval is used. By default, the linger option is set to 5000 milliseconds.
     */
    LINGER,
  }
  
  /**
   * Creates a new reactor. The reactor is created but is not started, and you
   * have to start it using the {@link Reactor#start()} method. The created reactor
   * will be registered in the OSGi service registry.
   * 
   * @param name the Reactor name.
   * @return the created reactor. The returned reactor is not started.
   */
  public abstract Reactor create(String name);
  
  /**
   * Gets a Reactor given its alias name.
   * @param name the reactor name (or alias)
   * @return the Reactor given its alias name
   */
  public abstract Reactor getReactor(String name);
  
  /**
   * Alias an existing reactor.
   * 
   * @param name A case insensitive reactor alias name
   * @param reactor the reactor to be aliased (will be unaliased if null)
   */
  public abstract void aliasReactor(String name, Reactor reactor);
  
  /**
   * Gets the reactor which is running in the current thread, or null.
   * 
   * @return the reactor which is running in the current thread, or null
   */
  public abstract Reactor getCurrentThreadReactor();
  
  /**
   * Opens a Tcp channel asynchronously.
   * 
   * @param reactor the reactor used to manage the channel
   * @param to the remote address to connect to
   * @param listener the channel listener used to react to channel io events
   * @param opts the channel options, or null (Keys must match ReactorProvider.TcpClientOption enum)
   */
  public abstract void tcpConnect(Reactor reactor, InetSocketAddress to, TcpClientChannelListener listener,
                                  Map<?, Object> opts);
  
  /**
   * Create a new Server Channel.
   * 
   * @param reactor the reactor used to manage the channel
   * @param listenAddr the server address
   * @param listener the server channel listener usd to react to channel io events.
   *        The {@link TcpServerChannelListener#serverConnectionOpened(TcpServerChannel)}
   *        and {@link TcpServerChannelListener#serverConnectionFailed(TcpServerChannel, Throwable)}
   *        are not used by this method.
   * @param opts the server channel options, or null
   * @return the tcp server channel.
   * @throws IOException if an I/O exception occurs.
   */
  public abstract TcpServerChannel tcpAccept(Reactor reactor, InetSocketAddress listenAddr,
                                             TcpServerChannelListener listener,
                                             Map<TcpServerOption, Object> opts) throws IOException;
  
  /**
   * Binds a Datagram Channel.
   * 
   * @param reactor the reactor used to manage the channel
   * @param local the local address to bind to
   * @param listener the listener used to react to channel io events
   * @param opts the udp channel options, or null
   * @return the bound udp channel
   * @throws IOException if the udp channel could not be bound
   */
  public abstract UdpChannel udpBind(Reactor reactor, InetSocketAddress local, UdpChannelListener listener,
                                     Map<UdpOption, Object> opts) throws IOException;
  
  /**
   * Creates a new SCTP client channel.
   * 
   * @param reactor the reactor to use
   * @param to the destination address
   * @param listener the channel listener
   * @param options the channel options
   */
  public abstract void sctpConnect(Reactor reactor, SocketAddress to, SctpClientChannelListener listener,
                                   Map<SctpClientOption, Object> options);
  
  /**
   * Creates a new SCTP server channel.
   * @param reactor the reactor to use
   * @param local The local server address
   * @param listener the sctp channel listener
   * @param options the channel options, or null
   * 
   * @return the bound SCTP Server channel 
   * @throws IOException if an I/O exception occurs.
   */
  public abstract SctpServerChannel sctpAccept(Reactor reactor, SocketAddress local,
                                               SctpServerChannelListener listener,
                                               Map<SctpServerOption, Object> options) throws IOException;
    
}
