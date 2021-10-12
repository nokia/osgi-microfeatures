// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.TcpMessageParser;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpServerOption;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.utils.IntHashtable;

public class MuxFactoryImpl extends MuxFactory {
  // Our logger
  private final static Logger _logger = Logger.getLogger("as.service.mux.MuxConnectionImpl");
  
  // Reactor Provider used to create Reactors
  protected volatile ReactorProvider _reactorProvider;
  
  // injected from activator
  protected volatile RuntimeStatistics _mgmtRtService;
  
  // socket connection timeout
  protected long _cnxTimeout = 15000L;
  
  // timer service
  protected volatile TimerService _timerService;
  
  // Platform Executors
  protected volatile PlatformExecutors _execs;
  
  protected void bindReactorProvider(ReactorProvider provider) {
	  _reactorProvider = provider;	  
  }
  
  protected void bindTimerService(TimerService timerService) {
	  _timerService = timerService;
  }
  
  protected void bindPlatformExecutors(PlatformExecutors execs) {
	  _execs = execs;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public MuxConnection newMuxConnection(Reactor reactor, ConnectionListener listener, MuxHandler muxHandler,
                                        InetSocketAddress to, int stackId, String stackName,
                                        String stackHost, String stackInstance, Map opts) {
    Logger logger = (opts != null) ? (Logger) opts.get(OPT_LOGGER) : _logger;
    InetSocketAddress from = (opts != null) ? (InetSocketAddress) opts.get(OPT_LOCAL_ADDR) : null;
    long connectionTimeout = _cnxTimeout;
    if (opts != null && opts.get(OPT_CONNECTION_TIMEOUT) != null) {
      connectionTimeout = (Long) opts.get(OPT_CONNECTION_TIMEOUT);
    }
    
    Object attachment = (opts != null) ? (Object) opts.get(OPT_ATTACH) : null;
    IntHashtable flags = (opts != null) ? (IntHashtable) opts.get(OPT_FLAGS) : null;
    
    MuxConnection cnx = new MuxConnectionImpl(reactor, muxHandler, listener, from, to, stackId, stackName,
        stackHost, stackInstance, attachment, flags, logger, connectionTimeout, _mgmtRtService, 
        _timerService, opts, _execs);
    return cnx;
  }
  
  //   @Deprecated use the MuxFactory service with the OSGI service property "local=true".
  @Override
  public MuxConnection newLocalMuxConnection(Reactor reactor, MuxHandler mh, int stackAppId,
                                             String stackAppName, String stackInstance, TcpMessageParser parser, Logger logger) {
      throw new RuntimeException("use the MuxFactory service with the OSGI service property \"type=local\"");
  }
  
  @Override
  public void connect(MuxConnection cnx) {
    if (!(cnx instanceof MuxConnectionImpl)) {
      return;
    }
    MuxConnectionImpl impl = (MuxConnectionImpl) cnx;
    Connector connector = new Connector(impl);
    
    Executor inExec = null;
    Map muxOpts = impl.getMuxOptions();
    inExec = (Executor) muxOpts.get(OPT_INPUT_EXECUTOR);
    
    Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
    opts.put(TcpClientOption.FROM_ADDR, cnx.getLocalAddress());
    opts.put(TcpClientOption.TIMEOUT, impl.getConnectionTimeout());
    opts.put(TcpClientOption.PRIORITY, AsyncChannel.MAX_PRIORITY);
    opts.put(TcpClientOption.TCP_NO_DELAY, Boolean.TRUE);
    if (inExec != null) {
      opts.put(TcpClientOption.INPUT_EXECUTOR, inExec);
    }
    
    _reactorProvider.tcpConnect(impl.getReactor(), cnx.getRemoteAddress(), connector, opts);
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public InetSocketAddress accept(Reactor r, ConnectionListener l, MuxHandler mh, InetSocketAddress from,
                                  Map opts) throws IOException {
    Logger logger = (opts != null) ? (Logger) opts.get(OPT_LOGGER) : _logger;
    Object attach = opts != null ? (Object) opts.get(OPT_ATTACH) : null;
    IntHashtable flags = (opts != null) ? (IntHashtable) opts.get(OPT_FLAGS) : null;
    
    Executor inExec = (opts != null) ? (Executor) opts.get(OPT_INPUT_EXECUTOR) : null;
    
    Map<ReactorProvider.TcpServerOption, Object> o = new HashMap<ReactorProvider.TcpServerOption, Object>();
    o.put(TcpServerOption.SECURE, Boolean.FALSE);
    TcpServerChannel serverChannel = _reactorProvider.tcpAccept(r, from, new Acceptor(l, mh, attach, flags,
        logger, inExec), o);
    return serverChannel.getLocalAddress();
  }
  
  // --------------------- Private methods ----------------------------------------------
  
  static class Connector implements TcpClientChannelListener {
    MuxConnectionImpl _mci;
    
    Connector(MuxConnectionImpl cnx) {
      _mci = cnx;
    }
    
    public void connectionEstablished(TcpChannel cnx) {
      _mci.muxOpened(cnx, true /* client */, null);
    }
    
    public void connectionFailed(TcpChannel cnx, Throwable err) {
      _mci.muxOpened(cnx, true /* client */, err);
    }
    
    public void connectionClosed(TcpChannel cnx) {
      _mci.muxClosed();
    }
    
    public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
      return _mci.handleData(cnx, msg);
    }
    
    public void receiveTimeout(TcpChannel cnx) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("receive timeout on cnx: " + cnx);
      }
    }
    
    public void writeBlocked(TcpChannel cnx) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("Write blocked on cnx: " + cnx);
      }
    }
    
    public void writeUnblocked(TcpChannel cnx) {
      if (_logger.isDebugEnabled()) {
        _logger.debug("Write unblocked on cnx: " + cnx);
      }
    }
  }
  
  // --------------------- Our Object notified about tcp server channel events
  
  class Acceptor implements TcpServerChannelListener {
    private final ConnectionListener connectionListener;
    private final MuxHandler _muxHandler;
    private final Object _attach;
    private final IntHashtable _flags;
    private final Logger _acceptorLogger;
    private final Executor _inExec;
    
    Acceptor(ConnectionListener cl, MuxHandler mh, Object attach, IntHashtable flags, Logger logger,
             Executor inExec) {
      connectionListener = cl;
      _muxHandler = mh;
      _attach = attach;
      _flags = flags;
      _acceptorLogger = logger;
      _inExec = inExec;
    }
    
    public void connectionAccepted(TcpServerChannel tsc, TcpChannel cnx) {
      MuxConnectionImpl mci = new MuxConnectionImpl(cnx.getReactor(), _muxHandler, connectionListener,
          cnx.getLocalAddress(), cnx.getRemoteAddress(), 0, null, null, null, _attach, _flags,
          _acceptorLogger, 0L, _mgmtRtService, _timerService, null, _execs);
      
      if (_inExec != null) {
        cnx.setInputExecutor(_inExec);
      }
      mci.muxOpened(cnx, false, null);
      cnx.attach(mci);
    }
    
    public void connectionClosed(TcpChannel cnx) {
      MuxConnectionImpl mci = (MuxConnectionImpl) cnx.attachment();
      mci.muxClosed();
    }
    
    public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
      MuxConnectionImpl mci = (MuxConnectionImpl) cnx.attachment();
      return mci.handleData(cnx, msg);
    }
    
    public void receiveTimeout(TcpChannel cnx) {
      if (_acceptorLogger.isDebugEnabled()) {
        _acceptorLogger.debug("receive timeout on cnx: " + cnx);
      }
    }
    
    public void writeBlocked(TcpChannel cnx) {
      if (_acceptorLogger.isDebugEnabled()) {
        _acceptorLogger.debug("Write blocked on cnx: " + cnx);
      }
    }
    
    public void writeUnblocked(TcpChannel cnx) {
      if (_acceptorLogger.isDebugEnabled()) {
        _acceptorLogger.debug("Write unblocked on cnx: " + cnx);
      }
    }
    
    public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
    }
    
    public void serverConnectionClosed(TcpServerChannel server) {
    }
    
    public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
    }
    
    public void serverConnectionOpened(TcpServerChannel server) {
    }
  }
}
