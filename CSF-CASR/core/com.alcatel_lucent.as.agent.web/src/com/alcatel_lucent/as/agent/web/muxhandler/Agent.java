package com.alcatel_lucent.as.agent.web.muxhandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.Promise;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.util.config.ConfigHelper;
import com.alcatel_lucent.as.agent.web.container.AgentProperties;
import com.alcatel_lucent.as.agent.web.container.Container;
import com.alcatel_lucent.as.agent.web.http.client.HttpClientSocket;
import com.alcatel_lucent.as.service.dns.DNSHelper;
import com.alcatel_lucent.as.service.dns.RecordAddress;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.mux.util.MuxHandlerMeters;
import com.nextenso.mux.util.MuxUtils;

/**
 * Our MuxHandler implementation: it handles MUX communication and serves as the underlying IO
 * for both the web container and the client instances.
 * It is registered in the OSGi registry as a MuxHandler component with the property protocol=web.
 */

@Component(provides=MuxHandler.class) 
@Property(name="protocol", value="web")
@Property(name="autoreporting", value="false")
public class Agent extends MuxHandler {

  final static Logger LOGGER = Logger.getLogger("agent.web.muxhandler");
  private final static int VERSION = ((1 << 16) | 0);
  
  //accept incoming traffic when container is started.
  private boolean _autostart = true; 
  
  private enum ERROR_CAUSE {
    CONNECTION_REFUSED,
    HOSTNAME_CANNOT_BE_RESOLVED,
  }
  
  /** our MuxConnectionManager */
  private MuxConnectionManager cnxManager;
  /** our container */
  private Container container;  
  
  /** Pending sockets */
  private AtomicLong currentPendingId = new AtomicLong();
  private ConcurrentHashMap<Long, Promise<HttpClientSocket>> pendingConnections = new ConcurrentHashMap<Long, Promise<HttpClientSocket>>();
  private MuxHandlerMeters meters;
  
  @ServiceDependency
  private MeteringService _metering;
  
  @Inject
  private BundleContext _bctx;
  
  private final static int[] HTTP_IOH_ID = new int[] {
    286
  };

  // ----------------- Declarative Service dependencies and life cycle -------------------------------------------------

  @ConfigurationDependency(pid="webagent") 
  void updated(Dictionary<?, ?> conf) { 
    if (conf != null) { 
		_autostart = ConfigHelper.getBoolean(conf, AgentProperties.AUTOSTART, true);
		if (LOGGER.isDebugEnabled()) LOGGER.debug("config updated: autostart=" + _autostart);
    } 
  }

  /**
   * dependency on our web container provided as a separate component
   */
  @ServiceDependency
  protected void bindContainer(Container container) { 
    this.container = container;
  }
  
  /** component activation: all required dependencies available */
  @Start
  protected void activate() {
    LOGGER.debug("Web Agent activated");
  }

  /** component de-activation */
  @Stop
  protected void deactivate() {
    LOGGER.debug("Web Agent deactivated");
  }

  // ---------------- MuxHandler interface -----------------------------------------------------------
  
  /** Called by the CalloutAgent when it has seen our MuxHandler */
  @SuppressWarnings("unchecked")
  @Override
  public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
    // Don't forget to call the super.init method !
    super.init(appId, appName, appInstance, muxContext);

    try {
      LOGGER.info("Initializing Web agent");
      String[] meterConf =  new String[] {"tcp"};
      SimpleMonitorable containerMon = container.getContainerMonitorable();
      meters = new MuxHandlerMeters(_metering, containerMon);
      meters.initMeters(meterConf);
      containerMon.start(_bctx);

      // Configure our MUX handler for the Web protocol
      getMuxConfiguration().put(CONF_THREAD_SAFE, Boolean.TRUE);
      getMuxConfiguration().put(CONF_STACK_ID, HTTP_IOH_ID);
      getMuxConfiguration().put(CONF_TCP_PARSER, TcpParser.getInstance());
	  getMuxConfiguration().put(CONF_MUX_START, Boolean.TRUE);
	  getMuxConfiguration().put(CONF_L4_PROTOCOLS, meterConf);
	  getMuxConfiguration().put(CONF_HANDLER_METERS, meters);

      // This object will help to keep track of our MUX connections. TODO reserved for HTTPClient ?
      cnxManager = new MuxConnectionManager();
    }

    catch (Throwable t) {
      LOGGER.error("error while initializing Web Agent", t);
    }
  }

  @Override
  public void muxOpened (final MuxConnection connection) {
    LOGGER.info("mux opened: " + connection);    
    // Keep Track of this MUX connection
    cnxManager.addMuxConnection(connection);
    
    SimpleMonitorable mon = (SimpleMonitorable) connection.getMonitorable();
    container.registerCnxMonitorable(mon, connection);
    
	if (_autostart) {
		connection.sendMuxStart();
	} else {
		connection.sendMuxStop();
	}
  }

  @Override
  public void muxClosed(MuxConnection connection) {
    LOGGER.warn("Mux closed: " + connection);
    cnxManager.removeMuxConnection(connection);
    container.unregisterCnxMonitorable(connection);
    
    Enumeration<?> elements = connection.getSocketManager().getSockets(Socket.TYPE_TCP);
    while (elements.hasMoreElements()) {
      container.removeClient((WebAgentSocket) elements.nextElement());
    }
  }

  @Override
  public void tcpSocketListening(MuxConnection connection, int sockId, int localIP, int localPort, boolean secure, 
                                 long listenId, int errno) {
    tcpSocketListening(connection, sockId, MuxUtils.getIPAsString(localIP), localPort, secure, listenId, errno);
  }

  @Override
  public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort, boolean secure,
                                 long listenId, int errno) {
    LOGGER.info("tcpSocketListening sockId=" + sockId);
  }


  @Override
  public void tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort, int localIP, int localPort, 
         int virtualIP, int virtualPort, boolean secure, boolean clientSocket, long connectionId, 
         int errno)  {
    tcpSocketConnected(connection, sockId, MuxUtils.getIPAsString(remoteIP), remotePort, 
                       MuxUtils.getIPAsString(localIP), localPort, MuxUtils.getIPAsString(virtualIP), virtualPort, 
                       secure, clientSocket, connectionId, errno);
  }

  @Override
  public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort, String localIP, int localPort,
                                 String virtualIP, int virtualPort, boolean secure, boolean clientSocket, long connectionId, 
                                 int errno) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("tcpSocketConnected sockId="+sockId +",connectionId="+connectionId+",client="+clientSocket); 
    if (clientSocket) {
      WebAgentSocket socket = new WebAgentSocket(connection, sockId, remoteIP, remotePort, localIP,
          localPort, secure, container.getTpExecutor());
      connection.getSocketManager().addSocket(socket);
      container.addClient(socket);
    }
    else {
      Promise<HttpClientSocket> promise = pendingConnections.remove(connectionId);
      if (promise != null) {
        if (errno != 0)
          promise.failed(new Throwable(ERROR_CAUSE.CONNECTION_REFUSED.toString()));
        else {
          HttpClientSocket socket = new HttpClientSocket(connection, sockId, container.getIOExecutor());
          connection.getSocketManager().addSocket(socket);          
          promise.succeeded(socket);                  
        }
      }
    }
  }

  @Override
  public void tcpSocketClosed(MuxConnection connection, int sockId) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("tcpSocketClosed sockId="+sockId); 
    WebAgentSocketInterface socket = (WebAgentSocketInterface) connection.getSocketManager().removeSocket(Socket.TYPE_TCP, sockId);
    if (socket != null) {
      socket.closed(container);
    }
  }
  
  @Override
  public void tcpSocketAborted(MuxConnection connection, int sockId) {
    // Should never happen since "abort" is never called
    if (LOGGER.isDebugEnabled()) LOGGER.debug("tcpSocketAborted sockId="+sockId);
    tcpSocketClosed(connection, sockId);
  }

  @Override
  public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer buf) {
    tcpSocketData(connection, sockId, sessionId, buf.array(), buf.position(), buf.remaining());
  }

  @Override
  public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off, int len) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("tcpSocketData sockId="+sockId + " len="+len);
    WebAgentSocketInterface socket = (WebAgentSocketInterface) connection.getSocketManager().getSocket(Socket.TYPE_TCP, sockId);
    if (socket != null) {
      socket.received(data, off, len);
    }
    else {
      if (LOGGER.isDebugEnabled()) LOGGER.debug("tcpSocketData dropped cnx=" + connection + ",sockId="+sockId);
    }
  }

  @Override
  public void destroy () {
    // The Callout is gone and asks us to destroy ourself ...
    LOGGER.info("Destroying Web Agent");
  }

  @Override
  public int getMinorVersion() {
    return VERSION & 0xFFFF;
  }

  @Override
  public int getMajorVersion() {
    return VERSION >>> 16;
  }

  @Override
  public int[] getCounters() {
    throw new RuntimeException("deprecated method, should not be used anymore");
  }

  @Override
  public void commandEvent(int command, int[] intParams, String[] strParams) {
  }
  
  // ---------------- HttpClient connections -----------------------------------------------------------
  
  public void connect(InetSocketAddress address, boolean secure, Promise<HttpClientSocket> promise) {
    MuxConnection connection = cnxManager.getRandomMuxConnection();
    if (connection == null) {
      promise.failed(new Throwable(ERROR_CAUSE.CONNECTION_REFUSED.toString()));
    }
    else {
      String host = address.getHostString();
      List<RecordAddress> list = DNSHelper.getHostByName(host);
      if (list.isEmpty()) {
        promise.failed(new Throwable(ERROR_CAUSE.HOSTNAME_CANNOT_BE_RESOLVED.toString()));
      }
      else {  
        long id = 0L;
        while(id  <= 0) {
          id = currentPendingId.incrementAndGet() & 0x7FFFFFFFFFFFFFFFL;
        }
        String to = list.get(0).getAddress();
        int port = address.getPort();
        if (connection.sendTcpSocketConnect(id, to, port, 0, 0, secure)) {
          pendingConnections.put(id, promise);
        }
        else {
          promise.failed(new Throwable(ERROR_CAUSE.CONNECTION_REFUSED.toString()));
        }
      }      
    }
  }

}
