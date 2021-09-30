package com.nextenso.diameter.agent;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventHandler;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.shutdown.Shutdown;
import com.alcatel.as.service.shutdown.Shutdownable;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.diameter.agent.StopListeningService.Listener;
import com.nextenso.diameter.agent.engine.DiameterProxyletContainer;
import com.nextenso.diameter.agent.ha.HaManager;
import com.nextenso.diameter.agent.impl.DiameterClientFactoryFacade;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.metrics.DiameterChannelMeters;
import com.nextenso.diameter.agent.metrics.DiameterAgentMeters;
import com.nextenso.diameter.agent.metrics.DiameterMeters;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.diameter.agent.peer.PeerSocket;
import com.nextenso.diameter.agent.peer.PeerTable;
import com.nextenso.diameter.agent.peer.RSocket;
import com.nextenso.diameter.agent.peer.RemotePeer;
import com.nextenso.diameter.agent.peer.RouteTableManager;
import com.nextenso.diameter.agent.peer.StaticPeer;
import com.nextenso.diameter.agent.peer.TableManager;
import com.nextenso.diameter.agent.peer.statemachine.DWListener;
import com.nextenso.diameter.agent.peer.statemachine.DiameterStateMachine;
import com.nextenso.diameter.agent.peer.xml.CapabilitiesParser;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.socket.SocketManager;
import com.nextenso.mux.util.MuxHandlerMeters;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterFilterTable;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeer.Protocol;
import com.nextenso.proxylet.diameter.DiameterRouteTable;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.engine.ProxyletApplication;

/**
 * The Diameter Agent.
 */
@Component(provides={MuxHandler.class, EventHandler.class}, properties={
	@Property(name="protocol", value="Diameter"),
	@Property(name="event.topics", value={"com/alcatel_lucent/as/util/event/DISCONNECT"})
})
public class Agent extends MuxHandler implements Shutdownable, Listener, EventHandler {
	
	@Inject
	private volatile BundleContext _bctx;
	
	@ServiceDependency
	private volatile MeteringService _metering;

	private static final Logger LOGGER = Logger.getLogger("agent.diameter");

	private static final int VERSION = ((1 << 16) | 0); // version 1, release 0

	private static final String STACK_INSTANCE = "diameteragent.stackInstance";

	public static final int STACK_ID = 277;
	public static final int JAVA_STACK_ID = 289;
	private static final int[] STACK_IDS = { STACK_ID, JAVA_STACK_ID };
	private static final int[] FAKE_COUNTERS = new int[0];

	private boolean _isInitialized = false;
	private Dictionary _agentConf;
	private DiameterProxyletContainer _proxyletContainer;
	private ProxyletApplication _app;
	private AtomicInteger _shutdownCountdown = null;
	private Shutdown _shutdown;

	private final AtomicInteger _nbConnectedSocket = new AtomicInteger(0);

	private boolean _stoppingListening = false;
	private final AtomicInteger _nbIncomingSocketToClose = new AtomicInteger(0);	
	private final StopListeningService _stopListening = new StopListeningService();

	private boolean _delayConnections;
	private final List<MuxConnection> _delayedConnections = new ArrayList<>();
	private boolean _autostart = true; // accept incoming traffic when container is started.
	
	private volatile boolean _diamterServicesRegistered = false; // indicates if Diameter services (client/peertable, etc ...) are registered.
	
	private DiameterAgentMeters _agentMeters;
	
	/**
	 * Name of the service properties of the object which tells if we have to start/stop listening.
	 * (See FelixConnectLauncher/com.alcatel_lucent.as.service.felixconnect.launcher.impl.LauncherImpl)
	 */
	private final static String START_LISTENING = "com.alcatel_lucent.as.util.event.listening";

	public Agent() {
	  _stopListening.register(this); // our handleEvent method will use the stopListeningService
	}

	/**
	 * Handles events. For now, we only support DISCONNECT events.
	 */
	@Override
	public void handleEvent(org.osgi.service.event.Event event) {
		LOGGER.info("Disconnecting remote sockets ..."); // don't change this log (used by tests)

		if (event.getTopic().equals("com/alcatel_lucent/as/service/callout/launcher/DISCONNECT")) {
			int disconnectCause = Integer.valueOf(event.getProperty("cause").toString());
			LOGGER.warn("received DISCONNECT event: disconnecting all peer connectionx using cause=" + disconnectCause);
			final CountDownLatch latch = new CountDownLatch(1);
			_stopListening.stop(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					LOGGER.warn("all peer connections disconnected.");
					latch.countDown();
				}}, 10000, disconnectCause);
      
			try {
				if (!latch.await(15000, TimeUnit.MILLISECONDS)) {
					LOGGER.warn("could not close remote peers timely");
				}
			} catch (InterruptedException e) {
				LOGGER.warn("Thread Interrupted while closing remote peers", e);
			}
		}
	}
			
	@ServiceDependency(filter="(service.pid=system)")
	protected void bindSystemConfig(Dictionary d) {
		_proxyletContainer = new DiameterProxyletContainer(d);
	}

	@ServiceDependency
	protected void bindPlatformExecutors(PlatformExecutors pfExecutors) {
		Utils.setPlatformExecutors(pfExecutors);
	}
	
	@ServiceDependency(filter="(" + TimerService.STRICT + "=false)")
	protected void bindTimerService(TimerService timerService) {
	    if (LOGGER.isInfoEnabled()) {
	        LOGGER.info("bindTimerService: " + timerService);
	    }
	    Utils.setTimerService(timerService);
	}

	@ServiceDependency
	protected void bindDiameterFilterTable(DiameterFilterTable filterTable) {
		Utils.setFilterTable(filterTable);
	}
	
	@ServiceDependency
	protected void bindRouteTableManager(DiameterRouteTable routeTableManager) {
		Utils.setRouteTableManager((RouteTableManager) routeTableManager);
	}
	
	@ServiceDependency(filter="(protocol=diameter)", removed="unbindProxyletApplication")
	protected void bindProxyletApplication(ProxyletApplication app) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("binding Diameter ProxyletApplication " + app);
		}
		_app = app;
	}
	
	@ServiceDependency(filter="(" + START_LISTENING + "=true)", required=false, removed="stopListening")
	protected synchronized void startListening(Object startListen) {
		LOGGER.info("startListening");
		if (_delayConnections) {
			_delayConnections = false;
			// Restart previously stopped mux connections
			for (Enumeration<MuxConnection> enumeration = Utils.getConnectionManager().getMuxConnections(); enumeration
					.hasMoreElements();) {
				MuxConnection connection = enumeration.nextElement();
				LOGGER.info("starting mux connection: " + connection);
				connection.sendMuxStart();
			}
			// Open delayed mux connection
			for (MuxConnection cnx : _delayedConnections) {
				doMuxOpened(cnx);
			}
			_delayedConnections.clear();
		}
	}
	
	protected synchronized void stopListening(Object startListen) {
		LOGGER.info("stopListening");

		if (! _delayConnections) {
			_delayConnections = true;
			for (Enumeration<MuxConnection> enumeration = Utils.getConnectionManager().getMuxConnections(); enumeration.hasMoreElements();) {
				MuxConnection connection = enumeration.nextElement();
				LOGGER.info("stoppig mux connection: " + connection);
				connection.sendMuxStop();
			}
		}
	}

	protected void unbindProxyletApplication(@SuppressWarnings("unused") ProxyletApplication app) {
		// TODO
	}

	@ServiceDependency
	public void bindMeteringService(com.alcatel.as.service.metering.MeteringService service) {
		Utils.setMeteringService(service);
	}

	@ConfigurationDependency(pid="diameteragent")
	public synchronized void setAgentConfig(Dictionary<String, String> cnf) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("updating agent conf: " + cnf);
		}
		_agentConf = cnf;
		_delayConnections = ConfigHelper.getBoolean(_agentConf, PropertiesDeclaration.DELAY_CONNECTION, false);
		_autostart = ConfigHelper.getBoolean(_agentConf, PropertiesDeclaration.AUTOSTART, true);
		LOGGER.debug("setAgentConfig: delayConnection: " + _delayConnections);
		LOGGER.debug("setAgentConfig: autostart: " + _autostart);

		if (! _isInitialized) {
		  return;
		}
		try {
		  DiameterProperties.init(_agentConf);
		} catch (Throwable t) {
		  LOGGER.warn("DiameterAgent could not handle configuration change", t);
		}
				
		for (String key: Collections.list(cnf.keys())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("updateAgentConfig: key=" + key);
				}
				if (STACK_INSTANCE.equals(key)) {
					String stacks = ConfigHelper.getString(cnf, STACK_INSTANCE, "*").trim();
					if (stacks.equals("*")) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("updateAgentConfig: Available for any diameter IO handler");
						}
						getMuxConfiguration().put(CONF_STACK_INSTANCE, new String[0]);
					} else {
						StringTokenizer st = new StringTokenizer(stacks);
						String[] stackInstance = new String[st.countTokens()];
						int k = 0;
						while (st.hasMoreTokens()) {
							stackInstance[k] = st.nextToken();
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("updateAgentConfig: Serving diameter stack: " + stackInstance[k]);
							}
							++k;
						}
						getMuxConfiguration().put(CONF_STACK_INSTANCE, stackInstance);
					}
				} else if (PropertiesDeclaration.SESSION_LIFETIME.equals(key)) {
					DiameterProperties.setSessionLifetime(cnf);
				} else if (PropertiesDeclaration.CLIENT_ATTEMPTS.equals(key)) {
					DiameterProperties.setClientAttempts(cnf);
				} else if (PropertiesDeclaration.ROUTES.equals(key)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("propertyChanged: Routes have been changed");
					}
					DiameterProperties.updateRoutesXml(cnf);
					String xml = DiameterProperties.getRoutesXml();
					if (xml != null) {
						Utils.getRouteTableManager().updateRoutes(xml);
					}
				} else if (PropertiesDeclaration.PEERS.equals(key)) {
					if (LOGGER.isDebugEnabled())
						LOGGER.debug("propertyChanged: Peers have been changed");
					DiameterProperties.updatePeersXml(cnf);
				} 
			}
	}

	/**
	 * The SuperAgent is initializing our mux handler
	 */
	@Override
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
		if (_isInitialized) {
			return;
		}

		try {
			super.init(appId, appName, appInstance, muxContext);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("init: instance=" + appInstance);
			}

			Utils.setAgentPlatformThread();
			Utils.setAgentInstanceName(appInstance);
			
			String[] meterConf = new String[] {"tcp", "sctp"};
			SimpleMonitorable _agentMon = new SimpleMonitorable("agent.diameter", "Aggregated Meters for Diameter Agent");
			MuxHandlerMeters muxMeters = new MuxHandlerMeters(_metering, _agentMon);
			muxMeters.initMeters(meterConf);
			_agentMeters = new DiameterAgentMeters(_agentMon, _metering);
			_agentMon.start(_bctx);
			
			getMuxConfiguration().put(CONF_THREAD_SAFE, Boolean.TRUE);
			getMuxConfiguration().put(CONF_STACK_ID, STACK_IDS);
			getMuxConfiguration().put(CONF_IPV6_SUPPORT, Boolean.TRUE);
			getMuxConfiguration().put(CONF_TCP_PARSER, StandaloneTcpParser.getInstance());
			getMuxConfiguration().put(CONF_MUX_START, Boolean.TRUE); 
			getMuxConfiguration().put(CONF_L4_PROTOCOLS, meterConf);
			getMuxConfiguration().put(CONF_HANDLER_METERS, muxMeters);
			
			Object delay = _agentConf.get("diameteragent.exitDelay");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("init (conf): delay=" + delay);
			}
			if (delay != null) {
				getMuxConfiguration().put(CONF_EXIT_DELAY, Long.valueOf(delay.toString()));
			}

			DiameterProperties.init(_agentConf);
			DiameterMessageFacade.setSynchronized (DiameterProperties.isMessageSynchronized ());
			CapabilitiesParser parser = new CapabilitiesParser();
			parser.parse(DiameterProperties.getCapabilitiesXml(), Utils.getCapabilities());
			Utils.getCapabilities().update();

			if (DiameterProperties.isHa()) {
				HaManager.init();
			}

			// set the names of the IO handler
			//getMuxConfiguration().put (CONF_STACK_NAME, hostnames);
			_proxyletContainer.init(_app);
			_app.initDone();
			Utils.setEngine(_proxyletContainer.getDiameterProxyletEngine());
			String stack = ConfigHelper.getString(_agentConf, STACK_INSTANCE);
			if (!"*".equals(stack)) {
				getMuxConfiguration().put(CONF_STACK_INSTANCE, stack);
			}
			
			_isInitialized = true;
		}
		catch (Throwable t) {
			LOGGER.error("Could not initialize diameter agent", t);
		}

		Utils.getTableManager().updateWhiteListFilters();
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#destroy()
	 */
	@Override
	public void destroy() {
		_proxyletContainer.destroy();
		_agentMeters.agentStopped();
		if (_shutdown != null)
			_shutdown.done (this);
	}

	public synchronized void shutdown (Shutdown shutdown){
		LOGGER.info("shutdown");
		_shutdown = shutdown;
		_shutdownCountdown = new AtomicInteger (Utils.getConnectionManager().size ());
		boolean waitClose = _shutdownCountdown.get () > 0;
		// Disconnect all the peers for all the handlers
		for (Enumeration e = Utils.getConnectionManager().getMuxConnections(); e.hasMoreElements();) {
			MuxConnection connection = (MuxConnection) e.nextElement();
			connection.close ();
		}
		for (MuxConnection connection : _delayedConnections)
			connection.close ();
		if (!waitClose)
			destroy ();
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getCounters()
	 */
	public int[] getCounters() {
		return FAKE_COUNTERS;
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getMajorVersion()
	 */
	public int getMajorVersion() {
		return VERSION >>> 16;
	}

	/**
	 * @see com.nextenso.mux.event.MuxMonitorable#getMinorVersion()
	 */
	public int getMinorVersion() {
		return VERSION & 0xFFFF;
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#muxOpened(com.nextenso.mux.MuxConnection)
	 */
	@Override
	public synchronized void muxOpened(MuxConnection connection) {
		if (_shutdownCountdown != null){
			connection.close ();
			return;
		}
		if (_delayConnections) {
			LOGGER.info("muxOpen: connection delayed (traffic not enabled)");
			_delayedConnections.add(connection);
		} else {
			doMuxOpened(connection);
		}
	}
	
	private void doMuxOpened(MuxConnection connection) {
		LOGGER.info("muxOpen: starting mux connection: " + connection);
		
		if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn("muxOpened: connection=" + connection);
		}

		String handlerName = Utils.getHandlerName(connection);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("muxOpened: handlerName=" + handlerName);
		}
		connection.attach (handlerName);

		Utils.addHandler(handlerName, connection);
		TableManager tableManager = Utils.getTableManager();

		try {
			tableManager.muxOpened(handlerName);
		}
		catch (Exception e) {
			LOGGER.error("cannot init the peers", e);
			connection.close();
			return;
		}
		
		SimpleMonitorable mon = (SimpleMonitorable) connection.getMonitorable();
		if(mon != null) {
			DiameterChannelMeters diamCnxMetrics = createConnectionMetrics(mon);
			connection.setAttributes(new Object[] {diamCnxMetrics});		
			
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("Attached Diameter metrics to monitorable for " + connection);
			}
		}

		Utils.getConnectionManager().addMuxConnection(connection);
		LocalPeer localPeer = (LocalPeer) tableManager.getLocalDiameterPeer(handlerName);
		localPeer.muxOpened(connection);
		
		if (_autostart) {
			connection.sendMuxStart();
		} else {
			connection.sendMuxStop();
		}

		// AFTER sendMuxStart
		// notify the listeners
		// send the configured static peer connect
		tableManager.connected (localPeer);
		PeerTable table = tableManager.getPeerTable(handlerName);
		table.muxOpened();
		
		// we have at least one mux cnx: we can now register our diameter services that require a mux connection.
		if (! _diamterServicesRegistered) {
			try {
				if (!ConfigHelper.getBoolean (_agentConf, PropertiesDeclaration.CLIENT_FACTORY_IMMEDIATE, false)){
					LOGGER.debug("muxOpened: registering diameter client factory");
					DiameterClientFactoryFacade.getInstance().registerService(_bctx);
				}
			} catch (Exception e) {
				LOGGER.error("Can't register DiameterClientFactory service", e);
			}
			try {
				if (!ConfigHelper.getBoolean (_agentConf, PropertiesDeclaration.PEER_TABLE_IMMEDIATE, false)){
					LOGGER.debug("muxOpened: registering peer table manager");
					tableManager.registerService(_bctx);					
				}
			} catch (Exception e) {
				LOGGER.error("Can't register DiameterPeerTable service", e);
			}
			_diamterServicesRegistered = true;
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#muxClosed(com.nextenso.mux.MuxConnection)
	 */
	@Override
	public void muxClosed(MuxConnection connection) {
		if (Utils.getConnectionManager().removeMuxConnection(connection) == null)
			return;
		if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn("IO Handler connection closed: connection=" + connection);
		}
		
		String handlerName = connection.attachment ();
		Utils.getTableManager().destroy(handlerName);
		// clear the routes table
		Utils.getRouteTableManager().destroy(handlerName);
		// remove the handler from the list
		Utils.removeHandler(handlerName);
		
		stopConnectionMetrics(connection);

		Enumeration enumer = connection.getSocketManager().getSockets(Socket.TYPE_TCP);
		while (enumer.hasMoreElements()) {
			((PeerSocket) enumer.nextElement()).closed();
		}
		connection.getSocketManager().removeSockets(Socket.TYPE_TCP);

		enumer = connection.getSocketManager().getSockets(Socket.TYPE_SCTP);
		while (enumer.hasMoreElements()) {
			((PeerSocket) enumer.nextElement()).closed();
		}
		connection.getSocketManager().removeSockets(Socket.TYPE_SCTP);

		if (_shutdownCountdown != null && _shutdownCountdown.decrementAndGet () == 0)
			destroy ();
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#tcpSocketConnected(com.nextenso.mux.MuxConnection,
	 *      int, int, int, int, int, int, int, boolean, boolean, long, int)
	 */
	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, int remoteIP, int remotePort, int localIP, int localPort, int virtualIP,
			int virtualPort, boolean secure, boolean clientSocket, long connectionId, int errno) {
		tcpSocketConnected(connection, sockId, MuxUtils.getIPAsString(remoteIP), remotePort, MuxUtils.getIPAsString(localIP), localPort, MuxUtils.getIPAsString(virtualIP), virtualPort, secure, clientSocket, connectionId, errno);
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#tcpSocketConnected(com.nextenso.mux.MuxConnection,
	 *      int, java.lang.String, int, java.lang.String, int, java.lang.String,
	 *      int, boolean, boolean, long, int)
	 */
	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort, String localIP, int localPort,
			String virtualIP, int virtualPort, boolean secure, boolean fromClient, long connectionId, int errno) {

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Diameter TCP socket opened id=" + sockId + ", error=" + errno);
		}
		socketConnected(Protocol.TCP, connection, sockId, connectionId, errno, secure, remoteIP, fromClient, remotePort, localIP, localPort, virtualIP, virtualPort, null, null, -1, -1);
	}

	private void socketConnected(Protocol protocol, MuxConnection connection, int sockId, long connectionId, int errno, boolean secure,
			String remoteIP, boolean fromClient, int remotePort, String localIP, int localPort, String virtualIP, int virtualPort, String[] remoteAddrs,
			String[] localAddrs, int maxOutStreams, int maxInStreams) {
		if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("socketConnected: socket id=" + sockId + ", connection=" + connection + ", connection id=" + connectionId + ", clientSocket="
			     + fromClient + ", secure="+secure+", error=" + errno);
		LOGGER.debug("socketConnected: remote IP=" + remoteIP + ", port=" + remotePort + ", local IP=" + localIP + ", port=" + localPort
			     + ", remote adresses=" + Arrays.toString(remoteAddrs) + ", local addresses=" + Arrays.toString(localAddrs));
		}
		if (errno == 0) {
			int nb = _nbConnectedSocket.incrementAndGet();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("socketConnected: nb connected sockets=" + nb);
			}
			
			if (fromClient) {
				nb = _nbIncomingSocketToClose.incrementAndGet();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("socketConnected: nb sockets to close=" + nb);
				}

				if (_stoppingListening) {
					LOGGER.info("The agent is stopping listening -> closing the new connected socket.");
					connection.close();
					return;
				}
			}
		}
		
		Object[] attributes = connection.getAttributes();
		DiameterChannelMeters cnxMeters = null;
		if(attributes != null) {
			cnxMeters = (DiameterChannelMeters) attributes[0];	
		}

		if (fromClient) {
			// A new client is connected -> make a new R-Socket waiting for CER
			PeerSocket socket = null;
			if (protocol == Protocol.TCP) {
			    socket = new RSocket(connection, sockId, connectionId, remoteIP, remotePort, localIP, localPort, virtualIP, virtualPort, secure, protocol, new String[]{remoteIP}, new String[]{localIP});
			} else if (protocol == Protocol.SCTP) {
			    socket = new RSocket(connection, sockId, connectionId, remoteIP, remotePort, localIP, localPort, null, 0, secure, protocol, remoteAddrs, localAddrs);
			}
			
			if(cnxMeters != null) {
				if(DiameterProperties.perSocketMetrics()) {
					SimpleMonitorable mon = new SimpleMonitorable("diameter.channel." + socket.hashCode(), socket.toString());
					DiameterMeters diamMeters = new DiameterChannelMeters(mon, _metering, cnxMeters, true);
					mon.start(_bctx);
					
					socket.setDiameterMeters(diamMeters);
				} else {
					socket.setDiameterMeters(cnxMeters);
				}
			}
			SocketManager manager = connection.getSocketManager();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("socketConnected: add to socket manager=" + manager + ", socket=" + socket);
			}
			manager.addSocket(socket);
			return;
		}

		// this is a static peer which is reconnecting.
		String handlerName = connection.attachment ();
		DiameterPeer peer = Utils.getTableManager().getDiameterPeerById(handlerName, connectionId);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("socketConnected: peer=" + peer);
		}

		if (peer == null) {
			if (errno != 0) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("I-Socket connection-nack : too late - msg=" + MuxUtils.getErrorMessage(errno));
				}
			} else {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("I-Socket connection-ack : too late");
				}
				connection.sendTcpSocketClose(sockId);
			}
			return;
		}

		// Indicate to the static peer that the socket is connected (or not).
		PeerSocket peerSocket = ((StaticPeer) peer).getStateMachine().getISocket();
		if (peerSocket == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("socketConnected: the socket has been cleared -> do nothing");
			}
			return;
		}

		if (errno == 0) {
			if(cnxMeters != null) {
				if(DiameterProperties.perSocketMetrics()) {
					SimpleMonitorable mon = new SimpleMonitorable("diameter.channel." + peerSocket.hashCode(), peerSocket.toString());
					DiameterChannelMeters diamMeters = new DiameterChannelMeters(mon, _metering, cnxMeters, true);
					mon.start(_bctx);

					peerSocket.setDiameterMeters(diamMeters);
				} else {
					peerSocket.setDiameterMeters(cnxMeters);
				}
			}
			if (protocol == Protocol.TCP)
				peerSocket.tcpConnected(sockId, remoteIP, localIP, localPort);
			else
				peerSocket.sctpConnected(sockId, remoteAddrs, localAddrs, localPort, maxOutStreams, maxInStreams);
			SocketManager manager = connection.getSocketManager();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("socketConnected: add to socket manager=" + manager + ", socket=" + peerSocket);
			manager.addSocket(peerSocket);
		} else {
			peerSocket.notConnected();
		}

	}

	/**
	 * @see com.nextenso.mux.MuxHandler#tcpSocketClosed(com.nextenso.mux.MuxConnection,
	 *      int)
	 */
	@Override
	public void tcpSocketClosed(MuxConnection connection, int sockId) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Diameter TCP socket close: socket id=" + sockId);
		}
		PeerSocket socket = (PeerSocket) connection.getSocketManager().removeSocket(Socket.TYPE_TCP, sockId);		
		socketClosed(connection, sockId, socket);
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#tcpSocketData(com.nextenso.mux.MuxConnection,
	 *      int, long, byte[], int, int)
	 */
	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off, int len) {
		SocketManager manager = connection.getSocketManager();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("tcpSocketData:  length=" + len + ", offset=" + off);
			LOGGER.debug("tcpSocketData:  socket id=" + sockId + ", connection=" + connection + ", socket manager=" + manager);
		}
		

		try{
		    PeerSocket socket = (PeerSocket) manager.getSocket(Socket.TYPE_TCP, sockId);
		    dataReceived(sockId, socket, connection, sessionId, data, off, len);
		}catch(Throwable t){
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug ("Exception while handling data, closing the connection", t);
		    else if (LOGGER.isInfoEnabled ())
			LOGGER.info ("Exception while handling data, closing the connection : "+t);
		    connection.sendTcpSocketClose (sockId);
		}
	} // tcpSocketData

	private DiameterMessageFacade makeMessage(PeerSocket socket, MuxConnection connection, long sessionId, byte[] data, int offset, int len) {

		// we first extract the timestamp
		long timestamp = 0;
		byte b = data[offset];
		if (b == (byte)'T'){
		    offset++;
		    timestamp = data[offset] & 0xFF;
		    timestamp <<= 8;
		    timestamp |= data[offset + 1] & 0xFF;
		    timestamp <<= 8;
		    timestamp |= data[offset + 2] & 0xFF;
		    timestamp <<= 8;
		    timestamp |= data[offset + 3] & 0xFF;
		    timestamp <<= 8;
		    timestamp |= data[offset + 4] & 0xFF;
		    timestamp <<= 8;
		    timestamp |= data[offset + 5] & 0xFF;
		    offset += 6;
		    len -= 7;
		}
		
		int offset_orig = offset;
		DiameterMeters meters = socket.getDiameterMeters();
		
		if (len < 20){
		    // useless check in JDiameter (thanks to StandaloneTcpParser)
		    // useful in ASR
		    if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn("makeMessage: bad message length : "+len);
		    }
		    
			if(meters != null) {
				meters.incParseFailedMeter();
			}
		    
		    socket.close ();
		    return null;
		}
		// version
		int version = data[offset++] & 0xFF;
		if (version != 1) {
		    if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn("makeMessage: bad version=" + version);
		    }
		    
			if(meters != null) {
				meters.incParseFailedMeter();
			}
		    socket.close ();
		    return null;
		}
		// skip length (3 bytes)

		int length = data[offset++] & 0xFF;
		length <<= 8;
		length |= data[offset++] & 0xFF;
		length <<= 8;
		length |= data[offset++] & 0xFF;

		int flags = data[offset++] & 0xFF;
		int code = data[offset++] & 0xFF;
		code <<= 8;
		code |= data[offset++] & 0xFF;
		code <<= 8;
		code |= data[offset++] & 0xFF;
		long appId = Unsigned32Format.getUnsigned32(data, offset);
		offset += 4;
		int hopIdentifier = Integer32Format.getInteger32(data, offset);
		offset += 4;
		int endIdentifier = Integer32Format.getInteger32(data, offset);
		offset += 4;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("makeMessage: peer socket=" + socket + ", HopByHop Identifier=" + hopIdentifier + ", end Identifier=" + endIdentifier
					+ ", length=" + length);
		}

		DiameterMessageFacade diameterMessage;
		if ((flags & DiameterRequestFacade.REQUEST_FLAG) == DiameterRequestFacade.REQUEST_FLAG) {
			// the message is a request
			String handlerName = connection.attachment ();
			diameterMessage = new DiameterRequestFacade(handlerName, sessionId, appId, code, flags, hopIdentifier, endIdentifier);
			diameterMessage.setStackTimestamp (timestamp);
			newRequestReceived(diameterMessage, data, offset_orig, length);
		} else {
			// the message is a response
			DiameterRequestFacade request = socket.getRequestManager().getRequest(hopIdentifier);
			if (request == null) {
				if (LOGGER.isEnabledFor(Level.DEBUG)) {
					LOGGER.debug("Ignoring message with no matching HopByHop Identifier: " + hopIdentifier);
				}
				return null;
			}

			if (!request.mustResponseBeProcessed()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("makeMessage: response must not be processed -> do nothing");
				}
				return null;
			}

			// notify that a response has been received
			request.responseReceived(socket);

			// make the response
			DiameterResponseFacade response = request.getResponseFacade();
			response.setStackTimestamp (timestamp);
			response.setFlags(flags);
			response.removeDiameterAVPs(); // remove the session-id avp (and others)
			diameterMessage = response;
		}

		// sets the local IP where the message has been received
		diameterMessage.setReceptionAddresses (socket.getLocalInetSocketAddresses ());
		
		try {
			if (diameterMessage.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES ||
			    !DiameterProperties.isMessageScheduled ()) // dont copy data for nothing
				diameterMessage.readData(data, offset, length - 20);
			else // note that the msg.toString does not show AVPs
				diameterMessage.setData(data, offset, length - 20);
		}
		catch (DiameterMessageFacade.ParsingException pe){
			if(meters != null) {
				meters.incParseFailedMeter();
			}
		    if (diameterMessage.isRequest ()){
			// we let the parser fail again later for app messages
			if (diameterMessage.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES){
			    if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn(socket+" : exception while parsing control message : "+pe.getMessage ()+" : closing");
			    }
			    if (diameterMessage.getDiameterCommand() == DiameterBaseConstants.COMMAND_CER) {
				socket.rejectCER (diameterMessage, pe);
				return null;
			    }
			    socket.close();
			    return null;
			}
			diameterMessage.setData(data, offset, length - 20);
		    } else {
			// close socket for control messages
			if (diameterMessage.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES){
			    if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn(socket+" : exception while parsing control message : "+pe.getMessage ()+" : closing");
			    }
			    socket.close();
			    return null;
			}
			// drop response for app messages
			LOGGER.warn(socket + " : exception while parsing response - dropping it : "+pe.getMessage ());
			return null;
		    }
		}
		catch (Exception e) {
			if (LOGGER.isEnabledFor(Level.WARN)) {
				LOGGER.warn("Exception while parsing message on " + socket, e);
			}
			if(meters != null) {
				meters.incParseFailedMeter();
			}
			socket.close();
			return null;
		}
		
		Counter sizeCounter = Utils.getMsgSizeCounter(diameterMessage, false);
		if (sizeCounter != null) {
			sizeCounter.add(len - 20);
		}
		Rate nbCounter = Utils.getMsgNbRate(diameterMessage, false);
		if (nbCounter != null) {
			nbCounter.hit();
		}

		return diameterMessage;
	}

	/**
	 * Called when a new request is received.
	 * 
	 * @param diameterMessage The message.
	 * @param data The raw request.
	 * @param offset The offset of the beginning of the request in the data.
	 * @param length The length of the request in the data.
	 */
	protected void newRequestReceived(DiameterMessageFacade diameterMessage, byte[] data, int offset, int length) {
		// Do nothing
		// must be used if an inherited class wants to do something with a new request 
		// when it is received.
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#sctpSocketConnected(com.nextenso.mux.MuxConnection,
	 *      int, long, java.lang.String[], int, java.lang.String[], int, int, int,
	 *      boolean, int)
	 */
	@Override
	public void sctpSocketConnected(MuxConnection connection, int sockId, long connectionId, String[] remoteAddrs, int remotePort, String[] localAddrs,
					int localPort, int maxOutStreams, int maxInStreams, boolean fromClient, boolean secure, int errno) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Diameter SCTP socket opened id=" + sockId + ", error=" + errno);
		}

		String localAddress = null;
		if (localAddrs != null && localAddrs.length > 0) {
			localAddress = localAddrs[0];
		}

		String remoteAddress = null;
		if (remoteAddrs != null && remoteAddrs.length > 0) {
			remoteAddress = remoteAddrs[0];
		}
		
		socketConnected(Protocol.SCTP, connection, sockId, connectionId, errno, secure, remoteAddress, fromClient, remotePort, localAddress, localPort, null, 0, remoteAddrs, localAddrs, maxOutStreams, maxInStreams);

	}

	/**
	 * @see com.nextenso.mux.MuxHandler#sctpSocketData(com.nextenso.mux.MuxConnection,
	 *      int, long, java.nio.ByteBuffer, java.lang.String, boolean, boolean,
	 *      int, int)
	 */
	@Override
	public void sctpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer dataBuffer, String addr, boolean isUnordered,
			boolean isComplete, int ploadPID, int streamNumber) {
		PeerSocket socket = (PeerSocket) connection.getSocketManager().getSocket(Socket.TYPE_SCTP, sockId);
		byte[] rawData = dataBuffer.array();
		int len = dataBuffer.remaining();
		int offset = dataBuffer.position() + dataBuffer.arrayOffset();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("sctpSocketData: offset=" + offset + ", length=" + len);
		}
		try{
		    dataReceived(sockId, socket, connection, sessionId, rawData, offset, len);
		}catch(Throwable t){
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug ("Exception while handling data, closing the connection", t);
		    else if (LOGGER.isInfoEnabled ())
			LOGGER.info ("Exception while handling data, closing the connection : "+t);
		    connection.sendSctpSocketClose (sockId);
		}
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#sctpSocketClosed(com.nextenso.mux.MuxConnection,
	 *      int)
	 */
	@Override
	public void sctpSocketClosed(MuxConnection connection, int sockId) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Diameter SCTP  socket close: socket id=" + sockId);
		}
		
		PeerSocket socket = (PeerSocket) connection.getSocketManager().removeSocket(Socket.TYPE_SCTP, sockId);
		
		socketClosed(connection, sockId, socket);
	}

	/**
	 * @see com.nextenso.mux.MuxHandler#sctpSocketSendFailed(com.nextenso.mux.MuxConnection,
	 *      int, java.lang.String, int, java.nio.ByteBuffer, int)
	 */
	@Override
	public void sctpSocketSendFailed(MuxConnection cnx, int sockId, String addr, int streamNumber, ByteBuffer buf, int errcode) {
		// TODO 
	}

	@Override
	public void sctpPeerAddressChanged(MuxConnection connection,
					   int sockId,
					   String addr,
					   int port,
					   MuxHandler.SctpAddressEvent event){
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Diameter SCTP peer address changed : socket id=" + sockId+", addr="+addr+", event="+event);
		}
		PeerSocket socket = (PeerSocket) connection.getSocketManager().getSocket(Socket.TYPE_SCTP, sockId);
		if (socket == null) {
			LOGGER.debug("sctpPeerAddressChanged:  unknown peer socket -> do nothing");
			return;
		}
		
		socket.sctpPeerAddressChanged (addr, port, event);
	}

	private void socketClosed(MuxConnection connection, int sockId, PeerSocket socket) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("socketClosed:  socket id=" + sockId + ", peer socket=" + socket + ", connection=" + connection);
		}
		if (socket == null) {
			LOGGER.debug("socketClosed:  unknown peer socket -> do nothing");
			return;
		}

		int nb = _nbConnectedSocket.decrementAndGet();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("socketClosed: nb connected sockets=" + nb);
		}

		if (socket instanceof RSocket) {
			nb = _nbIncomingSocketToClose.decrementAndGet();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("socketClosed: nb sockets to close=" + nb);
			}
		}
		
		socket.closed(sockId);
	}

	@SuppressWarnings("unused")
	private void dataReceived(final int sockId, final PeerSocket socket, final MuxConnection connection, final long sessionId, final byte[] data, int off, int len) {
	    processReceivedData(socket, connection, sessionId, data, off, len);
	}

    private void processReceivedData(PeerSocket socket, MuxConnection connection, long sessionId, byte[] data, int off, int len) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("processReceivedData: connection= " + connection + ", socket=" + socket);
		}
		if (socket == null) {
			return;
		}

		DiameterMessageFacade diameterMessage = makeMessage(socket, connection, sessionId, data, off, len);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("processReceivedData: Received message :\n" + diameterMessage);
		}
		

		if (diameterMessage == null) {
			return;
		}
		
		DiameterMeters socketMeters = socket.getDiameterMeters();
		if(socketMeters != null) {
			socketMeters.incClientReadMeter(diameterMessage);
		}
		DiameterStateMachine sm = socket.getStateMachine();
		boolean mustProcess = true;
		if (sm == null && DiameterProperties.isForcedWatchdog())
			// if first CER, verify that this origin-host is not yet used, if we already have  
			// a peer with this value, verify that is still alive :
			//  - in the DW automate, if the status is SUSPECT -> use this new one
			//  - else send a DWR and wait for the DWA with a defined delay.
			// if no DWA  is received -> use this new one 
			// use this new one = close the previous connection and continue.
			// else = continue

			if (diameterMessage.isRequest() && diameterMessage.getDiameterApplication() == DiameterBaseConstants.APPLICATION_COMMON_MESSAGES
					&& diameterMessage.getDiameterCommand() == DiameterBaseConstants.COMMAND_CER) {

				DiameterAVP originHostAVP = diameterMessage.getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
				String originHost = IdentityFormat.getIdentity(originHostAVP.getValue());
				TableManager tm = Utils.getTableManager();
				String handlerName = connection.attachment ();
				DiameterPeer localPeer = tm.getLocalDiameterPeer(handlerName);
				RemotePeer peer = (RemotePeer) tm.getDiameterPeer(localPeer, originHost);
				if (peer != null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("processReceivedData: a CER is received for known Origin-Host -> force a DWR?");
					}

					PeerSocket existingSocket = peer.getStateMachine().getRSocket();
					if (existingSocket == null) {
						existingSocket = peer.getStateMachine().getISocket();
					}
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("processReceivedData: The existing socket=" + existingSocket);
					}
					if (existingSocket != null){
					    // already used origin-host -> verify the connection status
					    if (peer.getStateMachine ().isConnected ()){
						LOGGER.debug("processReceivedData: verify whether the existing socket is still responding.");
						mustProcess = false;
						DWListener listener = new DWListener(peer.getStateMachine(), socket, existingSocket, diameterMessage);
						peer.getStateMachine().sendForcedDWR(listener);
					    } else {
						// we are setting up the existingSocket : dont send the DWR
						LOGGER.debug("processReceivedData: dont verify whether the existing socket is still responding : setting up the connections");
					    }
					}
				}

			}

		if (mustProcess) {
			socket.processMessage(diameterMessage);
		}

	}

	@Override
	public int stoppingService(int disconnectCause) {
		LOGGER.debug("stoppingService...");

		if (!_stoppingListening) {
			LOGGER.debug("stoppingService: do the job!");

			_stoppingListening = true;
			_nbIncomingSocketToClose.set(0);

			// Disconnect all the peers for all the handlers
			List<DiameterPeer> res = new ArrayList<DiameterPeer>();

			for (Enumeration e = Utils.getConnectionManager().getMuxConnections(); e.hasMoreElements();) {
				MuxConnection connection = (MuxConnection) e.nextElement();
				String handlerName = connection.attachment ();

				PeerTable table = Utils.getTableManager().getPeerTable(handlerName);
				for (DiameterPeer peer : table.getPeers()) {
					if (!peer.isLocalInitiator() && !peer.isLocalDiameterPeer()) {
						res.add(peer);
					}
				}
			}

			_nbIncomingSocketToClose.addAndGet(res.size());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("stoppingService: nb socket to close=" + _nbIncomingSocketToClose.get());
				LOGGER.debug("stoppingService: socket to close=" + res);
			}

			for (DiameterPeer peer : res) {
				peer.disconnect(disconnectCause);
			}
		}

		int nbSocketsToClose = _nbIncomingSocketToClose.get();
		if (nbSocketsToClose == 0) {
			LOGGER.debug("stoppingService: no more socket to close -> finished");
			return 0;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("stoppingService: is already running and not finished (nb sockets=" + nbSocketsToClose + ")-> wait a while ");
		}

		if (_nbIncomingSocketToClose.get() <= 0) {
			return 0;
		}

		return 50;
	}

    public void muxData(MuxConnection connection,
			MuxHeader header,
			byte[] data,
			int off,
			int len){
	LOGGER.info ("HOP_ID_MASK set to : "+header.getChannelId ());
	DiameterRequestFacade.HOP_ID_MASK = header.getChannelId ();
	// advertize support of latency
	com.nextenso.mux.MuxHeaderV0 h = new com.nextenso.mux.MuxHeaderV0 ();
	h.set (0, 0, 1);
	connection.sendMuxData (h, null, 0, 0, false);
    }
    
	protected DiameterChannelMeters createConnectionMetrics(SimpleMonitorable mon) {
		DiameterChannelMeters cnxDiamMeters = new DiameterChannelMeters(mon, _metering, _agentMeters, false);
		mon.updated();
		
		return cnxDiamMeters;
	}
	
	protected void stopConnectionMetrics(MuxConnection cnx) {	
		Object[] attributes = cnx.getAttributes();
		DiameterChannelMeters cnxMeters = null;
		if(attributes != null) {
			cnxMeters = (DiameterChannelMeters) attributes[0];	
		}

		if(cnxMeters != null) {
			cnxMeters.socketClosed();
		}
	}
}
