// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.util.sctp.SctpSocketOption;
//import com.nokia.as.service.tlsexport.TlsExportService;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import alcatel.tess.hometop.gateways.utils.CIString;

/**
 * ReactorProvider implementation.
 */
@SuppressWarnings("deprecation")
public class ReactorProviderImpl extends ReactorProvider {
  /**
   * Our logger
   */
  private final static Logger _logger = Logger.getLogger("as.service.reactor.ReactorProviderImpl");
  
  /**
   * Map of all created reactors
   */
  private ConcurrentHashMap<CIString, Reactor> _aliases = new ConcurrentHashMap<CIString, Reactor>();
  
  /**
   * thread local used to get current reactor associated to current thread
   */
  private ThreadLocal<Reactor> _reactorThreadLocal = new ThreadLocal<Reactor>();
  
  /**
   * strict jdk Timer service (injected)
   */
  protected TimerService _strictTimerService;
  
  /**
   * wheel Timer service (injected)
   */
  protected TimerService _approxTimerService;
  
  /**
   * Metering service (injected)
   */
  protected MeteringService _meteringService;
  
  /**
   * Platform Executors (injected)
   */
  protected PlatformExecutors _executors;
  
  /**
   * Bundle Context (injected)
   */
  protected BundleContext _bctx;
  
  /**
   * Number of available cores
   */
  private final static int PROCS = Runtime.getRuntime().availableProcessors();
  
  /**
   * Number of selectors to be used for read/write
   */
  private final static String DEF_SELECTORS = String.valueOf((int) Math.ceil((double) PROCS / 10)); // choose a reasonable number of selectors, based on available procs.
  private final static int CONF_SELECTORS = Integer.parseInt(System.getProperty("reactor.selectors", DEF_SELECTORS)); // "0" means all processors
  private final static int SELECTORS = CONF_SELECTORS == 0 ? PROCS : CONF_SELECTORS;
  
  /**
   * Selectors to use for IO events 
   */
  private final NioSelector[] _selectors = new NioSelector[SELECTORS];
    
  /**
   * Counter used to assign names to selector threads
   */
  private final static AtomicInteger _selectorsCount = new AtomicInteger();
  
  /**
   * Reactor Meters
   */
  private volatile Meters _meters;

  /**
   * Tls Export Service (injected, optional)
   */
  //private volatile TlsExportService _tlsExport;
    
  /**
   * Default SO_LINGER option to use
   */
  private final static long DEF_SO_LINGER = Long.getLong("reactor.so_linger", 5000L);
  
  protected void start() {
	_logger.info("Initializing reactor provider (selectors = " + _selectors.length + ")");
    _meters = new Meters(_meteringService, _bctx);    
    Helpers.setDefaultOutputExecutor(_executors.getProcessingThreadPoolExecutor().toExecutor(ExecutorPolicy.SCHEDULE_HIGH));
    initSelectors(_selectors);
  }
    
  protected synchronized void stop() {
    _logger.info("Stopping ReactorProvider service");
    for (Reactor r : _aliases.values()) {
      r.stop();
    }
    _aliases.clear();
    Stream.of(_selectors).forEach(NioSelector::shutdown);
  }
  
  @Override
  public void aliasReactor(String alias, Reactor reactor) {
    if (reactor == null) {
      _aliases.remove(new CIString(alias));
    } else {
      _aliases.put(new CIString(alias), reactor);
    }
  }
  
  @Override
  public Reactor getReactor(String alias) {
    return _aliases.get(new CIString(alias));
  }
  
  @Override
  public Reactor getCurrentThreadReactor() {
    return _reactorThreadLocal.get();
  }
  
  @Override
  public Reactor getDefaultReactor() throws IOException {
    CIString main = new CIString("Main");
    Reactor defReactor;
    synchronized (this) {
      Reactor old = _aliases.get(main);
      if (old != null) {
        return old;
      }
      defReactor = create("Main");
      _aliases.put(main, defReactor);
    }
    defReactor.start();
    return defReactor;
  }
  
  @Override
  public Reactor newReactor(String name, boolean start, Logger logger) throws IOException {
    Reactor r = create(name);
    if (start) {
      r.start();
    }
    return r;
  }
  
  @Override
  public Reactor newReactor(Logger tr) throws IOException {
    return create(null);
  }
  
  @Override
  public Reactor newReactor(Logger logger, String threadName) throws IOException {
    return newReactor(threadName, true, logger);
  }
  
  @Override
  public void newTcpClientChannel(InetSocketAddress to, TcpClientChannelListener listener, Reactor reactor,
                                  Object attachment, long timeout, Logger tr) {
    checkStarted(reactor);
    newTcpClientChannel(to, listener, reactor, attachment, timeout, AsyncChannel.MAX_PRIORITY, tr);
  }
  
  @Override
  public void newTcpClientChannel(InetSocketAddress from, InetSocketAddress to,
                                  TcpClientChannelListener listener, Reactor reactor, Object attachment,
                                  long timeout, int priority, Logger tr, boolean secure) {
    checkStarted(reactor);
    Security security = secure ? Helpers.getDefaulClientSSLConfig() : null;
    
    TcpChannelConnector connector = new TcpChannelConnector((ReactorImpl) reactor, listener, from, to,
        attachment, priority, ((ReactorImpl) reactor).getPlatformExecutor(), true, false, 0, false, 0, 0, security, DEF_SO_LINGER);
    
    connector.connect(timeout);
  }
  
  @Override
  public void newTcpServerChannel(InetSocketAddress local, TcpServerChannelListener listener,
                                  Reactor reactor, Object data, Logger tr, boolean secure) {
    checkStarted(reactor);
    Security security = secure ? Helpers.getDefaulClientSSLConfig() : null;
    TcpServerChannelImpl tsci = new TcpServerChannelImpl((ReactorImpl) reactor, listener, local, data,
        1024, true, true, false, 0, 0, 0, security, 1000, DEF_SO_LINGER);
    tsci.listen();
  }
  
  @Override
  public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor,
                            Object attachment, Logger tr) {
    checkStarted(reactor);
    newUdpChannel(local, listener, reactor, AsyncChannel.MAX_PRIORITY, attachment, tr);
  }
  
  @Override
  public TcpServerChannel newTcpServerChannel(Reactor reactor, TcpServerChannelListener listener,
                                              InetSocketAddress local, Object attachment, boolean secure,
                                              Logger tr) throws IOException {
    checkStarted(reactor);
    Security security = secure ? Helpers.getDefaulClientSSLConfig() : null;

    TcpServerChannelImpl channel = new TcpServerChannelImpl((ReactorImpl) reactor, listener, local,
        attachment, 1024, true, true, false, 0, 0, 0, security, 1000, DEF_SO_LINGER);
    channel.listenSync();
    return channel;
  }
  
  @Override
  public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor,
                            int priority, Object attachment, Logger tr) {
    checkStarted(reactor);
    UdpChannelImpl channel = new UdpChannelImpl((ReactorImpl) reactor, listener, local, priority, attachment,
        reactor, null, true, 0, 0, false, false);
    channel.bind();
  }
  
  @Override
  public void newSctpClientChannel(SocketAddress local, InetAddress[] secondaryLocal, int maxOutStreams,
                                   int maxInStreams, SocketAddress to, Object attachment, long timeout,
                                   int priority, Logger logger, SctpClientChannelListener listener,
                                   Reactor reactor) {
    checkStarted(reactor);
    SctpChannelConnector connector = new SctpChannelConnector(local, secondaryLocal, maxOutStreams,
        maxInStreams, to, attachment, priority, listener, (ReactorImpl) reactor, reactor.getPlatformExecutor(), 0, 0, false, true, null, null, null, DEF_SO_LINGER, Collections.emptyMap());
    
    connector.connect(timeout);
  }
  
  @Override
  public SctpServerChannel newSctpServerChannel(SocketAddress local, InetAddress[] secondaryLocals,
                                                int maxOutStreams, int maxInStreams, int priority,
                                                Logger logger, SctpServerChannelListener listener,
                                                Object attachment, Reactor reactor) throws IOException {
    checkStarted(reactor);
    SctpServerChannelImpl ssc = new SctpServerChannelImpl(local, secondaryLocals, maxOutStreams,
        maxInStreams, priority, listener, (ReactorImpl) reactor, 1024, 0, 0, true, false, null, null, null, Collections.emptyMap(), 1000, DEF_SO_LINGER);
    ssc.attach(attachment);
    ssc.listen();
    return ssc;
  }
  
  @Override
  public synchronized Reactor create(String name) {
    try {
      Reactor reactor = null;
      if (name != null) {
        reactor = getReactor(name);
        if (reactor == null) {
          Logger logger = Logger.getLogger("as.service.reactor." + name);
          reactor = new ReactorImpl(name, logger, this);
          aliasReactor(name, reactor);
        } else {
          throw new IllegalArgumentException("Reactor " + name + " is already existing");
        }
      } else {
        reactor = new ReactorImpl(null, null, this);
      }
      return reactor;
    } catch (IOException e) {
      throw new RuntimeException("Could not create reactor " + name, e);
    }
  }
  
  @Override
  public void tcpConnect(Reactor r, InetSocketAddress to, TcpClientChannelListener listener,
                         Map<?, Object> options) {
    checkStarted(r);
    InetSocketAddress from = null;
    long timeout = 0L;
    Object attachment = null;
    int priority = AsyncChannel.MAX_PRIORITY;
    Executor inputExec = r;
    boolean tcpNoDelay = true;
    boolean useDirectBuffer = false;
    boolean useIpTransparent = false;
    int autoFlushSize = 0;
    int rcvbuf = 0;
    int sndbuf = 0;
    Security security = null;
    long linger = DEF_SO_LINGER;

    if (options != null) {
      from = (InetSocketAddress) options.get(TcpClientOption.FROM_ADDR);
      Long t = (Long) options.get(TcpClientOption.TIMEOUT);
      if (t != null) {
        timeout = t;
      }
      attachment = options.get(TcpClientOption.ATTACHMENT);
      Integer p = (Integer) options.get(TcpClientOption.PRIORITY);
      if (p != null) {
        priority = p;
        checkPriority(priority);
      }
      security = (Security) options.get(TcpClientOption.SECURITY);
      if (security == null) {
    	  Boolean s = (Boolean) options.get(TcpClientOption.SECURE);
    	  if (s != null && s == true) {
    		  security = Helpers.getDefaulClientSSLConfig();
    	  }
      }
      Executor e = (Executor) options.get(TcpClientOption.INPUT_EXECUTOR);
      if (e != null) {
        inputExec = e;
      }
      Boolean noDelay = (Boolean) options.get(TcpClientOption.TCP_NO_DELAY);
      if (noDelay != null) {
        tcpNoDelay = noDelay;
      }
      Boolean direct = (Boolean) options.get(TcpClientOption.USE_DIRECT_BUFFER);
      if (direct != null) {
        useDirectBuffer = direct;
      }
      Integer i = (Integer) options.get(TcpClientOption.AUTO_FLUSH_SIZE);
      if (i != null) {
        autoFlushSize = i;
      }
      if (options.get(TcpClientOption.SO_SNDBUF) != null) {
        sndbuf = (Integer) options.get(TcpClientOption.SO_SNDBUF);
      }
      if (options.get(TcpClientOption.SO_RCVBUF) != null) {
        rcvbuf = (Integer) options.get(TcpClientOption.SO_RCVBUF);
      }
      
      Boolean ipTransparent = (Boolean) options.get(TcpClientOption.IP_TRANSPARENT);
      if(ipTransparent != null) {
          useIpTransparent = ipTransparent;
      }
      if (options.get(TcpClientOption.LINGER) != null) {
    	  linger = (Long) options.get(TcpClientOption.LINGER);
      }
    }
    
    TcpChannelConnector connector = new TcpChannelConnector((ReactorImpl) r, listener, from, to, attachment,
        priority, inputExec, tcpNoDelay, useDirectBuffer, autoFlushSize, useIpTransparent, sndbuf, rcvbuf, security, linger);
    
    connector.connect(timeout);
  }
  
  @Override
  public TcpServerChannel tcpAccept(Reactor r, InetSocketAddress listenAddr,
                                    TcpServerChannelListener listener, Map<TcpServerOption, Object> options)
      throws IOException {
    checkStarted(r);
    Object attachment = null;
    int backlog = 100000;
    boolean tcpNoDelay = true;
    Boolean enableRead = Boolean.TRUE;
    Boolean useDirectBuffer = Boolean.FALSE;
    int autoFlushSize = 0;
    int sndbuf = 0;
    int rcvbuf = 0;
    Security security = null;
    long disableAcceptTimeout = 0; // will use default value    
    long linger = DEF_SO_LINGER;

    if (options != null) {
      attachment = options.get(TcpServerOption.ATTACHMENT);
      security = (Security) options.get(TcpServerOption.SECURITY);
      if (security == null) {    	  
    	  Boolean s = (Boolean) options.get(TcpServerOption.SECURE);
    	  if (s != null && s == true) {
    		  security = Helpers.getDefaulServerSSLConfig();
    	  }
      }
      Integer backlogI = (Integer) options.get(TcpServerOption.BACKLOG);
      if (backlogI != null) {
        backlog = backlogI.intValue();
      }
      Boolean noDelay = (Boolean) options.get(TcpServerOption.TCP_NO_DELAY);
      if (noDelay != null) {
        tcpNoDelay = noDelay;
      }
      if (options.get(TcpServerOption.ENABLE_READ) != null) {
        enableRead = (Boolean) options.get(TcpServerOption.ENABLE_READ);
      }
      Boolean direct = (Boolean) options.get(TcpServerOption.USE_DIRECT_BUFFER);
      if (direct != null) {
        useDirectBuffer = direct;
      }
      Integer i = (Integer) options.get(TcpServerOption.AUTO_FLUSH_SIZE);
      if (i != null) {
        autoFlushSize = i;
      }
      if (options.get(TcpServerOption.SO_SNDBUF) != null) {
        sndbuf = (Integer) options.get(TcpServerOption.SO_SNDBUF);
      }
      if (options.get(TcpServerOption.SO_RCVBUF) != null) {
        rcvbuf = (Integer) options.get(TcpServerOption.SO_RCVBUF);
      }
      if (options.get(TcpServerOption.DISABLE_ACCEPT_TIMEOUT) != null) {
          disableAcceptTimeout = (Long) options.get(TcpServerOption.DISABLE_ACCEPT_TIMEOUT);
          if (disableAcceptTimeout < 0) {
        	  throw new IllegalArgumentException("Invalid DISABLE_ACCEPT_TIMEOUT parameter value: value can't be negative: " + disableAcceptTimeout);
          }
      }
      if (options.get(TcpServerOption.LINGER) != null) {
    	  linger = (Long) options.get(TcpServerOption.LINGER);
      }
    }
    
    TcpServerChannelImpl channel = new TcpServerChannelImpl((ReactorImpl) r, listener, listenAddr,
        attachment, backlog, tcpNoDelay, enableRead, useDirectBuffer, autoFlushSize, sndbuf, rcvbuf,
        security, disableAcceptTimeout, linger);
    channel.listenSync();
    return channel;
  }
  
  @Override
  public UdpChannel udpBind(Reactor r, InetSocketAddress local, UdpChannelListener listener,
                            Map<UdpOption, Object> options) throws IOException {
    checkStarted(r);
    Object attachment = null;
    int priority = AsyncChannel.MAX_PRIORITY;
    Executor inputExec = r;
    Executor outputExec = Helpers.getDefaultOutputExecutor();
    Boolean enableRead = Boolean.TRUE;
    int sndbuf = 0;
    int rcvbuf = 0;
    boolean directBuffer = false;
    boolean ipTransparent = false;
    Security security = null;
    boolean isClient = false;
    long sessionTimeout = 5000L;

    if (options != null) {
      attachment = options.get(UdpOption.ATTACHMENT);
      Integer p = (Integer) options.get(UdpOption.PRIORITY);
      if (p != null) {
        priority = p;
        checkPriority(priority);
      }
      Executor e = (Executor) options.get(UdpOption.INPUT_EXECUTOR);
      if (e != null) {
        inputExec = e;
      }
      if (options.get(UdpOption.ENABLE_READ) != null) {
        enableRead = (Boolean) options.get(UdpOption.ENABLE_READ);
      }
      if (options.get(UdpOption.SO_SNDBUF) != null) {
        sndbuf = (Integer) options.get(UdpOption.SO_SNDBUF);
      }
      if (options.get(UdpOption.SO_RCVBUF) != null) {
        rcvbuf = (Integer) options.get(UdpOption.SO_RCVBUF);
      }
      Boolean direct = (Boolean) options.get(UdpOption.USE_DIRECT_BUFFER);
      if (direct != null) {
        directBuffer = direct;
      }
      
      Boolean transparent = (Boolean) options.get(UdpOption.IP_TRANSPARENT);
      if (transparent != null) {
        ipTransparent = transparent;
      }
      
      Security sec = (Security) options.get(UdpOption.SECURITY);
      if(sec != null) {
    	  security = sec;
    	  Boolean client = (Boolean) options.get(UdpOption.IS_CLIENT);
    	  isClient = client != null ? client : isClient;
      }
      Long ssTimeout = (Long) options.get(UdpOption.SESSION_TIMEOUT);
      if (ssTimeout != null) {
    	  sessionTimeout = ssTimeout;
      }
    }
    if(security == null) { 
    	UdpChannelImpl channel = new UdpChannelImpl((ReactorImpl) r, listener, local, priority, attachment,
    				inputExec, outputExec, enableRead, sndbuf, rcvbuf, directBuffer, ipTransparent);
    	channel.bindSync();
    	return channel;
    } else {
    	UdpChannelSecureImpl channel = new UdpChannelSecureImpl((ReactorImpl) r, listener, local, priority, attachment, 
    				inputExec, outputExec, enableRead, sndbuf, rcvbuf, directBuffer, ipTransparent, isClient, security, sessionTimeout);
    	channel.bindSync();
    	return channel;
    }
  }
  
  @Override
  public void sctpConnect(Reactor reactor, SocketAddress to, SctpClientChannelListener listener,
                          Map<SctpClientOption, Object> options) {
    checkStarted(reactor);
    SocketAddress localAddr = null;
    InetAddress[] secondaryLocalAddrs = null;
    int maxOutStreams = 0;
    int maxInStreams = 0;
    Object attachment = null;
    int priority = AsyncChannel.MAX_PRIORITY;
    long timeout = 0L;
    Executor exec = reactor;
    int rcvBufSize = 0, sndBufSize = 0;
    boolean directBuffer = false;
	Security security = null;
	boolean nodelay = true;
	Boolean disableFragments = null;
	Boolean fragmentInterleave = null;
	Map<SctpSocketOption, Object> sockopts = Collections.emptyMap();
	long linger = DEF_SO_LINGER;

    if (options != null) {
      if(_logger.isDebugEnabled()) _logger.debug("Options in sctpConnect " + options.get(SctpClientOption.SOCKET_OPTIONS));
     
      localAddr = (SocketAddress) options.get(SctpClientOption.LOCAL_ADDR);
      secondaryLocalAddrs = (InetAddress[]) options.get(SctpClientOption.SECONDARY_LOCAL_ADDRS);
      Integer maxOut = (Integer) options.get(SctpClientOption.MAX_OUT_STREAMS);
      if (maxOut != null) {
        maxOutStreams = maxOut;
      }
      Integer maxIn = (Integer) options.get(SctpClientOption.MAX_IN_STREAMS);
      if (maxIn != null) {
        maxInStreams = maxIn;
      }
      attachment = options.get(SctpClientOption.ATTACHMENT);
      Integer p = (Integer) options.get(SctpClientOption.PRIORITY);
      if (p != null) {
        priority = p;
        checkPriority(priority);
      }
      Long t = (Long) options.get(SctpClientOption.TIMEOUT);
      if (t != null) {
        timeout = t;
      }
      Executor e = (Executor) options.get(SctpClientOption.INPUT_EXECUTOR);
      if (e != null) {
        exec = e;
      }
      if (options.get(SctpClientOption.SO_RCVBUF) != null) {
        rcvBufSize = (Integer) options.get(SctpClientOption.SO_RCVBUF);
      }
      if (options.get(SctpClientOption.SO_SNDBUF) != null) {
        sndBufSize = (Integer) options.get(SctpClientOption.SO_SNDBUF);
      }
      if (options.get(SctpClientOption.USE_DIRECT_BUFFER) != null) {
        directBuffer = (Boolean) options.get(SctpClientOption.USE_DIRECT_BUFFER);
      }
      if (options.get(SctpClientOption.SECURITY) != null) {
    	security = (Security) options.get(SctpClientOption.SECURITY);
      }
      if (options.get(SctpClientOption.SOCKET_OPTIONS) != null) {
      	sockopts = (Map<SctpSocketOption, Object>) options.get(SctpClientOption.SOCKET_OPTIONS);
      }
      if(options.get(SctpClientOption.NO_DELAY) != null) {
    	nodelay = (Boolean) options.get(SctpClientOption.NO_DELAY);
      }
      if(options.get(SctpClientOption.DISABLE_FRAGMENTS) != null) {
      	disableFragments = (Boolean) options.get(SctpClientOption.DISABLE_FRAGMENTS);
      }
      if(options.get(SctpClientOption.FRAGMENT_INTERLEAVE) != null) {
    	  fragmentInterleave = (Boolean) options.get(SctpClientOption.FRAGMENT_INTERLEAVE);
      }
      if (options.get(SctpClientOption.LINGER) != null) {
    	  linger = (Long) options.get(SctpClientOption.LINGER);
      }
    }
    
    if (rcvBufSize == 0) {
      int n = Integer.getInteger("reactor.sctp.so_rcvbuf", -1);
      if (n != -1) {
        rcvBufSize = new Integer(n);
      }
    }
    if (sndBufSize == 0) {
      int n = Integer.getInteger("reactor.sctp.so_sndbuf", -1);
      if (n != -1) {
        sndBufSize = new Integer(n);
      }
    }
        
    SctpChannelConnector connector = new SctpChannelConnector(localAddr, secondaryLocalAddrs, maxOutStreams,
        maxInStreams, to, attachment, priority, listener, (ReactorImpl) reactor, exec, rcvBufSize, sndBufSize, 
        directBuffer, nodelay, disableFragments, fragmentInterleave, security, linger, sockopts);
    connector.connect(timeout);
  }
  
  @Override
  public SctpServerChannel sctpAccept(Reactor reactor, SocketAddress local,
                                      SctpServerChannelListener listener,
                                      Map<SctpServerOption, Object> options) throws IOException {
    checkStarted(reactor);
    InetAddress[] secondaryLocalAddrs = null;
    int maxOutStreams = 0;
    int maxInStreams = 0;
    Object attachment = null;
    int priority = AsyncChannel.MAX_PRIORITY;
    int backlog = 1024;
    int rcvBufSize = 0, sndBufSize = 0;
    Boolean autoStart = Boolean.TRUE;
    boolean directBuffer = false;
	Boolean disableFragments = null;
	Boolean fragmentInterleave = null;
	Security security = null;
	Map<SctpSocketOption, Object> sockopts = Collections.emptyMap();
	long disableAcceptTimeout = 0; // will use default value
	long linger = DEF_SO_LINGER;

    if (options != null) {
      if(_logger.isDebugEnabled()) _logger.debug("Options in sctpAccept " + options.get(SctpServerOption.SOCKET_OPTIONS));
      
      secondaryLocalAddrs = (InetAddress[]) options.get(SctpServerOption.SECONDARY_LOCAL_ADDRS);
      Integer maxOut = (Integer) options.get(SctpServerOption.MAX_OUT_STREAMS);
      if (maxOut != null) {
        maxOutStreams = maxOut;
      }
      Integer maxIn = (Integer) options.get(SctpServerOption.MAX_IN_STREAMS);
      if (maxIn != null) {
        maxInStreams = maxIn;
      }
      attachment = options.get(SctpServerOption.ATTACHMENT);
      Integer p = (Integer) options.get(SctpServerOption.PRIORITY);
      if (p != null) {
        priority = p;
        checkPriority(priority);
      }
      Integer backlogI = (Integer) options.get(SctpServerOption.BACKLOG);
      if (backlogI != null) {
        backlog = backlogI.intValue();
      }
      if (options.get(SctpServerOption.SO_RCVBUF) != null) {
        rcvBufSize = (Integer) options.get(SctpServerOption.SO_RCVBUF);
      }
      if (options.get(SctpServerOption.SO_SNDBUF) != null) {
        sndBufSize = (Integer) options.get(SctpServerOption.SO_SNDBUF);
      }
      if (options.get(SctpServerOption.ENABLE_READ) != null) {
        autoStart = (Boolean) options.get(SctpServerOption.ENABLE_READ);
      }
      if (options.get(SctpServerOption.USE_DIRECT_BUFFER) != null) {
        directBuffer = (Boolean) options.get(SctpServerOption.USE_DIRECT_BUFFER);
      }
      if (options.get(SctpServerOption.SECURITY) != null) {
    	security = (Security) options.get(SctpServerOption.SECURITY);
      }
      if (options.get(SctpServerOption.SOCKET_OPTIONS) != null) {
        	sockopts = (Map<SctpSocketOption, Object>) options.get(SctpServerOption.SOCKET_OPTIONS);
      }
      if(options.get(SctpServerOption.DISABLE_FRAGMENTS) != null) {
    	  disableFragments = (Boolean) options.get(SctpServerOption.DISABLE_FRAGMENTS);
      }
      if(options.get(SctpServerOption.FRAGMENT_INTERLEAVE) != null) {
    	  fragmentInterleave = (Boolean) options.get(SctpServerOption.FRAGMENT_INTERLEAVE);
      }
      if (options.get(SctpServerOption.DISABLE_ACCEPT_TIMEOUT) != null) {
          disableAcceptTimeout = (Long) options.get(SctpServerOption.DISABLE_ACCEPT_TIMEOUT);
          if (disableAcceptTimeout < 0) {
        	  throw new IllegalArgumentException("Invalid DISABLE_ACCEPT_TIMEOUT parameter value: value can't be negative: " + disableAcceptTimeout);
          }
      }    
      if(options.get(SctpServerOption.LINGER) != null) {
    	  linger = (Long) options.get(SctpServerOption.LINGER);
      }
    }
        
    if (rcvBufSize == 0) {
      int n = Integer.getInteger("reactor.sctp.so_rcvbuf", -1);
      if (n != -1) {
        rcvBufSize = new Integer(n);
      }
    }
    if (sndBufSize == 0) {
      int n = Integer.getInteger("reactor.sctp.so_sndbuf", -1);
      if (n != -1) {
        sndBufSize = new Integer(n);
      }
    }

    SctpServerChannelImpl ssc = new SctpServerChannelImpl(local, secondaryLocalAddrs, maxOutStreams,
        maxInStreams, priority, listener, (ReactorImpl) reactor, backlog, rcvBufSize, sndBufSize, autoStart, directBuffer, 
        disableFragments, fragmentInterleave, security, sockopts, disableAcceptTimeout, linger);
    ssc.attach(attachment);
    ssc.listenSync();
    return ssc;
  }
  
  TimerService getStrictTimerService() {
    return _strictTimerService;
  }
  
  TimerService getApproxTimerService() {
    return _approxTimerService;
  }
  
  PlatformExecutors getExecutors() {
    return _executors;
  }
  
  void setReactorThreadLocal(Reactor r) {
    _reactorThreadLocal.set(r);
  }
  
  NioSelector getSelector(AtomicInteger counter) {
	  return getSelector(counter, _selectors); 
  }
  
  NioSelector[] getSelectors() {
	  return _selectors;
  }

    /*
  TlsExportService getTlsExport() {
      return _tlsExport;
  }
    */
  
  private void initSelectors(NioSelector[] selectors) {
	  for (int i = 0; i < selectors.length; i++) {
		  try {
			  int count = _selectorsCount.incrementAndGet();
			  String selectorName = "Selector-" + count;
			  selectors[i] = new NioSelector(selectorName, _strictTimerService, _meters.newSelectorMeters(selectorName));
			  selectors[i].start(Executors.newSingleThreadExecutor((Runnable r) -> {
				  Thread t = new Thread(r, "Selector-" + count);
				  t.setDaemon(true);
				  return t;
			  }));
		  } catch (IOException e) {
			  throw new RuntimeException("Could not create Reactor NIO selectors", e);
		  }
	  }	        
  }

  private NioSelector getSelector(AtomicInteger counter, NioSelector[] selectors) {	
	  int index = counter.getAndIncrement() & Integer.MAX_VALUE; // make sure we have a positive value
	  return selectors[index % selectors.length];
  }
  
  private void checkStarted(Reactor r) {
    if (!((ReactorImpl) r).isStarted()) {
      throw new IllegalStateException("Reactor " + r.getName() + " is not started");
    }
  }
  
  private void checkPriority(int priority) {
    switch (priority) {
    case AsyncChannel.MAX_PRIORITY:
    case AsyncChannel.MIN_PRIORITY:
      break;
    default:
      throw new IllegalArgumentException("Invalid priority: " + priority);
    }
  }
  
  void closeReactorChannels(ReactorImpl reactorImpl, boolean abort) {
    for (NioSelector selector : _selectors) {
      selector.closeReactorChannels(reactorImpl, abort);
    }
  }
  
  void dump() {
    StringBuilder sb = new StringBuilder("Active reactor channels:");
    for (NioSelector selector : _selectors) {
      selector.dump(sb);
    }
    _logger.warn(sb.toString());
  }
    
  Meters getMeters() {
    return _meters;
  }

}
