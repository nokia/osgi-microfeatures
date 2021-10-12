// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.local;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.util.sctp.*;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.AsyncChannel.WriteBlockedPolicy;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpServerOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpServerOption;
import alcatel.tess.hometop.gateways.reactor.SctpAssociation;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import alcatel.tess.hometop.gateways.utils.Log;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxFactory.ConnectionListener;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHandler.SctpAddressEvent;
import com.nextenso.mux.impl.MuxConnectionImpl;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.socket.SocketManager;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.util.SocketManagerImpl;
import com.nextenso.mux.util.TimeoutManager;

/**
 * Creates a new ReactorConnection, allowing to simulate a remote stack locally
 * (inside the JVM).
 */
@SuppressWarnings("unchecked")
public class ReactorConnection implements MuxConnection, TcpClientChannelListener, TcpServerChannelListener,
    UdpChannelListener, SctpClientChannelListener, SctpServerChannelListener {
  
  private final static String SYSTEM_CONNECT_TIMEOUT = "reactorConnection.connectionTimeout";
  private final static String CONNECT_TIMEOUT = "15000"; // 15 seconds by default
  protected boolean _ipv6Support;
  private boolean _byteBufferMode;
  protected Reactor _reactor;
  protected ReactorProvider _provider;
  protected Log _logger;
  protected volatile boolean _open;
  private int _stackAppId;
  private String _stackAppName;
  private String _stackInstance = "Reactor";
  private SocketManager _socketManager;
  private TimeoutManager _timeoutManager;
  protected MuxHandler _handler;
  private Object[] _attributes;
  protected TcpMessageParser _tcpParser;
  private Map<InetSocketAddress, Integer> _sockids = new Hashtable<InetSocketAddress, Integer>();
  private ConcurrentHashMap<Integer, AsyncChannel> _channels = new ConcurrentHashMap<Integer, AsyncChannel>();
  private ConcurrentHashMap<Integer, TcpServerChannel> _serverChannels = new ConcurrentHashMap<Integer, TcpServerChannel>();
  private Object _attachment;
  private MuxFactory.ConnectionListener _cnxListener;
  private int _id;
  protected final static AtomicInteger SOCK_ID = new AtomicInteger(1);
  private static AtomicInteger ID = new AtomicInteger(1);
  private ConcurrentHashMap<Integer, SctpServerChannel> _sctpServerChannels = new ConcurrentHashMap<Integer, SctpServerChannel>();
  private boolean _threadSafe;
  private final PlatformExecutors _execs = PlatformExecutors.getInstance();
  private InetSocketAddress _remoteAddr;
  private String _stackAddr = "127.0.0.1";
  private int _stackPort = -1;

  private static class ListenAttachment {
    volatile long _listenId;
    volatile int sockId;
    volatile boolean _secure;
    
    protected ListenAttachment(long listenId, boolean secure) {
      _listenId = listenId;
      _secure = secure;
    }
  }
  
  private static class ConnectAttachment {
    volatile long _connectionId;
    
    ConnectAttachment(long connectionId) {
      _connectionId = connectionId;
    }
  }
  
  private static class ChannelAttachment {
    volatile ByteBuffer _buf; // used for fragmented messages
    volatile int _sockId;
    
    ChannelAttachment(int sockId) {
      _sockId = sockId;
    }
  }
  
  /**
   * Creates a new Reactor Connection.
   * 
   * @param reactor The reactor to be used by this connection
   * @param mh the mux handler that will be notified about connection events
   * @param cnxListener the connection listener tracking socket open/close
   *          events
   * @param stackAppId the stack id of the connected stack
   * @param stackAppName the stack name of the connected stack
   * @param parser the tcp message parsed, used to parse incoming messages
   * @param logger the logger used by this connection
   */
  public ReactorConnection(Reactor reactor, MuxHandler mh, ConnectionListener cnxListener, int stackAppId,
                           String stackAppName, TcpMessageParser parser, Logger logger) {
    this(reactor, mh, cnxListener, stackAppId, stackAppName, "Reactor", parser, logger);
  }
  
  /**
   * Creates a new Reactor connection.
   * 
   * @param reactor The reactor to be used by this connection
   * @param mh the mux handler that will be notified about connection events
   * @param cnxListener the connection listener tracking socket open/close
   *          events
   * @param stackAppId the id of the connected stack
   * @param stackAppName the name of the connected stack
   * @param stackInstance the instance name of the connected stack
   * @param parser the tcp message parsed, used to parse incoming messages
   * @param logger the logger used by this connection
   */
  public ReactorConnection(Reactor reactor, MuxHandler mh, ConnectionListener cnxListener, int stackAppId,
                           String stackAppName, String stackInstance, TcpMessageParser parser, Logger logger) {
    _reactor = reactor;
    _handler = mh;
    _cnxListener = cnxListener;
    _stackAppId = stackAppId;
    _stackAppName = stackAppName;
    _stackInstance = stackInstance;
    _tcpParser = parser;
    _logger = Log.getLogger(logger);
    _socketManager = new SocketManagerImpl();
    _provider = ServiceLoader.getService(ReactorProvider.class);
    @SuppressWarnings("rawtypes")
    java.util.Hashtable muxConf = _handler.getMuxConfiguration();
    _ipv6Support = ((Boolean) muxConf.get(MuxHandler.CONF_IPV6_SUPPORT)).booleanValue();
    _byteBufferMode = ((Boolean) muxConf.get(MuxHandler.CONF_USE_NIO)).booleanValue();
    _threadSafe = ((Boolean) muxConf.get(MuxHandler.CONF_THREAD_SAFE)).booleanValue();
    _id = ID.getAndIncrement();
  }

    public String toString (){
	return new StringBuilder ().append ("ReactorConnection[").append (_stackInstance != null ? _stackInstance : "-def-").append (']').toString ();
    }
  
  void setRemoteAddress(InetSocketAddress to) {
    _remoteAddr = to;
    if (_remoteAddr != null) {
      _stackAddr = _remoteAddr.getAddress().getHostAddress();
      _stackPort = _remoteAddr.getPort();
    }
  }
  
  // Open the mux handler (invoke muxHandler.muxOpened method).
  void open() {
    synchronized (this) {
      if (_open) {
        return;
      }
      _open = true;
    }
    _reactor.scheduleNow(new Runnable() {
      
      @Override
      public void run() {
        _logger.info("ReactorConnection starting");
        try {
          _handler.muxOpened(ReactorConnection.this);
        } catch (Throwable t) {
          _open = false;
          _logger.warn("exception while calling muxOpened method on mux handler %s", t, _stackAppName);
        }
      }
    });
  }
  
  /**
   * Close this mux connection.
   */
  public void close() {
    if (_open) {
      _open = false;
      _reactor.scheduleNow(new Runnable() {
        
        @Override
        public void run() {
          try {
            _handler.muxClosed(ReactorConnection.this);
          } catch (Throwable t) {
            _logger.warn("MuxHandler close method has thrown the following exception:", t);
          }
        }
      });
      
      //            // close tcp/udp channels
      //            int clients = _channels.size();
      //            if (clients > 0)
      //            {
      //                _logger.info("closing tcp/udb channels: " + clients);
      //                for (final Channel channel : _channels.values())
      //                {
      //                    // connectionClosed will be called back, and each callback will
      //                    // then check if no more channels are open: and if no more channels
      //                    // are remaining, then the muxHandler will be closed.
      //                    channel.close();
      //                }
      //            }            
      //            
      //            // close tcp server channels
      //            int servers = _serverChannels.size();
      //            if (servers > 0)
      //            {
      //                _logger.info("closing tcp server channels: " + servers);
      //                for (final Channel channel : _serverChannels.values())
      //                {
      //                    // connectionClosed will be called back, and each callback will
      //                    // then check if no more channels are open: and if no more channels
      //                    // are remaining, then the muxHandler will be closed.
      //                    channel.close();
      //                }
      //            }
      //            
      //            // close sctp server channels
      //            int sctpServers = _sctpServerChannels.size();
      //            if (sctpServers > 0)
      //            {
      //                _logger.info("closing stcp server channels: " + sctpServers);
      //                for (final Channel channel : _sctpServerChannels.values())
      //                {
      //                    // connectionClosed will be called back, and each callback will
      //                    // then check if no more channels are open: and if no more channels
      //                    // are remaining, then the muxHandler will be closed.
      //                    channel.close();
      //                }
      //            }
      //            
      //            if (clients == 0 && servers == 0 && sctpServers == 0)
      //            {
      //                checkMuxClosed();
      //            }
    }
  }
  
  MuxFactory.ConnectionListener getConnectionListener() {
    return _cnxListener;
  }
  
  /**
   */
  public InetSocketAddress getLocalAddress() {
    return null;
  }
  
  public InetSocketAddress getRemoteAddress() {
    return _remoteAddr;
  }
  
  void setLogger(Logger logger) {
    _logger = Log.getLogger(logger);
  }
  
  Reactor getReactor() {
    return _reactor;
  }
  
  /**
   * Specifies if the MuxConnection is opened. <br/>
   * A MuxConnection is opened until muxClosed() is called on the MuxHandler.
   */
  public boolean isOpened() {
    return _open;
  }
  
  /**
   * Close this mux connection.
   */
  public void shutdown() {
    close();
  }
  
  /**
   * Returns this MuxConnection Id.
   * 
   * @return the MuxConnection Id.
   */
  public int getId() {
    return _id;
  }
  
  /**
   * Returns the input channel from which the last passed data by the
   * MuxConnection is originated. This value has a meaning when the double
   * socket mux function is activated. It returns always 0, when the double
   * socket is not activated. You can have the following returned values: 0 -
   * the data comes from the channel/socket with lowest priority, 1 - the data
   * comes from the channel/socket with higher priority, Note: This interface
   * can evolve, to offer more distinguished values in the future.
   */
  public int getInputChannel() {
    return 0;
  }
  
  public boolean setKeepAlive(int interval, int idleFactor) {
    throw new UnsupportedOperationException("method setKeepAlive() is not supported by ReactorConnection");
  }
  
  public boolean useKeepAlive() {
    return false;
  }
  
  /**
   * Returns the Stack Application Id.
   * 
   * @return the Stack Application Id.
   */
  public int getStackAppId() {
    return _stackAppId;
  }
  
  /**
   * Returns the Stack Application name.
   * 
   * @return the Stack Application name.
   */
  public String getStackAppName() {
    return _stackAppName;
  }
  
  /**
   * Returns the Stack instance name.
   * 
   * @return the Stack instance name.
   */
  public String getStackInstance() {
    return _stackInstance;
  }
  
  /**
   * Returns the Stack host name.
   * 
   * @return the Stack host name.
   */
  public String getStackHost() {
    return "localhost";
  }
  
  /**
   * Returns the Stack address.
   * 
   * @return the Stack address.
   */
  public String getStackAddress() {
    return _stackAddr;
  }
  
  /**
   * Returns the Stack port number.
   * 
   * @return the Stack port number.
   */
  public int getStackPort() {
    return _stackPort;
  }
  
  /************************* attributes *************************/
  
  /**
   * Returns the SocketManager.
   */
  public SocketManager getSocketManager() {
    return _socketManager;
  }
  
  /**
   * Sets the TimeoutManager.
   */
  public void setTimeoutManager(TimeoutManager manager) {
    _timeoutManager = manager;
  }
  
  /**
   * Returns the TimeoutManager.
   */
  public TimeoutManager getTimeoutManager() {
    return _timeoutManager;
  }
  
  /**
   * Returns the associated MuxHandler.
   */
  public MuxHandler getMuxHandler() {
    return _handler;
  }
  
  /**
   * Sets the attributes.
   * 
   * @deprecated
   */
  @Deprecated
  public void setAttributes(Object[] attributes) {
    _attributes = attributes;
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#getAttributes()
   * @deprecated
   */
  @Deprecated
  public Object[] getAttributes() {
    return _attributes;
  }
  
  public void attach(Object attachment) {
    _attachment = attachment;
  }
  
  public <T> T attachment() {
    return (T) _attachment;
  }
  
  /************************* mux *************************/
  
  /**
   * Notifies the Stack that the MuxHandler is ready. <br/>
   * The Stack behavior is protocol-specific.
   */
  public boolean sendMuxStart() {
    _logger.debug("sendMuxStart: do nothing -> return true");
    return true;
  }
  
  /**
   * Notifies the Stack that the MuxHandler is shutting down. <br/>
   * The Stack behavior is protocol-specific.
   */
  public boolean sendMuxStop() {
    _logger.debug("sendMuxStop: do nothing -> return true");
    return true;
  }
  
  /**
   * Sends opaque mux data.
   */
  public boolean sendMuxData(MuxHeader header, byte[] data, int off, int len, boolean copy) {
    if (data != null)
      return sendMuxData(header, copy, ByteBuffer.wrap(data, off, len));
    else
      return sendMuxData(header, copy, (ByteBuffer) null);
  }
  
  /**
   * Sends opaque mux data using a nio byte buffer.
   * 
   * @deprecated
   */
  @Deprecated
  public boolean sendMuxData(MuxHeader header, ByteBuffer buf) {
    return sendMuxData(header, true, buf);
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendMuxData(com.nextenso.mux.MuxHeader, boolean, java.nio.ByteBuffer[])
   */
  public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... buf) {
    return false;
  }
  
  /**
   * We don't support mux identification when using the reactor connection.
   */
  public boolean sendMuxIdentification(MuxIdentification id) {
    return true;
  }
  
  /************************* tcp *************************/
  
  /**
   * Sends a Tcp Socket Listen request. secure stands for tls
   */
  public boolean sendTcpSocketListen(long listenId, int localIP, int localPort, boolean secure) {
    String ip = (localIP == 0) ? "0.0.0.0" : MuxUtils.getIPAsString(localIP);
    return sendTcpSocketListen(listenId, ip, localPort, secure);
  }
  
  public boolean sendTcpSocketListen(long listenId, String localIP, int localPort, boolean secure) {
    _logger.debug("sendTcpSocketListen ip=%s, port=%d, listenId=%d, secure=%b", localIP, localPort, listenId,
                  secure);
    
    if (!_open)
      return false;
    
    final ListenAttachment attachment = new ListenAttachment(listenId, secure);
    final InetSocketAddress address = new InetSocketAddress(localIP, localPort);
    Map<TcpServerOption, Object> opts = new HashMap<TcpServerOption, Object>();
    opts.put(TcpServerOption.ATTACHMENT, attachment);
    opts.put(TcpServerOption.SECURE, secure);
    opts.put(TcpServerOption.TCP_NO_DELAY, Boolean.TRUE);
    
    try {
      final TcpServerChannel server = _provider.tcpAccept(_reactor, address, this, opts);
      _reactor.schedule(new Runnable() {
        
        @Override
        public void run() {
          _logger.debug("serverConnectionOpened: server=%s, listenId=%d", server, attachment._listenId);
          int sockId = SOCK_ID.getAndIncrement();
          attachment.sockId = sockId;
          _serverChannels.put(sockId, server);
          
          InetSocketAddress local = server.getLocalAddress();
          
          if (!_ipv6Support) {
            _handler.tcpSocketListening(ReactorConnection.this, sockId,
                                        MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                                        local.getPort(), attachment._secure, // overrides
                                        // server.isSecure()
                                        attachment._listenId, 0);
          } else {
            _handler.tcpSocketListening(ReactorConnection.this, sockId, local.getAddress().getHostAddress(),
                                        local.getPort(), attachment._secure, // overrides
                                        // server.isSecure()
                                        attachment._listenId, 0);
          }
        }
      });
    } catch (final Throwable e) {
      _reactor.schedule(new Runnable() {
        
        @Override
        public void run() {
          if (_logger.isDebugEnabled()) {
            _logger.debug("tcpSocketListen failed on addr " + address, e);
          }
          if (_ipv6Support) {
            _handler.tcpSocketListening(ReactorConnection.this, 0, "", 0, false, attachment._listenId,
                                        MuxUtils.ERROR_CONNECTION_REFUSED);
          } else {
            _handler.tcpSocketListening(ReactorConnection.this, 0, 0, 0, false, attachment._listenId,
                                        MuxUtils.ERROR_CONNECTION_REFUSED);
          }
        }
      });
    }
    return true;
  }
  
  /**
   * Sends a Tcp Socket Connect request.
   */
  public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, int localIP,
                                      int localPort, boolean secure) {
    return sendTcpSocketConnect(connectionId, remoteHost, remotePort, MuxUtils.getIPAsString(localIP),
                                localPort, secure);
  }
  
  public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
                                      int localPort, boolean secure) {
    if (!_open)
      return false;
    
    InetSocketAddress from;
    
    if (localIP == null || localIP.equals("")) {
      from = new InetSocketAddress(localPort);
    } else {
      from = new InetSocketAddress(localIP, localPort);
    }
    
    Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
    if (from != null) {
      opts.put(TcpClientOption.FROM_ADDR, from);
    }
    opts.put(TcpClientOption.ATTACHMENT, Long.valueOf(connectionId));
    opts.put(TcpClientOption.TIMEOUT, new Long(System.getProperty(SYSTEM_CONNECT_TIMEOUT, CONNECT_TIMEOUT)));
    opts.put(TcpClientOption.SECURE, secure);
    opts.put(TcpClientOption.TCP_NO_DELAY, Boolean.TRUE);
    
    if (_threadSafe) {
      _logger.info("tcp client channel (" + remoteHost + ":" + remotePort + ")"
          + " will be handled in the thread pool");
      opts.put(TcpClientOption.INPUT_EXECUTOR,
               _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor(), "tcpout"));
    }
    _provider.tcpConnect(_reactor, new InetSocketAddress(remoteHost, remotePort), this, opts);
    return true;
  }
  public boolean sendTcpSocketConnect(long connectionId, String remoteHost, int remotePort, String localIP,
				      int localPort, boolean secure, Map<String, String> params) {
    // params not implemented
    return sendTcpSocketConnect (connectionId, remoteHost, remotePort, localIP, localPort, secure);
  }
  public boolean sendTcpSocketParams (int sockId, Map<String, String> param){
    // not implemented
    return true;
  }
  /**
   * Sends a Tcp Socket Close request.
   */
  public boolean sendTcpSocketReset(int sockId) {
    if (!_open) {
      return false;
    }
    
    // Close client socket, if its a client one.
    AsyncChannel channel = _channels.get(sockId);
    if (channel != null) {
      channel.shutdown();
      return true;
    }
    return false;
  }
  /**
   * Sends a Tcp Socket Close request.
   */
  public boolean sendTcpSocketClose(int sockId) {
    if (!_open) {
      return false;
    }
    
    // Close client socket, if its a client one.
    AsyncChannel channel = _channels.get(sockId);
    if (channel != null) {
      channel.close();
      return true;
    }
    
    // Close server socket, if its a server one.
    TcpServerChannel serverChannel = _serverChannels.get(sockId);
    if (serverChannel != null) {
      serverChannel.close();
      // Close all sockets accepted by this server socket.
      Iterator<AsyncChannel> it = _channels.values().iterator();
      while (it.hasNext()) {
        channel = it.next();
        if (channel.getLocalAddress().equals(serverChannel.getLocalAddress())) {
          channel.close(); // Will callback us in connectionClosed(TcpChannel cnx
        }
      }
      return true;
    }
    
    return false;
  }
  
  @Override
  public boolean sendTcpSocketAbort(int sockId) {
    return sendTcpSocketClose(sockId);
  }
  
  /**
   * Sends Tcp data.
   */
  public boolean sendTcpSocketData(int sockId, byte[] data, int off, int len, boolean copy) {
    if (!_open)
      return false;
    if (data == null)
      return true;
    return sendTcpSocketData(sockId, copy, ByteBuffer.wrap(data, off, len));
  }
  
  public boolean sendTcpSocketData(int sockId, boolean copy, ByteBuffer ... bufs) {
    if (!_open)
      return false;
    if (bufs == null)
      return true;
    TcpChannel channel = (TcpChannel) _channels.get(sockId);
    if (channel != null) {
      try {
        channel.send(bufs, copy);
      } catch (Throwable e) {
        _logger.error("Failed to send Tcp data", e);
      }
    }
    return true;
  }
  
  /************************* sctp *************************/
  
  @Override
  public boolean sendSctpSocketListen(long listenId, String[] localAddrs, int localPort, int maxOutStreams,
                                      int maxInStreams, boolean secure) {
      //
      // secure is ignored in this implementation (no DTLS in legacy jdiameter)
      //
    if (localAddrs == null || localAddrs.length < 1) {
      throw new IllegalArgumentException(
          "Invalid localAddrs parameter (must not be a non null/ non empty array)");
    }
    
    _logger.info("sctp server listening on localAddrs=%s, localPort=%d, listenId=%d, open=%b",
                  Arrays.toString(localAddrs), localPort, listenId, _open);
    
    if (!_open)
      return false;
    
    SocketAddress primaryLocalAddr;
    InetAddress[] secondaryLocalAddrs;
    
    try {
      primaryLocalAddr = getPrimaryLocalAddress(localAddrs, localPort);
      secondaryLocalAddrs = getSecondaryLocalAddresses(localAddrs);
    } catch (UnknownHostException e) {
      _logger.warn("Could not listen on sctp server socket", e);
      return false;
    }
    
    ListenAttachment attachment = new ListenAttachment(listenId, false);
    
    try {
      Map<SctpServerOption, Object> opts = new HashMap<ReactorProvider.SctpServerOption, Object>();
      opts.put(SctpServerOption.MAX_OUT_STREAMS, maxOutStreams);
      opts.put(SctpServerOption.MAX_IN_STREAMS, maxInStreams);
      opts.put(SctpServerOption.ATTACHMENT, attachment);
      if (secondaryLocalAddrs != null) {
        opts.put(SctpServerOption.SECONDARY_LOCAL_ADDRS, secondaryLocalAddrs);
      }
      
      SctpServerChannel server = _provider.sctpAccept(_reactor, primaryLocalAddr, this, opts);
      
      attachment = server.attachment();
      _logger.debug("sendTcpSocketListen: server=%s, listenId=%d", server, attachment._listenId);
      int sockId = SOCK_ID.getAndIncrement();
      attachment.sockId = sockId;
      _sctpServerChannels.put(sockId, server);
      
      Set<SocketAddress> allLocaladdrs = server.getAllLocalAddresses();
      if (allLocaladdrs.size() == 0) {
        _logger.error("Could not get list of sctp server bound addresses");
        return false;
      }
      
      String[] locals = new String[allLocaladdrs.size()];
      int index = 0;
      int port = -1;
      for (SocketAddress sa : allLocaladdrs) {
        port = ((InetSocketAddress) sa).getPort();
        locals[index++] = ((InetSocketAddress) sa).getAddress().getHostAddress();
      }
      
      if (port == -1) {
        _logger.error("Could not get sctp server bound port number");
        return false;
      }
      
      _handler.sctpSocketListening(this, sockId, attachment._listenId, locals, port, false, 0);      
      return true;
    } catch (Throwable t) {
      _logger.error("Could not listen on sctp server socket", t);
      return false;
    }
  }
  
  @Override
  public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
                                       String[] localAddrs, int localPort, int maxOutSreams, int maxInStreams, boolean secure) {
      //
      // secure is ignored in this implementation (no DTLS in legacy jdiameter)
      //
    if (!_open) {
      _logger.info("sctp socket connect dropped: reactor connection not opened");
      return false;
    }
    
    _logger.info("sctp socket connect: remote=%s, port=%d, localAddrs=%s, localPort=%d",
                 remoteHost, remotePort, localAddrs != null ? Arrays.toString(localAddrs): "", localPort);
    
    SocketAddress primaryLocalAddr;
    InetAddress[] secondaryLocalAddrs;
    SocketAddress remoteAddr;
    try {
      primaryLocalAddr = getPrimaryLocalAddress(localAddrs, localPort);
      secondaryLocalAddrs = getSecondaryLocalAddresses(localAddrs);
      remoteAddr = new InetSocketAddress(InetAddress.getByName(remoteHost), remotePort);
    } catch (UnknownHostException e) {
      _logger.warn("Could not listen on sctp server socket", e);
      return false;
    }
    
    Map<SctpClientOption, Object> opts = new HashMap<ReactorProvider.SctpClientOption, Object>();
    opts.put(SctpClientOption.MAX_OUT_STREAMS, maxOutSreams);
    opts.put(SctpClientOption.MAX_IN_STREAMS, maxInStreams);
    opts.put(SctpClientOption.ATTACHMENT, new ConnectAttachment(connectionId));
    opts.put(SctpClientOption.TIMEOUT, new Long(System.getProperty(SYSTEM_CONNECT_TIMEOUT, CONNECT_TIMEOUT)));
    
    if (primaryLocalAddr != null) {
      opts.put(SctpClientOption.LOCAL_ADDR, primaryLocalAddr);
    }
    if (secondaryLocalAddrs != null) {
      opts.put(SctpClientOption.SECONDARY_LOCAL_ADDRS, secondaryLocalAddrs);
    }
    
    if (_threadSafe) {
      _logger.debug("sctp client channel (" + remoteHost + ":" + remotePort + ")"
          + " will be handled in the thread pool");
      opts.put(SctpClientOption.INPUT_EXECUTOR,
               _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor(), "sctpout"));
    }
    
    _provider.sctpConnect(_reactor, remoteAddr, this, opts);
    return true;
  }
    public boolean sendSctpSocketConnect(long connectionId, String remoteHost, int remotePort,
					 String[] localAddrs, int localPort, int maxOutStreams, int maxInStreams, boolean secure, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions, Map<String, String> params){
	//
	// sctpSocketOptions & params are ignored
	//
	return sendSctpSocketConnect (connectionId, remoteHost, remotePort, localAddrs, localPort, maxOutStreams, maxInStreams, secure);
    }
    public boolean sendSctpSocketOptions(int sockId, java.util.Map<SctpSocketOption, SctpSocketParam> sctpSocketOptions){
	// not supported
	return true;
    }
    public boolean sendSctpSocketParams (int sockId, Map<String, String> params){
	// not supported
	return true;
    }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendSctpSocketData(int,
   *      java.lang.String, boolean, boolean, int, int, long, boolean,
   *      java.nio.ByteBuffer[])
   */
  @Override
  public boolean sendSctpSocketData(int sockId, String addr, boolean unordered, boolean complete,
                                    int ploadPID, int streamNumber, long timeToLive, boolean copy,
                                    ByteBuffer ... data) {
    if (!_open)
      return false;
    if (data == null)
      return true;
    SctpChannel channel = (SctpChannel) _channels.get(sockId);
    if (channel != null) {
      InetSocketAddress isa = null;
      if (addr != null) {
        int port = channel.getRemotePort();
        isa = new InetSocketAddress(addr, port);
      }
      channel.send(copy, isa, complete, ploadPID, streamNumber, timeToLive, unordered, data);
      return true;
    }
    
    return false;
  }

  @Override
  public boolean sendSctpSocketReset(int sockId) {
    if (!_open) {
      return false;
    }
    
    // Close client socket, if its a client one.
    SctpChannel channel = (SctpChannel) _channels.get(sockId);
    if (channel != null) {
      channel.shutdown();
      return true;
    }
    return true;
  }
    
  @Override
  public boolean sendSctpSocketClose(int sockId) {
    if (!_open) {
      return false;
    }
    
    // Close client socket, if its a client one.
    SctpChannel channel = (SctpChannel) _channels.get(sockId);
    if (channel != null) {
      channel.close(); // graceful close
      return true;
    }
    
    // Close server socket, if its a server one.
    SctpServerChannel serverChannel = _sctpServerChannels.get(sockId);
    if (serverChannel != null) {
      serverChannel.close();
      // Close all sockets accepted by this server socket.
      Iterator<AsyncChannel> it = _channels.values().iterator();
      while (it.hasNext()) {
        AsyncChannel asyncChannel = it.next();
        if (asyncChannel instanceof SctpChannel) {
          InetSocketAddress local;
          InetSocketAddress serverLocal;
          channel = (SctpChannel) asyncChannel;
          try {
            local = getPrimaryAddress(channel.getLocalAddresses());
            serverLocal = getPrimaryAddress(serverChannel.getAllLocalAddresses());
          } catch (IOException e) {
            _logger.error("Could not close sctp sockets", e);
            return false;
          }
          
          if (local.equals(serverLocal)) {
            channel.close(); // graceful; will callback us in connectionClosed(TcpChannel cnx
          }
        }
      }
      return true;
    }
    
    return false;
  }
  
  /************************* udp *************************/
  
  /**
   * Sends an Udp Socket Bind request.
   */
  public boolean sendUdpSocketBind(long bindId, int localIP, int localPort, boolean shared) {
    String ip = (localIP == 0) ? "0.0.0.0" : MuxUtils.getIPAsString(localIP);
    return sendUdpSocketBind(bindId, ip, localPort, shared);
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendUdpSocketBind(long,
   *      java.lang.String, int, boolean)
   */
  public boolean sendUdpSocketBind(long bindId, String localIP, int localPort, boolean shared) {
    if (!_open) {
      return false;
    }
    _provider.newUdpChannel(new InetSocketAddress(localIP, localPort), this, _reactor, Long.valueOf(bindId),
                            _logger.getLogger());
    return true;
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendUdpSocketClose(int)
   */
  public boolean sendUdpSocketClose(int sockId) {
    if (!_open) {
      return false;
    }
    
    AsyncChannel channel = _channels.get(sockId);
    if (channel != null) {
      channel.close();
      return true;
    }
    return false;
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendUdpSocketData(int, int, int, int,
   *      int, byte[], int, int, boolean)
   */
  public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
                                   byte[] data, int off, int len, boolean copy) {
    return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
                             MuxUtils.getIPAsString(virtualIP), virtualPort, data, off, len, copy);
  }
  
  /**
   * @see com.nextenso.mux.MuxConnection#sendUdpSocketData(int,
   *      java.lang.String, int, java.lang.String, int, byte[], int, int,
   *      boolean)
   */
  public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                   int virtualPort, byte[] data, int off, int len, boolean copy) {
    if (len > 0 && data != null) {
      return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, copy,
                               ByteBuffer.wrap(data, off, len));
    }
    return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, false, (ByteBuffer[]) null);
  }
  
  public boolean sendUdpSocketData(int sockId, int remoteIP, int remotePort, int virtualIP, int virtualPort,
                                   boolean copy, ByteBuffer ... bufs) {
    if (bufs != null) {
      return sendUdpSocketData(sockId, MuxUtils.getIPAsString(remoteIP), remotePort,
                               MuxUtils.getIPAsString(virtualIP), virtualPort, copy, bufs);
    }
    return sendUdpSocketData(sockId, remoteIP, remotePort, virtualIP, virtualPort, false, (ByteBuffer[]) null);
  }
  
  public boolean sendUdpSocketData(int sockId, String remoteIP, int remotePort, String virtualIP,
                                   int virtualPort, boolean copy, ByteBuffer ... bufs) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("sendUdpSocketData: open=%b, data=%s", _open, Arrays.toString(bufs));
    }
    if (!_open) {
      return false;
    }
    if (bufs == null) {
      return true;
    }
    
    UdpChannel channel = (UdpChannel) _channels.get(sockId);
    if (_logger.isDebugEnabled()) {
      _logger.debug("sendUdpSocketData: channel=%s", channel);
    }
    if (channel != null) {
      channel.send(new InetSocketAddress(remoteIP, remotePort), copy, bufs);
    }
    return true;
  }
  
  /************************* dns *************************/
  
  /**
   * Sends a Dns getByAddr request.
   */
  public boolean sendDnsGetByAddr(long reqId, String addr) {
    throw new UnsupportedOperationException("method sendDnsGetByAddr() is not supported by ReactorConnection");
  }
  
  /**
   * Sends a Dns getByName request.
   */
  public boolean sendDnsGetByName(long reqId, String name) {
    throw new UnsupportedOperationException("method sendDnsGetByName() is not supported by ReactorConnection");
  }
  
  /************************* release *************************/
  
  /**
   * Sends a Session release request.
   */
  public boolean sendRelease(final long sessionId) {
    _reactor.schedule(new Runnable() {
      
      public void run() {
        _handler.releaseAck(ReactorConnection.this, sessionId);
      }
    });
    return true;
  }
  
  /**
   * Sends a Session release confirm/cancel.
   */
  public boolean sendReleaseAck(long sessionId, boolean confirm) {
    return true;
  }
  
  /******************* read controls ************************/
  
  public void disableRead(int sockId) {
    TcpChannel channel = (TcpChannel) _channels.get(sockId);
    if (channel != null) {
      channel.disableReading();
    }
  }
  
  public void enableRead(int sockId) {
    TcpChannel channel = (TcpChannel) _channels.get(sockId);
    if (channel != null) {
      channel.enableReading();
    }
  }
  
  /******************* TcpChannelListener ******************/
  
  public void connectionClosed(TcpChannel cnx) {
    int sockId = ((Integer) cnx.attachment()).intValue();
    AsyncChannel channel = _channels.remove(sockId);
    if (channel != null) {
      _handler.tcpSocketClosed(this, sockId);
    }
    //checkMuxClosed();
  }
  
  public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
    if (!_open) {
      msg.position(msg.position() + msg.remaining());
      return 0;
    }
    
    int sockId = ((Integer) cnx.attachment()).intValue();
    if (_byteBufferMode) {
      int currLimit = msg.limit();
      do {
        int len = _tcpParser.parse(msg);
        if (len < 0)
          return -len;
        msg.limit(msg.position() + len);
        _handler.tcpSocketData(this, sockId, -1, msg);
        msg.limit(currLimit);
        if (_open == false) {
          msg.limit(msg.remaining());
          break;
        }
      } while (msg.remaining() > 0);
    } else {
      do {
        int len = _tcpParser.parse(msg);
        if (len < 0)
          return -len;
        _handler.tcpSocketData(this, sockId, -1, msg.array(), msg.arrayOffset() + msg.position(), len);
        msg.position(msg.position() + len);
        if (_open == false) {
          msg.limit(msg.remaining());
          break;
        }
      } while (msg.remaining() > 0);
    }
    return 0;
  }
  
  public void receiveTimeout(TcpChannel cnx) {
    // ???
  }
  
  public void writeBlocked(TcpChannel cnx) {
    _logger.info("tcp writeBlocked on %s", cnx);
  }
  
  public void writeUnblocked(TcpChannel cnx) {
    _logger.info("tcp writeUnBlocked on %s", cnx);
  }
  
  /*************** TcpClientChannelListener ******************/
  
  public void connectionEstablished(TcpChannel cnx) {
    cnx.setWriteBlockedPolicy(WriteBlockedPolicy.IGNORE);
    int sockId = SOCK_ID.getAndIncrement();
    long connectionId = (Long) cnx.attachment();
    cnx.attach(Integer.valueOf(sockId));
    _channels.put(sockId, cnx);
    
    InetSocketAddress remote = cnx.getRemoteAddress();
    InetSocketAddress local = cnx.getLocalAddress();
    if (!_ipv6Support) {
      _handler.tcpSocketConnected(this, sockId, MuxUtils.getIPAsInt(remote.getAddress().getHostAddress()),
                                  remote.getPort(), MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                                  local.getPort(), MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                                  local.getPort(), cnx.isSecure(), false, connectionId, 0);
    } else {
      _handler.tcpSocketConnected(this, sockId, remote.getAddress().getHostAddress(), remote.getPort(), local
          .getAddress().getHostAddress(), local.getPort(), local.getAddress().getHostAddress(), local
          .getPort(), cnx.isSecure(), false, connectionId, 0);
    }
  }
  
  public void connectionFailed(TcpChannel cnx, Throwable error) {
    long connectionId = (Long) (cnx.attachment());
    if (!_ipv6Support) {
      _handler.tcpSocketConnected(this, 0, 0, 0, 0, 0, 0, 0, cnx.isSecure(), false, connectionId,
                                  MuxUtils.ERROR_UNDEFINED // TODO
          // :
          // better
          // errcode
          );
    } else {
      _handler.tcpSocketConnected(this, 0, "", 0, "", 0, "", 0, cnx.isSecure(), false, connectionId,
                                  MuxUtils.ERROR_UNDEFINED // TODO
          // :
          // better
          // errcode
          );
    }
  }
  
  /**
   * Callback not called anymore, since we are using the @link
   * {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, Map)}
   * method.
   * @deprecated
   */
  @Deprecated
  public void serverConnectionOpened(TcpServerChannel server) {
  }
  
  /**
   * Callback not called anymore, since we are using the @link
   * {@link ReactorProvider#tcpAccept(Reactor, InetSocketAddress, TcpServerChannelListener, Map)}
   * method.
   * @deprecated
   */
  @Deprecated
  public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
  }
  
  /**
   * Method called when we close our server channel.
   */
  public void serverConnectionClosed(TcpServerChannel server) {
    _logger.debug("serverConnectionClosed: %s", server);
    int sockId = ((ListenAttachment) server.attachment()).sockId;
    if (_serverChannels.remove(sockId) != null) {
      _handler.tcpSocketClosed(this, sockId); // is it the correct call ???
    }
    //checkMuxClosed();
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener#connectionAccepted(alcatel.tess.hometop.gateways.reactor.TcpServerChannel,
   *      alcatel.tess.hometop.gateways.reactor.TcpChannel)
   */
  public void connectionAccepted(TcpServerChannel serverChannel, TcpChannel acceptedChannel) {
    _logger.info("tcp connection accepted: %s (mt=%b)", acceptedChannel, _threadSafe);
    acceptedChannel.setWriteBlockedPolicy(WriteBlockedPolicy.IGNORE);
    
    if (_threadSafe) {
      acceptedChannel.setInputExecutor(_execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor(), "tcpin"));
    }
    
    ListenAttachment attachment = (ListenAttachment) serverChannel.attachment();
    int sockId = SOCK_ID.getAndIncrement();
    acceptedChannel.attach(Integer.valueOf(sockId));
    _channels.put(sockId, acceptedChannel);
    InetSocketAddress remote = acceptedChannel.getRemoteAddress();
    InetSocketAddress local = acceptedChannel.getLocalAddress();
    
    if (!_ipv6Support) {
      _handler.tcpSocketConnected(this, sockId, MuxUtils.getIPAsInt(remote.getAddress().getHostAddress()),
                                  remote.getPort(), MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                                  local.getPort(), MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                                  local.getPort(), attachment._secure, // overrides
                                  // acceptedChannel.isSecure()
                                  true, 0, 0);
    } else {
      _handler.tcpSocketConnected(this, sockId, remote.getAddress().getHostAddress(), remote.getPort(), local
          .getAddress().getHostAddress(), local.getPort(), local.getAddress().getHostAddress(), local
          .getPort(), attachment._secure, // overrides
                                  // acceptedChannel.isSecure()
                                  true, 0, 0);
    }
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener#connectionFailed(alcatel.tess.hometop.gateways.reactor.TcpServerChannel,
   *      java.lang.Throwable)
   */
  public void connectionFailed(TcpServerChannel serverChannel, java.lang.Throwable err) {
    _logger.info("tcp connectionFailed on %s", err, serverChannel);
  }
  
  /************************* UDPChannelListener *************************/
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.UdpChannelListener#connectionOpened(alcatel.tess.hometop.gateways.reactor.UdpChannel)
   * @deprecated
   */
  @Deprecated
  public void connectionOpened(UdpChannel cnx) {
    Long bindId = (Long) cnx.attachment();
    _logger.info("udp channel bound: channel=%s, bindId=%d", cnx, bindId);
    int sockId = SOCK_ID.getAndIncrement();
    _channels.put(sockId, cnx);
    
    cnx.attach(Integer.valueOf(sockId));
    
    InetSocketAddress local = cnx.getLocalAddress();
    _sockids.put(local, Integer.valueOf(sockId));
    
    if (!_ipv6Support) {
      _handler.udpSocketBound(this, sockId, MuxUtils.getIPAsInt(local.getAddress().getHostAddress()),
                              local.getPort(), false, bindId, 0);
    } else {
      _handler.udpSocketBound(this, sockId, local.getAddress().getHostAddress(), local.getPort(), false,
                              bindId, 0);
    }
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.UdpChannelListener#connectionFailed(alcatel.tess.hometop.gateways.reactor.UdpChannel,
   *      java.lang.Throwable)
   * @deprecated
   */
  @Deprecated
  public void connectionFailed(UdpChannel cnx, java.lang.Throwable err) {
    Long bindId = (Long) cnx.attachment();
    InetSocketAddress local = cnx.getLocalAddress();
    Integer sockId = _sockids.get(local);
    
    if (!(err instanceof BindException) || sockId == null) {
      _logger.warn("connectionFailed : cnx=%s, bindId=%d", err, cnx, bindId);
      if (!_ipv6Support) {
        _handler.udpSocketBound(this, 0, 0, 0, false, bindId, MuxUtils.ERROR_UNDEFINED);
        // TODO better  error code
      } else {
        _handler.udpSocketBound(this, 0, "", 0, false, bindId, MuxUtils.ERROR_UNDEFINED);
        // TODO better  error code
      }
    } else {
      _logger.debug("connection: %s (bindId=%d) already bound with sockid=%d", cnx, bindId, sockId);
      if (!_ipv6Support) {
        _handler.udpSocketBound(this, sockId.intValue(),
                                MuxUtils.getIPAsInt(local.getAddress().getHostAddress()), local.getPort(),
                                false, bindId, 0);
      } else {
        _handler.udpSocketBound(this, sockId.intValue(), local.getAddress().getHostAddress(),
                                local.getPort(), false, bindId, 0);
      }
    }
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.UdpChannelListener#connectionClosed(alcatel.tess.hometop.gateways.reactor.UdpChannel)
   */
  public void connectionClosed(UdpChannel cnx) {
    Integer sockId = (Integer) cnx.attachment();
    AsyncChannel channel = _channels.remove(sockId);
    if (channel != null) {
      _handler.udpSocketClosed(this, sockId);
    }
    //checkMuxClosed();
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.UdpChannelListener#messageReceived(alcatel.tess.hometop.gateways.reactor.UdpChannel,
   *      java.nio.ByteBuffer, java.net.InetSocketAddress)
   */
  public void messageReceived(UdpChannel cnx, java.nio.ByteBuffer msg, java.net.InetSocketAddress addr) {
    Integer sockId = (Integer) cnx.attachment();
    String ip = addr.getAddress().getHostAddress();
    int port = addr.getPort();
    if (!_byteBufferMode) {
      if (_ipv6Support) {
        _handler.udpSocketData(this, sockId, 0, ip, port, ip, port, msg.array(),
                               msg.arrayOffset() + msg.position(), msg.remaining());
      } else {
        _handler.udpSocketData(this, sockId, 0, MuxUtils.getIPAsInt(ip), port, MuxUtils.getIPAsInt(ip), port,
                               msg.array(), msg.arrayOffset() + msg.position(), msg.remaining());
      }
      msg.position(msg.limit());
    } else {
      if (_ipv6Support) {
        _handler.udpSocketData(this, sockId, 0, ip, port, ip, port, msg);
      } else {
        _handler.udpSocketData(this, sockId, 0, MuxUtils.getIPAsInt(ip), port, MuxUtils.getIPAsInt(ip), port,
                               msg);
      }
    }
  }
  
  public void receiveTimeout(UdpChannel cnx) {
  }
  
  public void writeBlocked(UdpChannel cnx) {
  }
  
  public void writeUnblocked(UdpChannel cnx) {
  }
  
  /************************* SctpChannelListener *************************/
  
  @Override
  public void connectionClosed(SctpChannel cnx, Throwable err) {
    ChannelAttachment attach = cnx.attachment();
    int sockId = attach._sockId;
    AsyncChannel channel = _channels.remove(sockId);
    if (channel != null) {
      _handler.sctpSocketClosed(this, sockId);
    }
  }
  
  @Override
  public void messageReceived(SctpChannel cnx, ByteBuffer buf, SocketAddress addr, int bytes,
                              boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("sctp message received on %s (%d)", cnx, bytes);
    }
    ChannelAttachment attach = cnx.attachment();
    int sockId = attach._sockId;
    String address = ((InetSocketAddress) addr).getAddress().getHostAddress();
    
    // Check if a previous incomplete message is waiting for more bytes to be parsed.
    
    if (attach._buf != null) {
      ByteBuffer tmp = ByteBuffer.allocate(attach._buf.remaining() + buf.remaining());
      tmp.put(attach._buf);
      tmp.put(buf);
      tmp.flip();
      buf = tmp;
      attach._buf = null;
    }
    
    do {
      int parsedLen = _tcpParser.parse(buf);
      if (parsedLen < 0) {
        // Fragmented message: buffer and wait for more bytes.
        attach._buf = ByteBuffer.allocate(buf.remaining());
        attach._buf.put(buf);
        attach._buf.flip();
        return;
      }
      
      byte[] data = buf.array();
      int off = buf.arrayOffset() + buf.position();
      int len = parsedLen;
      _handler.sctpSocketData(this, sockId, -1, ByteBuffer.wrap(data, off, len), address, isUnordered,
                              isComplete, ploadPID, streamNumber);
      buf.position(buf.position() + parsedLen);
    } while (buf.remaining() > 0);
  }
  
  @Override
  public void sendFailed(SctpChannel cnx, SocketAddress addr, ByteBuffer buf, int errcode, int streamNumber) {
    int sockId = ((ChannelAttachment) cnx.attachment())._sockId;
    String address = ((InetSocketAddress) addr).getAddress().getHostAddress();
    // TODO verify if the port is really available
    //        int port = ((InetSocketAddress) addr).getPort();
    _handler.sctpSocketSendFailed(this, sockId, address, streamNumber, buf, errcode);
  }
  
  @Override
  public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, AddressEvent event) {
    int assocId = -1;
    try {
      assocId = cnx.getAssociation().associationID();
    } catch (IOException e) {
      _logger.warn("Could not get association id on peer addr change event: " + event);
    }
    int sockId = ((ChannelAttachment) cnx.attachment())._sockId;
    _logger.info("peerAddressChanged: cnx=%s, addr=%s, event=%s, sockId=%d%s", cnx, addr, event, sockId, assocId == -1 ? ""
        : (", assocId=" + assocId));
    String address = ((InetSocketAddress) addr).getAddress().getHostAddress();
    SctpAddressEvent evt = SctpAddressEvent.valueOf(event.name());
    _handler.sctpPeerAddressChanged(this, sockId, address, ((InetSocketAddress) addr).getPort (), evt);
  }

  @Override
  public void writeBlocked(SctpChannel cnx) {
    _logger.info("sctp write blocked on channel %s", cnx);
  }
  
  @Override
  public void writeUnblocked(SctpChannel cnx) {
    _logger.info("sctp write unblocked on channel %s", cnx);
  }
  
  @Override
  public void receiveTimeout(SctpChannel cnx) {
    _logger.info("sctp receive timeout on cnx: %s", cnx);
  }
  
  @Override
  public void connectionEstablished(SctpChannel cnx) {
    cnx.setWriteBlockedPolicy(WriteBlockedPolicy.IGNORE);
    int sockId = SOCK_ID.getAndIncrement();
    long connectionId = ((ConnectAttachment) cnx.attachment())._connectionId;
    cnx.attach(new ChannelAttachment(sockId));
    String[] remoteAddrs = null;
    String[] localAddrs = null;
    int err = 0;
    SctpAssociation assoc = null;
    try {
      remoteAddrs = getAddresses(cnx.getRemoteAddresses());
      localAddrs = getAddresses(cnx.getLocalAddresses());
      assoc = cnx.getAssociation();
      _logger.info("sctp association established: %s (associd=%d, sockid=%d, mt=%b)", cnx, 
                   assoc.associationID(), sockId, _threadSafe);
    } catch (IOException e) {
      _logger.error("Could not get local/remote address of sctp socket", e);
      err = MuxUtils.ERROR_UNDEFINED;
    }
    
    if (err == 0) {
      _channels.put(sockId, cnx);
    }
    _handler.sctpSocketConnected(this, sockId, connectionId, remoteAddrs, cnx.getRemotePort(), localAddrs,
                                 cnx.getLocalAddress().getPort(), assoc.maxOutboundStreams(),
                                 assoc.maxInboundStreams(), false, false, err);
  }
  
  @Override
  public void connectionFailed(SctpChannel cnx, Throwable error) {
    _logger.info("sctp connection failed: %s", error, cnx);
    ConnectAttachment attach = cnx.attachment();
    _handler.sctpSocketConnected(this, 0, attach._connectionId, new String[0], 0, new String[0], 0, 0, 0,
                                 false, false, MuxUtils.ERROR_UNDEFINED);
  }
  
  @Override
  public void connectionAccepted(SctpServerChannel ssc, SctpChannel sc) {
    sc.setWriteBlockedPolicy(WriteBlockedPolicy.IGNORE);

    if (_threadSafe) {
      sc.setInputExecutor(_execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor(), "sctpin"));
    }
    
    String[] localAddrs;
    String[] remoteAddrs;
    int maxInStreams = 0;
    int maxOutStreams = 0;
    int sockId = -1;
    try {
      localAddrs = getAddresses(sc.getLocalAddresses());
      remoteAddrs = getAddresses(sc.getRemoteAddresses());
      maxInStreams = sc.getAssociation().maxInboundStreams();
      maxOutStreams = sc.getAssociation().maxOutboundStreams();
      sockId = SOCK_ID.getAndIncrement();
      _logger.info("sctp association accepted: %s (associd=%d, sockid=%d, mt=%b)", sc, 
                   sc.getAssociation().associationID(), sockId, _threadSafe);
    } catch (IOException e) {
      _logger.error("could not get local/remote address of sctp channel", e);
      return;
    }
    
    sc.attach(new ChannelAttachment(sockId));
    _channels.put(sockId, sc);
    
    _handler.sctpSocketConnected(this, sockId, 0, remoteAddrs, sc.getRemotePort(), localAddrs, sc
				 .getLocalAddress().getPort(), maxOutStreams, maxInStreams, true, false, 0);
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener#serverConnectionClosed(alcatel.tess.hometop.gateways.reactor.SctpServerChannel, java.lang.Throwable)
   */
  @Override
  public void serverConnectionClosed(SctpServerChannel ssc, Throwable err) {
    _logger.info("sctp server closed: %s", ssc);
    int sockId = ((ListenAttachment) ssc.attachment()).sockId;
    if (_sctpServerChannels.remove(sockId) != null) {
      _handler.sctpSocketClosed(this, sockId);
    }
    //checkMuxClosed();
  }
  
  /************************* private methods *************************/
  
  private InetSocketAddress getPrimaryAddress(Set<SocketAddress> addrs) {
    return (InetSocketAddress) addrs.iterator().next();
  }
  
  private SocketAddress getPrimaryLocalAddress(String[] addrs, int port) throws UnknownHostException {
    if (addrs == null) {
      return new InetSocketAddress(port);
    } else {
      if (addrs.length == 0) {
        throw new IllegalArgumentException("empty local address array");
      }
      if (addrs[0] == null) // we allow passing this argument
	  return new InetSocketAddress (port);
      return new InetSocketAddress(InetAddress.getByName(addrs[0]), port);
    }
  }
  
  private InetAddress[] getSecondaryLocalAddresses(String[] addrs) throws UnknownHostException {
    if (addrs == null || addrs.length < 2) {
      return null;
    }
    
    InetAddress[] secondaryAddrs = new InetAddress[addrs.length - 1];
    for (int i = 0; i < addrs.length - 1; i++) {
      secondaryAddrs[i] = InetAddress.getByName(addrs[i + 1]);
    }
    return secondaryAddrs;
  }
  
  private String[] getAddresses(Set<SocketAddress> remoteAddresses) {
    String[] remoteAddrs = new String[remoteAddresses.size()];
    int index = 0;
    for (SocketAddress sa : remoteAddresses) {
      remoteAddrs[index++] = ((InetSocketAddress) sa).getAddress().getHostAddress();
    }
    return remoteAddrs;
  }
  
  @Override
  public void setInputExecutor(Executor inputExecutor) {
    // Nothing to do: we don't have real mux connection!
  }

  @Override
  public void close(Enum<?> reason, String info, Throwable err) {
    MuxConnectionImpl.logClosingMux(this, reason, info, err, _logger.getLogger());
    close();
  }

  @Override
  public void shutdown(Enum<?> reason, String info, Throwable err) {
    MuxConnectionImpl.logClosingMux(this, reason, info, err, _logger.getLogger());
    shutdown();
  }  
}
