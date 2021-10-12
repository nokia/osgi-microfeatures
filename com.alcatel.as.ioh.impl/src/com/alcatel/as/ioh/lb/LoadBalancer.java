// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;


import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.ioh.impl.conf.Property;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.management.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.reporter.api.CommandScopes;

import com.alcatel.as.service.discovery.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.tools.*;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LoadBalancer {
    
    public static final Logger LOGGER = Logger.getLogger ("as.ioh.lb");

    public static final String PROP_DEST_ID = "lb.dest.id";
    public static final String PROP_CLIENT_WRITE_BUFFER_MAX = "lb.client.write.buffer.max";
    public static final String PROP_DEST_WRITE_BUFFER_MAX = "lb.dest.write.buffer.max";
    public static final String PROP_CLIENT_LOG_MAPPING_LOGGER = "lb.client.log.mapping.logger";
    public static final String PROP_CLIENT_LOG_MAPPING_LEVEL = "lb.client.log.mapping.level";
    public static final String PROP_CLIENT_LOG_MAPPING_MSG_OPEN = "lb.client.log.mapping.msg.open";
    public static final String PROP_CLIENT_LOG_MAPPING_MSG_CLOSE = "lb.client.log.mapping.msg.close";
    public static final String PROP_CLIENT_LOG_TRAFFIC_LOGGER = "lb.client.log.traffic.logger";
    public static final String PROP_CLIENT_LOG_TRAFFIC_LEVEL = "lb.client.log.traffic.level";

    public static final String PROP_CLIENT_TRANSPARENT_IP = UdpClient.PROP_CLIENT_TRANSPARENT+".ip"; // only client ip is used / else ip+port

    private PlatformExecutors _executors;
    private MeteringService _metering;
    private ClientFactory _clientFactory;
    private ServerFactory _serverFactory;
    private BundleContext _osgi;
    private Map<String, RouterFactory> _routers = new HashMap<> ();
    private Map<String, UnicastRouterFactory> _urouters = new HashMap<> ();
    private Map<String, ParserFactory> _parsers = new HashMap<> ();
    private Map<String, MulticastProcessor> _mprocs = new HashMap<> ();
    private Map<String, UnicastProcessor> _uprocs = new HashMap<> ();

    @FileDataProperty(title="LoadBalancer Server",
		      fileData="defTcpLBServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening endpoints.")
		      public final static String CONF_TCP_SERVERS = "lb.tcp.servers";
    @FileDataProperty(title="LoadBalancer Destinations",
		      fileData="defTcpLBClient.txt",
		      required=true,
		      dynamic=true,
		      section="Destination",
		      help="Describes the destinations.")
		      public final static String CONF_TCP_CLIENTS = "lb.tcp.clients";
    @FileDataProperty(title="LoadBalancer Server",
		      fileData="defUdpLBServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening endpoints.")
		      public final static String CONF_UDP_SERVERS = "lb.udp.servers";
    @FileDataProperty(title="LoadBalancer Destinations",
		      fileData="defUdpLBClient.txt",
		      required=true,
		      dynamic=true,
		      section="Destination",
		      help="Describes the destinations.")
		      public final static String CONF_UDP_CLIENTS = "lb.udp.clients";
    
    @Reference()
    public void setExecutors(PlatformExecutors executors){
	LOGGER.info ("@Reference setExecutors");
	_executors = executors;
    }
    @Reference()
    public void setMetering(MeteringService metering){
	LOGGER.info ("@Reference setMetering");
	_metering = metering;
    }
    @Reference
    public void setClientFactory (ClientFactory factory) throws Exception {
	LOGGER.info ("@Reference setClientFactory");
	_clientFactory = factory;
    }
    @Reference
    public void setServerFactory (ServerFactory factory) throws Exception {
	LOGGER.info ("@Reference setServerFactory");
	_serverFactory = factory;
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setRouterFactory (RouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	LOGGER.info ("@Reference setRouterFactory : "+id+" : "+routerF);
	_routers.put (id, routerF);
	initProcs ();
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setUnicastRouterFactory (UnicastRouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	LOGGER.info ("@Reference setUnicastRouterFactory : "+id+" : "+routerF);
	_urouters.put (id, routerF);
	initProcs ();
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setParserFactory (ParserFactory parserF, Map<String, String> properties){
	String id = properties.get ("parser.id");
	LOGGER.info ("@Reference setParserFactory : "+id+" : "+parserF);
	_parsers.put (id, parserF);
	initProcs ();
    }
    public void unsetRouterFactory (RouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    public void unsetUnicastRouterFactory (UnicastRouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    public void unsetParserFactory (ParserFactory parserF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    
    protected ClientFactory getClientFactory (){ return _clientFactory;}
    public PlatformExecutors getPlatformExecutors (){ return _executors;}
    
    
    @Activate
    public synchronized void activate (BundleContext ctx, Map<String, String> conf) throws Exception {
	LOGGER.info ("@Activate");
	updated (conf);
	_osgi = ctx;
	initProcs ();
    }

    @Modified
    public void updated (Map<String, String> conf) throws Exception {
	LOGGER.info ("Configuration : "+conf);
	_serverFactory.newTcpServerConfig ("lb", conf.get (CONF_TCP_SERVERS));
	_clientFactory.newTcpClientConfig (conf.get (CONF_TCP_CLIENTS));
	_serverFactory.newUdpServerConfig ("lb", conf.get (CONF_UDP_SERVERS));
	_clientFactory.newUdpClientConfig (conf.get (CONF_UDP_CLIENTS));
    }

    @Deactivate
    public void deactivate (){
	LOGGER.warn ("@Deactivate");
    }

    private void initProcs (){
	if (_osgi == null) return;
	for (String rid: _routers.keySet ()){
	    for (String pid: _parsers.keySet ()){
		String id = (pid.equals (rid)) ? "ioh.lb."+pid : "ioh.lb."+pid+"."+rid;
		if (_mprocs.get (id) != null) continue;
		LOGGER.info ("Registering m-processor : "+id);
		MulticastProcessor proc = new MulticastProcessor (this, id, _parsers.get (pid), _routers.get (rid));
		_mprocs.put (id, proc);
		proc.register ();
	    }
	}
	for (String rid: _urouters.keySet ()){
	    for (String pid: _parsers.keySet ()){
		String id = (pid.equals (rid)) ? "ioh.lb."+pid : "ioh.lb."+pid+"."+rid;
		if (_uprocs.get (id) != null) continue;
		LOGGER.info ("Registering u-processor : "+id);
		UnicastProcessor proc = new UnicastProcessor (this, id, _parsers.get (pid), _urouters.get (rid));
		_uprocs.put (id, proc);
		proc.register ();
	    }
	}
    }
    
    protected static class MulticastProcessor implements TcpServerProcessor, UdpServerProcessor {
	
	protected Logger _logger;

	private LoadBalancer _lb;
	private ClientFactory _clientFactory;
	private RouterFactory _routerF;
	private String _id, _toString;
	private ParserFactory _parserF;
	
	public MulticastProcessor (LoadBalancer lb, String id, ParserFactory parserF, RouterFactory routerF){
	    _lb = lb;
	    _id = id;
	    _parserF = parserF;
	    _routerF = routerF;
	    _clientFactory = _lb.getClientFactory ();
	    _logger = Logger.getLogger ("as."+id);
	    _toString = "LB.MProcessor["+_id+"]";
	}

	public String toString (){ return _toString;}
    
	public LoadBalancer getLoadBalancer (){
	    return _lb;
	}

	protected void register (){
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    _lb._osgi.registerService (TcpServerProcessor.class.getName (), this, props);
	    _lb._osgi.registerService (UdpServerProcessor.class.getName (), this, props);
	}
    
	/**********************************************
	 *           TcpServer open/update/close      *
	 **********************************************/
    
	public void serverCreated (TcpServer server){
	    _logger.info ("serverCreated : "+server);
	    server.getProperties ().put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	    String name = (String) server.getProperties ().get (Server.PROP_SERVER_NAME);
	    Property.getProperty (PROP_DEST_ID, server.getProperties (), name, true);
	    Property.getProperty (PROP_DEST_WRITE_BUFFER_MAX, server.getProperties (), "1000000", true); // 1Meg per dest
	    Property.getProperty (PROP_CLIENT_WRITE_BUFFER_MAX, server.getProperties (), "10000000", true); // 10Meg per clientw
	    initLoggers (server);
	    Meters meters = new Meters ("as.ioh.lb."+name, "LoadBalancer for server : "+name, _lb._metering);
	    server.getProperties ().put (SimpleMonitorable.class.toString (), meters);	    
	    meters.init ();
	    meters.start (_lb._osgi);
	}
	public void serverOpened (TcpServer server){
	    _logger.info ("serverStarted : "+server);
	    server.getProperties ().put ("RouterFactory.Config", _routerF.newRouterConfig (server.getProperties ()));
	    server.getProperties ().put ("ParserFactory.Config", _parserF.newParserConfig (server.getProperties ()));
	}
	public void serverFailed (TcpServer server, Object cause){
	    _logger.debug ("serverFailed : "+server);
	}
	public void serverUpdated (TcpServer server){
	    _logger.info ("serverUpdated : "+server);
	    server.getProperties ().put ("RouterFactory.Config", _routerF.newRouterConfig (server.getProperties ()));
	    server.getProperties ().put ("ParserFactory.Config", _parserF.newParserConfig (server.getProperties ()));
	}
	public void serverClosed (TcpServer server){
	    _logger.info ("serverClosed : "+server);
	}
	public void serverDestroyed (TcpServer server){
	    _logger.info ("serverDestroyed : "+server);
	    getMeters (server).stop ();
	}
	public Meters getMeters (TcpServer server){
	    return (Meters) server.getProperties ().get (SimpleMonitorable.class.toString ());
	}
	
	/**********************************************
	 *           connection mgmt                  *
	 **********************************************/

	// called in the server Q
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    Router router = _routerF.newRouter (server.getProperties ().get ("RouterFactory.Config"));
	    Parser clientParser = _parserF.newClientParser (server.getProperties ().get ("ParserFactory.Config"), router.neededBuffer ());
	    Parser serverParser = null;
	    if (router.needServerData ()) serverParser = _parserF.newServerParser (server.getProperties ().get ("ParserFactory.Config"), router.neededBuffer ());
	    Logger logger = (Logger) server.getProperties ().get ("server.logger");
	    init (acceptedChannel, new TcpClientContext (this, acceptedChannel, clientParser, serverParser, router, logger, getMeters (server), props));
	}
	private void init (AsyncChannel channel, ClientContext ctx){
	    channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	    channel.attach (ctx);
	    ctx.start ();
	}
    
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
	
	/**********************************************
	 *           UdpServer open/update/close      *
	 **********************************************/

	public void serverCreated (UdpServer server){
	    _logger.info ("serverCreated : "+server);
	    server.getProperties ().put (UdpServer.PROP_READ_BUFFER_DIRECT, true);
	    String name = (String) server.getProperties ().get (Server.PROP_SERVER_NAME);
	    Property.getProperty (PROP_DEST_ID, server.getProperties (), name, true);
	    Property.getProperty (PROP_DEST_WRITE_BUFFER_MAX, server.getProperties (), "1000000", true); // 1Meg per dest
	    UdpMeters meters = new UdpMeters ("as.ioh.lb."+name, "LoadBalancer for server : "+name, _lb._metering);
	    server.getProperties ().put (SimpleMonitorable.class.toString (), meters);	    
	    meters.init ();
	    meters.start (_lb._osgi);
	}
	public void serverOpened (UdpServer server){
	    _logger.info ("serverStarted : "+server);
	    server.getServerChannel ().attach (new UdpChannelListenerImpl (server));
	    server.getServerChannel ().enableReading ();
	}
	public void serverFailed (UdpServer server, Object cause){
	    _logger.debug ("serverFailed : "+server);
	}
	public void serverUpdated (UdpServer server){
	    _logger.info ("serverUpdated : "+server);
	    ((UdpChannelListenerImpl) server.getServerChannel ().attachment ()).serverUpdated (server);
	}
	public void serverClosed (UdpServer server){
	    _logger.info ("serverClosed : "+server);
	    UdpChannelListenerImpl chL = server.getServerChannel ().attachment ();
	    chL.close ();
	}
	public void serverDestroyed (UdpServer server){
	    _logger.info ("serverDestroyed : "+server);
	    getMeters (server).stop ();
	}
	public UdpMeters getMeters (UdpServer server){
	    return (UdpMeters) server.getProperties ().get (SimpleMonitorable.class.toString ());
	}
	public UdpChannelListener getChannelListener (UdpChannel cnx){
	    return (UdpChannelListener) cnx.attachment ();
	}

	/****************************************************************
	 *           udp channel listener : 1 per UDP server            *
	 ****************************************************************/

	public class UdpChannelListenerImpl implements UdpChannelListener {

	    private Map<InetSocketAddress, UdpClientContext> _udpClientContexts = new HashMap<> ();
	    private UdpServer _server;
	    private UdpMeters _meters;
	    private UdpClient _udpClient;
	    private PlatformExecutor _exec;
	    private Logger _serverLogger;
	    private boolean _stateless, _stateful, _sessionsPooling;
	    private long _sessionTimeout, _readTimeout;
	    private int _sessionsPoolSize, _sessionsMax;
	    private List<UdpClientContext> _sessionsPool = new ArrayList<> ();
	    private boolean _closed;
	    private int _maxClientBuffer;
	    private Object _routerConfig, _parserConfig;
	    private int _readResponses;
	    private boolean _transparent;
	    private boolean _transparentIP;
	    
	    private UdpChannelListenerImpl (UdpServer server){
		_server = server;
		_meters = getMeters (server);
		_exec = (PlatformExecutor) server.getProperties ().get (UdpServer.PROP_READ_EXECUTOR);
		_routerConfig = _routerF.newRouterConfig (server.getProperties ());
		_parserConfig = _parserF.newParserConfig (server.getProperties ());
		_maxClientBuffer = Property.getIntProperty (PROP_CLIENT_WRITE_BUFFER_MAX, server.getProperties (), 10000000, true); // 10Megs by def on client side
		_sessionTimeout = (long) Property.getIntProperty ("session.timeout", server.getProperties (), 0, false);
		_stateless = _sessionTimeout == 0L;
		_stateful = !_stateless;
		if (_sessionTimeout > 0L){ // we force read.timeout to session.timeout
		    server.getProperties ().put (UdpClient.PROP_READ_TIMEOUT, _sessionTimeout);
		    _readTimeout = _sessionTimeout;
		} else {
		    _readTimeout = (long) Property.getIntProperty (UdpClient.PROP_READ_TIMEOUT, server.getProperties (), 0, false);
		}
		_sessionsPoolSize = Property.getIntProperty ("session.pool", server.getProperties (), 1000, false);
		_sessionsPooling = _sessionsPoolSize > 0;
		//_sessionPoolTimeout = (long) Property.getIntProperty ("session.pool.timeout", server.getProperties (), 60000, false);
		_sessionsMax = Property.getIntProperty ("session.max", server.getProperties (), 1500, false);
		_readResponses = Property.getIntProperty ("read.responses", server.getProperties (), -1, false);
		if (_sessionsPoolSize > _sessionsMax) _sessionsPoolSize = _sessionsMax;
		server.getProperties ().put (UdpClient.PROP_CLIENT_LOGGER, _serverLogger = (Logger) server.getProperties ().get ("server.logger"));
		_udpClient = _lb.getClientFactory ().newUdpClient ((String) server.getProperties ().get (LoadBalancer.PROP_DEST_ID), server.getProperties ());
		_transparent = Property.getBooleanProperty (UdpClient.PROP_CLIENT_TRANSPARENT, _udpClient.getProperties (), false, false); // can be in server or in client
		_transparentIP = Property.getBooleanProperty (PROP_CLIENT_TRANSPARENT_IP, _udpClient.getProperties (), false, false); // can be in server or in client
	    }
	    private void serverUpdated (UdpServer server){
		_routerConfig = _routerF.newRouterConfig (server.getProperties ());
		_parserConfig = _parserF.newParserConfig (server.getProperties ());
		_maxClientBuffer = Property.getIntProperty (PROP_CLIENT_WRITE_BUFFER_MAX, server.getProperties (), 10000000, true); // 10Megs by def on client side
	    }

	    private void close (){
		_closed = true;
		_udpClient.close ();
		_sessionsPooling = false;
		for (UdpClientContext ctx : _sessionsPool){
		    ctx.close ();
		}
		_sessionsPool.clear ();
	    }

	    public boolean done (UdpClientContext ctx){
		if (_stateful) _udpClientContexts.remove (ctx.getRemoteAddress ());
		_meters.getSessionsActiveMeter ().inc (-1);
		if (_sessionsPooling){
		    if (_sessionsPool.size () < _sessionsPoolSize) {
			if (_serverLogger.isDebugEnabled ())
			    _serverLogger.debug (ctx+" : join pool");
			_sessionsPool.add (ctx);
			_meters.getSessionsPoolMeter ().inc (1);
			return false;
		    }
		}
		_meters.getOpenUdpChannelsMeter ().inc (-1);
		_meters.getClosedUdpChannelsMeter ().inc (1);
		return true;
	    }

	    public MulticastProcessor getProcessor (){ return MulticastProcessor.this;}
	    public PlatformExecutor getExecutor (){ return _exec;}
	    public int getMaxClientBuffer (){ return _maxClientBuffer;}
	    public int getReadResponses (){ return _readResponses;}
	    public long getReadTimeout (){ return _readTimeout;}
	    public boolean transparent (){ return _transparent;}
	    public boolean transparentIP (){ return _transparentIP;}
	    
	    public void execute (Runnable r){
		_exec.execute (r);
	    }

	    public void connectionOpened(UdpChannel cnx){ // not called
	    }
	    public void connectionFailed(UdpChannel cnx,
					 java.lang.Throwable err){ // not called
	    }
	    public void connectionClosed(UdpChannel cnx){
		//TODO / not possible for now
	    }
	    public void messageReceived(UdpChannel cnx,
					java.nio.ByteBuffer msg,
					java.net.InetSocketAddress addr){
		_meters.getReadUdpMeter ().inc (msg.remaining ());
		if (_udpClient.getDestinations ().size () == 0){
		    if (_serverLogger.isDebugEnabled ())
			_serverLogger.debug ("messageReceived with no destination available : dropping");
		    _meters.getFailedUdpMeter ().inc (msg.remaining ());
		    msg.position (msg.limit ());
		    return;
		}
		UdpClientContext ctx = _stateful ? ctx = _udpClientContexts.get (addr) : null;
		if (ctx == null){
		    Router router = _routerF.newRouter (_routerConfig);
		    Parser parser = _parserF.newParser (_parserConfig, router.neededBuffer ());
		    if (_sessionsPooling){
			int size = _sessionsPool.size ();
			if (size > 0) {
			    ctx = _sessionsPool.remove (size - 1);
			    _meters.getSessionsPoolMeter ().inc (-1);
			    ctx.recycle (addr, parser, router);
			}
		    }
		    if (ctx == null){
			// the sessions pool is empty
			if (_meters.getSessionsActiveMeter ().getValue () >= _sessionsMax){
			    if (_serverLogger.isInfoEnabled ())
				_serverLogger.info ("Number of simultaneous sessions exceeded - dropping message from "+addr);
			    _meters.getFailedUdpMeter ().inc (msg.remaining ());
			    msg.position (msg.limit ());
			    return;
			}
			ctx = new UdpClientContext (this, cnx, _udpClient, addr, parser, router, _serverLogger, _meters, _server.getProperties ());
			if (ctx.init () == false){
			    _serverLogger.warn ("Failed to create UdpClientContext for "+addr);
			    _meters.getFailedUdpChannelsMeter ().inc (1);
			    _meters.getFailedUdpMeter ().inc (msg.remaining ());
			    msg.position (msg.limit ());
			    return;
			}
			_meters.getOpenUdpChannelsMeter ().inc (1);
		    }
		    if (_stateful) _udpClientContexts.put (addr, ctx);
		    _meters.getSessionsActiveMeter ().inc (1);
		}
		ctx.messageReceived (msg);
	    }
	    public void receiveTimeout(UdpChannel cnx){
	    }
	    public void writeBlocked(UdpChannel cnx){
	    }
	    public void writeUnblocked(UdpChannel cnx){
	    }
	}
    }

    /****************************************************************
     *           unicast tcp mode       *
     ****************************************************************/

    protected static class UnicastProcessor implements TcpServerProcessor {
	
	protected Logger _logger;

	private LoadBalancer _lb;
	private ClientFactory _clientFactory;
	private UnicastRouterFactory _routerF;
	private String _id, _toString;
	private ParserFactory _parserF;
	
	public UnicastProcessor (LoadBalancer lb, String id, ParserFactory parserF, UnicastRouterFactory routerF){
	    _lb = lb;
	    _id = id;
	    _parserF = parserF;
	    _routerF = routerF;
	    _clientFactory = _lb.getClientFactory ();
	    _logger = Logger.getLogger ("as."+id);
	    _toString = "LB.UProcessor["+_id+"]";
	}

	public String toString (){ return _toString;}
    
	public LoadBalancer getLoadBalancer (){
	    return _lb;
	}

	protected void register (){
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    _lb._osgi.registerService (TcpServerProcessor.class.getName (), this, props);
	}

	/**********************************************
	 *           TcpServer open/update/close      *
	 **********************************************/

	public void serverCreated (TcpServer server){
	    _logger.info (this+" : serverCreated : "+server);
	    server.getProperties ().put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	    String name = (String) server.getProperties ().get (Server.PROP_SERVER_NAME);
	    Property.getProperty (PROP_DEST_ID, server.getProperties (), name, true);
	    Property.getProperty (PROP_DEST_WRITE_BUFFER_MAX, server.getProperties (), "1000000", true); // 1Meg per dest
	    Property.getProperty (PROP_CLIENT_WRITE_BUFFER_MAX, server.getProperties (), "10000000", true); // 10Meg per client
	    initLoggers (server);
	}
	public void serverOpened (TcpServer server){
	    _logger.info (this+" : serverStarted : "+server);
	    ServerContext ctx = new ServerContext ();
	    ctx._logger = (Logger) server.getProperties ().get ("server.logger");
	    ctx._routerFC = _routerF.newUnicastRouterConfig (server.getProperties ());
	    ctx._parserFC = _parserF.newParserConfig (server.getProperties ());
	    String name = (String) server.getProperties ().get (Server.PROP_SERVER_NAME);
	    ctx._meters = new Meters ("as.ioh.lb."+name, "LoadBalancer for server : "+name, _lb._metering);
	    ctx._meters.init ();
	    ctx._meters.start (_lb._osgi);
	    server.getProperties ().put ("ServerContext", ctx);
	    startTracking (server);
	}
	public void serverFailed (TcpServer server, Object cause){
	}
	public void serverUpdated (TcpServer server){
	    _logger.info (this+" : serverUpdated : "+server);
	    ServerContext ctx = getServerContext (server);
	    ctx._routerFC = _routerF.newUnicastRouterConfig (server.getProperties ());
	    ctx._parserFC = _parserF.newParserConfig (server.getProperties ());
	}
	public void serverClosed (TcpServer server){
	    _logger.info (this+" : serverClosed : "+server);
	}
	public void serverDestroyed (TcpServer server){
	    _logger.info (this+" : serverDestroyed : "+server);
	    getServerContext (server)._meters.stop ();
	    stopTracking (server);
	}
	private ServerContext getServerContext (TcpServer server){
	    return (ServerContext) server.getProperties ().get ("ServerContext");
	}
	
	/**********************************************
	 *           connection mgmt                  *
	 **********************************************/

	// called in the server Q
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    ServerContext ctx = getServerContext (server);
	    UnicastRouter router = _routerF.newUnicastRouter (ctx._routerFC);
	    Parser parser = _parserF.newParser (ctx._parserFC, router.neededBuffer ());
	    init (acceptedChannel, new TcpClientContext (this, acceptedChannel, parser, router, ctx._logger, ctx._meters, ctx._adverts, props));
	}
	private void init (AsyncChannel channel, TcpClientContext ctx){
	    channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	    channel.attach (ctx);
	    ctx.start ();
	}
    
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}

	/**********************************************
	 *           tracking                   *
	 **********************************************/
	private boolean startTracking (final TcpServer server){
	    Map<String, Object> serverProps = server.getProperties ();
	    final ServerContext ctx = getServerContext (server);
	    final Logger logger = ctx._logger;
	    Map<String, Object> props = new HashMap<> ();
	    props.put ("client.ephemeral", true);
	    final TcpClient client = _lb.getClientFactory ().newTcpClient ((String) serverProps.get (PROP_DEST_ID), props);
	    Map<String, Object> clientProps = client.getProperties ();
	    String namespace = (String) clientProps.get (TcpClient.PROP_TRACK_NAMESPACE);
	    String podName = (String) clientProps.get (TcpClient.PROP_TRACK_POD_NAME);
	    String containerName = (String) clientProps.get (TcpClient.PROP_TRACK_CONTAINER_NAME);
	    String containerPortName = (String) clientProps.get (TcpClient.PROP_TRACK_CONTAINER_PORT_NAME);
	    Object label = clientProps.get (TcpClient.PROP_TRACK_POD_LABEL);
	    if (namespace != null ||
		podName != null ||
		containerName != null ||
		containerPortName != null ||
		label != null){
		if (logger.isDebugEnabled ())
		    logger.debug (server+" : startTracking : "+namespace+"/"+podName+"/"+containerName+"/"+containerPortName+"/"+label);

		AdvertisementTracker.Listener listener = new AdvertisementTracker.Listener (){
			public Object up (AdvertisementTracker tracker, final InetSocketAddress addr, ServiceReference ref){
			    String name = new StringBuilder ()
				.append (ref.getProperty ("namespace")).append ('.')
				.append (ref.getProperty ("pod.name")).append ('.')
				.append (ref.getProperty ("container.name")).append ('.')
				.append (ref.getProperty ("container.port.name"))
				.toString ();
			    if (logger.isInfoEnabled ())
				logger.info (server+" : advert UP : "+addr+" : "+name);
			    ctx._adverts.add (addr);
			    return addr;
			}
			public void down (AdvertisementTracker tracker, ServiceReference ref, Object attachment){
			    if (logger.isInfoEnabled ())
				logger.info (server+" : advert DOWN : "+ctx);
			    ctx._adverts.remove ((InetSocketAddress) attachment); // we do not allow twice the same ip/port in this case - which should be ok
			}
		    };
		AdvertisementTracker tracker = new AdvertisementTracker (listener);
		if (namespace != null) tracker.addFilter ("namespace", namespace, true);
		if (podName != null) tracker.addFilter ("pod.name", podName, true);
		if (containerName != null) tracker.addFilter ("container.name", containerName, true);
		if (containerPortName != null) tracker.addFilter ("container.port.name", containerPortName, true);
		if (label != null){
		    if (label instanceof String){
			String labelS = (String) label;
			int index = labelS.indexOf (':');
			if (index != -1 && index != 0 && index != (labelS.length () - 1)){
			    tracker.addFilter ("pod.label."+labelS.substring (0, index), labelS.substring (index+1), true);
			}
		    } else {
			List<String> list = (List<String>) label;
			for (String labelS : list){
			    int index = labelS.indexOf (':');
			    if (index != -1 && index != 0 && index != (labelS.length () - 1)){
				tracker.addFilter ("pod.label."+labelS.substring (0, index), labelS.substring (index+1), true);
			    }
			}
		    }
		}
		tracker.addFilter ("container.port.protocol", "TCP", true);
		ctx._adverts = new java.util.concurrent.ConcurrentSkipListSet<InetSocketAddress> (destIPcomparator);
		ctx._tracker = tracker;
		tracker.open (_lb._osgi);
		return true;
	    } else {
		return false;
	    }
	}
	private void stopTracking (TcpServer server){
	    ServerContext ctx = getServerContext (server);
	    if (ctx._tracker != null) ctx._tracker.close ();
	}

    }

    public static class ServerContext {
	public Logger _logger;
	public Meters _meters;
	public AdvertisementTracker _tracker;
	public Set<InetSocketAddress> _adverts;
	public Object _parserFC;
	public Object _routerFC;
    }

    /****************************** Utilities *********************/

    public static Comparator<InetSocketAddress> destIPcomparator = new Comparator<InetSocketAddress> (){
	    public int compare (InetSocketAddress a1, InetSocketAddress a2){
		return a1.toString ().compareTo (a2.toString ());
	    }
	};

    private static void initLoggers (TcpServer server){
	Level level = Level.toLevel (((String) Property.getProperty (PROP_CLIENT_LOG_MAPPING_LEVEL, server.getProperties (), "DEBUG", false)).toUpperCase ());
	server.getProperties ().put (PROP_CLIENT_LOG_MAPPING_LEVEL, level);
	String loggerS = (String) Property.getProperty (PROP_CLIENT_LOG_MAPPING_LOGGER, server.getProperties (), null, false);
	if (loggerS != null) server.getProperties ().put (PROP_CLIENT_LOG_MAPPING_LOGGER, Logger.getLogger (loggerS));
	level = Level.toLevel (((String) Property.getProperty (PROP_CLIENT_LOG_TRAFFIC_LEVEL, server.getProperties (), "TRACE", false)).toUpperCase ());
	server.getProperties ().put (PROP_CLIENT_LOG_TRAFFIC_LEVEL, level);
	loggerS = (String) Property.getProperty (PROP_CLIENT_LOG_TRAFFIC_LOGGER, server.getProperties (), null, false);
	if (loggerS != null) server.getProperties ().put (PROP_CLIENT_LOG_TRAFFIC_LOGGER, Logger.getLogger (loggerS));
    }

    public static StringBuilder dumpData (StringBuilder sb, java.nio.ByteBuffer data){
	int pos = data.position ();
	int size = data.remaining ();
	for (int k=0; k<size; k++){
	    String s = Integer.toHexString (data.get () & 0xFF);
	    if (k == 0){
		if (s.length () == 1)
		    sb.append ("0x0");
		else
		    sb.append ("0x");
	    } else {
		if (s.length () == 1)
		    sb.append (" 0x0");
		else
		    sb.append (" 0x");
	    }
	    sb.append (s);
	}
	data.position (pos);
	return sb;
    }
}
