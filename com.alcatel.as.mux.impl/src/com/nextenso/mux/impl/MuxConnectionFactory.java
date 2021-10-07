package com.nextenso.mux.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.utils.IntHashtable;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;

/**
 * @deprecated don't use this class. please use the new official com.nextenso.mux.MuxFactory and
 *             com.nextenso.mux.SimpleMuxFactory classes.
 */
@Deprecated
public class MuxConnectionFactory {
  public final static String FACTORY_PROPERTY_NAME = "MuxConnectionFactory.class";
  private final static MuxConnectionFactory _factory = new MuxConnectionFactory();
  private final static Logger _logger = Logger.getLogger("as.service.mux.legacy.MuxConnectionFactory");
  protected final ReactorProvider _reactorProvider = ReactorProvider.provider();
  protected final TimerService _timerService = ServiceLoader.getService(TimerService.class, "(strict=true)");
  protected final PlatformExecutors _execs = ServiceLoader.getService(PlatformExecutors.class);
  
  private MuxConnectionFactory() {
  }
  
  public final static synchronized MuxConnectionFactory getFactory() {
    return _factory;
  }
  
  /**
   * Create a mux mono/double cnx synchronously. This method blocks the calling thread, and
   * creates a new reactor thread. If the mux is disconnected, then the reactor thread is also
   * killed.
   */
  public final void newMuxConnectionImpl(int stackId, String stackName, String stackHost,
                                         String stackInstance, String stackAddr, int stackPort,
                                         MuxHandler handler, IntHashtable flags, Tracer tr)
      throws IOException {
    newMuxConnectionImpl(stackId, stackName, stackHost, stackInstance, stackAddr, stackPort, handler, flags,
                         tr.getLogger());
  }
  
  /**
   * Create a mux mono/double cnx asynchronously. Once opened, the mux handler is called back in
   * its muxOpened() method.
   */
  public final void newMuxConnectionImpl(int stackId, String stackName, String stackHost,
                                         String stackInstance, String stackAddr, int stackPort,
                                         MuxHandler handler, Object[] attrs, IntHashtable flags, Tracer tr,
                                         MuxConnectionImplHandler implHandler, Reactor reactor)
      throws IOException {
    newMuxConnectionImpl(stackId, stackName, stackHost, stackInstance, stackAddr, stackPort, handler, attrs,
                         flags, tr.getLogger(), implHandler, reactor);
  }
  
  /**
   * Accept a mux mono cnx asynchronously. Once opened, the mux handler is called back in its
   * muxOpened() method.
   */
  public final void acceptMuxConnectionImpl(MuxHandler handler, MuxServerConnectionImplHandler implHandler,
                                            InetSocketAddress addr, Reactor reactor, Tracer tr) {
    acceptMuxConnectionImpl(handler, implHandler, addr, reactor, tr.getLogger());
  }
  
  /**
   * Initialize the factory.
   */
  public void setDefaultParams(int defaultKeepAlive, long uid) {
    // The real implementation is already initialized: don't do anything.
  }
  
  /**
   * Create a mux mono/double cnx synchronously. This method blocks the calling thread, and
   * creates a new reactor thread. If the mux is disconnected, then the reactor thread is also
   * killed.
   */
  @SuppressWarnings({ "unchecked", "serial" })
  public void newMuxConnectionImpl(int stackId, String stackName, String stackHost, String stackInstance,
                                   String stackAddr, int stackPort, MuxHandler handler,
                                   final IntHashtable flags, final Logger logger) throws IOException {
    if (stackAddr.equals("0.0.0.0")) {
      stackAddr = stackHost;
    }
    
    if (_logger.isDebugEnabled()) {
      _logger.debug("Connecting to " + stackAddr);
    }
    
    Reactor reactor = ReactorProvider.provider().newReactor(stackInstance + "Reactor", true, logger);
    MuxFactory mf = MuxFactory.getInstance();
    
    InetSocketAddress addr = new InetSocketAddress(stackAddr, stackPort);
    SyncConnector connector = new SyncConnector(reactor, addr);
    Map opts = new HashMap() {
      {
        put(MuxFactory.OPT_CONNECTION_TIMEOUT, Long.valueOf(15000L));
        put(MuxFactory.OPT_FLAGS, flags);
        put(MuxFactory.OPT_LOGGER, logger);
      }
    };
    MuxConnection cnx = mf.newMuxConnection(reactor, connector, handler, addr, stackId, stackName, stackHost,
                                            stackInstance, opts);
    mf.connect(cnx);
    connector.await(15 /* seconds */);
    if (_logger.isDebugEnabled()) {
      _logger.debug("Connected to " + stackAddr);
    }
  }
  
  /**
   * Create a mux mono/double cnx asynchronously. Once opened, the mux handler is called back in
   * its muxOpened() method.
   */
  @SuppressWarnings({ "unchecked", "serial" })
  public void newMuxConnectionImpl(int stackId, String stackName, String stackHost, String stackInstance,
                                   String stackAddr, int stackPort, MuxHandler handler, Object[] attrs,
                                   final IntHashtable flags, final Logger logger,
                                   MuxConnectionImplHandler implHandler, Reactor reactor) throws IOException {
    if (stackAddr.equals("0.0.0.0")) {
      stackAddr = stackHost;
    }
    if (_logger.isDebugEnabled()) {
      _logger.debug("connecting to " + stackAddr);
    }
    
    InetSocketAddress addr = new InetSocketAddress(stackAddr, stackPort);
    AsyncConnector connector = new AsyncConnector(implHandler);
    MuxFactory mf = MuxFactory.getInstance();
    Map opts = new HashMap() {
      {
        put(MuxFactory.OPT_CONNECTION_TIMEOUT, Long.valueOf(15000L));
        put(MuxFactory.OPT_FLAGS, flags);
        put(MuxFactory.OPT_LOGGER, logger);
      }
    };
    MuxConnection cnx = mf.newMuxConnection(reactor, connector, handler, addr, stackId, stackName, stackHost,
                                            stackInstance, opts);
    cnx.setAttributes(attrs);
    mf.connect(cnx);
  }
  
  /**
   * Accept a mux mono cnx asynchronously. Once opened, the mux handler is called back in its
   * muxOpened() method.
   */
  public void acceptMuxConnectionImpl(final MuxHandler handler,
                                      final MuxServerConnectionImplHandler implHandler,
                                      final InetSocketAddress addr, Reactor reactor, final Logger tr) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Accepting cnx on address " + addr);
    }
    
    _reactorProvider.newTcpServerChannel(addr, new Acceptor(handler, implHandler, tr), reactor, null, tr);
  }
  
  /**
   * Connector used to connect synchronously.
   */
  private static class SyncConnector implements MuxFactory.ConnectionListener {
    private Reactor _reactor;
    private InetSocketAddress _addr;
    private final CountDownLatch _latch = new CountDownLatch(1);
    private Throwable _error;
    private final Logger _connectorLogger = Logger.getLogger("as.service.mux.legacy.SyncConnector");
    
    SyncConnector(Reactor reactor, InetSocketAddress addr) {
      _reactor = reactor;
      _addr = addr;
    }
    
    public void muxConnected(MuxConnection cnx, Throwable error) {
      _error = error;
      if (_connectorLogger.isDebugEnabled()) {
        _connectorLogger.debug("muxConnected (err=" + _error + ")");
      }
      _latch.countDown();
    }
    
    public void muxClosed(MuxConnection cnx) {
      if (_connectorLogger.isDebugEnabled()) {
        _connectorLogger.debug("muxClosed: " + cnx);
      }
      _reactor.stop();
    }
    
    public void muxAccepted(MuxConnection cnx, Throwable error) {
    }
    
    public void await(long secs) throws IOException {
      try {
        if (_connectorLogger.isDebugEnabled()) {
          _connectorLogger.debug("awaiting for mux connection establishment on address " + _addr);
        }
        
        if (!_latch.await(secs, TimeUnit.SECONDS)) {
          _reactor.stop();
          throw new IOException("Cound not connect to " + _addr + " timely");
        }
      } catch (InterruptedException e) {
        _reactor.stop();
        throw new IOException("Cound not connect to " + _addr + " (operation interrupted)");
      }
      
      if (_error != null) {
        if (_connectorLogger.isDebugEnabled()) {
          _connectorLogger.debug("Failed to connect to " + _addr, _error);
        }
        
        _reactor.stop();
        IOException ioe = new IOException("Could not connect to " + _addr);
        ioe.initCause(_error);
        throw ioe;
      }
    }
  }
  
  /**
   * Connector used to connect asynchronously.
   */
  private static class AsyncConnector implements MuxFactory.ConnectionListener {
    private MuxConnectionImplHandler _implHandler;
    private Logger _connectorLogger = Logger.getLogger("as.service.mux.legacy.AsyncConnector");
    
    AsyncConnector(MuxConnectionImplHandler implHandler) {
      _implHandler = implHandler;
    }
    
    public void muxConnected(MuxConnection cnx, Throwable error) {
      if (_connectorLogger.isDebugEnabled()) {
        _connectorLogger.debug("muxConnected (err=" + error + ")");
      }
      
      if (error != null) {
        _implHandler.muxConnectionFailed(cnx);
      } else {
        _implHandler.muxConnectionOpened(cnx);
      }
    }
    
    public void muxClosed(MuxConnection cnx) {
      if (_connectorLogger.isDebugEnabled()) {
        _connectorLogger.debug("mux closed for impl handler: " + _implHandler);
      }
      _implHandler.muxConnectionClosed(cnx);
    }
    
    public void muxAccepted(MuxConnection cnx, Throwable error) {
    }
  }
  
  /**
   * Acceptor used to accepts connections asynchronously.
   */
  private class Acceptor implements TcpServerChannelListener {
    private MuxServerConnectionImplHandler _implHandler;
    private MuxHandler _handler;
    private Logger _logger;
    
    Acceptor(MuxHandler handler, MuxServerConnectionImplHandler implHandler, Logger logger) {
      _handler = handler;
      _implHandler = implHandler;
      _logger = logger;
    }
    
    public void connectionAccepted(TcpServerChannel tsc, TcpChannel cnx) {
      MuxConnectionImpl mci = new MuxConnectionImpl(cnx.getReactor(), _handler, null, cnx.getLocalAddress(),
          cnx.getRemoteAddress(), 0, // TODO fixme
          null, // TODO fixme
          null, // TODO fixme
          null, // TODO fixme
          null, null, _logger, 0L, null, _timerService, null, _execs); // not used
      
      cnx.attach(mci);
      _implHandler.muxConnectionOpened(mci);
      mci.muxOpened(cnx, false, null);
    }
    
    public void connectionClosed(TcpChannel cnx) {
      MuxConnectionImpl mci = (MuxConnectionImpl) cnx.attachment();
      mci.muxClosed();
      _implHandler.muxConnectionClosed(mci);
    }
    
    public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
      MuxConnectionImpl mci = (MuxConnectionImpl) cnx.attachment();
      return mci.handleData(cnx, msg);
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
    
    public void connectionFailed(TcpServerChannel serverChannel, Throwable err) {
    }
    
    public void serverConnectionClosed(TcpServerChannel server) {
      _implHandler.muxServerConnectionClosed(_handler, server.getLocalAddress());
    }
    
    public void serverConnectionFailed(TcpServerChannel server, Throwable err) {
      _implHandler.muxServerConnectionFailed(_handler, server.getLocalAddress());
    }
    
    public void serverConnectionOpened(TcpServerChannel server) {
      _implHandler.muxServerConnectionOpened(_handler, server.getLocalAddress());
    }
  }
}
