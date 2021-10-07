package com.alcatel.as.diameter.ioh.impl;

import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.diameter.ioh.*;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.shutdown.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.discovery.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;
import com.nextenso.mux.MuxFactory.ConnectionListener;

import static com.alcatel.as.diameter.ioh.impl.DiameterIOH.getBooleanProperty;
import static com.alcatel.as.diameter.ioh.impl.DiameterIOH.getIntProperty;

public class DiameterIOHEngine extends IOHEngine implements Shutdownable {

    public static final String ENDPOINT_ADDRESS = "endpoint.address";
    
    private DiameterIOHRouter _router;
    private BundleContext _osgi;
    private Shutdown _shutdown;

    protected Meter _parserErrorMeter;
    protected Map<Object, Meter> _writeByTypeMeters = new HashMap<> ();
    protected Map<Object, Meter> _readByTypeMeters = new HashMap<> ();
    protected DiameterIOHMeters _diamMeters;
    protected List<InetAddress> _hostIPAddresses = new CopyOnWriteArrayList<> ();
    protected List<Object[]> _appCounters = new ArrayList<> ();
    protected Map<String, List<Integer>> _appRespCounters = new HashMap<> ();

    protected Meter _channelsDiameterTcpOpenMeter, _channelsDiameterTcpAcceptedOpenMeter, _channelsDiameterTcpConnectedOpenMeter, _channelsDiameterSctpOpenMeter, _channelsDiameterSctpAcceptedOpenMeter, _channelsDiameterSctpConnectedOpenMeter;
    protected Meter _channelsDiameterTcpClosedMeter, _channelsDiameterTcpAcceptedClosedMeter, _channelsDiameterTcpConnectedClosedMeter, _channelsDiameterSctpClosedMeter, _channelsDiameterSctpAcceptedClosedMeter, _channelsDiameterSctpConnectedClosedMeter;
    
    protected boolean _dropOnTcpOverload = false;
    protected boolean _dropOnSctpOverload = false;
    protected boolean _sctpUnordered = true;
    protected boolean _sctpMStreams = false;
    protected int _sctpConnectStreamOut = 0;
    protected boolean _latencyProcEngine, _latencyProcAgent, _latencyProcAny, _latencyProcSamplingAll;
    protected int _latencyProcSampling;
    protected PlatformExecutor _latencyQueue;
    
    protected DiameterIOHEngine (String name, IOHServices services, DiameterIOHRouterFactory routerFactory){
	super (name, services);
	_router = routerFactory.newDiameterIOHRouter ();
    }

    public DiameterIOHRouter getDiameterIOHRouter (){ return _router;}
    public DiameterIOHMeters getDiameterIOHMeters (){ return _diamMeters;}

    // to expose the methods in the package (they are protected in IOHEngine)
    protected Map<Integer, IOHChannel> getDiameterTcpChannels (){ return _tcpChannels;}
    protected Map<Integer, IOHChannel> getDiameterSctpChannels (){ return _sctpChannels;}
    
    public IOHEngine init (DiameterIOH ioh, TcpServer server, BundleContext osgi, Dictionary<String, String> system){
	_osgi = osgi;
	server.getProperties ().put (PROP_TCP_ACCEPT_SHARED, "true");
	server.getProperties ().put (PROP_TCP_ACCEPT_SHARED_CLOSE, "false");
	server.getProperties ().put (PROP_TCP_CONNECT_UNIQUE, "true");
	server.getProperties ().put (PROP_TCP_CONNECT_SHARED_CLOSE, "false");
	server.getProperties ().put (PROP_TCP_LISTEN_NOTIFY, "true");
	server.getProperties ().put (PROP_UDP, "false");
	server.getProperties ().put (PROP_SCTP_ACCEPT_SHARED, "true");
	server.getProperties ().put (PROP_SCTP_ACCEPT_SHARED_CLOSE, "false");
	server.getProperties ().put (PROP_SCTP_CONNECT_UNIQUE, "true");
	server.getProperties ().put (PROP_SCTP_CONNECT_SHARED_CLOSE, "false");
	server.getProperties ().put (PROP_SCTP_LISTEN_NOTIFY, "true");
	server.getProperties ().put (PROP_SCTP, "true");
	if (server.getProperties ().get (PROP_LOG_TCP_ACCEPTED) == null)
	    server.getProperties ().put (PROP_LOG_TCP_ACCEPTED, "WARN");
	if (server.getProperties ().get (PROP_LOG_TCP_CONNECTED) == null)
	    server.getProperties ().put (PROP_LOG_TCP_CONNECTED, "WARN");
	if (server.getProperties ().get (PROP_LOG_TCP_FAILED) == null)
	    server.getProperties ().put (PROP_LOG_TCP_FAILED, "INFO");
	if (server.getProperties ().get (PROP_LOG_TCP_CLOSED) == null)
	    server.getProperties ().put (PROP_LOG_TCP_CLOSED, "WARN");
	if (server.getProperties ().get (PROP_LOG_SCTP_ACCEPTED) == null)
	    server.getProperties ().put (PROP_LOG_SCTP_ACCEPTED, "WARN");
	if (server.getProperties ().get (PROP_LOG_SCTP_CONNECTED) == null)
	    server.getProperties ().put (PROP_LOG_SCTP_CONNECTED, "WARN");
	if (server.getProperties ().get (PROP_LOG_SCTP_FAILED) == null)
	    server.getProperties ().put (PROP_LOG_SCTP_FAILED, "INFO");
	if (server.getProperties ().get (PROP_LOG_SCTP_CLOSED) == null)
	    server.getProperties ().put (PROP_LOG_SCTP_CLOSED, "WARN");
	
	Map<String, Object> conf = server.getProperties ();
	_dwrTimeout = getIntProperty (DiameterIOH.CONF_DWR_TIMEOUT, conf, (int) ioh.getDwrTimeout ());
	_dwrAttempts = getIntProperty (DiameterIOH.CONF_DWR_ATTEMPTS, conf, ioh.getDwrAttempts ());
	_clientReqTimeout = getIntProperty (DiameterIOH.CONF_TIMER_CLIENT_REQ, conf, ioh.getClientReqTimeout ());
	_closeOnDpaDelay  = getIntProperty (DiameterIOH.CONF_DELAY_DPA_CLOSE, conf, 0);
	_closeOnDpaTimeout  = getIntProperty (DiameterIOH.CONF_TIMEOUT_DPA_CLOSE, conf, 250);
	_dprTimeout = getIntProperty (DiameterIOH.CONF_TIMER_DPR, conf, ioh.getDprTimeout ());
	_sendDpr = getBooleanProperty (DiameterIOH.CONF_DPR, conf, ioh.getSendDpr ());
	_dprReasonCode = getIntProperty (DiameterIOH.CONF_DPR_REASON, conf, ioh.getDprReasonCode ());
	_dprReasonCodeInternal = getIntProperty (DiameterIOH.CONF_DPR_REASON_INTERNAL, conf, -1);
	_routeLocal = getBooleanProperty (DiameterIOHRouterFactory.CONF_ROUTE_LOCAL, conf, false);
	_maxCERSize = getIntProperty (DiameterIOH.CONF_CER_MAX_SIZE, conf, ioh.getCERMaxSize ());
	_maxAppSize = getIntProperty (DiameterIOH.CONF_APP_MAX_SIZE, conf, ioh.getAppMaxSize ());
	_hostIPAddrCEAPolicy = getStringProperty (DiameterIOH.CONF_CEA_HOST_IP_ADDRESS_POLICY, conf, ioh.getCEAHostIPAddrPolicy ());
	_hostIPAddrCEAContent = getStringProperty (DiameterIOH.CONF_CEA_HOST_IP_ADDRESS_CONTENT, conf, ioh.getCEAHostIPAddrContent ());
	_hostIPAddrCERPolicy = getStringProperty (DiameterIOH.CONF_CER_HOST_IP_ADDRESS_POLICY, conf, ioh.getCERHostIPAddrPolicy ());
	_hostIPAddrCERContent = getStringProperty (DiameterIOH.CONF_CER_HOST_IP_ADDRESS_CONTENT, conf, ioh.getCERHostIPAddrContent ());
	_dropOnTcpOverload = getBooleanProperty ("ioh.tcp.overload.drop", conf, _dropOnTcpOverload);
	_dropOnSctpOverload = getBooleanProperty ("ioh.sctp.overload.drop", conf, _dropOnSctpOverload);
	_sctpUnordered = !getBooleanProperty (DiameterIOH.CONF_DIAMETER_SCTP_ORDERED, conf, false);
	_sctpMStreams = getBooleanProperty (DiameterIOH.CONF_DIAMETER_SCTP_MSTREAMS, conf, false);
	_sctpConnectStreamOut = getIntProperty (DiameterIOH.CONF_DIAMETER_SCTP_CONNECT_STREAM_OUT, conf, 0);
	_latencyProcEngine = getBooleanProperty (DiameterIOH.CONF_DIAMETER_LATENCY_PROC, conf, false);
	_latencyProcAgent = getBooleanProperty (DiameterIOH.CONF_DIAMETER_LATENCY_PROC_AGENT, conf, false);
	_latencyProcAny = _latencyProcEngine || _latencyProcAgent;
	_latencyProcSampling = getIntProperty (DiameterIOH.CONF_DIAMETER_LATENCY_PROC_SAMPLING, conf, 10);
	_latencyProcSamplingAll = _latencyProcSampling == 1;
	if (_latencyProcEngine) _latencyQueue = createQueueExecutor ();
	
	// we propagate the dwr timeout
	if (server.getProperties ().get (PROP_TCP_CONNECT_READ_TIMEOUT) == null)
	    server.getProperties ().put (PROP_TCP_CONNECT_READ_TIMEOUT, String.valueOf (_dwrTimeout));
	if (server.getProperties ().get (PROP_SCTP_CONNECT_READ_TIMEOUT) == null)
	    server.getProperties ().put (PROP_SCTP_CONNECT_READ_TIMEOUT, String.valueOf (_dwrTimeout));

	if (server.getProperties ().get (PROP_HISTORY_CHANNELS) == null)
	    server.getProperties ().put (PROP_HISTORY_CHANNELS, "true");
	
	super.init (server);
	_diamMeters = new DiameterIOHMeters (null, null);
	_diamMeters.initDiameterIOHEngine (getIOHMeters ());
	if (_latencyProcEngine) _diamMeters.initLatencyMeters (getIOHMeters ());
	_parserErrorMeter = getIOHMeters ().createIncrementalMeter ("parser.error", null);
	
	_channelsDiameterTcpOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.tcp.diameter", null);
	_channelsDiameterTcpAcceptedOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.tcp.accept.diameter", _channelsDiameterTcpOpenMeter);
	_channelsDiameterTcpConnectedOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.tcp.connect.diameter", _channelsDiameterTcpOpenMeter);
	_channelsDiameterSctpOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.sctp.diameter", null);
	_channelsDiameterSctpAcceptedOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.sctp.accept.diameter", _channelsDiameterSctpOpenMeter);
	_channelsDiameterSctpConnectedOpenMeter = getIOHMeters ().createIncrementalMeter ("channel.open.sctp.connect.diameter", _channelsDiameterSctpOpenMeter);

	_channelsDiameterTcpClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.tcp.diameter", null);
	_channelsDiameterTcpAcceptedClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.tcp.accept.diameter", _channelsDiameterTcpClosedMeter);
	_channelsDiameterTcpConnectedClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.tcp.connect.diameter", _channelsDiameterTcpClosedMeter);
	_channelsDiameterSctpClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.sctp.diameter", null);
	_channelsDiameterSctpAcceptedClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.sctp.accept.diameter", _channelsDiameterSctpClosedMeter);
	_channelsDiameterSctpConnectedClosedMeter = getIOHMeters ().createIncrementalMeter ("channel.closed.sctp.connect.diameter", _channelsDiameterSctpClosedMeter);
	
	Object o = server.getProperties ().get (DiameterIOH.PROP_METER_APP);
	if (o instanceof String){
	    Object[] x = DiameterIOHMeters.parseAppCounter (o.toString ());
	    if (x != null) _appCounters.add (x);
	}
	if (o instanceof List){
	    List<String> list = (List<String>) o;
	    for (String s : list){
		Object[] x = DiameterIOHMeters.parseAppCounter (s);
		if (x != null) _appCounters.add (x);
	    }
	}
	o = server.getProperties ().get (DiameterIOH.PROP_METER_APP_RESP);
	if (o instanceof String)
	    DiameterIOHMeters.parseAppRespCounter (o.toString (), _appRespCounters);
	if (o instanceof List){
	    List<String> list = (List<String>) o;
	    for (String s : list)
		DiameterIOHMeters.parseAppRespCounter (s, _appRespCounters);
	}
	fillAppMeters (_diamMeters, getIOHMeters ());
	_router.init (this);
	
	return this;
    }
    public DiameterIOHMeters fillAppMeters (DiameterIOHMeters meters, SimpleMonitorable dest){
	MeteringService metering = getIOHMeters ().getMetering ();
	for (Object[] x : _appCounters){
	    meters.addAppCounter (metering, dest, (Integer) x[0], (String) x[1], (String) x[2], _appRespCounters);
	}
	return meters;
    }

    @Override
    public boolean start (BundleContext osgi){
	if (super.start (osgi)){
	    new DiameterIOHGogoCommands (this).register (osgi);
	    Dictionary props = new Hashtable ();
	    osgi.registerService (Shutdownable.class.getName (), this, props);
	    return true;
	} else
	    return false;
    }

    public void shutdown (final Shutdown shutdown){
	Runnable r1 = new Runnable (){
		public void run (){
		    _logger.warn (DiameterIOHEngine.this+" : shutdown");
		    _shutdown = shutdown;
		    stop ();
		    Runnable r2 = new Runnable (){
			    public void run (){
				long tcp = getIOHMeters ().getOpenTcpChannelsMeter ().getValue ();
				long sctp = getIOHMeters ().getOpenSctpChannelsMeter ().getValue ();
				if (tcp == 0 &&
				    sctp == 0){
				    _logger.warn (DiameterIOHEngine.this+" : shutdown complete");
				    _shutdown.done (DiameterIOHEngine.this);
				} else {
				    _logger.debug (DiameterIOHEngine.this+" : delaying shutdown : tcp="+tcp+"/sctp="+sctp);
				    _exec.schedule (this, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
				}
			    }
			};
		    _exec.schedule (r2, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	    };
	schedule (r1);
    }

    // used when closing a tcp endpoint
    protected void closeClientConnection (TcpChannel cnx){
	DiameterIOHTcpChannel channel = (DiameterIOHTcpChannel) cnx.attachment ();
	channel.closeClientConnection ("Connection closed by IOH");
    }
    // used when closing an sctp endpoint
    protected void closeClientConnection (SctpChannel cnx){
	DiameterIOHSctpChannel channel = (DiameterIOHSctpChannel) cnx.attachment ();
	channel.closeClientConnection ("Connection closed by IOH");
    }

    // used from gogo
    protected void closeConnection (IOHChannel channel, Object reason){
	if (channel instanceof DiameterIOHTcpChannel)
	    ((DiameterIOHTcpChannel) channel).closeClientConnection (reason);
	else if (channel instanceof DiameterIOHTcpClientChannel)
	    ((DiameterIOHTcpClientChannel) channel).closeClientConnection (reason);
	else if (channel instanceof DiameterIOHSctpChannel)
	    ((DiameterIOHSctpChannel) channel).closeClientConnection (reason);
	else if (channel instanceof DiameterIOHSctpClientChannel)
	    ((DiameterIOHSctpClientChannel) channel).closeClientConnection (reason);
    }

    @Override
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_shutdown != null){
	    _logger.warn (DiameterIOHEngine.this+" : shutdown : closing connecting agent : "+agent);
	    agent.close ();
	    return false;
	}
	return super.agentConnected (agent, state);
    }

    @Override
    public boolean agentClosed (MuxClient agent){
	int min = getIntProperty (DiameterIOH.CONF_AGENT_REMOTE_MIN, 0);
	if (min > 0){
	    if (getMuxClientList ().sizeOfRemoteAgents (false) < min){
		history ("Not enough remote agents - closing peers");
		_logger.warn (DiameterIOHEngine.this+" : Not enough remote agents - closing peers");
		for (final IOHChannel channel : _tcpChannels.values ()){
		    closeConnection (channel, "Not enough remote agents");
		}
		for (final IOHChannel channel : _sctpChannels.values ()){
		    closeConnection (channel, "Not enough remote agents");
		}
	    }
	}
	return super.agentClosed (agent);
    }
    
    @Override
    public void initMuxClient (MuxClient agent){
	super.initMuxClient (agent);
	_router.initMuxClient (agent);
    }
    @Override
    public void resetMuxClient (MuxClient agent){
	super.resetMuxClient (agent);
	_router.resetMuxClient (agent);
    }

    @Override
    public int registerTcpServer (IOHChannel channel){
	_hostIPAddresses.add (channel.getLocalAddress ().getAddress ());
	return super.registerTcpServer (channel);
    }
    @Override
    public void unregisterTcpServer (IOHChannel channel){
	_hostIPAddresses.remove (channel.getLocalAddress ().getAddress ());
	super.unregisterTcpServer (channel);
    }
    @Override
    public int registerSctpServer (IOHChannel channel){
	try{
	    for (String ip : ((IOHSctpServerChannel) channel).getLocalAddresses ())
		_hostIPAddresses.add (InetAddress.getByName (ip));
	}catch(Exception e){
	    _logger.warn ("Failed to registerSctpServer addresses", e);
	}
	return super.registerSctpServer (channel);
    }
    @Override
    public void unregisterSctpServer (IOHChannel channel){
	try{
	    for (String ip : ((IOHSctpServerChannel) channel).getLocalAddresses ())
		_hostIPAddresses.remove (InetAddress.getByName (ip));
	}catch(Exception e){
	    _logger.warn ("Failed to unregisterSctpServer addresses", e);
	}
	super.unregisterSctpServer (channel);
    }

    @Override
    public void tcpConnect(final MuxClient agent, final MuxClientState state, final InetSocketAddress remote, final Map<ReactorProvider.TcpClientOption, Object> opts){
	long connectionId = state.connectionId ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : tcpConnect : "+agent+" : connectionId="+connectionId+" : "+remote+" isShared="+isShared (connectionId)+" isNotShared="+isNotShared (connectionId));
	super.tcpConnect (agent, state, remote, opts);
    }
    @Override
    public void sctpConnect(final MuxClient agent, final MuxClientState state, final InetSocketAddress remote, final Map<ReactorProvider.SctpClientOption, Object> opts){
	long connectionId = state.connectionId ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : sctpConnect : "+agent+" : connectionId="+connectionId+" : "+remote+" isShared="+isShared (connectionId)+" isNotShared="+isNotShared (connectionId));
	super.sctpConnect (agent, state, remote, opts);
    }

    private boolean isShared (long connectionId){
	// see DiameterAgent / Peer.SEED_REMOTE_I_SHARED
	return (connectionId & 0x1000000000000000L) == 0x1000000000000000L;
    }
    private boolean isNotShared (long connectionId){
	// see DiameterAgent / Peer.SEED_REMOTE_I_NOT_SHARED
	return (connectionId & 0x0800000000000000L) == 0x0800000000000000L;
    }

    @Override
    protected boolean allowUniqueTcpConnect (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	return enoughRemoteAgents ();
    }
    @Override
    protected boolean allowUniqueSctpConnect (MuxClient agent, MuxClientState state, InetSocketAddress remote, Map<ReactorProvider.SctpClientOption, Object> opts){
	return enoughRemoteAgents ();
    }
    protected boolean enoughRemoteAgents (){
	int min = getIntProperty (DiameterIOH.CONF_AGENT_REMOTE_MIN, 0);
	if (min > 0)
	    return (_agentsList.sizeOfRemoteAgents (false) >= min);
	return true;
    }
    @Override
    protected IOHChannel getUniqueTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote){
	return getUniqueClientChannel (connectionId, remote, _uniqueTcpClientChannels);
    }
    @Override
    protected IOHChannel getUniqueSctpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote){
	return getUniqueClientChannel (connectionId, remote, _uniqueSctpClientChannels);
    }
    private IOHChannel getUniqueClientChannel (long connectionId, InetSocketAddress remote, Map<Object, IOHChannel> channels){
	if (isNotShared (connectionId)) // the application provided -1
	    return null;
	if (isShared (connectionId)) // legacy : the application did not provide anything : share by remote addr
	    return channels.get (remote);
	// the application provided the id : share by connectionId
	return channels.get (connectionId);
    }
    @Override
    protected void registerUniqueTcpClientChannel (IOHTcpClientChannel channel){
	registerUniqueClientChannel (channel, channel.getRemoteAddress (), _uniqueTcpClientChannels);
    }
    @Override
    protected void registerUniqueSctpClientChannel (IOHSctpClientChannel channel){
	registerUniqueClientChannel (channel, channel.getRemoteAddress (), _uniqueSctpClientChannels);
    }
    private void registerUniqueClientChannel (IOHChannel channel, InetSocketAddress remote, Map<Object, IOHChannel> channels){
	long connectionId = channel.getConnectionId ();
	if (isNotShared (connectionId))
	    return;
	if (isShared (connectionId)){
	    channels.put (remote, channel);
	    return;
	}
	channels.put (connectionId, channel);
    }
    @Override
    protected void unregisterUniqueTcpClientChannel (IOHTcpClientChannel channel){
	unregisterUniqueClientChannel (channel, channel.getRemoteAddress (), _uniqueTcpClientChannels);
    }
    @Override
    protected void unregisterUniqueSctpClientChannel (IOHSctpClientChannel channel){
	unregisterUniqueClientChannel (channel, channel.getRemoteAddress (), _uniqueSctpClientChannels);
    }
    protected void unregisterUniqueClientChannel (IOHChannel channel, InetSocketAddress remote, Map<Object, IOHChannel> channels){
	long connectionId = channel.getConnectionId ();
	if (isNotShared (connectionId))
	    return;
	if (isShared (connectionId)){
	    channels.remove (remote);
	    return;
	}
	channels.remove (connectionId);
    }
    
    @Override
    protected TcpChannelListener newMuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	return new RemoteDiameterMuxClient (engine, channel, props, isRemoteIOH);
    }

    @Override
    protected IOHLocalMuxFactory.IOHLocalMuxConnection newLocalMuxClient (MuxHandler muxHandler, ConnectionListener listener, Map opts){
	return new LocalDiameterMuxClient (this, muxHandler, listener, opts);
    }
    
    @Override
    protected IOHTcpServerChannel newTcpServerChannel (IOHEngine engine, TcpServer server){
	return new DiameterIOHTcpServerChannel (engine, server);
    }
    
    @Override
    protected IOHTcpChannel newTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	return new DiameterIOHTcpChannel (engine, server, channel, props);
    }
    
    @Override
    protected IOHTcpClientChannel newTcpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote, Map<ReactorProvider.TcpClientOption, Object> opts){
	return new DiameterIOHTcpClientChannel (this, agent, connectionId, remote, opts);
    }

    @Override
    protected IOHSctpChannel newSctpChannel (IOHEngine engine, SctpServer server, SctpChannel channel, Map<String, Object> props){
	return new DiameterIOHSctpChannel (engine, server, channel, props);
    }
    
    @Override
    protected IOHSctpClientChannel newSctpClientChannel (MuxClient agent, long connectionId, InetSocketAddress remote, Map<ReactorProvider.SctpClientOption, Object> opts){
	return new DiameterIOHSctpClientChannel (this, agent, connectionId, remote, opts);
    }

    @Override
    public boolean sendMuxData(MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... buf) {
	// in agent thread
	if (header.getFlags () == 1){
	    ((DiameterMuxClient) agent).latencySupported (true);
	}
	return true;
    }

    public static interface DiameterMuxClient {
	public DiameterIOHMeters getDiameterIOHMeters ();
	public void latencySupported (boolean b);
	public boolean latencySupported ();
    }

    public static class RemoteDiameterMuxClient extends MuxClient implements DiameterMuxClient {
	DiameterIOHMeters _diamMeters;
	boolean _latencySupported;
	private RemoteDiameterMuxClient (IOHEngine engine, TcpChannel channel, Map<String, Object> props, boolean isRemoteIOH){
	    super (engine, channel, props, isRemoteIOH);
	}
	public DiameterIOHMeters getDiameterIOHMeters (){
	    return _diamMeters;
	}
	public void latencySupported (boolean b){ _latencySupported = b;}
	public boolean latencySupported (){ return _latencySupported;}
	protected void createDefaultMeters (){
	    _diamMeters = new DiameterIOHMeters (null, null);
	    _diamMeters.initDiameterMuxClient (getIOHMeters ());
	    DiameterIOHEngine engine = (DiameterIOHEngine) _engine;
	    if (engine._latencyProcAgent) _diamMeters.initLatencyMeters (getIOHMeters ());
	    engine.fillAppMeters (_diamMeters, getIOHMeters ());
	    super.createDefaultMeters ();
	}
	@Override
	protected void agentConnected (){
	    if (!_latencySupported) _logger.warn (_engine+" : "+this+" : latency calculation not available in agent");
	    super.agentConnected ();
	}
    }
    public static class LocalDiameterMuxClient extends IOHLocalMuxFactory.IOHLocalMuxConnection implements DiameterMuxClient {
	DiameterIOHMeters _diamMeters;
	private LocalDiameterMuxClient (IOHEngine engine, MuxHandler handler, ConnectionListener listener, Map opts){
	    super (engine, handler, listener, opts);
	}
	public DiameterIOHMeters getDiameterIOHMeters (){
	    return _diamMeters;
	}
	// no actual need to check latency support, since co-located, hence same version
	public void latencySupported (boolean b){}
	public boolean latencySupported (){return true;}
	protected void createDefaultMeters (){
	    _diamMeters = new DiameterIOHMeters (null, null);
	    _diamMeters.initDiameterMuxClient (getIOHMeters ());
	    DiameterIOHEngine engine = (DiameterIOHEngine) _engine;
	    if (engine._latencyProcAgent) _diamMeters.initLatencyMeters (getIOHMeters ());
	    engine.fillAppMeters (_diamMeters, getIOHMeters ());
	    super.createDefaultMeters ();
	}
    }
    
    private static class DiameterIOHTcpServerChannel extends IOHTcpServerChannel {
	protected DiameterIOHTcpServerChannel (IOHEngine engine, TcpServer server){
	    super (engine, server);
	}
    }

    public static class DiameterIOHTcpChannel extends IOHTcpChannel {
	private DiameterIOHEngine _diamEngine;
	private TcpClientContext _context;
	protected DiameterParser _parserClient = new DiameterParser ();
	protected DiameterIOHMeters _diamMeters;
	protected boolean _dropOnTcpOverload;
	protected long _soTimeout;
	private DiameterIOHTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	    super (engine, channel, props);
	    _diamEngine = (DiameterIOHEngine) engine;
	    _isText = false;
	    _diamMeters = _diamEngine._diamMeters;
	    props.put (ENDPOINT_ADDRESS, new String[]{server.getAddress ().getAddress ().getHostAddress ()});
	    _context = new TcpClientContext (this, channel, props, true);
	    _dropOnTcpOverload = _diamEngine._dropOnTcpOverload;
	    _soTimeout = getLongProperty (TcpServer.PROP_READ_TIMEOUT, props, 0L);
	    Security sec = channel.getSecurity ();
	    _context.upgradeToSecure (sec != null && sec.isDelayed (),
				      getBooleanProperty (DiameterIOH.CONF_DIAMETER_SECURE_DELAYED_REQUIRED_R, props, false));
	}
	@Override
	public boolean agentConnected (MuxClient agent, MuxClientState state){
	    return super.agentConnected (agent, state) && _context.agentConnected (agent, state);
	}
	@Override
	public boolean agentClosed (MuxClient agent){
	    if (_closed) return false;
	    if (_agentsList.remove (agent)){
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
		if (_engine.historyChannels ())
		    history ("agentClosed : "+agent);
		_context.agentClosed (agent);
		// we deactivate auto-close when agentsNb = 0 , so we can do graceful shutdown
		return true;
	    } else
		return false;
	}
	@Override
	public boolean agentStopped (MuxClient agent){
	    return super.agentStopped (agent) && _context.agentStopped (agent);
	}
	@Override
	public boolean agentUnStopped (MuxClient agent){
	    return super.agentUnStopped (agent) && _context.agentUnStopped (agent);
	}
	@Override
	public int messageReceived(TcpChannel cnx,
				   ByteBuffer buff){
	    if (disabled (buff))
		return 0;
	    _readMeter.inc (buff.remaining ());
	    while (true){
		DiameterMessage msg = null;
		try{
		    msg = _parserClient.parseMessage (buff);
		} catch (Throwable t){
		    ((DiameterIOHEngine)_engine)._parserErrorMeter.inc (1);
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception", t);
		    buff.position (buff.limit ());
		    close ();
		    return 0;
		}
		if (msg == null)
		    return 0;
		_diamEngine.timestamp (msg);
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
		if (_context.handleClientMessage (msg) == false){
		    buff.position (buff.limit ()); // empty by precaution
		    close ();
		    return 0;
		}
	    }
	}
	@Override
	public void connectionClosed (){
	    super.connectionClosed ();
	    _context.handleClientClosed ();
	}
	@Override
	public void receiveTimeout(){
	    _context.handleClientTimeout ();
	}

	protected void closeClientConnection (Object reason){
	    _context.closeClientConnection (reason);
	}
	
	@Override
	// called in agent thread --> mthreaded
	public boolean sendOut (final MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    if (agent == null){
		if (_dropOnTcpOverload){
		    // patch for CSD : in case of overload, do not shutdown the connection, drop the message
		    if (checkBuffer){
			if (checkSendBufferOut (null) == false){
			    _sendDroppedMeter.inc (1);
			    // just drop the message
			    return false;
			}
		    }
		    return super.sendOut (null, null, false, copy, buffs);
		} else { // legacy behavior
		    // direct send - locally initiated
		    return super.sendOut (null, null, checkBuffer, copy, buffs);
		}
	    }
	    // sent from an agent - needs to be intercepted - we know that there is a diameter msg per mux msg
	    DiameterParser parser = new DiameterParser ();
	    for (ByteBuffer buff : buffs){
		while (true){
		    if (buff.remaining () == 0) return true;
		    long[] ts = _diamEngine.parseTimestamps (buff);
		    final DiameterMessage msg = parser.parseMessage (buff);
		    if (msg == null) return true;
		    msg.timestamp1 (ts[0]).timestamp2 (ts[1]);
		    _diamEngine.calcAgentLatency (agent, msg);
		    Runnable r = new Runnable (){ public void run (){
			if (agent.getLogger ().isDebugEnabled ())
			    agent.getLogger ().debug (DiameterIOHTcpChannel.this+" : "+agent+" : RESPOND : "+msg);
			_context.handleAgentMessage (agent, msg);
		    }};
		    schedule (r);
		}
	    }
	    return true;
	}

	@Override
	protected void applyParams (Map<String, String> params){
	    if (_closed) return;
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" applyParams "+params);
	    long soTimeout = IOHEngine.getLongProperty ("dwr.delay",
							params,
							-1L
							);
	    if (soTimeout == 0L) soTimeout = _soTimeout; // 0 means go back to default
	    if (soTimeout != -1L) _channel.setSoTimeout (soTimeout);
	    long dprTimeout = IOHEngine.getLongProperty ("dpa.delay",
							 params,
							 -1L
							 );
	    if (dprTimeout != -1L) _context.setDprTimeout (dprTimeout);
	};
    }
    private static class DiameterIOHTcpClientChannel extends IOHTcpClientChannel {
	private DiameterIOHEngine _diamEngine;
	private TcpClientContext _context;
	protected DiameterParser _parserClient = new DiameterParser ();
	protected DiameterIOHMeters _diamMeters;
	protected long _dwrDelay = -1L, _dpaDelay = -1L;
	protected boolean _updateSoTimeout; // update the soTimeout to dwr.delay after the CEA (1st message)
	protected boolean _dropOnTcpOverload;
	protected DiameterIOHTcpClientChannel (DiameterIOHEngine engine, MuxClient agent, long connectionId, InetSocketAddress dest, Map<ReactorProvider.TcpClientOption, Object> opts){
	    super (agent, connectionId, dest, opts);
	    _diamEngine = engine;
	    _isText = false;
	    _diamMeters = engine._diamMeters;
	    Map parameters = (Map) opts.get (ReactorProvider.TcpClientOption.ATTACHMENT);
	    // PROP_CONNECTION_TIMEOUT and PROP_CEA_DELAY are taken into account here
	    _dwrDelay = getLongProperty ("dwr.delay",
					 parameters,
					 _soTimeout);
	    _dpaDelay = getLongProperty ("dpa.delay",
					 parameters,
					 -1L);
	    _soTimeout = getLongProperty ("cea.delay",
					  parameters,
					  _soTimeout);
	    long timeout = getLongProperty ("connection.timeout",
					    parameters,
					    -1L);
	    if (timeout != -1L) opts.put(ReactorProvider.TcpClientOption.TIMEOUT, timeout);
	    _updateSoTimeout = _dwrDelay != _soTimeout;
	    _dropOnTcpOverload = engine._dropOnTcpOverload;
	}
	@Override
	public void connectionEstablished(TcpChannel cnx){
	    _context = new TcpClientContext (this, cnx, _engine.getProperties (), false);
	    if (_dpaDelay != -1L) _context.setDprTimeout (_dpaDelay);
	    super.connectionEstablished (cnx);
	    Security sec = cnx.getSecurity ();
	    _context.upgradeToSecure (sec != null && sec.isDelayed (),
				      getBooleanProperty (DiameterIOH.CONF_DIAMETER_SECURE_DELAYED_REQUIRED_I, _diamEngine.getProperties (), false));
	    if (_diamEngine.enoughRemoteAgents () == false){ // remote agent is gone after allowUniqueConnect was initially called
		_context.closeClientConnection ("Not enough remote agents");
		return;
	    }
	}
	@Override
	protected void applyParams (Map<String, String> params){
	    if (_closed) return;
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" applyParams "+params);
	    long dwrDelay = getLongProperty ("dwr.delay",
					     params,
					     -1L);
	    if (dwrDelay == 0L) dwrDelay = _soTimeout; // 0 means go back to default
	    if (dwrDelay != -1L) _channel.setSoTimeout (dwrDelay);
	    long dpaDelay = IOHEngine.getLongProperty ("dpa.delay",
						       params,
						       -1L
						       );
	    if (dpaDelay != -1L) _context.setDprTimeout (dpaDelay);
	};
	@Override
	public boolean agentJoined (MuxClient agent, MuxClientState state){
	    return super.agentJoined (agent, state) && _context.agentConnected (agent, state);
	}
	@Override
	public boolean agentClosed (MuxClient agent){
	    if (_closed) return false;
	    if (_agentsList.remove (agent)){
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
		if (_engine.historyChannels ())
		    history ("agentClosed : "+agent);
		_context.agentClosed (agent);
		// we deactivate auto-close when agentsNb = 0 , so we can do graceful shutdown
		return true;
	    } else
		return false;
	}
	@Override
	public boolean agentStopped (MuxClient agent){
	    return super.agentStopped (agent) && _context.agentStopped (agent);
	}
	@Override
	public boolean agentUnStopped (MuxClient agent){
	    return super.agentUnStopped (agent) && _context.agentUnStopped (agent);
	}
	@Override
	public int messageReceived(TcpChannel cnx,
				   ByteBuffer buff){
	    if (disabled (buff))
		return 0;
	    _readMeter.inc (buff.remaining ());
	    while (true){
		DiameterMessage msg = null;
		try{
		    msg = _parserClient.parseMessage (buff);
		} catch (Throwable t){
		    ((DiameterIOHEngine)_engine)._parserErrorMeter.inc (1);
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception", t);
		    buff.position (buff.limit ());
		    close ();
		    return 0;
		}
		if (msg == null)
		    return 0;
		_diamEngine.timestamp (msg);
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
		if (_updateSoTimeout){
		    cnx.setSoTimeout (_dwrDelay);
		    _updateSoTimeout = false;
		}
		if (_context.handleClientMessage (msg) == false){
		    buff.position (buff.limit ()); // empty by precaution
		    close ();
		    return 0;
		}
	    }
	}
	@Override
	public void connectionClosed (){
	    super.connectionClosed ();
	    _context.handleClientClosed ();
	}
	@Override
	public void receiveTimeout(){
	    _context.handleClientTimeout ();
	}

	protected void closeClientConnection (Object reason){
	    _context.closeClientConnection (reason);
	}
	
	@Override
	// called in agent thread --> mthreaded
	public boolean sendOut (final MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    if (agent == null){
		if (_dropOnTcpOverload){
		    // patch for CSD : in case of overload, do not shutdown the connection, drop the message
		    if (checkBuffer){
			if (checkSendBufferOut (null) == false){
			    _sendDroppedMeter.inc (1);
			    // just drop the message
			    return false;
			}
		    }
		    return super.sendOut (null, null, false, copy, buffs);
		} else { // legacy behavior
		    // direct send - locally initiated
		    return super.sendOut (null, null, checkBuffer, copy, buffs);
		}
	    }
	    // sent from an agent - needs to be intercepted - we know that there is a diameter msg per mux msg
	    DiameterParser parser = new DiameterParser ();
	    for (ByteBuffer buff : buffs){
		while (true){
		    if (buff.remaining () == 0) return true;
		    long[] ts = _diamEngine.parseTimestamps (buff);
		    final DiameterMessage msg = parser.parseMessage (buff);
		    if (msg == null) return true;
		    msg.timestamp1 (ts[0]).timestamp2 (ts[1]);
		    _diamEngine.calcAgentLatency (agent, msg);
		    Runnable r = new Runnable (){ public void run (){
			if (agent.getLogger ().isDebugEnabled ())
			    agent.getLogger ().debug (DiameterIOHTcpClientChannel.this+" : "+agent+" : RESPOND : "+msg);
			_context.handleAgentMessage (agent, msg);
		    }};
		    schedule (r);
		}
	    }
	    return true;
	}
    }


    public static class DiameterIOHSctpChannel extends IOHSctpChannel {
	private DiameterIOHEngine _diamEngine;
	private SctpClientContext _context;
	protected DiameterParser _parserClient = new DiameterParser ();
	protected DiameterIOHMeters _diamMeters;
	protected boolean _dropOnSctpOverload;
	protected boolean _unordered;
	protected AtomicInteger _streamIndex;
	protected int _streams;
	protected long _soTimeout;
	private DiameterIOHSctpChannel (IOHEngine engine, SctpServer server, SctpChannel channel, Map<String, Object> props){
	    super (engine, channel, props);
	    _diamEngine = (DiameterIOHEngine) engine;
	    _isText = false;
	    _diamMeters = _diamEngine._diamMeters;
	    IOHSctpServerChannel iohServer = server.attachment ();
	    props.put (ENDPOINT_ADDRESS, iohServer.getLocalAddresses ());
	    _context = new SctpClientContext (this, channel, props, true); // the socket can be rejected if getRemoteAddress fails
	    _dropOnSctpOverload = _diamEngine._dropOnSctpOverload;
	    _unordered = _diamEngine._sctpUnordered;
	    if (_diamEngine._sctpMStreams){
		try{
		    _streams = channel.getAssociation ().maxOutboundStreams ();
		}catch(Exception e){
		    _streams = 1;
		}
		if (_logger.isDebugEnabled ()) _logger.debug (channel+" : using multiple streams to send messages : nb="+_streams);
		if (_streams > 1)
		    _streamIndex = new AtomicInteger (0);		
	    }
	    _soTimeout = getLongProperty (SctpServer.PROP_READ_TIMEOUT, props, 0L);
	}
	@Override
	public boolean agentConnected (MuxClient agent, MuxClientState state){
	    return super.agentConnected (agent, state) && _context.agentConnected (agent, state);
	}
	@Override
	public boolean agentClosed (MuxClient agent){
	    if (_closed) return false;
	    if (_agentsList.remove (agent)){
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
		if (_engine.historyChannels ())
		    history ("agentClosed : "+agent);
		_context.agentClosed (agent);
		// we deactivate auto-close when agentsNb = 0 , so we can do graceful shutdown
		return true;
	    } else
		return false;
	}
	@Override
	public boolean agentStopped (MuxClient agent){
	    return super.agentStopped (agent) && _context.agentStopped (agent);
	}
	@Override
	public boolean agentUnStopped (MuxClient agent){
	    return super.agentUnStopped (agent) && _context.agentUnStopped (agent);
	}
	@Override
	public void messageReceived(SctpChannel cnx,
				java.nio.ByteBuffer buff,
				java.net.SocketAddress addr,
				int bytes,
				boolean isComplete,
				boolean isUnordered,
				int ploadPID,
				int streamNumber){
	    if (disabled (buff))
		return;
	    _readMeter.inc (buff.remaining ());
	    while (true){
		DiameterMessage msg = null;
		try{
		    msg = _parserClient.parseMessage (buff);
		} catch (Throwable t){
		    ((DiameterIOHEngine)_engine)._parserErrorMeter.inc (1);
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception", t);
		    buff.position (buff.limit ());
		    close ();
		    return;
		}
		if (msg == null)
		    return;
		_diamEngine.timestamp (msg);
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
		if (_context.handleClientMessage (msg) == false){
		    buff.position (buff.limit ()); // empty by precaution
		    close ();
		    return;
		}
	    }
	}
	@Override
	public void connectionClosed (){
	    super.connectionClosed ();
	    _context.handleClientClosed ();
	}
	@Override
	public void receiveTimeout(){
	    _context.handleClientTimeout ();
	}

	protected void closeClientConnection (Object reason){
	    _context.closeClientConnection (reason);
	}

	@Override
	// the diameter ioh uses this call to factorize with tcp
	public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	    if (checkBuffer){
		if (checkSendBufferAgent (agent, null) == false){
		    return false;
		}
	    }
	    logSendAgent (agent, null, buffs);
	    if (copy)
		agent.getMuxHandler ().sctpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs), null, false, true, 0, 0);
	    else
		agent.getExtendedMuxHandler ().sctpSocketData (agent, _id, sessionId, buffs, null, false, true, 0, 0);
	    return true;
	}

	@Override
	public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    int streamId = _streamIndex == null ? 0 : ((_streamIndex.getAndIncrement () & 0xFF_FF) % _streams);
	    return sendSctpOut (null, null, _unordered, true, 0, streamId, 0L, checkBuffer, copy, buffs);
	}
	
	@Override
	// called in agent thread --> mthreaded
	public boolean sendSctpOut (final MuxClient agent, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    if (agent == null){
		if (_dropOnSctpOverload){
		    // patch for CSD : in case of overload, do not shutdown the connection, drop the message
		    if (checkBuffer){
			if (checkSendBufferOut (null) == false){
			    _sendDroppedMeter.inc (1);
			    // just drop the message
			    return false;
			}
		    }
		    return super.sendSctpOut (null, addr, unordered, complete, ploadPID, streamNumber, timeToLive, false, copy, buffs);
		} else { // legacy behavior
		    // direct send - locally initiated
		    return super.sendSctpOut (null, addr, unordered, complete, ploadPID, streamNumber, timeToLive, checkBuffer, copy, buffs);
		}
	    }
	    // sent from an agent - needs to be intercepted - we know that there is a diameter msg per mux msg
	    DiameterParser parser = new DiameterParser ();
	    for (ByteBuffer buff : buffs){
		while (true){
		    if (buff.remaining () == 0) return true;
		    long[] ts = _diamEngine.parseTimestamps (buff);
		    final DiameterMessage msg = parser.parseMessage (buff);
		    if (msg == null) return true;
		    msg.timestamp1 (ts[0]).timestamp2 (ts[1]);
		    _diamEngine.calcAgentLatency (agent, msg);
		    Runnable r = new Runnable (){ public void run (){
			if (agent.getLogger ().isDebugEnabled ())
			    agent.getLogger ().debug (DiameterIOHSctpChannel.this+" : "+agent+" : RESPOND : "+msg);
			_context.handleAgentMessage (agent, msg);
		    }};
		    schedule (r);
		}
	    }
	    return true;
	}

	@Override
	protected void applyParams (Map<String, String> params){
	    if (_closed) return;
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" applyParams "+params);
	    long soTimeout = IOHEngine.getLongProperty ("dwr.delay",
							params,
							-1L
							);
	    if (soTimeout == 0L) soTimeout = _soTimeout; // 0 means go back to default
	    if (soTimeout != -1L) _channel.setSoTimeout (soTimeout);
	    long dprTimeout = IOHEngine.getLongProperty ("dpa.delay",
							 params,
							 -1L
							 );
	    if (dprTimeout != -1L) _context.setDprTimeout (dprTimeout);
	};
    }
    private static class DiameterIOHSctpClientChannel extends IOHSctpClientChannel {
	private DiameterIOHEngine _diamEngine;
	private SctpClientContext _context;
	protected DiameterParser _parserClient = new DiameterParser ();
	protected DiameterIOHMeters _diamMeters;
	protected long _dwrDelay = -1L, _dpaDelay = -1L;
	protected boolean _updateSoTimeout; // update the soTimeout to dwr.delay after the CEA (1st message)
	protected boolean _dropOnSctpOverload;
	protected boolean _unordered;
	protected AtomicInteger _streamIndex;
	protected int _streams;
	protected DiameterIOHSctpClientChannel (DiameterIOHEngine engine, MuxClient agent, long connectionId, InetSocketAddress dest, Map<ReactorProvider.SctpClientOption, Object> opts){
	    super (agent, connectionId, dest, opts);
	    _diamEngine = engine;
	    _isText = false;
	    _diamMeters = engine._diamMeters;
	    Map parameters = (Map) opts.get (ReactorProvider.SctpClientOption.ATTACHMENT);
	    // PROP_CONNECTION_TIMEOUT and PROP_CEA_DELAY are taken into account here
	    _dwrDelay = getLongProperty ("dwr.delay",
					 parameters,
					 _soTimeout);
	    _dpaDelay = getLongProperty ("dpa.delay",
					 parameters,
					 -1L);
	    _soTimeout = getLongProperty ("cea.delay",
					  parameters,
					  _soTimeout);
	    long timeout = getLongProperty ("connection.timeout",
					    parameters,
					    -1L);
	    if (timeout != -1L) opts.put(ReactorProvider.SctpClientOption.TIMEOUT, timeout);
	    _updateSoTimeout = _dwrDelay != _soTimeout;
	    _dropOnSctpOverload = engine._dropOnSctpOverload;
	    _unordered = engine._sctpUnordered;
	    if (engine._sctpMStreams){
		_streamIndex = new AtomicInteger (0);
		if (engine._sctpConnectStreamOut > 0)
		    opts.put (alcatel.tess.hometop.gateways.reactor.ReactorProvider.SctpClientOption.MAX_OUT_STREAMS, engine._sctpConnectStreamOut);
	    }
	}
	@Override
	public void connectionEstablished(SctpChannel cnx){
	    if (_streamIndex != null){
		try{
		    _streams = cnx.getAssociation ().maxOutboundStreams ();
		    if (_streams == 1) _streamIndex = null;
		    if (_logger.isDebugEnabled ()) _logger.debug (cnx+" : using multiple streams to send messages : nb="+_streams);
		}catch(Exception e){
		    _streamIndex = null;
		}		    
	    }
	    _context = new SctpClientContext (this, cnx, _engine.getProperties (), false);
	    if (_dpaDelay != -1L) _context.setDprTimeout (_dpaDelay);
	    super.connectionEstablished (cnx);
	    if (_diamEngine.enoughRemoteAgents () == false){ // remote agent is gone after allowUniqueConnect was initially called
		_context.closeClientConnection ("Not enough remote agents");
		return;
	    }
	}
	@Override
	protected void applyParams (Map<String, String> params){
	    if (_closed) return;
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" applyParams "+params);
	    long dwrDelay = getLongProperty ("dwr.delay",
					     params,
					     -1L);
	    if (dwrDelay == 0L) dwrDelay = _soTimeout; // 0 means go back to default
	    if (dwrDelay != -1L) _channel.setSoTimeout (dwrDelay);
	    long dpaDelay = IOHEngine.getLongProperty ("dpa.delay",
						       params,
						       -1L
						       );
	    if (dpaDelay != -1L) _context.setDprTimeout (dpaDelay);
	};
	@Override
	public boolean agentJoined (MuxClient agent, MuxClientState state){
	    return super.agentJoined (agent, state) && _context.agentConnected (agent, state);
	}
	@Override
	public boolean agentClosed (MuxClient agent){
	    if (_closed) return false;
	    if (_agentsList.remove (agent)){
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
		if (_engine.historyChannels ())
		    history ("agentClosed : "+agent);
		_context.agentClosed (agent);
		// we deactivate auto-close when agentsNb = 0 , so we can do graceful shutdown
		return true;
	    } else
		return false;
	}
	@Override
	public boolean agentStopped (MuxClient agent){
	    return super.agentStopped (agent) && _context.agentStopped (agent);
	}
	@Override
	public boolean agentUnStopped (MuxClient agent){
	    return super.agentUnStopped (agent) && _context.agentUnStopped (agent);
	}
	@Override
	public void messageReceived(SctpChannel cnx,
				java.nio.ByteBuffer buff,
				java.net.SocketAddress addr,
				int bytes,
				boolean isComplete,
				boolean isUnordered,
				int ploadPID,
				int streamNumber){
	    if (disabled (buff))
		return;
	    _readMeter.inc (buff.remaining ());
	    while (true){
		DiameterMessage msg = null;
		try{
		    msg = _parserClient.parseMessage (buff);
		} catch (Throwable t){
		    ((DiameterIOHEngine)_engine)._parserErrorMeter.inc (1);
		    if (_logger.isDebugEnabled ()) _logger.debug (this+" : parsing exception", t);
		    buff.position (buff.limit ());
		    close ();
		    return;
		}
		if (msg == null)
		    return;
		_diamEngine.timestamp (msg);
		if (_updateSoTimeout){
		    cnx.setSoTimeout (_dwrDelay);
		    _updateSoTimeout = false;
		}
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : RECEIVED :\n["+msg+"]");
		if (_context.handleClientMessage (msg) == false){
		    buff.position (buff.limit ()); // empty by precaution
		    close ();
		    return;
		}
	    }
	}
	@Override
	public void connectionClosed (){
	    super.connectionClosed ();
	    _context.handleClientClosed ();
	}
	@Override
	public void receiveTimeout(){
	    _context.handleClientTimeout ();
	}

	protected void closeClientConnection (Object reason){
	    _context.closeClientConnection (reason);
	}

	@Override
	// the diameter ioh uses this call
	public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	    if (checkBuffer){
		if (checkSendBufferAgent (agent, null) == false){
		    return false;
		}
	    }
	    logSendAgent (agent, null, buffs);
	    if (copy)
		agent.getMuxHandler ().sctpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs), null, false, true, 0, 0);
	    else
		agent.getExtendedMuxHandler ().sctpSocketData (agent, _id, sessionId, buffs, null, false, true, 0, 0);
	    return true;
	}

	@Override
	public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    int streamId = _streamIndex == null ? 0 : ((_streamIndex.getAndIncrement () & 0xFF_FF) % _streams);
	    return sendSctpOut (null, null, _unordered, true, 0, streamId, 0L, checkBuffer, copy, buffs);
	}
	
	@Override
	// called in agent thread --> mthreaded
	public boolean sendSctpOut (final MuxClient agent, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	    if (agent == null){
		if (_dropOnSctpOverload){
		    // patch for CSD : in case of overload, do not shutdown the connection, drop the message
		    if (checkBuffer){
			if (checkSendBufferOut (null) == false){
			    _sendDroppedMeter.inc (1);
			    // just drop the message
			    return false;
			}
		    }
		    return super.sendSctpOut (null, addr, unordered, complete, ploadPID, streamNumber, timeToLive, false, copy, buffs);
		} else { // legacy behavior
		    // direct send - locally initiated
		    return super.sendSctpOut (null, addr, unordered, complete, ploadPID, streamNumber, timeToLive, checkBuffer, copy, buffs);
		}
	    }
	    // sent from an agent - needs to be intercepted - we know that there is a diameter msg per mux msg
	    DiameterParser parser = new DiameterParser ();
	    for (ByteBuffer buff : buffs){
		while (true){
		    if (buff.remaining () == 0) return true;
		    long[] ts = _diamEngine.parseTimestamps (buff);
		    final DiameterMessage msg = parser.parseMessage (buff);
		    if (msg == null) return true;
		    msg.timestamp1 (ts[0]).timestamp2 (ts[1]);
		    _diamEngine.calcAgentLatency (agent, msg);
		    Runnable r = new Runnable (){ public void run (){
			if (agent.getLogger ().isDebugEnabled ())
			    agent.getLogger ().debug (DiameterIOHSctpClientChannel.this+" : "+agent+" : RESPOND : "+msg);
			_context.handleAgentMessage (agent, msg);
		    }};
		    schedule (r);
		}
	    }
	    return true;
	}
    }

    private static final long[] DEF_TIMESTAMPS = new long[]{0L, 0L};
    private long[] parseTimestamps (ByteBuffer buff){
	if (_latencyProcAny){
	    long ts1 = 0L;
	    long ts2 = 0L;
	    switch (buff.get () & 0xFF){
	    case 10:
		ts1 = DiameterParser.getUnsigned48 (buff);
		break;
	    case 20:
		ts1 = DiameterParser.getUnsigned48 (buff);
		ts2 = DiameterParser.getUnsigned48 (buff);
		break;
	    default:
		buff.position (buff.position () - 1); // rewind
		return DEF_TIMESTAMPS;
	    }
	    return new long[]{ts1, ts2};
	} else
	    return DEF_TIMESTAMPS;
    }
    
    private int _dwrTimeout, _dwrAttempts, _dprTimeout, _clientReqTimeout, _dprReasonCode, _dprReasonCodeInternal;
    private int _closeOnDpaDelay, _closeOnDpaTimeout;
    private int _maxCERSize, _maxAppSize;
    private boolean _sendDpr;
    private boolean _routeLocal;
    private String _hostIPAddrCEAPolicy, _hostIPAddrCEAContent, _hostIPAddrCERPolicy, _hostIPAddrCERContent;

    public int getCERMaxSize (){ return _maxCERSize;}
    public int getAppMaxSize (){ return _maxAppSize;}
    public long getDwrTimeout (){ return (long) _dwrTimeout;}
    public int getDwrAttempts (){ return _dwrAttempts; }
    public int getDprTimeout (){ return _dprTimeout; }
    public int getClientReqTimeout (){ return _clientReqTimeout; }
    public int getCloseOnDPADelay (){ return _closeOnDpaDelay;}
    public int getCloseOnDPATimeout (){ return _closeOnDpaTimeout;}
    public boolean getSendDpr (){ return _sendDpr; }
    public int getDprReasonCode (){ return _dprReasonCode; }
    public int getDprReasonCodeInternal (){ return _dprReasonCodeInternal; }
    public boolean routeLocal (){ return _routeLocal;}
    public String getCEAHostIPAddrPolicy (){ return _hostIPAddrCEAPolicy;}
    public String getCEAHostIPAddrContent (){ return _hostIPAddrCEAContent;}
    public String getCERHostIPAddrPolicy (){ return _hostIPAddrCERPolicy;}
    public String getCERHostIPAddrContent (){ return _hostIPAddrCERContent;}
    public List<InetAddress> getHostIPAddresses (){ return _hostIPAddresses;}

    public void diameterChannelOpened (boolean tcp, boolean accepted){
	if (tcp){
	    if (accepted) _channelsDiameterTcpAcceptedOpenMeter.inc (1);
	    else _channelsDiameterTcpConnectedOpenMeter.inc (1);
	} else {
	    if (accepted) _channelsDiameterSctpAcceptedOpenMeter.inc (1);
	    else _channelsDiameterSctpConnectedOpenMeter.inc (1);
	}
    }
    public void diameterChannelClosed (boolean tcp, boolean accepted){
	if (tcp){
	    if (accepted){
		_channelsDiameterTcpAcceptedOpenMeter.inc (-1);
		_channelsDiameterTcpAcceptedClosedMeter.inc (1);
	    } else {
		_channelsDiameterTcpConnectedOpenMeter.inc (-1);
		_channelsDiameterTcpConnectedClosedMeter.inc (1);
	    }
	} else {
	    if (accepted){
		_channelsDiameterSctpAcceptedOpenMeter.inc (-1);
		_channelsDiameterSctpAcceptedClosedMeter.inc (1);
	    } else {
		_channelsDiameterSctpConnectedOpenMeter.inc (-1);
		_channelsDiameterSctpConnectedClosedMeter.inc (1);
	    }
	}
    }

    public void timestamp (DiameterMessage msg){
	if (_latencyProcAny){
	    if (msg.isRequest ()){
		if (_latencyProcSamplingAll ||
		     ThreadLocalRandom.current ().nextInt (_latencyProcSampling) == 0)
		    msg.timestamp1 (System.currentTimeMillis ());
	    } else {
		msg.timestamp1 (System.currentTimeMillis ());
	    }
	}
    }
    public void calcAgentLatency (MuxClient agent, DiameterMessage msg){
	if (_latencyProcAgent &&
	    msg.timestamp1 () != 0L){
	    ((DiameterMuxClient) agent).getDiameterIOHMeters ().calcMessageLatency (msg);
	}
    }
    public void calcEngineLatency (DiameterMessage msg){
	if (_latencyProcEngine &&
	    msg.timestamp1 () != 0L){
	    _latencyQueue.execute (() -> {_diamMeters.calcMessageLatency (msg);});
	}
    }
}
