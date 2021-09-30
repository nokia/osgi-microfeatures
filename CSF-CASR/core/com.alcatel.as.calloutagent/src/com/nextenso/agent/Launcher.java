package com.nextenso.agent;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_ID;
import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.HOST_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_ID;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.tracer.Log4jTracer;
import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.IPAddr;
import alcatel.tess.hometop.gateways.utils.Utils;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.management.ShutdownService;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.alcatel_lucent.as.service.dns.DNSFactory;
import com.nextenso.agent.Launcher.ListeningConnection.Protocol;
import com.nextenso.agent.event.AsynchronousEventScheduler;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.PlatformConstants;
import com.nextenso.proxylet.engine.ProxyletConstants;
import com.nextenso.proxylet.engine.ProxyletUtils;

public abstract class Launcher {
  public enum CloseReasons {
      SHUTDOWN, // Agent has been shot down
  }
  private static AtomicLong SEED = new AtomicLong (1L);
  
  protected static class ListeningConnection {
    private static final String GROUP_NAME_DEF = "def";
    public enum Protocol {
      TCP, SCTP, UDP;
    }
    
    private long _id;
    private Integer _socketId;
    private String[] _ip;
    private int _port;
    private boolean _isSecure;
    private Protocol _protocol = Protocol.TCP;
    private String _group;
    
    public ListeningConnection(long listenId, String group, String[] ip, int port, boolean isSecure, Protocol protocol) {
      _id = listenId;
      _group = group;
      _ip = ip;
      _port = port;
      _isSecure = isSecure;
      _protocol = protocol;
    }
    
    public final Integer getSocketId() {
      return _socketId;
    }
    
    public final void setSocketId(int id) {
      if (id >= 0) {
        _socketId = Integer.valueOf(id);
      } else {
        _socketId = null;
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("setSocketId: this=" + this);
      }
    }
    
    public final long getId() {
      return _id;
    }
    
    public final String getIP() {
      return _ip[0];
    }

    public final String[] getIPs (){
      return _ip;
    }
    
    public final int getPort() {
      return _port;
    }
    
    public final boolean isSecure() {
      return _isSecure;
    }
    
    public final Protocol getProtocol() {
      return _protocol;
    }

    public final String getGroup (){
      return _group;
    }
    
    @Override
    public String toString() {
      StringBuilder res = new StringBuilder("listening connection: ");
      res.append(getProtocol());
      res.append(", id=").append(_id);
      if (!GROUP_NAME_DEF.equals (_group)) res.append(", group=").append(_group);
      res.append(", socket id=").append(_socketId);
      for (String s: _ip) res.append(", IP=").append(s);
      res.append(", port=").append(_port);
      res.append(", secure=").append(_isSecure);
      return res.toString();
    }
    
  }
  
  public static final String LAUNCHER_APPLICATION_IMPL = "com.alcatel.as.proxylet.deployer.launcher.LauncherProxyletApplicationImpl";
  public static final String CONF_PREFIX_FILE = "file-";
  private static boolean _staticAttributesInitialized;
  private Config _config;
  private MuxHandler _wrappedMuxHandler;
  private static volatile ThreadPool _thPool;
  private static volatile AtomicInteger _refCount = new AtomicInteger(0);
  private static volatile MeteringService _meteringService;
  private static volatile PlatformExecutors _platformExecutors;
  private static volatile DNSFactory _dnsFactory;
  private HashMap<String, MuxConnection> _muxConnections = new HashMap<>();
  private final static Logger LOGGER = Logger.getLogger("launcher");
  //    private static final String SCTPMODE = "reactorconnection.sctp";
  private Reactor _reactor;
  private volatile int _errno = -1; // -1 means: not yet connect, O means OK, > 0 means
  private List<ListeningConnection> _connections = new ArrayList<Launcher.ListeningConnection>();
  private final MuxHandlerDesc _muxHandlerDesc;
  private final String _instanceName;
  private final MuxContext _muxCtx;
  private CountDownLatch _stopLatch;
  private final ShutdownService _shutdownService;
  private final static String CONNECT_TIMEOUT = "reactorConnection.connectionTimeout";
  
  protected abstract String getDescFile();
  
  protected abstract TcpMessageParser getTcpMessageParser();
  
  protected abstract int getStackAppId();
  
  protected abstract String getStackAppName();
  
  protected abstract void init(Config config, MuxHandler mh);
  
  protected MeteringService getMeteringService() {
    return _meteringService;
  }
  
  protected ShutdownService getShutdownService() {
    return _shutdownService;
  }
  
  protected PlatformExecutors getPlatformExecutors() {
    return _platformExecutors;
  }
  
  public static boolean isInLauncherMode() {
    return ProxyletUtils.isInLauncherMode();
  }
  
  protected static Config loadConfig(String s) throws ConfigException {
    Config config = new Config(s);
    loadFiles(config);
    return config;
  }
  
  public void updateConfig(String s) throws ConfigException {
    LOGGER.info("Launcher updating configuration from " + s);
    final Config newConfig = loadConfig(s);
    Runnable r = new Runnable() {
      
      @Override
      @SuppressWarnings("rawtypes")
      public void run() {
        Enumeration enumeration = newConfig.getKeys();
        while (enumeration.hasMoreElements()) {
          String key = (String) enumeration.nextElement();
          String val = newConfig.getProperty(key);
          _config.setProperty(key, val);
        }
        try {
          _config.notifyListeners();
        } catch (ConfigException ce) {
          LOGGER.error("Exception while updating configuration", ce);
        }
      }
    };
    _reactor.schedule(r);
  }
  
  public static void setConnectTimeout(int seconds) {
    System.setProperty(CONNECT_TIMEOUT, String.valueOf(seconds * 1000));
  }
  
  /**
   * Launcher constructor.
   */
  @SuppressWarnings("unchecked")
  protected Launcher(Object arg, Config config, final MuxHandler muxHandler) throws Exception {
    initStaticAttributes(config, MuxHandlerDescriptors.getFromResource(getDescFile()).getProtocol());
    
    _config = config;
    _shutdownService = ServiceLoader.loadClass(ShutdownService.class, null, null, null);
    
    // TODO: implements a ManagementService and inject it into ContextMonitor and
    // MuxMonitorImpl.
    
    // We wrap the mux handler with our own mux handler, which allows to bind on tcp/udp
    // addresses synchronously.
    _wrappedMuxHandler = new WrappedMuxHandler(muxHandler);
    
    // Populate the config with system properties.
    _config.setProperty(GROUP_NAME, _config.getProperty(GROUP_NAME, "Launchergroup"));
    _config.setProperty(INSTANCE_NAME, _config.getProperty(INSTANCE_NAME, getStackAppName()));
    _config.setProperty(HOST_NAME, _config.getProperty(HOST_NAME, InetAddress.getLocalHost().getHostName()));
    _config.setProperty(COMPONENT_NAME, _config.getProperty(COMPONENT_NAME, getStackAppName()));
    _config.setProperty(INSTANCE_PID, _config.getProperty(INSTANCE_PID, "0"));
    _config.setProperty(INSTANCE_ID, _config.getProperty(INSTANCE_ID, "0"));
    _config.setProperty(COMPONENT_ID, _config.getProperty(COMPONENT_ID, "0"));
    _config.setProperty(PLATFORM_ID, _config.getProperty(PLATFORM_ID, "0"));
    
    _config.setProperty(ProxyletConstants.PARAM_NAME_LAUNCHER_MODE, "true");
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX
        + ProxyletConstants.PARAM_NAME_LAUNCHER_MODE, "true");
    
    // We put the params into the Config so proxylets can read them when initializing
    String pid = String.valueOf(AgentConstants.AGENT_PID);
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_NAME,
                        AgentConstants.AGENT_APP_NAME);
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_ID,
                        String.valueOf(AgentConstants.AGENT_APP_ID));
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_PID,
                        pid);
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX
        + ProxyletConstants.PARAM_NAME_APP_INSTANCE, AgentConstants.AGENT_INSTANCE);
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_HOST,
                        AgentConstants.AGENT_HOSTNAME);
    
    _config.setProperty(PlatformConstants.CNF_COMPONENT_NAME, AgentConstants.AGENT_APP_NAME);
    _config.setProperty(PlatformConstants.CNF_COMPONENT_ID, String.valueOf(AgentConstants.AGENT_APP_ID));
    _config.setProperty(PlatformConstants.CNF_COMPONENT_PID, String.valueOf(AgentConstants.AGENT_PID));
    _config.setProperty(PlatformConstants.CNF_COMPONENT_GROUP, AgentConstants.AGENT_GROUP);
    _config.setProperty(PlatformConstants.CNF_COMPONENT_INSTANCE, AgentConstants.AGENT_INSTANCE);
    
    _config.setProperty(PlatformConstants.CNF_COMPONENT_HOST, AgentConstants.AGENT_HOSTNAME);
    _config.setProperty(PlatformConstants.CNF_COMPONENT_DBHOST, "localhost");
    _config.setProperty(PlatformConstants.CNF_COMPONENT_DBPORT, "2080");
    _config.setProperty(PlatformConstants.CNF_COMPONENT_UID, String.valueOf(AgentConstants.AGENT_UID));
    _config.setProperty(PlatformConstants.CNF_COMPONENT_GUID, String.valueOf(AgentConstants.GROUP_UID));
    _config.setProperty(PlatformConstants.CNF_COMPONENT_PFUID, String.valueOf(AgentConstants.PLATFORM_UID));
    
    // We also put them in the system properties
    System.setProperty(ProxyletConstants.PARAM_NAME_APP_NAME, AgentConstants.AGENT_APP_NAME);
    System.setProperty(ProxyletConstants.PARAM_NAME_APP_ID, String.valueOf(AgentConstants.AGENT_APP_ID));
    System.setProperty(ProxyletConstants.PARAM_NAME_APP_PID, pid);
    System.setProperty(ProxyletConstants.PARAM_NAME_APP_INSTANCE, AgentConstants.AGENT_INSTANCE);
    System.setProperty(ProxyletConstants.PARAM_NAME_APP_HOST, AgentConstants.AGENT_HOSTNAME);
    // Set the group
    System.setProperty(ProxyletConstants.CONFIG_CONTAINER_GROUP, AgentConstants.AGENT_GROUP);
    
    // init the launcher argument
    if (arg != null) {
      _config.put(ProxyletConstants.PARAM_NAME_LAUNCHER_ARG, arg);
    }
    
    // Init the muxHandler
    
    _muxHandlerDesc = MuxHandlerDescriptors.getFromResource(getDescFile());
    _reactor = ServiceLoader.getService(ReactorProvider.class).create(_muxHandlerDesc.getProtocol());
    _reactor.start();
    
    _muxCtx = new MuxContextImpl();
    ((MuxContextImpl) _muxCtx).setMuxHandler(muxHandler);
    _instanceName = Utils.removeSpaces(AgentConstants.AGENT_INSTANCE + "-" + _muxHandlerDesc.getProtocol());
    
    muxHandler.getMuxConfiguration().put("tracer.msg", new Log4jTracer(LOGGER));
    muxHandler.getMuxConfiguration().put("tracer.pxlet", new Log4jTracer(Logger.getLogger("pxlet")));
    muxHandler.getMuxConfiguration().put("reactor", _reactor);
        
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_NAME,
                        _muxHandlerDesc.getAppName());
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX + ProxyletConstants.PARAM_NAME_APP_ID,
                        String.valueOf(_muxHandlerDesc.getAppId()));
    _config.setProperty(ProxyletConstants.CONFIG_ALL_CONTEXTS_PREFIX
        + ProxyletConstants.PARAM_NAME_APP_INSTANCE, _instanceName);
    
    // Initialize the launcher and the mux handler in the proper reactor thread.
    Callable<Throwable> callable = new Callable<Throwable>() {
      @Override
      public Throwable call() throws Exception {
        try {
          LOGGER.info("Initializing agent " + getStackAppName());
          init(_config, muxHandler);
          Context.setMuxHandlerLocal(muxHandler);
          muxHandler.init(_muxHandlerDesc.getAppId(), _muxHandlerDesc.getAppName(), _instanceName, _muxCtx);
          muxHandler.init(_config);
          return null;
        } catch (Throwable t) {
          return t;
        }
      }
    };
    FutureTask<Throwable> ft = new FutureTask<Throwable>(callable);
    _reactor.schedule(ft);
    try {
      Throwable t = ft.get(10000, TimeUnit.MILLISECONDS);
      if (t != null) {
        LOGGER.error("Could not initialize agent " + getStackAppName(), t);
      }
    }
    
    catch (Throwable t2) {
      LOGGER.error("Could not initialize agent " + getStackAppName(), t2);
    }
    
    Enumeration enumeration = _config.getKeys("reactor.listen.*");
    while (enumeration.hasMoreElements ()){
      String key = (String) enumeration.nextElement ();
      if (key.endsWith (".group") == false) continue;
      String group = (String) _config.get (key);
      if (_muxConnections.get (group) != null){
	  continue;
      }
      LOGGER.warn ("Defined new MuxConnection : group = "+group);
      // Initialize mux connection. The MuxHandler.muxOpen methods will be called by
      // the MuxFactory.newLocalMuxConnection method.
      
      // TODO clean this ! the newLocalMuxConnection should not be used anymore

      MuxConnection muxConnection = MuxFactory.getInstance().newLocalMuxConnection(_reactor, _wrappedMuxHandler,
										   getStackAppId(), getStackAppName (), group,
										   getTcpMessageParser(), Logger.getLogger("reactor."+group));
      _muxConnections.put (group, muxConnection);
    }
    if (_muxConnections.size () == 0)
      // usual case when no group is used
      _muxConnections.put (ListeningConnection.GROUP_NAME_DEF, MuxFactory.getInstance().newLocalMuxConnection(_reactor, _wrappedMuxHandler,
													      getStackAppId(), getStackAppName(), ListeningConnection.GROUP_NAME_DEF,
													      getTcpMessageParser(), Logger.getLogger("reactor")));
    
    boolean ipv6Support = Boolean.TRUE.equals(muxHandler.getMuxConfiguration()
        .get(MuxHandler.CONF_IPV6_SUPPORT));
    
    readAddresses(ipv6Support);
  }
  
  @SuppressWarnings("rawtypes")
  private void readAddresses(boolean ipv6Support) throws Exception {
    
    String reactorIP = _config.getString("reactor.ip", null);
    List<String> list = getIPlist(reactorIP, ipv6Support, false);
    
    boolean isSecure = _config.getBoolean("reactor.listen.tls", false)
        || _config.getBoolean("reactor.listen.secure", false);
    

    String portRegexp = "reactor.listen.[0-9]+";
    Enumeration enumeration = _config.getKeys("reactor.listen.*");
    
    /**
     * for compliancy the port can be either
     * - reactor.listen.1 = 3868	// legacy mode
     * - reactor.listen.1.port=3868	// new mode
     */
    
    _connections.clear();
    while (enumeration.hasMoreElements()) {
      try {
        String key = (String) enumeration.nextElement();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("constr: the key=" + key);
        }
	String val = null;
	if (key.matches(portRegexp)) {
	  val = _config.getString(key);
	} else if (key.endsWith (".port")){
	  val = _config.getString(key);
	  key = key.substring (0, key.length () - ".port".length ());
	} else
          continue;
	String type = _config.getString(key + ".type", null);
        boolean isSecureSpecific = _config.getBoolean(key+".secure", isSecure);
	String group = _config.getString(key + ".group", ListeningConnection.GROUP_NAME_DEF);
	if (_muxConnections.get (group) == null){
	  // possible if a ListeningConnection has no group name specified while others have (then the def was not created)
	  // we know group = GROUP_NAME_DEF
	  _muxConnections.put (ListeningConnection.GROUP_NAME_DEF, MuxFactory.getInstance().newLocalMuxConnection(_reactor, _wrappedMuxHandler,
														  getStackAppId(), getStackAppName(), group,
														  getTcpMessageParser(), Logger.getLogger("reactor")));
	}
	Protocol protocol = Protocol.TCP;
        if ("udp".equalsIgnoreCase(type)) {
          protocol = Protocol.UDP;
        } else if ("sctp".equalsIgnoreCase(type)) {
          protocol = Protocol.SCTP;
        }
	reactorIP = _config.getString(key+".ip", null);
	List<String> specificIPs = list;
	if (reactorIP != null) {
	  specificIPs = getIPlist(reactorIP, ipv6Support, false);
	}
	if (LOGGER.isDebugEnabled()) {
	  LOGGER.debug("readAddresses: accepted addresses=" + specificIPs);
	}
        int port = Integer.parseInt(val);
	if (protocol == Protocol.SCTP){
	  long listenId = SEED.getAndIncrement ();
	  String[] ips = specificIPs.toArray (new String[0]);
	  ListeningConnection connection = new ListeningConnection(listenId, group, ips, port, isSecureSpecific, protocol);
	  _connections.add(connection);
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("readAddresses: add connection= " + connection);
	  }
	} else 
	  for (String ip : specificIPs) {
	    long listenId = SEED.getAndIncrement ();
	    String[] ips = new String[]{ip};
	    ListeningConnection connection = new ListeningConnection(listenId, group, ips, port, isSecureSpecific, protocol);
	    _connections.add(connection);
	    if (LOGGER.isDebugEnabled()) {
	      LOGGER.debug("readAddresses: add connection= " + connection);
	    }
	  }
      } catch (Throwable t) {
        LOGGER.error("constr: cannot build the launcher", t);
        stop();
        if (t instanceof Exception) {
          throw (Exception) t;
        }
        throw new Exception("cannot build the launcher", t);
      }
    }
  }
  
    protected static List<String> getIPlist(String reactorIP, boolean ipv6Support, boolean resolve0000) throws Exception {
    List<String> list = new ArrayList<String>();
    if (reactorIP == null) {
      return list;
    }
    
    if (!resolve0000 || !reactorIP.equals("0.0.0.0")) {
      for (StringTokenizer tok = new StringTokenizer(reactorIP, " ,"); tok.hasMoreTokens();) {
        String s = tok.nextToken().trim();
        try {
          IPAddr addr = new IPAddr(s);
          // If the mux handler does not support ipv6, we must refuse an ipv6 address.
          if (!ipv6Support && addr.isIPv6()) {
            throw new IllegalArgumentException("Agent does not support ipv6 address: " + s);
          }
          list.add(s);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getIPlist: add ip (reactor.ip parsing)=" + s);
          }
        } catch (Exception e) {
          LOGGER.warn("The reactor.ip properties contains a not well formed IP address: " + s
              + " -> the address is ignored", e);
        }
      }
    } else {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
	NetworkInterface ni = interfaces.nextElement();
	Enumeration<InetAddress> addresses = ni.getInetAddresses();
	while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address instanceof Inet4Address || (ipv6Support && address instanceof Inet6Address)) {
            String s = address.getHostAddress();
            int index = s.indexOf("%");
            if (index > 0) {
              s = s.substring(0, index);
            }
            
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("getIPlist: ip (from 0.0.0.0)=" + s);
            }
            list.add(s);
          }
        }
      }
    }
    
    return list;
  }
  
  private void setSocketId(long listenId, int errno, String localIP, int localPort, int socketId) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("setSocketId: errno=" + errno + ", localIP=" + localIP + ", localPort=" + localPort
          + " , socketId=" + socketId);
    }
    if (errno == 0) {
      // find the listening connection and sets the sockId
      for (ListeningConnection c : _connections) {
        if (c.getId() == listenId) {
          c.setSocketId(socketId);
          break;
        }
      }
    }
  }
  
  /**
   * Method invoked by the WrappedMuxHandler when a tcp/udp address is bound.
   * 
   * @param errno 0 if the address is bound, or a positive number on any errors.
   */
  private synchronized void setConnectStatus(int errno) {
    
    if (_errno == -1) {
      _errno = errno;
      notifyAll();
    }
  }
  
  public void startListening() throws InterruptedException, IOException {
    for (ListeningConnection connection : _connections) {
      _errno = -1;
      
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("startListening: start connection= " + connection);
      }
      
      // Use proper
      boolean listening = false;
      MuxConnection muxConnection = _muxConnections.get (connection.getGroup ());
      // bind synchronously
      if (connection.getProtocol() == Protocol.TCP) {
        listening = muxConnection.sendTcpSocketListen(connection.getId(), connection.getIP(),
						      connection.getPort(), connection.isSecure());
      } else if (connection.getProtocol() == Protocol.UDP) {
        listening = muxConnection.sendUdpSocketBind(connection.getId(), connection.getIP(),
						    connection.getPort(), true);
      } else if (connection.getProtocol() == Protocol.SCTP) {
	listening = muxConnection.sendSctpSocketListen(connection.getId(),
						       connection.getIPs (),
						       connection.getPort(), 5, //maxOutStreams
						       5, false); //maxInStreams
      }
      if (!listening) {
        throw new IllegalStateException("Reactor not ready while bind to " + connection);
      }
      
      synchronized (this) {
        while (_errno == -1) {
          try {
            wait();
          } catch (InterruptedException e) {
            throw new InterruptedException("Operation interrupted while binding to " + connection);
          }
        }
      }
      
      if (_errno != 0) {
        throw new IOException("Could not listen on tcp address " + connection);
      }
    }
  }
  
  public void stopListening() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("stopListening:connections= " + _connections);
    }
    for (ListeningConnection connection : _connections) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("stopListening: stop connection= " + connection);
      }
      if (connection.getSocketId() != null) {
	MuxConnection muxConnection = _muxConnections.get (connection.getGroup ());
        if (connection.getProtocol() == Protocol.TCP) {
          muxConnection.sendTcpSocketClose(connection.getSocketId());
        } else if (connection.getProtocol() == Protocol.UDP) {
          muxConnection.sendUdpSocketClose(connection.getSocketId());
        } else if (connection.getProtocol() == Protocol.SCTP) {
          muxConnection.sendSctpSocketClose(connection.getSocketId());
        }
      }
    }
  }
  
  protected void stop() {
    LOGGER.warn("Stopping agent " + getStackAppName());
    _stopLatch = new CountDownLatch(1);
    
    _shutdownService.shutdown(new Runnable() {
      @Override
      public void run() {
        // This will invoke muxHandler.muxClosed() in the reactor thread.
        // And from our wrapped muxClosed method, we'll then count down our latch
        for (MuxConnection muxConnection : _muxConnections.values ())
            muxConnection.close(CloseReasons.SHUTDOWN, "shutdown", null);
      }
    }, 10000);
    
    try {
      if (!_stopLatch.await(15000, TimeUnit.MILLISECONDS)) {
        LOGGER.error("could not stop agent " + getStackAppName() + " timely");
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Thread Interrupted while stopping agent " + getStackAppName(), e);
    }
  }
  
  // called when our wrapped mux handler has been invoked in muxClosed.
  protected void stopped() {
    LOGGER.warn("Stopped " + getStackAppName());
    
    if (_reactor != null) {
      LOGGER.info("Stopping reactor for " + _reactor.getName());
      _reactor.stop();
      _reactor = null;
    }
        
    // Stop shared resources
    if (_refCount.decrementAndGet() <= 0) {
      if (_thPool != null) {
        LOGGER.info("Stopping thread pool");
        _thPool.terminate(true);
        _thPool.join(true);
        _thPool = null;
      }
    }
    
    LOGGER.info("Launcher stopped for " + getStackAppName());
    _stopLatch.countDown();
  }
  
  /**
   * Method called each time a Launcher is created.
   * 
   * @param config
   * @param protocol
   * @throws ConfigException
   */
  private static synchronized void initStaticAttributes(Config config, String protocol)
      throws ConfigException {
    // Initialize list of protocols
    String[] protocols = AgentConstants.PROTOCOLS;
    if (protocols == null) {
      protocols = new String[] { protocol };
    } else {
      String[] tmp = new String[protocols.length + 1];
      System.arraycopy(protocols, 0, tmp, 0, protocols.length);
      tmp[protocols.length] = protocol;
      protocols = tmp;
    }
    AgentConstants.PROTOCOLS = protocols;
    
    if (_staticAttributesInitialized) {
      // If we goes here, it means that a previous launcher has been created. Just
      // increment the ThreadPool counter usage (we'll destroy the thread pool from 
      // the stop() method, only when the ref counter equals 0).
      _refCount.incrementAndGet();
      return;
    }
    
    // Set Launcher Mode.
    ProxyletUtils.setIsInAgentMode(true);
    
    // Load system.properties. If the "system.properties" is not found, then load system
    // config from the provided config file.
    
    Config system = null;
    try {
      system = new Config("system.properties");
      loadFiles(system);
    } catch (ConfigException e) {
      LOGGER.warn("Loading system configuration from property " + config.getName());
      system = config;
    }
    
    // Set the connection timeout
    setConnectTimeout(system.getInt("connect.timeout", 15));
    
    // Init the Metering Service.
    _meteringService = (MeteringService) ServiceLoader.loadClass(MeteringService.class, null,
                                                                 new Object[] { system },
                                                                 new Class[] { Dictionary.class });
           
    // Load into System properties all files found in resource/properties/*.properties
    Utils.loadSystemProperties();
    
    // Init the AgentConstants and the MuxContextImpl
    AgentConstants.AGENT_HOSTNAME = system.getString("HOSTNAME", "localhost");
    AgentConstants.AGENT_PID = -1;
    AgentConstants.AGENT_APP_NAME = system.getString("APP_NAME", "Launcher");
    AgentConstants.AGENT_APP_ID = -1;
    AgentConstants.AGENT_INSTANCE = "Launcher";
    AgentConstants.AGENT_GROUP = "";
    AgentConstants.AGENT_UID = -1;
    AgentConstants.PLATFORM_UID = "";
    CRC32 crc = new CRC32();
    crc.update(AgentConstants.AGENT_GROUP.getBytes());
    AgentConstants.GROUP_UID = crc.getValue();
        
    // PlatformExecutors initialization. Notice that the MeteringService must be initialized
    // prior to loading the PlatformExecutor implementation ...
    _platformExecutors = ServiceLoader.loadClass(PlatformExecutors.class, null, new Object[] { system },
                                                 new Class[] { Dictionary.class });
    
    // Legacy ThreadPool
    _thPool = ThreadPool.getInstance();

    // DNS Service initialization
    _dnsFactory = ServiceLoader.loadClass(DNSFactory.class, null, new Object[] { system },
                                          new Class[] { Dictionary.class });
    
    // Mux Context initialization
    MuxContextImpl.init(_thPool);
    
    // Init asynchronous scheduler
    AsynchronousEventScheduler.init(system, _platformExecutors);
    _refCount.incrementAndGet();
    _staticAttributesInitialized = true;
  }
  
  @SuppressWarnings("rawtypes")
  private static void loadFiles(Config config) throws ConfigException {
    mainloop: while (true) {
      Enumeration enumeration = config.getKeys(CONF_PREFIX_FILE + "*");
      while (enumeration.hasMoreElements()) {
        String key = (String) enumeration.nextElement();
        String val = config.getContentFileFromCP(key);
        config.removeProperty(key);
        config.setProperty(key.substring(CONF_PREFIX_FILE.length()), val);
        continue mainloop;
      }
      break;
    }
  }
  
  /**
   * This class allow to bind (tcp/udp) synchronously. We just wrap the mux
   * handler with our own mux handler, which contains two additional
   * tcpBind/udpBind methods ...
   */
  class WrappedMuxHandler extends MuxHandler {
    
    MuxHandler _wrapped;
    
    // "could not connect"
    
    public WrappedMuxHandler(MuxHandler wrapped) {
      _wrapped = wrapped;
    }
    
    // Here, we just delegate methods to our concrete mux handler.
    
    @Override
    public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort,
                                   boolean secure, long listenId, int errno) {
      _wrapped.tcpSocketListening(connection, sockId, localIP, localPort, secure, listenId, errno);
      setSocketId(listenId, errno, localIP, localPort, sockId);
      setConnectStatus(errno);
    }
    
    @Override
    public void udpSocketBound(MuxConnection connection, int sockId, String localIP, int localPort,
                               boolean shared, long bindId, int errno) {
      _wrapped.udpSocketBound(connection, sockId, localIP, localPort, shared, bindId, errno);
      setSocketId(bindId, errno, localIP, localPort, sockId);
      setConnectStatus(errno);
    }
    
    @Override
    public boolean accept(int stackAppId, String stackName, String stackHost, String stackInstance) {
      return _wrapped.accept(stackAppId, stackName, stackHost, stackInstance);
    }
    
    @Override
    public void commandEvent(int command, int[] intParams, String[] strParams) {
      _wrapped.commandEvent(command, intParams, strParams);
    }
    
    @Override
    public void destroy() {
      _wrapped.destroy();
    }
    
    @Override
    public void dnsGetByAddr(long reqId, String[] response, int errno) {
      _wrapped.dnsGetByAddr(reqId, response, errno);
    }
    
    @Override
    public void dnsGetByName(long reqId, String[] response, int errno) {
      _wrapped.dnsGetByName(reqId, response, errno);
    }
    
    @Override
    public boolean equals(Object arg0) {
      return _wrapped.equals(arg0);
    }
    
    @Override
    public int[] getCounters() {
      return _wrapped.getCounters();
    }
    
    @Override
    public int getMajorVersion() {
      return _wrapped.getMajorVersion();
    }
    
    @Override
    public int getMinorVersion() {
      return _wrapped.getMinorVersion();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Hashtable getMuxConfiguration() {
      return _wrapped.getMuxConfiguration();
    }
        
    @Override
    public int hashCode() {
      return _wrapped.hashCode();
    }
    
    @Override
    public void init(Config cnf) throws ConfigException {
      _wrapped.init(cnf);
    }
    
    @Override
    public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
      _wrapped.init(appId, appName, appInstance, muxContext);
    }
    
    @Override
    public void muxOpened(MuxConnection connection) {
      _wrapped.muxOpened(connection);
    }
    
    @Override
    public void muxClosed(MuxConnection connection) {
      LOGGER.info("Agent " + getStackAppName() + " closed");
      _wrapped.muxClosed(connection);
      _wrapped.destroy();
      
      Long delay = (Long) _wrappedMuxHandler.getMuxConfiguration().get(MuxHandler.CONF_EXIT_DELAY);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("stop: handler delay=" + delay);
      }
      
      if (delay != null && delay > 0) {
        long stopDate = System.currentTimeMillis() + delay * 1000;
        while (stopDate - System.currentTimeMillis() > 0) {
          try {
            long remaining = stopDate - System.currentTimeMillis();
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("stop: handler remaining delay=" + remaining);
            }
            Thread.sleep(remaining);
          } catch (InterruptedException e) {
            LOGGER.warn("sleep has been interrupted while not expected");
          }
        }
      }
      
      stopped(); // finish to stop the agent          
    }
    
    @Override
    public void muxData(MuxConnection connection, MuxHeader header, byte[] data, int off, int len) {
      _wrapped.muxData(connection, header, data, off, len);
    }
    
    @Override
    public void muxData(MuxConnection connection, MuxHeader header, ByteBuffer data) {
      _wrapped.muxData(connection, header, data);
    }
    
    @Override
    public void muxGlobalEvent(int identifierI, String identifierS, byte[] data, int off, int len) {
      _wrapped.muxGlobalEvent(identifierI, identifierS, data, off, len);
    }
    
    @Override
    public void muxLocalEvent(int identifierI, String identifierS, Object data) {
      _wrapped.muxLocalEvent(identifierI, identifierS, data);
    }
    
    @Override
    public void releaseAck(MuxConnection connection, long sessionId) {
      _wrapped.releaseAck(connection, sessionId);
    }
    
    @Override
    public void tcpSocketClosed(MuxConnection connection, int sockId) {
      _wrapped.tcpSocketClosed(connection, sockId);
      for (ListeningConnection c : _connections) {
        if (c.getSocketId() != null && c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort,
                                   int localIP, int localPort, int virtualIP, int virtualPort,
                                   boolean secure, boolean clientSocket, long connectionId, int errno) {
      _wrapped.tcpSocketConnected(connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP,
                                  virtualPort, secure, clientSocket, connectionId, errno);
    }
    
    @Override
    public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
                                   String localIP, int localPort, String virtualIP, int virtualPort,
                                   boolean secure, boolean clientSocket, long connectionId, int errno) {
      _wrapped.tcpSocketConnected(connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP,
                                  virtualPort, secure, clientSocket, connectionId, errno);
    }
    
    @Override
    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off,
                              int len) {
      _wrapped.tcpSocketData(connection, sockId, sessionId, data, off, len);
    }
    
    @Override
    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data) {
      _wrapped.tcpSocketData(connection, sockId, sessionId, data);
    }
    
    @Override
    public void tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort,
                                   boolean secure, long listenId, int errno) {
      _wrapped.tcpSocketListening(connection, sockId, localIP, localPort, secure, listenId, errno);
      setSocketId(listenId, errno, MuxUtils.getIPAsString(localIP), localPort, sockId);
      setConnectStatus(errno);
    }
    
    @Override
    public String toString() {
      return _wrapped.toString();
    }
    
    @Override
    public void udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort,
                               boolean shared, long bindId, int errno) {
      _wrapped.udpSocketBound(connection, sockId, localIP, localPort, shared, bindId, errno);
      setSocketId(bindId, errno, MuxUtils.getIPAsString(localIP), localPort, sockId);
      setConnectStatus(errno);
    }
    
    @Override
    public void udpSocketClosed(MuxConnection connection, int sockId) {
      _wrapped.udpSocketClosed(connection, sockId);
      for (ListeningConnection c : _connections) {
        if (c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP,
                              int remotePort, int virtualIP, int virtualPort, byte[] data, int off, int len) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort,
                             data, off, len);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP,
                              int remotePort, int virtualIP, int virtualPort, ByteBuffer data) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort,
                             data);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP,
                              int remotePort, String virtualIP, int virtualPort, byte[] data, int off, int len) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort,
                             data, off, len);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP,
                              int remotePort, String virtualIP, int virtualPort, ByteBuffer data) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort,
                             data);
    }
    
    @Override
    public void tcpSocketAborted(MuxConnection connection, int sockId) {
      _wrapped.tcpSocketAborted(connection, sockId);
    }
    
    /**
     * @see com.nextenso.mux.MuxHandler#sctpSocketListening(com.nextenso.mux.MuxConnection, int, long, java.lang.String[], int, int)
     */
    @Override
    public void sctpSocketListening(MuxConnection cnx, int sockId, long listenerId, String[] localAddrs,
                                    int localPort, boolean secure, int errno) {
	_wrapped.sctpSocketListening(cnx, sockId, listenerId, localAddrs, localPort, secure, errno);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("sctpSocketListening: localAddrs=" + Arrays.asList(localAddrs));
      }
      setSocketId(listenerId, errno, localAddrs[0], localPort, sockId);
      setConnectStatus(errno);
    }
    
    /**
     * @see com.nextenso.mux.MuxHandler#sctpSocketConnected(com.nextenso.mux.MuxConnection, int, long, java.lang.String[], int, java.lang.String[], int, int, int, boolean, int)
     */
    @Override
    public void sctpSocketConnected(MuxConnection cnx, int sockId, long listenerId, String[] remoteAddrs,
                                    int remotePort, String[] localAddrs, int localPort, int maxOutStreams,
                                    int maxInStreams, boolean fromClient, boolean secure, int errno) {
      _wrapped.sctpSocketConnected(cnx, sockId, listenerId, remoteAddrs, remotePort, localAddrs, localPort,
                                   maxOutStreams, maxInStreams, fromClient, secure, errno);
    }
    
    @Override
    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data,
                               String addr, boolean isUnordered, boolean isComplete, int ploadPID,
                               int streamNumber) {
      _wrapped.sctpSocketData(connection, sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID,
                              streamNumber);
    }
    
    @Override
    public void sctpSocketClosed(MuxConnection cnx, int sockId) {
      _wrapped.sctpSocketClosed(cnx, sockId);
      for (ListeningConnection c : _connections) {
        if (c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void sctpSocketSendFailed(MuxConnection cnx, int sockId, String addr, int streamNumber,
                                     ByteBuffer buf, int errcode) {
      _wrapped.sctpSocketSendFailed(cnx, sockId, addr, streamNumber, buf, errcode);
    }
    
    @Override
    public void sctpPeerAddressChanged(MuxConnection connection, int sockId, String addr, int port,
                                       MuxHandler.SctpAddressEvent event) {
      _wrapped.sctpPeerAddressChanged(connection, sockId, addr, port, event);
    }
  }
}
