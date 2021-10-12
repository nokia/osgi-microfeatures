// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl.local;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProviderCompatibility;
import alcatel.tess.hometop.gateways.utils.Config;
import alcatel.tess.hometop.gateways.utils.ConfigException;
import alcatel.tess.hometop.gateways.utils.IPAddr;
import alcatel.tess.hometop.gateways.utils.Log;

import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.socket.TcpMessageParser;
import com.nextenso.mux.util.MuxUtils;

/**
 * This service is used in standalone mode, where stacks (ioh) are emulated.
 */
public class LocalMuxFactoryImpl extends MuxFactory implements EventHandler {
  // Our logger
  private final static Log _logger = Log.getLogger("as.service.mux.LocalMuxFactoryImpl");
  
  // Event admin used to receive start_listening/stop_listening events from launcher.
  private volatile EventAdmin _eventAdmin;
  
  // Reactor Provider used to create Reactors
  protected ReactorProvider _reactorProvider = ReactorProviderCompatibility.provider();
  
  // Configuration (containing all stack informations)
  protected Dictionary<String, String> _conf;
  
  // Map between stack instance names and corresponding listening Map<StackInstance, ListeningConnection>
  protected final List<ListeningConnection> _listeningCnx = new ArrayList<ListeningConnection>();
  
  // Our system configuration (injected by DM)
  private Dictionary<String, String> _system;
  
  // Our bundle context (auto injected)
  private BundleContext _bctx;
  
  // List of stack adverts registered in the osgi service registry.
  final List<ServiceRegistration> _stackAdverts = new ArrayList<>();
  
  // Our group name, found from system config.
  private String _groupName;
  
  private static AtomicLong SEED = new AtomicLong(1L);
  
  enum Protocol {
    TCP, UDP, SCTP
  }
  
  /**
   * A Stack listening connection (some stacks must be bound automatically by our service).
   */
  protected class ListeningConnection {
    private long _id;
    private Integer _socketId;
    private final String[] _ip;
    private final int _port;
    private final boolean _isSecure;
    private final Protocol _protocol;
    private final String _stackAppName, _stackInstance;
    private MuxConnection _cnx; // mux cnx used to listen.
    
    public ListeningConnection(long listenId, String[] ip, int port, Protocol protocol, boolean isSecure,
                               String stackAppName, String stackInstance) {
      _id = listenId;
      _ip = ip;
      _port = port;
      _isSecure = isSecure;
      _protocol = protocol;
      _stackAppName = stackAppName;
      _stackInstance = _groupName + "__" + stackInstance;
    }
    
    public String getStackAppName() {
      return _stackAppName;
    }

    public String getSackInstance() {
      return _stackInstance;
    }
    
    public final Integer getSocketId() {
      return _socketId;
    }
    
    public final void setSocketId(int id) {
      _logger.debug("setSocketId: id=%d", id);
      
      if (id >= 0) {
        _socketId = Integer.valueOf(id);
      } else {
        _socketId = null;
      }
      if (_logger.isDebugEnabled()) {
        _logger.debug("setSocketId: this=" + this);
      }
    }
    
    public final long getId() {
      return _id;
    }
    
    public final String getIP() {
      return _ip[0];
    }
    
    public final String[] getIPs() {
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
    
    @Override
    public String toString() {
      StringBuilder res = new StringBuilder("listening connection: ");
      switch (_protocol) {
      case TCP:
        res.append("TCP");
        break;
      case SCTP:
        res.append("SCTP");
        break;
      case UDP:
        res.append("UDP");
        break;
      }
      
      res.append(", id=").append(_id);
      res.append(", socket id=").append(_socketId);
      res.append(", IP=").append(Arrays.toString(_ip));
      res.append(", port=").append(_port);
      res.append(", secure=").append(_isSecure);
      return res.toString();
    }
    
    public void listen(MuxConnection cnx) {
      _cnx = cnx;
      
      boolean listening = false;
      switch (_protocol) {
      case TCP:
        listening = cnx.sendTcpSocketListen(getId(), getIP(), getPort(), isSecure());
        break;
      case UDP:
        listening = cnx.sendUdpSocketBind(getId(), getIP(), getPort(), true);
        break;
      case SCTP:
        listening = cnx.sendSctpSocketListen(getId(), getIPs(), getPort(), 5, //maxOutStreams
                                             5, //maxInStreams
					     false); // no DTLS
        break;
      }
    }
  }
  
  protected void updated(Dictionary<String, String> conf) {
    _conf = conf;
  }
  
  protected void start() throws Exception {
    // Load all ioh instances which must be auto-listened
    _logger.debug("localstacks.cfg=%s", _conf);
    _logger.debug("system.cfg=%s", _system);
    _groupName = ConfigHelper.getString(_system, ConfigConstants.GROUP_NAME);
    
    if (_groupName == null) {
      _logger.error("did not find group name in system.cfg");
    }
    
    String stacks = ConfigHelper.getString(_conf, "stacks", null, true);
    if (stacks == null) {
      _logger.info("No stack will be auto listened (\"stacks\" parameter is not set in localstacks.cfg)");
      return;
    }
    
    // read stack listening points
    
    int index = 1;
    for (String stack : stacks.split(",")) {
      stack = stack.trim();
      // Initialize stack address listening points
      readAddressesFor(stack);
      
      // Register advertisements for this stack. Notice that this discovery is not injected to the callout agent,
      // this is the job of the avert tracker, which will re-register our discovery with a provider=XXX attribute ...
      String appName = ConfigHelper.getString(_conf, stack + ".appName", null, true);
      if (appName == null) {
        _logger.error("missing app name for stack " + stack + " in localstacks.cfg");
      }
      
      int appId = ConfigHelper.getInt(_conf, stack + ".appId", -1);
      if (appId == -1) {
        _logger.error("missing app id for stack " + stack + " in localstacks.cfg");
      }
      
      Hashtable props = new Hashtable();
      props.put(ConfigConstants.COMPONENT_NAME, appName); // similar to APP field from monconf/Application.XXX
      props.put(ConfigConstants.MODULE_ID, appId); // similar to ID field from monconf/Application.XXX
      props.put("mux.factory.local", "local");
      props.put("mux.factory.remote", "local");
      Advertisement advert = new Advertisement("127.0.0.1", 0);
      _logger.info("Registering stack advert: stakName=%s, stackInstance=%s", appName, stack);
      _stackAdverts.add(_bctx.registerService(Advertisement.class.getName(), advert, props));
    }
    
    if (_logger.isDebugEnabled()) {
      _logger.debug("Found autobind listening addresses: " + _listeningCnx);
    }
  }
  
  private void readAddressesFor(String stack) throws Exception {
    String stackAppName = ConfigHelper.getString(_conf, stack + ".appName");
    boolean ipv6Support = ConfigHelper.getBoolean(_conf, stack + ".ipv6Support", false);
    String reactorIP = ConfigHelper.getString(_conf, stack + ".ip", null);
    List<String> list = getIPlist(reactorIP, ipv6Support);
    boolean isSecure = ConfigHelper.getBoolean(_conf, stack + ".listen.tls", false)
        || ConfigHelper.getBoolean(_conf, stack + ".listen.secure", false);
    
    String portRegexp = stack + ".listen.[0-9]+";
    Enumeration enumeration = getKeys(stack + ".listen.*");
    
    /**
     * for compliancy the port can be either
     * - reactor.listen.1 = 3868  // legacy mode
     * - reactor.listen.1.port=3868 // new mode
     */
    
    while (enumeration.hasMoreElements()) {
      try {
        String key = (String) enumeration.nextElement();
        _logger.debug("constr: the key=%s", key);
        String val = null;
        if (key.matches(portRegexp)) {
          val = ConfigHelper.getString(_conf, key);
        } else if (key.endsWith(".port")) {
          val = ConfigHelper.getString(_conf, key);
          key = key.substring(0, key.length() - ".port".length());
        } else {
          continue;
        }
        String type = ConfigHelper.getString(_conf, key + ".type", null);
        boolean isSecureSpecific = ConfigHelper.getBoolean(_conf, key + ".secure", isSecure);
        Protocol protocol = Protocol.TCP;
        if ("udp".equalsIgnoreCase(type)) {
          protocol = Protocol.UDP;
        } else if ("sctp".equalsIgnoreCase(type)) {
          protocol = Protocol.SCTP;
        }
        reactorIP = ConfigHelper.getString(_conf, key + ".ip", null);
        List<String> specificIPs = list;
        if (reactorIP != null) {
          specificIPs = getIPlist(reactorIP, ipv6Support);
        }
        _logger.debug("readAddresses: accepted addresses=%s", specificIPs);
        int port = Integer.parseInt(val);
        if (protocol == Protocol.SCTP) {
          long listenId = SEED.getAndIncrement();
          String[] ips = specificIPs.toArray(new String[0]);
          ListeningConnection connection = new ListeningConnection(listenId, ips, port, protocol, isSecureSpecific,
                                                                   stackAppName, stack);
          _listeningCnx.add(connection);
          _logger.debug("readAddresses: add connection=%s", connection);
        } else
          for (String ip : specificIPs) {
            long listenId = SEED.getAndIncrement();
            String[] ips = new String[] { ip };
            ListeningConnection connection = new ListeningConnection(listenId, ips, port, protocol, isSecureSpecific,
                                                                     stackAppName, stack);
            _listeningCnx.add(connection);
            _logger.debug("readAddresses: add connection=%s", connection);
          }
      } catch (Throwable t) {
        _logger.error("constr: cannot build the launcher", t);
        if (t instanceof Exception) {
          throw (Exception) t;
        }
        throw new Exception("cannot build the launcher", t);
      }
    }
  }
  
  private Enumeration getKeys(String pattern) {
    pattern = pattern.trim();
    
    Enumeration e = _conf.keys();
    Hashtable h = new Hashtable();
    boolean fromStart = false, fromEnd = false, fromMiddle = false;
    String patternStart = null, patternEnd = null;
    
    if (pattern.charAt(0) == '*') {
      pattern = pattern.substring(1);
      fromEnd = true;
    } else if (pattern.charAt(pattern.length() - 1) == '*') {
      fromStart = true;
      pattern = pattern.substring(0, pattern.length() - 1);
    } else if (pattern.indexOf("*") != -1) {
      fromMiddle = true;
      patternStart = pattern.substring(0, pattern.indexOf("*"));
      patternEnd = pattern.substring(pattern.indexOf("*") + 1);
    }
    
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      boolean matched = false;
      
      if (fromStart) {
        if (key.startsWith(pattern)) {
          matched = true;
        }
      } else if (fromEnd) {
        if (key.endsWith(pattern)) {
          matched = true;
        }
      } else if (fromMiddle) {
        if (key.startsWith(patternStart) && key.endsWith(patternEnd)) {
          matched = true;
        }
      } else {
        if (key.equals(pattern)) {
          matched = true;
        }
      }
      
      if (matched) {
        Object value = _conf.get(key);
        if (value != null) {
          h.put(key, value);
        }
      }
    }
    
    return (h.keys());
  }
  
  protected List<String> getIPlist(String ip, boolean ipv6Support) throws Exception {
    List<String> list = new ArrayList<String>();
    if (ip == null) {
      return list;
    }
    
    if (!ip.equals("0.0.0.0")) {
      for (StringTokenizer tok = new StringTokenizer(ip, " ,"); tok.hasMoreTokens();) {
        String s = tok.nextToken().trim();
        try {
          IPAddr addr = new IPAddr(s);
          // If the mux handler does not support ipv6, we must refuse an ipv6 address.
          if (!ipv6Support && addr.isIPv6()) {
            throw new IllegalArgumentException("Agent does not support ipv6 address: " + s);
          }
          list.add(s);
          _logger.debug("getIPlist: add ip (reactor.ip parsing)=%s", s);
        } catch (Exception e) {
          _logger.warn("The reactor.ip properties contains a not well formed IP address: %s -> the address is ignored",
                       e, s);
        }
      }
    }
    
    // no valid or 
    if (list.isEmpty()) {
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
            
            _logger.debug("getIPlist: ip (from 0.0.0.0)=%s", s);
            list.add(s);
          }
        }
      }
    }
    
    return list;
  }
  
  private Object[] parseIpPort(String tcpAddr) {
    tcpAddr = tcpAddr.trim();
    // if addr contains a space, use it as separator between addr and port number, else use ":" as delimiter.
    int space = tcpAddr.indexOf(" ");
    String ip = null;
    int port = -1;
    if (space != -1) {
      ip = tcpAddr.substring(0, space);
      port = Integer.parseInt(tcpAddr.substring(space + 1));
    } else {
      String[] addr = tcpAddr.split(":");
      ip = addr[0].trim();
      port = Integer.parseInt(addr[1].trim());
    }
    return new Object[] { ip, port };
  }
  
  public void stop() {
    // unregister advertisements
    for (ServiceRegistration adverts : _stackAdverts) {
      try {
        adverts.unregister();
      } catch (Throwable t) {
        _logger.warn("Error while unregistering stack advertisement", t);
      }
    }
  }
  
  /**
   * @see com.nextenso.mux.MuxFactory#newMuxConnection(alcatel.tess.hometop.gateways.reactor.Reactor,
   *      com.nextenso.mux.MuxFactory.ConnectionListener,
   *      com.nextenso.mux.MuxHandler, java.net.InetSocketAddress, int,
   *      java.lang.String, java.lang.String, java.lang.String, java.util.Map)
   */
  @Override
  public MuxConnection newMuxConnection(Reactor reactor, final ConnectionListener listener, MuxHandler muxHandler,
                                        InetSocketAddress to, int stackId, String stackName, String stackHost,
                                        String stackInstance, Map opts) {
    
    Logger logger = (opts != null) ? (Logger) opts.get(OPT_LOGGER) : _logger.getLogger();
    // Check if a TCP parser has been provided by the MUX handler.
    TcpMessageParser parser = (TcpMessageParser) muxHandler.getMuxConfiguration().get(MuxHandler.CONF_TCP_PARSER);
    if (parser == null) {
      throw new IllegalArgumentException("Invalid Mux Connection parameters: the stackInstance specifies a "
          + "reactor connection, but no tcp message parsed is found from mux " + "handler configuration");
    }
    
    _logger.info("Creating reactor connection for stack instance %s", stackInstance);
    
    // We wrap mux handler in order to catch listened connection id, so we'll be able to stop/restart listening ...
    final ReactorConnection rc = newReactorConnection(reactor, new WrappedMuxHandler(muxHandler), listener, stackId,
                                                      stackName, stackInstance, parser, logger);
    rc.setRemoteAddress(to);
    return rc;
  }
  
  @Override
    public MuxConnection newLocalMuxConnection(Reactor reactor, MuxHandler mh, int stackAppId, String stackAppName, String stackInstance,
					       TcpMessageParser parser, Logger logger) {
    ReactorConnection rc = newReactorConnection(reactor, mh, null, stackAppId, stackAppName, stackInstance,
                                                parser, logger);
    rc.open(); // will invoke muxOpened
    return rc;
  }
  
  /**
   * @see com.nextenso.mux.MuxFactory#connect(com.nextenso.mux.MuxConnection)
   */
  @Override
  public void connect(final MuxConnection connection) {
    _logger.debug("connect: cnx=%s", connection);
    if (!(connection instanceof ReactorConnection)) {
      return;
    }
    
    final ReactorConnection rc = (ReactorConnection) connection;
    Reactor reactor = rc.getReactor();
    reactor.schedule(new Runnable() {
      public void run() {
        Throwable error = null;
        try {
          rc.open(); // will invoke muxHandler.muxOpened();
          
          // check if the stackInstance must be automatically listened (using _listeningCnx).
          _logger.debug("Checking autolisten addrs for stack %s (app name %s)", connection.getStackInstance(),
                        connection.getStackAppName());
          for (ListeningConnection lc : _listeningCnx) {
            if (connection.getStackAppName().equals(lc.getStackAppName())) {
              _logger.debug("Will auto listen addrs %s for stack %s (app name  %s)", lc, connection.getStackInstance(),
                            connection.getStackAppName());
              lc.listen(connection);
            }
          }
          
        } catch (Throwable err) {
          error = err;
        }
        rc.getConnectionListener().muxConnected(rc, error);
      }
    });
  }
  
  @Override
  public InetSocketAddress accept(Reactor r, ConnectionListener l, MuxHandler mh, InetSocketAddress from, Map opts)
      throws IOException {
    throw new UnsupportedOperationException("local mux connection can't be accepted for now");
  }
  
  static ReactorConnection newReactorConnection(Reactor reactor, MuxHandler mh, ConnectionListener cl, int stackAppId,
                                                String stackAppName, String stackInstance, TcpMessageParser parser,
                                                Logger logger) {
    ReactorConnection rc = new ReactorConnection(reactor, mh, cl, stackAppId, stackAppName, stackInstance, parser,
        logger);
    return rc;
  }
  
  @Override
  public void handleEvent(Event event) {
    if (event.getTopic().equals("com/alcatel_lucent/as/service/callout/launcher/STOP_LISTENING")) {
      for (ListeningConnection lc : _listeningCnx) {
        _logger.debug("stopListening: listening connection=%s", lc);
        if (lc.getSocketId() != null) {
          if (lc.getProtocol() == Protocol.TCP) {
            lc._cnx.sendTcpSocketClose(lc.getSocketId());
          } else if (lc.getProtocol() == Protocol.UDP) {
            lc._cnx.sendUdpSocketClose(lc.getSocketId());
          } else if (lc.getProtocol() == Protocol.SCTP) {
            lc._cnx.sendSctpSocketClose(lc.getSocketId());
          }
        }
      }
    } else if (event.getTopic().equals("com/alcatel_lucent/as/service/callout/launcher/START_LISTENING")) {
      for (ListeningConnection lc : _listeningCnx) {
        _logger.debug("startListening: listening connection=%s", lc);
        if (lc.getProtocol() == Protocol.TCP) {
          lc._cnx.sendTcpSocketListen(lc.getId(), lc.getIP(), lc.getPort(), lc.isSecure());
        } else if (lc.getProtocol() == Protocol.UDP) {
          lc._cnx.sendUdpSocketBind(lc.getId(), lc.getIP(), lc.getPort(), true);
        } else if (lc.getProtocol() == Protocol.SCTP) {
          lc._cnx.sendSctpSocketListen(lc.getId(), lc.getIPs(), lc.getPort(), 5, //maxOutStreams
                                       5,  //maxInStreams
				       false); // no DLTS
        }
      }
    }
  }
  
  private void setSocketId(long listenId, int errno, String localIP, int localPort, int socketId) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("setSocketId: errno=%s, localIP=%s, localPort=%d, socketId=%d",  errno, localIP, localPort, socketId);
    }
    if (errno == 0) {
      // find the listening connection and sets the sockId
      for (ListeningConnection c : _listeningCnx) {
        if (c.getId() == listenId) {
          c.setSocketId(socketId);
          break;
        }
      }
    }
  }
  
  class WrappedMuxHandler extends MuxHandler {
    MuxHandler _wrapped;
    
    // "could not connect"
    
    public WrappedMuxHandler(MuxHandler wrapped) {
      _wrapped = wrapped;
    }
    
    // Here, we just delegate methods to our concrete mux handler.
    
    @Override
    public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort, boolean secure,
                                   long listenId, int errno) {
      _wrapped.tcpSocketListening(connection, sockId, localIP, localPort, secure, listenId, errno);
      setSocketId(listenId, errno, localIP, localPort, sockId);
    }
    
    @Override
    public void udpSocketBound(MuxConnection connection, int sockId, String localIP, int localPort, boolean shared,
                               long bindId, int errno) {
      _wrapped.udpSocketBound(connection, sockId, localIP, localPort, shared, bindId, errno);
      setSocketId(bindId, errno, localIP, localPort, sockId);
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
      _wrapped.muxClosed(connection);
      _wrapped.destroy();
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
      for (ListeningConnection c : _listeningCnx) {
        if (c.getSocketId() != null && c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort, int localIP,
                                   int localPort, int virtualIP, int virtualPort, boolean secure, boolean clientSocket,
                                   long connectionId, int errno) {
      _wrapped.tcpSocketConnected(connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort,
                                  secure, clientSocket, connectionId, errno);
    }
    
    @Override
    public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
                                   String localIP, int localPort, String virtualIP, int virtualPort, boolean secure,
                                   boolean clientSocket, long connectionId, int errno) {
      _wrapped.tcpSocketConnected(connection, sockId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort,
                                  secure, clientSocket, connectionId, errno);
    }
    
    @Override
    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off, int len) {
      _wrapped.tcpSocketData(connection, sockId, sessionId, data, off, len);
    }
    
    @Override
    public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data) {
      _wrapped.tcpSocketData(connection, sockId, sessionId, data);
    }
    
    @Override
    public void tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort, boolean secure,
                                   long listenId, int errno) {
      _wrapped.tcpSocketListening(connection, sockId, localIP, localPort, secure, listenId, errno);
      setSocketId(listenId, errno, MuxUtils.getIPAsString(localIP), localPort, sockId);
    }
    
    @Override
    public String toString() {
      return _wrapped.toString();
    }
    
    @Override
    public void udpSocketBound(MuxConnection connection, int sockId, int localIP, int localPort, boolean shared,
                               long bindId, int errno) {
      _wrapped.udpSocketBound(connection, sockId, localIP, localPort, shared, bindId, errno);
      setSocketId(bindId, errno, MuxUtils.getIPAsString(localIP), localPort, sockId);
    }
    
    @Override
    public void udpSocketClosed(MuxConnection connection, int sockId) {
      _wrapped.udpSocketClosed(connection, sockId);
      for (ListeningConnection c : _listeningCnx) {
        if (c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP, int remotePort,
                              int virtualIP, int virtualPort, byte[] data, int off, int len) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off,
                             len);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, int remoteIP, int remotePort,
                              int virtualIP, int virtualPort, ByteBuffer data) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP, int remotePort,
                              String virtualIP, int virtualPort, byte[] data, int off, int len) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data, off,
                             len);
    }
    
    @Override
    public void udpSocketData(MuxConnection connection, int sockId, long sessionId, String remoteIP, int remotePort,
                              String virtualIP, int virtualPort, ByteBuffer data) {
      _wrapped.udpSocketData(connection, sockId, sessionId, remoteIP, remotePort, virtualIP, virtualPort, data);
    }
    
    @Override
    public void tcpSocketAborted(MuxConnection connection, int sockId) {
      _wrapped.tcpSocketAborted(connection, sockId);
    }
    
    /**
     * @see com.nextenso.mux.MuxHandler#sctpSocketListening(com.nextenso.mux.MuxConnection, int, long, java.lang.String[], int, int)
     */
    @Override
    public void sctpSocketListening(MuxConnection cnx, int sockId, long listenerId, String[] localAddrs, int localPort,
                                    boolean secure, int errno) {
      _wrapped.sctpSocketListening(cnx, sockId, listenerId, localAddrs, localPort, secure, errno);
      _logger.debug("sctpSocketListening: localAddrs=%s", Arrays.asList(localAddrs));
      setSocketId(listenerId, errno, localAddrs[0], localPort, sockId);
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
    public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data, String addr,
                               boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber) {
      _wrapped.sctpSocketData(connection, sockId, sessionId, data, addr, isUnordered, isComplete, ploadPID,
                              streamNumber);
    }
    
    @Override
    public void sctpSocketClosed(MuxConnection cnx, int sockId) {
      _wrapped.sctpSocketClosed(cnx, sockId);
      for (ListeningConnection c : _listeningCnx) {
        if (c.getSocketId() != null && c.getSocketId() == sockId) {
          c.setSocketId(-1);
          break;
        }
      }
    }
    
    @Override
    public void sctpSocketSendFailed(MuxConnection cnx, int sockId, String addr, int streamNumber, ByteBuffer buf,
                                     int errcode) {
      _wrapped.sctpSocketSendFailed(cnx, sockId, addr, streamNumber, buf, errcode);
    }
    
    @Override
    public void sctpPeerAddressChanged(MuxConnection connection, int sockId, String addr, int port,
                                       MuxHandler.SctpAddressEvent event) {
      _wrapped.sctpPeerAddressChanged(connection, sockId, addr, port, event);
    }
  }
}
