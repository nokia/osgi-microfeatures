package com.alcatel.as.diameter.lb.impl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.security.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import alcatel.tess.hometop.gateways.reactor.*;

import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.tools.*;

public class ClientContext<T extends AsyncChannel> implements TcpClientListener, DiameterClient {

    private static final int AVP_CODE_CONGESTION_LEVEL = 72;
    private static final int AVP_CODE_LOAD_FACTOR = 73;
    private static final int AVP_CODE_REJECTION_FACTOR = 74; // not used yet
    private static final int LUCENT_VENDOR_ID = 1751;

    private Logger LOGGER;
    private Object[] _attachment;
    private Map<String, Object> _props;
    private DiameterMessage _cer;
    private List<ClientRequest> _pendingClientReqs = new ArrayList<ClientRequest> ();
    private DestinationManagerMap _destManagerMap;
    private DiameterMessage _myServerDwr, _myClientDwr, _myServerDwa, _myClientDwa;
    private TcpClient _tcpClient;
    private T _client;
    private Quorum _quorum;
    private State _state;
    private PlatformExecutor _exec;
    private DiameterRouter _router;
    private long _clientReqTimeout, _dprTimeout;
    private String _id, _originH, _originR;
    private Future _clientReqTimeoutFuture;
    private int _dwrMaxServer;
    private PeerState _clientState;
    private boolean _sendDpr;
    private int _dprReasonCode;
    private boolean _useLoadFactor, _hostIPAddrTransformCER;
    private String _hostIPAddrTransformCEA;
    private List<Destination> _remoteLBs;
    
    protected ClientContext (String id, T client, DiameterProcessor processor, Map<String, Object> props){
	DiameterLoadBalancer lb = processor.getDiameterLoadBalancer ();
	_state = _initState;
	_id = id;
	LOGGER = Logger.getLogger ("as.diameter.lb");
	_client = client;
	_props = props;
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_router = processor.getDiameterRouter ();
	ReactorProvider reactorProvider = (ReactorProvider) props.get ("system.reactor.provider");
	Reactor reactor = null;
	String name = (String) props.get (Client.PROP_CLIENT_REACTOR);
	if (name == null) reactor = (Reactor) props.get (Server.PROP_SERVER_REACTOR);
	else {
	    name = new StringBuilder ().append ("ioh.client.").append (name).toString ();
	    reactor = reactorProvider.getReactor (name);
	}
	try{
	    if (reactor == null){
		reactor = reactorProvider.create (name);
		reactor.start ();
	    }
	}catch(Throwable t){ // not sure if possible at all....
	    LOGGER.error (this+" : failed to create server side reactor", t);
	    reactor = (Reactor) props.get (Server.PROP_SERVER_REACTOR);
	}
	if (props.get (Server.PROP_READ_TIMEOUT) == null) client.setSoTimeout (DiameterLoadBalancer.DEF_READ_TIMEOUT);
	int dwrMaxClient = getIntProperty (DiameterLoadBalancer.CONF_DWR_ATTEMPTS, props, lb.getDwrAttempts ());
	props.remove (DiameterLoadBalancer.CONF_DWR_ATTEMPTS);
	_clientState = new PeerState (dwrMaxClient);
	Counters serverCounters = (Counters) props.get ("diameter.lb.counters");
	_clientState.initCounters ("diameter.lb.client:"+props.get (Server.PROP_SERVER_NAME)+":"+props.get ("client.id"), serverCounters, true);
	_sendDpr = getBooleanProperty (DiameterLoadBalancer.CONF_DPR, props, lb.getSendDpr ());
	props.remove (DiameterLoadBalancer.CONF_DPR);
	_dprReasonCode = getIntProperty (DiameterLoadBalancer.CONF_DPR_REASON, props, lb.getDprReasonCode ());
	props.remove (DiameterLoadBalancer.CONF_DPR_REASON);
  	props.put (Client.PROP_CLIENT_REACTOR, reactor);
	//props.put (Client.PROP_READ_EXECUTOR, _exec); // already set since same key as Server.PROP_READ_EXECUTOR
	// now we remove client-specific config which is not meant to be on the server side
	props.remove (Client.PROP_CLIENT_PRIORITY);
	props.remove (Client.PROP_READ_TIMEOUT);
	props.remove (TcpClient.PROP_TCP_NO_DELAY);
	_tcpClient = lb.getClientFactory ().newTcpClient ((String) props.get (DiameterLoadBalancer.PROP_CLIENT_ID), props);
	// props are now filled with client config values - we set defaults
	String quorum = (String) props.get (DiameterLoadBalancer.CONF_QUORUM);
	if (quorum == null) quorum = lb.getQuorum ();
	else {
	    quorum = quorum.trim ().replace (" ", "");
	    if (quorum.length () == 0) quorum = "1";
	}
	int q = 1;
	try{
	    if (quorum.endsWith ("%")){
		if (quorum.length () > 1){
		    q = Integer.parseInt (quorum.substring (0, quorum.length () - 1));
		    if (q <= 0 || q > 100) throw new Exception ();
		    q = Math.round ((q*_tcpClient.getDestinations ().size ())/100);
		    if (q == 0) q = 1;
		    if (q > _tcpClient.getDestinations ().size ()) q = _tcpClient.getDestinations ().size ();
		} // else keep q = 1
	    }else{
		q = Integer.parseInt (quorum);
		if (q > _tcpClient.getDestinations ().size ()) q = _tcpClient.getDestinations ().size ();
	    }
	    if (q <= 0) throw new Exception ();
	}catch(Throwable t){
	    LOGGER.error (this+" : invalid quorum value : "+props.get (DiameterLoadBalancer.CONF_QUORUM)+" : using 1");
	    q = 1;
	}
	if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : set quorum to : "+q);
	_quorum = new Quorum (q);
	_dprTimeout = getIntProperty (DiameterLoadBalancer.CONF_TIMER_DPR, props, lb.getDprTimeout ());
	getIntProperty (Client.PROP_CLIENT_PRIORITY, props, DiameterLoadBalancer.DEF_PRIORITY);
	getIntProperty (Client.PROP_READ_TIMEOUT, props, DiameterLoadBalancer.DEF_READ_TIMEOUT);
	_clientReqTimeout = getIntProperty (DiameterLoadBalancer.CONF_TIMER_CLIENT_REQ, props, lb.getClientReqTimeout ());
	_dwrMaxServer = getIntProperty (DiameterLoadBalancer.CONF_DWR_ATTEMPTS, props, lb.getDwrAttempts ());
	_hostIPAddrTransformCER = getBooleanProperty (DiameterLoadBalancer.CONF_CER_HOST_IP_ADDRESS_TRANSFORM, props, lb.getCERHostIPAddrTransform ());
	_hostIPAddrTransformCEA = (String) props.get (DiameterLoadBalancer.CONF_CEA_HOST_IP_ADDRESS_TRANSFORM);
	if (_hostIPAddrTransformCEA == null) _hostIPAddrTransformCEA = lb.getCEAHostIPAddrTransform ();
	_useLoadFactor = getBooleanProperty (DiameterLoadBalancer.CONF_LOADFACTOR, props, lb.getUseLoadFactor ());
	List<Destination> servers = _tcpClient.getDestinations ();
	for (int i =0; i<servers.size (); ){
	    Destination server = servers.get (i);
	    if (isRemoteLB (server)){
		if (_remoteLBs == null) _remoteLBs = new ArrayList<Destination> ();
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (this+" : registering remote LB : "+server);
		_remoteLBs.add (server);
		servers.remove (i);
	    } else
		i++;
	    Object o = server.getProperties ().get ("server.mode"); // server.mode=standby implies _useLoadFactor regardless of the specific prop (easier to config)
	    if ("standby".equals (o)) _useLoadFactor = true;
	}
	_destManagerMap = _useLoadFactor ? new DestinationManagerMap (s -> new WeightedDestinationManager (s, _tcpClient.getDestinations ().size () * 100)) :
	    new DestinationManagerMap (s -> new SimpleDestinationManager (s));
    }
    public String toString (){
	return new StringBuilder ().append (_id).append (_state).toString ();
    }
    protected void init (){
	_openingState.enter (EVENT_STARTING);
    }
    protected Counters getCounters (){ return _clientState._counters;}
    protected String getOriginHost (){ return _originH;}
    protected String getOriginRealm (){ return _originR;}
    
    public void clientMessage (DiameterMessage msg){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (this+" : received from client : "+msg);
	_clientState._counters.CLIENT_IN_BYTES.inc (msg.getBytes ().length);
	_clientState.alive ();
	if (msg.isRequest ()){
	    switch (msg.getType ()){
	    case Watchdog:
		_clientState._counters.CLIENT_IN_DWR.inc (1);
		_state.clientDWR (msg); return;
	    case Disconnection:
		_clientState._counters.CLIENT_IN_DPR.inc (1);
		_state.clientDPR (msg); return;
	    case Capabilities:
		_clientState._counters.CLIENT_IN_CER.inc (1);
		_state.clientCER (msg);	return;
	    case Application: 
		_clientState._counters.CLIENT_IN_REQUESTS.inc (1);
		_state.clientRequest (msg); return;
	    }
	} else {
	    switch (msg.getType ()){
	    case Watchdog:
		_clientState._counters.CLIENT_IN_DWA.inc (1);
		return;
	    case Disconnection:
		_clientState._counters.CLIENT_IN_DPA.inc (1);
		_state.clientDPA (msg);
		return;
	    case Capabilities: // handle like client response for now
		_clientState._counters.CLIENT_IN_CEA.inc (1);
		break;
	    case Application: 
		_clientState._counters.CLIENT_IN_RESPONSES.inc (1);
		break;
	    }
	    _state.clientResponse (msg);
	}
    }
    
    public void clientTimeout (){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client inactivity detected");
	if (_clientState.timeout (_client, "client"))
	    sendToClient (DiameterUtils.updateIdentifiers (_myClientDwr.clone ()), _clientState._counters.CLIENT_OUT_DWR);
    }

    protected void clientClosed (){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client closed");
	_state.clientClosed ();
    }

    protected void clientBlocked (){ // not called but remains available for router
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client blocked");
	_router.clientBlocked (this);
    }

    protected void clientUnblocked (){ // not called but remains available for router
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client unblocked");
	_router.clientUnblocked (this);
    }

    /**************************
     ** TcpClientListener **
     **************************/

    public TcpChannelListener connectionEstablished (TcpClient client, final Destination dest){
	_clientState._counters.SERVER_TCP_CONNECTIONS.inc (1);
	boolean isRemoteLB = isRemoteLB (dest);
	if (isRemoteLB) _clientState._counters.SERVER_LB_CONNECTIONS.inc (1);
	if (LOGGER.isInfoEnabled ()){
	    if (isRemoteLB)
		LOGGER.info (this+" : remote LB opened : "+dest+" : "+Counters.AGGREGATED.SERVER_TCP_CONNECTIONS.getValue ()+"/"+Counters.AGGREGATED.SERVER_LB_CONNECTIONS.getValue ());
	    else
		LOGGER.info (this+" : server opened : "+dest+" : "+Counters.AGGREGATED.SERVER_TCP_CONNECTIONS.getValue ());
	}
	dest.getChannel ().setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	PeerState state = new PeerState (_dwrMaxServer)
	    .setRemoteLB (isRemoteLB);
	String group = (String) dest.getProperties ().get ("server.group");
	state._destManager = _destManagerMap.getCreate (group);
	if (group != null){
	    state.setOrigin ((String) client.getProperties ().get (group+".Origin-Host"), (String) client.getProperties ().get (group+".Origin-Realm"));
	}
	if (_useLoadFactor){
	    try{
		String s = (String) dest.getProperties ().get ("server.availability");
		if (s == null){
		    String tmp  = (String) dest.getProperties ().get ("server.mode");
		    if ("active".equalsIgnoreCase (tmp)) s = "100";
		    else if ("standby".equalsIgnoreCase (tmp)) s = "0";
		}
		if (s != null){
		    int i = Integer.parseInt (s);
		    if (i < 0 || i > 100) throw new Exception ();
		    state._availabilityFactor = i;
		}
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug (this+" : "+dest+" : availability="+state._availabilityFactor);
	    }catch(Exception e){
		LOGGER.warn (this+" : "+dest+" : invalid availability factor : "+dest.getProperties ());
	    }
	}
	String serverId = new StringBuilder ()
	    .append ("server:")
	    .append (dest.getProperties ().get ("server.name").toString ().replace (":", "/")) // used to indicate the port - maybe broken by json
	    .append ('#')
	    .append (dest.getProperties ().get ("client.id"))
	    .toString ();
	state.initCounters (//"server:"+dest.getProperties ().get ("client.id").toString (),
			    serverId,
			    _clientState._counters,
			    false);
	dest.getChannel ().attach (state);
	_router.serverOpened (this, dest);
	_state.serverOpened (dest, isRemoteLB ? _quorum.unchanged () : _quorum.joined ());
	return new ServerMessageProcessor (dest);
    }
    private class ServerMessageProcessor extends TcpMessageProcessor<DiameterMessage> {
	private Destination _dest;
	private ServerMessageProcessor (Destination dest){
	    super (new DiameterParser ());
	    _dest = dest;
	}
	public void messageReceived (TcpChannel channel, DiameterMessage msg){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (ClientContext.this+" : received from server : "+_dest+" : "+msg);
	    serverMessage (_dest, msg);
	}
	public void receiveTimeout (TcpChannel channel){
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (ClientContext.this+" : server inactivity : "+_dest);
	    serverTimeout (_dest);
	}
	public void writeBlocked (TcpChannel channel){ // not used but remains available for routers
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : server blocked : "+_dest);
	    _router.serverBlocked (ClientContext.this, _dest);
	}
	public void writeUnblocked (TcpChannel channel){ // not used but remains available for routers
	    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : server unblocked : "+_dest);
	    _router.serverUnblocked (ClientContext.this, _dest);
	}
	public void connectionClosed (TcpChannel channel){
	    _clientState._counters.SERVER_TCP_CONNECTIONS.inc (-1);
	    PeerState state = channel.attachment ();
	    if (state.isRemoteLB ()) _clientState._counters.SERVER_LB_CONNECTIONS.inc (-1);
	    if (LOGGER.isInfoEnabled ()){
		if (state.isRemoteLB ())
		    LOGGER.info (ClientContext.this+" : remote LB closed : "+_dest+" : "+Counters.AGGREGATED.SERVER_TCP_CONNECTIONS.getValue ()+"/"+Counters.AGGREGATED.SERVER_LB_CONNECTIONS.getValue ());
		else
		    LOGGER.info (ClientContext.this+" : server closed : "+_dest+" : "+Counters.AGGREGATED.SERVER_TCP_CONNECTIONS.getValue ());
	    }
	    _router.serverClosed (ClientContext.this, _dest);
	    _state.serverClosed (_dest, state.isRemoteLB () ? _quorum.unchanged () : _quorum.left ());
	    state._counters.stop ();
	}
    }
    
    // called when a server connection failed
    public void connectionFailed (TcpClient client, Destination dest){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : server failed : "+dest);
	_state.serverFailed (dest);
    }

    private void serverMessage (Destination server, DiameterMessage msg){
	PeerState state = server.getChannel ().attachment ();
	state._counters.SERVER_IN_BYTES.inc (msg.getBytes ().length);
	state.alive ();
	if (state.setAvailable () && !state.isRemoteLB ())
	    state._destManager.available (server, state._availabilityFactor);
	if (msg.isRequest ()){
	    switch (msg.getType ()){
	    case Disconnection:
		state._counters.SERVER_IN_DPR.inc (1);
		checkLoadFactor (server, msg, true);
		_state.serverDPR (server, msg); return;
	    case Capabilities: // tbd in Future - handle like app for now
		state._counters.SERVER_IN_CER.inc (1);
		checkLoadFactor (server, msg, true);
		_state.serverRequest (server, msg); return;
	    case Application:
		state._counters.SERVER_IN_REQUESTS.inc (1);
		_state.serverRequest (server, msg); return;
	    case Watchdog:
		state._counters.SERVER_IN_DWR.inc (1);
		checkLoadFactor (server, msg, false);
		sendToServer (server, DiameterUtils.updateDwa (msg, _myServerDwa.clone ()), state._counters.SERVER_OUT_DWA);
		return;
	    }
	} else {
	    switch (msg.getType ()){
	    case Application:
		state._counters.SERVER_IN_RESPONSES.inc (1);
		_state.serverResponse (server, msg); return;
	    case Disconnection:
		state._counters.SERVER_IN_DPA.inc (1);
		checkLoadFactor (server, msg, true);
		_state.serverDPA (server, msg);return;
	    case Capabilities:
		state._counters.SERVER_IN_CEA.inc (1);
		checkLoadFactor (server, msg, true);
		_state.serverCEA (server, msg); return;
	    case Watchdog:
		state._counters.SERVER_IN_DWA.inc (1);
		checkLoadFactor (server, msg, false);
		return;
	    }
	}
    }

    private void serverTimeout (Destination server){
	PeerState state = server.getChannel ().attachment ();
	if (state.timeout (server.getChannel (), "server "+server) && _myServerDwr != null){
	    if (state.setUnavailable () && !state.isRemoteLB ()) state._destManager.unavailable (server);
	    sendToServer (server, DiameterUtils.updateIdentifiers (_myServerDwr.clone ()), state._counters.SERVER_OUT_DWR);
	}
    }

    /**************************
     ** State machine **
     **************************/
    private static class Event{
	private String _msg;
	private Level _level;
	private Object _attachment;
	private Event (String msg, Level level){ this (msg, level, null);}
	private Event (String msg, Level level, Object attachment){ _msg = msg; _level = level; _attachment = attachment;}
	public String toString (){ return _msg; }
	private Level getLogLevel (){ return _level; }
	private Object attachment (){ return _attachment; }
    }
    private static final Event EVENT_STARTING = new Event ("EVENT_STARTING", Level.INFO);
    private static final Event EVENT_CLIENT_CLOSED = new Event ("EVENT_CLIENT_CLOSED", Level.INFO);
    private static final Event EVENT_QUORUM_LOST = new Event ("EVENT_QUORUM_LOST", Level.WARN);
    private static final Event EVENT_QUORUM_FAILED = new Event ("EVENT_QUORUM_FAILED", Level.WARN);
    private static final Event EVENT_CLEAN = new Event ("EVENT_CLEAN", Level.DEBUG);
    private static final Event EVENT_SENT_DPA_TO_CLIENT = new Event ("EVENT_SENT_DPA_TO_CLIENT", Level.INFO);
    private static final Event EVENT_SENT_DPA_TO_SERVER = new Event ("EVENT_SENT_DPA_TO_SERVER", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_REQUEST = new Event ("EVENT_UNEXPECTED_CLIENT_REQUEST", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_RESPONSE = new Event ("EVENT_UNEXPECTED_CLIENT_RESPONSE", Level.INFO);
    private static final Event EVENT_UNEXPECTED_SERVER_DPR = new Event ("EVENT_UNEXPECTED_SERVER_DPR", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_CER = new Event ("EVENT_UNEXPECTED_CLIENT_CER", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DWR = new Event ("EVENT_UNEXPECTED_CLIENT_DWR", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DPR = new Event ("EVENT_UNEXPECTED_CLIENT_DPR", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DPA = new Event ("EVENT_UNEXPECTED_CLIENT_DPA", Level.INFO);
    private static final Event EVENT_SENT_CEA = new Event ("EVENT_SENT_CEA", Level.DEBUG);
    private static final Event EVENT_RECEIVED_DPA = new Event ("EVENT_RECEIVED_DPA", Level.DEBUG);
    private static final Event EVENT_ADMIN_DPR = new Event ("EVENT_ADMIN_DPR", Level.WARN);
    
    private class State {
	protected void logEnter (Event event){
	    if (LOGGER.isEnabledFor (event.getLogLevel ()))
		LOGGER.log (event.getLogLevel (),
			    ClientContext.this+" : changing state : from : "+_state+" to : "+this+" event : "+event);
	}
	protected void enter (Event event){};
	protected void clientClosed (){ _terminatedState.enter (EVENT_CLIENT_CLOSED); }
	protected void clientRequest (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_REQUEST); }
	protected void clientResponse (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_RESPONSE); }
	protected void clientCER (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_CER); }
	protected void clientDWR (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DWR); }
	protected void clientDPR (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DPR); }
	protected void clientDPA (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DPA); }
	protected void serverOpened (Destination server, Quorum.Transition transition){}
	protected void serverFailed (Destination server){}
	protected void serverClosed (Destination server, Quorum.Transition transition){}
	protected void serverRequest (Destination server, DiameterMessage msg){}
	protected void serverResponse (Destination server, DiameterMessage msg){}
	protected void serverCEA (Destination server, DiameterMessage msg){}
	protected void serverDPR (Destination server, DiameterMessage msg){_terminatedState.enter (EVENT_UNEXPECTED_SERVER_DPR);}
	protected void serverDPA (Destination server, DiameterMessage msg){}
    }
    private State _initState = new State (){
	    public String toString (){ return "[Initial]";}
	};
    private State _openingState = new State (){
	    private int _pendingCallbacks;
	    private int maxFailed, failed;
	    private boolean _reached;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		int serversSize = _tcpClient.getDestinations ().size ();
		if (serversSize == 0){
		    LOGGER.warn (ClientContext.this+" : no destination identified - closing client connection");
		    _client.close ();
		    return;
		}
		_router.clientOpened (ClientContext.this);
		maxFailed = serversSize - _quorum.getQuorum ();
		_pendingCallbacks = serversSize; // or set to 1 for responsiveness
		_tcpClient.open (ClientContext.this);
		_tcpClient.disableConnect ();
	    }
	    protected void serverOpened (Destination server, Quorum.Transition transition){
		_pendingCallbacks--;
		_reached = transition.reached ();
		callbacked ();
	    }
	    protected void serverFailed (Destination server){
		_pendingCallbacks--;
		failed++;
		callbacked ();
	    }
	    protected void serverClosed (Destination server, Quorum.Transition transition){
		failed++;
		_reached = transition.reached ();
		callbacked ();
	    }
	    protected void callbacked (){
		if (failed > maxFailed)
		    _terminatedState.enter (EVENT_QUORUM_FAILED);
		else if (_pendingCallbacks <= 0 && _reached)
		    _handshakingState.enter (new Event ("EVENT_QUORUM_REACHED", Level.DEBUG, failed));
	    }
	    public String toString (){ return "[Opening]"; }
	};
    private State _handshakingState = new State (){
	    private CEAManager2 _ceaMgr;
	    protected void enter (Event event){
		int opened = _tcpClient.getDestinations ().size () - (Integer) event.attachment ();
		_ceaMgr = new CEAManager2 (opened, -1); // use _quorum.getQuorum() for responsiveness
		logEnter (event);
		_state = this;
		_client.enableReading ();
	    }
	    protected void clientCER (DiameterMessage msg){
		if (_cer != null){
		    _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_CER);
		    return;
		}
		try{
		    byte[] avp = msg.getAvp (264, 0);
		    _originH = avp != null ? new String (avp, "utf-8") : null;
		    avp = msg.getAvp (296, 0);
		    _originR = avp != null ? new String (avp, "utf-8") : null;
		    String diameterId = _originH+"@"+_originR;
		    _id = _id + "/" + diameterId;
		    _props.put ("Origin-Host", _originH);
		    _props.put ("Origin-Realm", _originR);
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : client is : "+diameterId);
		}catch(Exception e){} // cannot happen with utf8
		_myServerDwr = DiameterUtils.makeDwr (msg); // if cer is crap, dwr is crap : consistent
		_myServerDwa = DiameterUtils.makeDwa (msg);
		int port = 0;
		_cer = transformCERHostIPAddress (msg);
		sendToAllServers (_cer, Counters.SERVER_OUT_CER_DEF, false);
	    }
	    protected void serverOpened (Destination server, Quorum.Transition transition){
		// cannot happen in CEAManager2
		if (_cer != null){
		    PeerState state = server.getChannel ().attachment ();
		    sendToServer (server, _cer, state._counters.SERVER_OUT_CER);
		}
	    }
	    protected void serverFailed (Destination server){
		// cannot happen in CEAManager2
	    }
	    protected void serverClosed (Destination server, Quorum.Transition transition){
		PeerState state = server.getChannel ().attachment ();
		if (state.getDPRManager () != null){
		    state.getDPRManager ().serverClosed ();
		    return;
		}
		if (state.isActive ()){
		    state._destManager.remove (server);
		}
		serverOut (state.isActive ());
	    }
	    protected void serverOut (boolean wasActive){
		switch (_ceaMgr.serverClosed (wasActive)){
		case 1:
		    DiameterMessage cea = _ceaMgr.getOKCEA ();
		    sendToClient (transformCEAHostIPAddress (cea), _clientState._counters.CLIENT_OUT_CEA);
		    if (_ceaMgr.getOKSize () >= _quorum.getQuorum ())
			_activeState.enter (EVENT_SENT_CEA);
		    else
			_terminatedState.enter (EVENT_QUORUM_FAILED);
		    break;
		case 0:
		    break;
		case -1:
		    cea = _ceaMgr.getKOCEA ();
		    sendToClient (transformCEAHostIPAddress (cea), _clientState._counters.CLIENT_OUT_CEA);
		    _terminatedState.enter (EVENT_QUORUM_FAILED);
		    break;
		}
	    }
	    protected void serverCEA (Destination server, DiameterMessage msg){
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (ClientContext.this+" : serverCEA : result-code="+msg.getResultCode ());
		int action = _ceaMgr.receivedCEA (server, msg);
		if (action != -1){
		    PeerState state = server.getChannel ().attachment ();
		    state.active ();
		    state._destManager.add (server, state._availabilityFactor);
		    if (_myClientDwr == null){
			_myClientDwr = DiameterUtils.makeDwr (msg);
			_myClientDwa = DiameterUtils.makeDwa (msg);
		    }
		    if (action == 1){
			sendToClient (transformCEAHostIPAddress (msg), _clientState._counters.CLIENT_OUT_CEA);
			// we gave -1 to CEAManager2 --> we check the quorum here
			if (_ceaMgr.getOKSize () >= _quorum.getQuorum ())
			    _activeState.enter (EVENT_SENT_CEA);
			else
			    _terminatedState.enter (EVENT_QUORUM_FAILED);
		    }		    
		}
	    }
	    protected void serverDPR (Destination server, DiameterMessage msg){
		if (_cer == null){
		    _terminatedState.enter (EVENT_UNEXPECTED_SERVER_DPR);
		    return;
		}
		PeerState state = server.getChannel ().attachment ();
		boolean wasActive = state.isActive ();
		if (state.isActive ()){
		    state._destManager.remove (server);
		}
		// else the server may have responded with non-2001 CEA
		// so lets be cool and return a DPA even if not active
		state.inactive ();
		DPRManager mgr = new DPRManager (server, msg, false); // we set active=false since it is used in activeState
		state.setDPRManager (mgr);
		sendToServer (server, mgr.makeDpa (_myServerDwr), state._counters.SERVER_OUT_DPA);
		mgr.scheduleClose (_exec);
		serverOut (wasActive);
	    }
	    public String toString (){ return "[Handshaking/"+_ceaMgr+"]"; }
	};
    private State _activeState = new State (){
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientReqTimeoutFuture = _exec.scheduleAtFixedRate (_clientReqTimeoutTask,
								     _clientReqTimeout,
								     _clientReqTimeout,
								     java.util.concurrent.TimeUnit.MILLISECONDS);
		_clientState.active ();
		_clientState.startCounters (_id); // the _id was updated with the CER
		if (_remoteLBs != null){
		    for (Destination remote : _remoteLBs)
			_tcpClient.getDestinations ().add (remote);
		}
		_tcpClient.enableConnect ();
	    }
	    protected void clientRequest (DiameterMessage msg){ _router.doClientRequest (ClientContext.this, msg); }
	    protected void clientResponse (DiameterMessage msg){ _router.doClientResponse (ClientContext.this, msg); }	
	    protected void clientCER (DiameterMessage msg){
		_cer = msg = transformCERHostIPAddress (msg);
		_pendingClientReqs.add (new ClientRequest (msg, 1));
		sendToAllServers (msg, Counters.SERVER_OUT_CER_DEF, true);
	    }
	    protected void clientDWR (DiameterMessage msg){
		if (_router.checkClientOverload (ClientContext.this, msg))
		    sendToClient (DiameterUtils.updateDwa (msg, _myClientDwa.clone ()), _clientState._counters.CLIENT_OUT_DWA);
	    }
	    protected void clientDPR (DiameterMessage msg){
		_terminatingByClientState.enter (new Event ("EVENT_RECEIVED_CLIENT_DPR", Level.INFO, msg));
	    }
	    protected void serverOpened (Destination server, Quorum.Transition transition){
		PeerState state = server.getChannel ().attachment ();
		sendToServer (server, _cer, state._counters.SERVER_OUT_CER);
	    }
	    protected void serverClosed (Destination server, Quorum.Transition transition){
		PeerState state = server.getChannel ().attachment ();
		if (state.isActive ()){
		    if (!state.isRemoteLB ()) state._destManager.remove (server);
		}
		else if (state.getDPRManager () != null){
		    state.getDPRManager ().serverClosed ();
		}
		if (transition.reached () == false){
		    if (_sendDpr){
			sendToClient (DiameterUtils.makeDpr (_myClientDwr,_dprReasonCode),_clientState._counters.CLIENT_OUT_DPR);
			_terminatingByServerState.enter (EVENT_QUORUM_LOST);
		    } else {
			_terminatedState.enter (EVENT_QUORUM_LOST);
		    }
		}
	    }
	    protected void serverCEA (Destination server, DiameterMessage msg){
		PeerState state = server.getChannel ().attachment ();
		if (state.isActive () == false && state.getDPRManager () == null){
		    if (msg.getResultCode () != 2001){
			server.getChannel ().close ();
			return;
		    }
		    state.active ();
		    if (!state.isRemoteLB ()) state._destManager.add (server, state._availabilityFactor);
		    if (!msg.isTwin (_cer)){
			sendToServer (server, _cer, state._counters.SERVER_OUT_CER);
		    }
		}
		if (state.isRemoteLB ()) return; // dont take CEA from remote LB into account in quorum
		// TODO non 2001 ?
		ClientRequest ctx = getClientRequest (msg);
		if (ctx == null || ctx.ping () == false) return;
		_pendingClientReqs.remove (ctx);
		sendToClient (transformCEAHostIPAddress (msg), _clientState._counters.CLIENT_OUT_CEA);
	    }
	    protected void serverRequest (Destination server, DiameterMessage msg){
		_router.doServerRequest (ClientContext.this, server, msg);
	    }
	    protected void serverResponse (Destination server, DiameterMessage msg){
		_router.doServerResponse (ClientContext.this, server, msg);
	    }
	    protected void serverDPR (final Destination server, DiameterMessage msg){
		// we handle the case where server == null : this is an admin command to disconnect
		if (server != null){
		    final PeerState state = server.getChannel ().attachment ();
		    boolean active = state.isActive ();
		    if (active && !state.isRemoteLB ())
			state._destManager.remove (server);
		    state.inactive ();
		    final DPRManager mgr = new DPRManager (server, msg, active);
		    state.setDPRManager (mgr);
		    Runnable r = new Runnable (){
			    public void run (){
				sendToServer (server, mgr.makeDpa (_myServerDwr), state._counters.SERVER_OUT_DPA);
				mgr.sentDPA (_exec);
			    }
			};
		    mgr.scheduledDPA (_exec.schedule (r, _dprTimeout, TimeUnit.MILLISECONDS));
		} else {
		    msg = DiameterUtils.makeDpr (_myClientDwr,_dprReasonCode);
		    sendToClient (msg, _clientState._counters.CLIENT_OUT_DPR);
		    _terminatingByServerState.enter (EVENT_ADMIN_DPR);
		}
	    }
	    public String toString (){
		return "[Active]";
	    }
	};
    private State _terminatingByClientState = new State (){
	    private DiameterMessage _dpa;
	    private int _nbDpa = 0;
	    private Future _cleaningFuture;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientState.inactive ();
		_tcpClient.disableConnect ();
		for (Destination server : sendToAllServers ((DiameterMessage)event.attachment(), Counters.SERVER_OUT_DPR_DEF, true)){
		    PeerState state = server.getChannel ().attachment ();
		    state.inactive ();
		    state.setTerminatingState (TERMINATING_STATE.WAITING_DPA);
		    _nbDpa++;
		}
		Runnable r = new Runnable (){
			public void run (){
			    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture triggered : "+_dpa);
			    if (_dpa != null){
				sendToClient (_dpa, _clientState._counters.CLIENT_OUT_DPA);
				_terminatedState.enter (EVENT_SENT_DPA_TO_CLIENT);
			    } else {
				_terminatedState.enter (EVENT_CLEAN);
			    }
			}
		    };
		_cleaningFuture = _exec.schedule (r, _dprTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	    }
	    private void processServerDPA (DiameterMessage dpa){ // dpa is null in case of server close
		if (dpa != null) _dpa = dpa; // we store the DPA
		if (--_nbDpa == 0){
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture cancelled");
		    _cleaningFuture.cancel (false);
		    if (_dpa != null){
			sendToClient (_dpa, _clientState._counters.CLIENT_OUT_DPA);
			_terminatedState.enter (EVENT_SENT_DPA_TO_CLIENT);
		    } else {
			_terminatedState.enter (EVENT_CLEAN);
		    }
		}
	    }
	    protected void clientClosed (){
		if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture cancelled");
		_cleaningFuture.cancel (false);
		_terminatedState.enter (EVENT_CLIENT_CLOSED);
	    }
	    protected void serverOpened (Destination server, Quorum.Transition transition){
		server.getChannel ().close ();
	    }
	    protected void serverClosed (Destination server, Quorum.Transition transition){
		PeerState state = server.getChannel ().attachment ();
		if (state.getDPRManager () != null){
		    state.getDPRManager ().serverClosed ();
		    return;
		}
		TERMINATING_STATE serverState = state.getTerminatingState ();
		if (serverState == null){}
		else if (serverState == TERMINATING_STATE.WAITING_DPA){
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : server closed in state WAITING_DPA");
		    if (!state.isRemoteLB ()) state._destManager.remove (server);
		    processServerDPA (null);
		} else if (serverState == TERMINATING_STATE.RECEIVED_DPA){} // nothing to do
	    }
	    protected void serverCEA (Destination server, DiameterMessage msg){
		PeerState state = server.getChannel ().attachment ();
		TERMINATING_STATE serverState = state.getTerminatingState ();
		if (serverState == null){
		    server.getChannel ().close ();
		} else {
		    if (state.isRemoteLB ()) return; // dont take CEA from remote LB into account in quorum
		    ClientRequest ctx = getClientRequest (msg);
		    if (ctx == null || ctx.ping () == false) return;
		    _pendingClientReqs.remove (ctx);
		    sendToClient (transformCEAHostIPAddress (msg), _clientState._counters.CLIENT_OUT_CEA);
		}
	    }
	    protected void serverRequest (Destination server, DiameterMessage msg){
		_router.doServerRequest (ClientContext.this, server, msg);
	    }
	    protected void serverResponse (Destination server, DiameterMessage msg){
		_router.doServerResponse (ClientContext.this, server, msg);
	    }
	    protected void serverDPR (Destination server, DiameterMessage msg){} // ignored
	    protected void serverDPA (Destination server, DiameterMessage msg){
		server.getChannel ().close ();
		PeerState state = server.getChannel ().attachment ();
		if (state.getTerminatingState () == TERMINATING_STATE.WAITING_DPA){ // a safety guard if the server sends 2 DPA
		    state.setTerminatingState (TERMINATING_STATE.RECEIVED_DPA);
		    if (!state.isRemoteLB ()) state._destManager.remove (server);
		    processServerDPA (msg);
		}
	    }
	    protected void clientRequest (DiameterMessage msg){
		_router.doClientRequest (ClientContext.this, msg); // not expected though
	    }
	    protected void clientResponse (DiameterMessage msg){
		_router.doClientResponse (ClientContext.this, msg);
	    }
	    protected void clientCER (DiameterMessage msg){} // ignored
	    protected void clientDWR (DiameterMessage msg){
		if (_router.checkClientOverload (ClientContext.this, msg))
		    sendToClient (DiameterUtils.updateDwa (msg, _myClientDwa.clone ()), _clientState._counters.CLIENT_OUT_DWA);
	    }
	    protected void clientDPR (DiameterMessage msg){} // ignored
	    protected void clientDPA (DiameterMessage msg){} // ignored
	    public String toString (){ return "[TerminatingByClient]"; }
	};
    
    private State _terminatingByServerState = new State (){
	    private Future _cleaningFuture;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientState.inactive ();
		_tcpClient.disableConnect ();
		Runnable r = new Runnable (){
			public void run (){
			    _terminatedState.enter (EVENT_CLEAN);
			}
		    };
		_cleaningFuture = _exec.schedule (r, _dprTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	    }
	    protected void clientClosed (){
		_cleaningFuture.cancel (false);
		_terminatedState.enter (EVENT_CLIENT_CLOSED);
	    }
	    protected void serverOpened (Destination server, Quorum.Transition transition){
		server.getChannel ().close ();
	    }
	    protected void serverClosed (Destination server, Quorum.Transition transition){
		PeerState state = server.getChannel ().attachment ();
		if (state.isActive ()){
		    if (!state.isRemoteLB ()) state._destManager.remove (server);
		}
		else if (state.getDPRManager () != null){
		    state.getDPRManager ().serverClosed ();
		}
	    }
	    protected void serverCEA (Destination server, DiameterMessage msg){} // ignore for now
	    protected void serverRequest (Destination server, DiameterMessage msg){} // cannot send it
	    protected void serverResponse (Destination server, DiameterMessage msg){
		// not sure if allowed....
		_router.doServerResponse (ClientContext.this, server, msg);
	    }
	    protected void serverDPR (Destination server, DiameterMessage msg){} // ignore for now
	    protected void serverDPA (Destination server, DiameterMessage msg){} // should not happen
	    protected void clientRequest (DiameterMessage msg){
		_router.doClientRequest (ClientContext.this, msg);
	    }
	    protected void clientResponse (DiameterMessage msg){
		_router.doClientResponse (ClientContext.this, msg);
	    }
	    protected void clientCER (DiameterMessage msg){}
	    protected void clientDWR (DiameterMessage msg){
		if (_router.checkClientOverload (ClientContext.this, msg))
		    sendToClient (DiameterUtils.updateDwa (msg, _myClientDwa.clone ()), _clientState._counters.CLIENT_OUT_DWA);
	    }
	    protected void clientDPR (DiameterMessage msg){} // ignore it
	    protected void clientDPA (DiameterMessage msg){
		_cleaningFuture.cancel (false);
		_terminatedState.enter (EVENT_RECEIVED_DPA);
	    }
	    public String toString (){ return "[TerminatingByServer]"; }
	};
    private State _terminatedState = new State (){
	    private Future _clientCloseFuture;
	    public void enter (Event event){
		if (_state == this) return; // make it idempotent for cleaning and for closed callbacks
		boolean wasActive = (_state == _activeState ||
				     _state == _terminatingByClientState ||
				     _state == _terminatingByServerState);
		logEnter (event);
		_state = this;
		if (event != EVENT_CLIENT_CLOSED && event != EVENT_SENT_DPA_TO_CLIENT) _client.close ();
		_tcpClient.close ();
		_destManagerMap.clear ();
		if (event == EVENT_SENT_DPA_TO_CLIENT){
		    Runnable r = new Runnable (){
			    public void run (){
				_client.close ();
				_clientCloseFuture = null;
			    }};
		    _clientCloseFuture = _exec.schedule (r, 250L, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
		if (wasActive){
		    _clientReqTimeoutFuture.cancel (false);
		    _clientState._counters.stop ();
		} else {
		    _clientState._counters.abort (); // we have not started the counters - but we still need to stop the rates
		}
		_router.clientClosed (ClientContext.this);
	    }
	    protected void clientClosed (){
		if (_clientCloseFuture != null){
		    _clientCloseFuture.cancel (false);
		    _clientCloseFuture = null;
		}
	    }
	    protected void clientRequest (DiameterMessage msg){}
	    protected void clientResponse (DiameterMessage msg){}
	    protected void clientCER (DiameterMessage msg){}
	    protected void clientDWR (DiameterMessage msg){}
	    protected void clientDPR (DiameterMessage msg){}
	    protected void clientDPA (DiameterMessage msg){}
	    protected void serverOpened (Destination server, Quorum.Transition transition){}
	    protected void serverClosed (Destination server, Quorum.Transition transition){}
	    protected void serverFailed (Destination server){}
	    protected void serverRequest (Destination server, DiameterMessage msg){}
	    protected void serverResponse (Destination server, DiameterMessage msg){}
	    protected void serverCEA (Destination server, DiameterMessage msg){}
	    protected void serverDPR (Destination server, DiameterMessage msg){}
	    protected void serverDPA (Destination server, DiameterMessage msg){}
	    public String toString (){ return "[Terminated]"; }
	};

    private ClientRequest getClientRequest (DiameterMessage response){
	for (ClientRequest ctx : _pendingClientReqs){
	    if (response.isTwin (ctx.getRequest ()))
		return ctx;
	}
	return null;
    }

    private Runnable _clientReqTimeoutTask = new Runnable (){
	    public void run (){
		long now = System.currentTimeMillis () + 50; // add 50ms to avoid edge effects
		while (_pendingClientReqs.size () > 0){
		    ClientRequest req = _pendingClientReqs.get (0);
		    if ((now - req.timestamp) > _clientReqTimeout){
			if (LOGGER.isDebugEnabled ())
			    LOGGER.debug (ClientContext.this+" : cleaning request : "+req.getRequest ());
			_pendingClientReqs.remove (0);
		    } else
			break;
		}
	    }
	};
    
    private static class ClientRequest {
	private long timestamp;
	private int countdown;
	private DiameterMessage _request;
	private ClientRequest (DiameterMessage msg, int count){
	    _request = msg;
	    countdown = count;
	    timestamp = System.currentTimeMillis ();
	}
	private boolean ping (){
	    return (--countdown == 0);
	}
	private DiameterMessage getRequest (){
	    return _request;
	}
    }
    
    private static enum TERMINATING_STATE {WAITING_DPA, RECEIVED_DPA, SENT_DPA};
    
    private class PeerState {
	private int _availabilityFactor = 100;
	private boolean _active;
	private boolean _unavailable;
	private boolean _isRemoteLB;
	private DPRManager _dprManager;
	private int _ping, _max;
	private Object _attachment;
	private Counters _counters;
	private TERMINATING_STATE _terminatingState;
	private byte[] _originH, _originR;
	private int _originHPadLen, _originRPadLen;
	private AbstractDestinationManager _destManager;
	private PeerState (int max){
	    _max = max;
	}
	private Counters initCounters (String id, Counters parent, boolean client){
	    return _counters = new Counters (id, parent, client);
	}
	private void startCounters (String description){
	    _counters.start (description);
	}
	private void active (){
	    _active = true;
	}
	private void inactive (){ // deactivate all monitoring
	    _active = false;
	    _max = Integer.MAX_VALUE;
	}
	private boolean isActive (){ return _active; }
	private void alive (){ _ping = 0; }
	private boolean timeout (AsyncChannel channel, Object target){
	    if (++_ping > _max){
		LOGGER.warn (ClientContext.this+" : "+target+" inactivity timeout");
		channel.shutdown ();
		return false;
	    }
	    return _active;
	}
	private boolean setUnavailable (){
	    if (_unavailable) return false;
	    return _unavailable = true;
	}
	private boolean setAvailable (){
	    if (_unavailable){
		_unavailable = false;
		return true;
	    }
	    return false;
	}
	private DPRManager getDPRManager (){ return _dprManager;}
	private void setDPRManager (DPRManager dprManager){ _dprManager = dprManager;}
	private TERMINATING_STATE getTerminatingState (){ return _terminatingState;}
	private void setTerminatingState (TERMINATING_STATE ts){ _terminatingState = ts;}
	private PeerState setRemoteLB (boolean isRemoteLB) { _isRemoteLB = isRemoteLB; return this;}
	private boolean isRemoteLB (){ return _isRemoteLB;}
	private PeerState setOrigin (String host, String realm){
	    if (host != null){
		byte[] tmp = host.getBytes (DiameterUtils.UTF8);
		_originH = DiameterUtils.padValue (tmp);
		_originHPadLen = _originH.length - tmp.length;
	    }
	    if (realm != null){
		byte[] tmp = realm.getBytes (DiameterUtils.UTF8);
		_originR = DiameterUtils.padValue (tmp);
		_originRPadLen = _originR.length - tmp.length;
	    }
	    return this;
	}
    }

    private static int getIntProperty (String name, Map<String, Object> props, Integer def){
	Object o = props.get (name);
	if (o == null){
	    props.put (name, def);
	    return def.intValue ();
	}
	if (o instanceof String){
	    int i = Integer.parseInt (((String)o).trim ());
	    props.put (name, i);
	    return i;
	}
	if (o instanceof Integer){
	    return ((Integer) o).intValue ();
	}
	props.put (name, def);
	return def.intValue ();
    }
    private static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def){
	Object o = props.get (name);
	if (o == null){
	    props.put (name, def);
	    return def;
	}
	if (o instanceof String){
	    boolean b = Boolean.parseBoolean (((String)o).trim ());
	    props.put (name, b);
	    return b;
	}
	if (o instanceof Boolean){
	    return ((Boolean) o).booleanValue ();
	}
	props.put (name, def);
	return def;
    }
    
    public static boolean isRemoteLB (TcpClient.Destination server){
	return getBooleanProperty (DiameterLoadBalancer.CONF_REMOTE_LB, server.getProperties (), false);
    }

    /**********************************
     ***** impl of DiameterClient    **
     **********************************/

    public String getDiameterId (){ return _id; }

    public boolean isOpen (){ return _state == _activeState; };

    public PlatformExecutor getExecutor (){ return _exec; }

    public SimpleMonitorable getMonitorable (){ return _clientState._counters.getMonitorable ();}
    public SimpleMonitorable getMonitorable (Destination server){
	PeerState state = server.getChannel ().attachment ();
	return state._counters.getMonitorable ();
    }

    public Logger getLogger (){ return LOGGER; }
    
    public AsyncChannel getClientChannel (){ return _client; }

    public Map<String, Object> getProperties (){ return _props; }
    
    public void attach (Object[] attachment){ _attachment = attachment; }

    public Object[] attachment (){ return _attachment; }

    public <T> T attachment (int index){ return (T) _attachment[index]; }

    public DestinationManager getDestinationManager (){ return getDestinationManager (null); }
    public DestinationManager getDestinationManager (String group){ return _destManagerMap.get (group); }
    public DestinationManager getDestinationManager (int hashcode){ return _destManagerMap.get (hashcode); }

    public void sendToClient (DiameterMessage msg){
	sendToClient (msg, msg.isRequest () ? _clientState._counters.CLIENT_OUT_REQUESTS : _clientState._counters.CLIENT_OUT_RESPONSES);
    }

    private void sendToClient (DiameterMessage msg, Meter msgCounter){
	if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : sending to client : "+msg);
	_clientState._counters.CLIENT_OUT_BYTES.inc (msg.getBytes ().length);
	msgCounter.inc (1);
	_client.send (msg.getBytes (), false);
    }

    public void sendToServer (Destination server, DiameterMessage msg){
	sendToServer (server, msg, null);
    }

    private void sendToServer (Destination server, DiameterMessage msg, Meter msgCounter){
	if (server != null){
	    PeerState state = server.getChannel ().attachment ();
	    if (state.getDPRManager () != null){
		// it is a response
		if (state.getDPRManager ().dpaSent ())
		    return;
	    }
	    DiameterUtils.updateDestination (msg, state._originH, state._originHPadLen, state._originR, state._originRPadLen);
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : sending to server : "+server+" : "+msg);
	    if (msgCounter == null){
		if (msg.isRequest ())
		    msgCounter = state._counters.SERVER_OUT_REQUESTS;
		else
		    msgCounter = state._counters.SERVER_OUT_RESPONSES;
	    }
	    msgCounter.inc (1);
	    state._counters.SERVER_OUT_BYTES.inc (msg.getBytes ().length);
	    server.getChannel ().send (msg.getBytes (), false);
	} else {
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : cannot send to server : dropping : "+msg);
	}
    }

    private List<Destination> sendToAllServers (DiameterMessage msg, Counters.Definition counterDef, boolean activeServersOnly){
	List<Destination> servers = new ArrayList<Destination> (_tcpClient.getDestinations ().size ());
	for (Destination server : _tcpClient.getDestinations ()){
	    if (server.isOpen () == false) continue;
	    boolean doSend = true;
	    PeerState state = server.getChannel ().attachment ();
	    if (activeServersOnly){
		doSend = state.isActive ();
	    }
	    if (doSend){
		sendToServer (server, msg, state._counters.getMeter (counterDef));
		servers.add (server);
	    }
	}
	return servers;
    }
    
    /********************* BEGIN GOGO COMMANDS **************/
    public boolean infoCommand (TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	Map info = (Map) map.get (client);
	if (_originH != null) info.put ("Origin-Host", _originH);
	if (destinations != null){
	    for (TcpClient.Destination dest : destinations){
		if (!dest.isOpen ()) continue;
		TcpChannel cnx = dest.getChannel ();
		PeerState state = cnx.attachment ();
		Counters counters = state._counters;
		info = (Map) map.get (dest);
		info.put ("PING", String.valueOf (state._ping));
		info.put ("AVAILABILITY", String.valueOf (state._availabilityFactor));
		for (Meter meter : counters.METERS_ALL)
		    info.put (Counters.getName (meter), String.valueOf (meter.getValue ()));
	    }
	}
	return true;
    }
    public boolean disconnectCommand (TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	if (_state == _activeState){
	    map.put ("System.out", this+" : disconnecting");
	    if (arg != null) _dprReasonCode = Integer.parseInt (arg);
	    _state.serverDPR (null, null);
	    return true;
	} else {
	    map.put ("System.out", this+" : Cannot disconnect - closing");
	    _client.close ();
	    return false;
	}
    }
    // updates a destination availability : open TODO : down + wait + close
    public boolean availabilityCommand (TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	if ("up".equalsIgnoreCase (arg)) arg = "100";
	else if ("down".equalsIgnoreCase (arg)) arg = "0";
	StringBuilder sb = new StringBuilder ();
	for (TcpClient.Destination dest : destinations){
	    if (!dest.isOpen ()) continue; // TODO handle this case ?
	    TcpChannel cnx = dest.getChannel ();
	    PeerState state = cnx.attachment ();
	    int n = Integer.parseInt (arg);
	    if (LOGGER.isInfoEnabled ())
		LOGGER.info ("Received admin request to update availability factor for "+dest+" : "+n);
	    sb.append (dest).append (" : set availability to : ").append (n).append ('\n');
	    map.put ("System.out", sb.toString ());
	    int old = state._availabilityFactor;
	    state._availabilityFactor = n;
	    if (state._destManager instanceof WeightedDestinationManager){
		((WeightedDestinationManager) state._destManager).update (dest, state._availabilityFactor);
	    } else {
		//TODO check the state
		if (n == 0){
		    state._destManager.remove (dest);
		} else if (n == 100){
		    if (old == 0 && // make sure it was unset
			state.isActive ()
			) 
			state._destManager.add (dest, n);
		}
	    }
	}
	return true;
    }
    /********************* END GOGO COMMANDS **************/

    private void checkLoadFactor (Destination server, DiameterMessage msg, boolean remove){
	if (!_useLoadFactor) return;
	byte[] bytes = msg.getBytes ();
	int[] index = DiameterMessage.indexOf (AVP_CODE_CONGESTION_LEVEL, LUCENT_VENDOR_ID,
					       bytes, 0, bytes.length, true);
	if (index == null) return;
	int off = index[0];
	int len = index[1];
	index = DiameterMessage.indexOf (AVP_CODE_LOAD_FACTOR, LUCENT_VENDOR_ID,
					 bytes, index[2], index[3], false);
	if (index != null){
	    if (index[3] != 4){
		LOGGER.warn (ClientContext.this+" : received invalid load factor from "+server+" : length is "+index[3]);
	    } else {
		int loadFactor = DiameterMessage.getIntValue (bytes, index[2], 4);
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (ClientContext.this+" : received Congestion info from : "+server+" : load Factor is : "+loadFactor);
		if (loadFactor < 0){
		    LOGGER.warn (ClientContext.this+" : received invalid load factor from "+server+" : "+loadFactor+" : setting it to 0");
		    loadFactor = 0;
		}
		if (loadFactor > 100){
		    LOGGER.warn (ClientContext.this+" : received invalid load factor from "+server+" : "+loadFactor+" : setting it to 100");
		    loadFactor = 100;
		}
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug (ClientContext.this+" : updating loadFactor for "+server+" : "+loadFactor);
		PeerState state = (PeerState) server.getChannel ().attachment ();
		state._availabilityFactor = 100 - loadFactor;
		((WeightedDestinationManager) state._destManager).update (server, state._availabilityFactor);
	    }
	}
	if (remove) msg.removeValue (off, len);
    }
    
    private DiameterMessage transformCERHostIPAddress (DiameterMessage cer){
	if (!_hostIPAddrTransformCER) return cer;
	int port = 0;
	if (_client instanceof TcpChannel){
	    port = ((TcpChannel)_client).getRemoteAddress ().getPort ();
	} else {
	    port = ((SctpChannel)_client).getRemotePort ();
	}
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (this+" : transformCERHostIPAddress : remote port="+port);
	int offset = 0;
	boolean skipHeader = true;
	loop : while (true){
	    byte[] data = cer.getBytes (); // always refresh the pointer
	    int length = data.length - offset;
	    if (length <= 0) break loop;
	    int[] index = DiameterMessage.indexOf (257, 0, data, offset, length, skipHeader);
	    if (skipHeader) skipHeader = false;
	    if (index == null) break loop;
	    try{
		if (index[3] < 2){
		    LOGGER.warn (this+" : found unexpected Host-IP-Address : avp length = "+index[3]+" : skipping");
		    continue loop;
		}
		if (data[index[2]] != 0){
		    LOGGER.warn (this+" : found unexpected Host-IP-Address : type="+(data[index[2]]&0xFF)+"/"+(data[index[2]+1]&0xFF)+" : skipping");
		    continue loop;
		}
		switch (data[index[2]+1]){
		case 1: // IPV4
		    if (index[3] != 6){
			LOGGER.warn (this+" : found illegal Host-IP-Address V4 : length="+index[3]+" : skipping");
			continue loop;
		    }
		    byte[] digested = digest (data, index[2]+2, 4, port);
		    byte[] value = new byte[23];
		    value[0] = 0;
		    value[1] = 0;
		    value[2] = 26; // length = 26
		    value[3] = 0;
		    value[4] = 2; // IPV6
		    System.arraycopy (digested, 0, value, 5, 16);
		    value[21] = 0;
		    value[22] = 0; // padding
		    cer.replaceValue (index[0]+5, 11, value, 0, value.length);
		    index[1] = 28; // update index[1] for offset shifting in finally block
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (this+" : replaced IPV4 address in Host-IP-Address");
		    continue loop;
		case 2: // IPV6
		    if (index[3] != 18){
			LOGGER.warn (this+" : found illegal Host-IP-Address V6 : length="+index[3]+" : skipping");
			continue loop;
		    }
		    value = digest (data, index[2]+2, 16, port);
		    System.arraycopy (value, 0, data, index[2]+2, 16);
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (this+" : replaced IPV6 address in Host-IP-Address");
		    continue loop;
		default:
		    LOGGER.warn (this+" : found unexpected Host-IP-Address : type="+(data[index[2]]&0xFF)+"/"+(data[index[2]+1]&0xFF)+" : skipping");
		    continue loop;
		}
	    }finally{
		offset = index[0] + index[1];
	    }
	}
	return cer;
    }
    private static byte[] digest (byte[] in, int off, int len, int port){
	try{
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    md.update (in, off, len);
	    md.update ((byte)(port >> 8));
	    md.update ((byte)port);
	    return md.digest ();
	}catch(Exception e){} // cannot happen with md5
	return new byte[16];
    }
    
    private DiameterMessage transformCEAHostIPAddress (DiameterMessage cea){
	if (_hostIPAddrTransformCEA.equalsIgnoreCase ("IGNORE")) return cea;
	List<InetAddress> ips = new ArrayList<> ();
	if (_client instanceof TcpChannel){
	    ips.add (((TcpChannel) _client).getLocalAddress ().getAddress ());
	} else {
	    try{
		Iterator<SocketAddress> it = ((SctpChannel) _client).getLocalAddresses ().iterator ();
		while (it.hasNext ())
		    ips.add (((InetSocketAddress)it.next ()).getAddress ());
	    }catch(Throwable t){
		LOGGER.warn (this+" : failed to transformCEAHostIPAddress : cannot list sctp addresses", t);
	    }
	}
	Object o = _props.get (DiameterLoadBalancer.CONF_CEA_HOST_IP_ADDRESS);
	if (o != null){
	    try{
		if (o instanceof String){
		    ips.add (InetAddress.getByName ((String)o));
		} else if (o instanceof List){
		    List<String> list = (List<String>)o;
		    for (String s: list) ips.add (InetAddress.getByName (s));
		}
	    }catch (Throwable t){
		LOGGER.warn (this+" : failed to transformCEAHostIPAddress : cannot parse configured addresses", t);
	    }
	}
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (this+" : transformCEAHostIPAddress : ips="+ips);
	int insertIndex = -1;
	if (_hostIPAddrTransformCEA.equalsIgnoreCase ("REPLACE")){
	    // remove existing HostIPAddresses
	    int offset = 0;
	    loop : while (true){
		byte[] data = cea.getBytes (); // always refresh the pointer
		int[] index = DiameterMessage.indexOf (257, 0, data, offset, data.length - offset, offset == 0);
		if (index == null) break;
		if (insertIndex == -1) insertIndex = index[0];
		cea.removeValue (index[0], index[1]);
		offset = index[0];
	    }
	}
	// if no HostIPAddresses removed : find the best place to insert the new HostIPAddresses
	if (insertIndex == -1){
	    int[] index = DiameterMessage.indexOf (296, 0, cea.getBytes (), 0, cea.getBytes ().length, true); // look for origin-realm
	    if (index != null) insertIndex = index[0] + index[1];
	    else insertIndex = cea.getBytes ().length;
	}
	for (InetAddress address : ips){
	    boolean ipv4 = (address instanceof Inet4Address); // else ipv6
	    byte[] addressData = address.getAddress ();
	    byte[] avp = new byte[ipv4 ? 16 : 28];
	    DiameterUtils.setIntValue (257, avp, 0);
	    DiameterUtils.setIntValue (ipv4 ? 14 : 26, avp, 4); // dont include padding
	    avp[4] = (byte) 0x40;
	    avp[9] = ipv4 ? (byte) 1 : (byte) 2;
	    System.arraycopy (addressData, 0, avp, 10, addressData.length);
	    cea.insertValue (insertIndex, avp, 0, avp.length);
	    insertIndex += avp.length;
	}
	return cea;
    }
    
    public boolean getClientInfo (Map info){
	info.put ("Origin-Host", _originH);
	info.put ("Origin-Realm", _originR);
	for (Meter meter : _clientState._counters.METERS_ALL){
	    info.put (Counters.getName (meter), String.valueOf (meter.getValue ()));
	}
	return true;
    }
    public boolean getClientAliases (List<String> aliases){
	if (_originH != null) aliases.add (_originH);
	if (_originR != null) aliases.add (_originR);
	return true;
    }
    public String dumpCER (){
	if (_cer == null){
	    return null;
	} else {
	    StringBuilder sb = new StringBuilder ();
	    return DiameterParser.toString (_cer, sb.append ("*** CER for ").append (_originH).append (" @ ").append (_client).append ('\n')).append ("\n***********").toString ();
	}
    }
}
