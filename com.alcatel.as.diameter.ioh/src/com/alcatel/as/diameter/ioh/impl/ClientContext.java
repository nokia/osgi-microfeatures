// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.nio.*;
import java.security.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import alcatel.tess.hometop.gateways.reactor.*;

import org.osgi.framework.*;

import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.diameter.ioh.impl.utils.*;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;
import com.alcatel.as.ioh.engine.*;

import com.alcatel.as.diameter.ioh.impl.DiameterIOHEngine.DiameterIOHTcpChannel;
import com.alcatel.as.diameter.ioh.impl.DiameterIOHEngine.DiameterMuxClient;

import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.service.metering2.*;

import static com.alcatel.as.diameter.ioh.impl.DiameterIOH.getBooleanProperty;
import static com.alcatel.as.diameter.ioh.impl.DiameterIOH.getIntProperty;

public class ClientContext<T extends AsyncChannel> implements DiameterIOHChannel {

    public static InetAddress ALL_V4, ALL_V6;
    static {try {
	    ALL_V4 = InetAddress.getByName ("0.0.0.0");
	    ALL_V6 = InetAddress.getByName ("0::0");
	}catch(Exception e){}} // cannot happen

    private Logger LOGGER;
    private DiameterIOHEngine _engine;
    private Object[] _attachment;
    private Map<String, Object> _props;
    private DiameterMessage _cer, _cea;
    private List<ClientRequest> _pendingClientReqs = new ArrayList<ClientRequest> ();
    private DiameterMessage _myAgentDwr, _myClientDwr, _myAgentDwa, _myClientDwa;
    private IOHChannel _client;
    private State _state;
    private DiameterIOHRouter _router;
    private long _clientReqTimeout, _dprTimeout, _closeOnDpaDelay, _closeOnDpaTimeout;
    private String _id, _originH, _originR;
    private DiameterUtils.Avp _myoriginH, _myoriginR;
    private Future _clientReqTimeoutFuture;
    private ClientState _clientState;
    private boolean _sendDpr;
    private int _dprReasonCode, _dprReasonCodeInternal;
    private String _hostIPAddrCEAPolicy, _hostIPAddrCEAContent, _hostIPAddrCERPolicy, _hostIPAddrCERContent;
    private BundleContext _osgi;
    private MeteringService _metering;
    private Map<MuxClient, AgentState> _agentStates = new HashMap<> ();
    private DiameterIOHMeters _diamMeters;
    private boolean _incoming, _tcp;
    private MuxClientMap _activeAgents = new MuxClientMap ();
    private byte[] _inbandSecId = null;
    private int _dwrMaxClient;
    private int _maxCERSize = -1, _maxAppSize = -1;
    private boolean _routeLocal;
    private boolean _upgradeToTLS, _upgradeToTLSRequired;
    
    protected ClientContext (String id, IOHChannel client, Map<String, Object> props, boolean tcp, boolean incoming){
	DiameterIOHEngine engine = client.getIOHEngine ();
	_engine = engine;
	_state = _initState;
	_id = id;
	LOGGER = Logger.getLogger ("as.diameter.ioh");
	_client = client;
	_props = props;
	_tcp = tcp;
	_incoming = incoming;
	_osgi = (BundleContext) _props.get ("system.osgi");
	_metering = (MeteringService) _props.get ("system.metering");
	_router = engine.getDiameterIOHRouter ();
	_routeLocal = getDiameterIOHEngine ().routeLocal ();
	_dwrMaxClient = getIntProperty (DiameterIOH.CONF_DWR_ATTEMPTS, props, engine.getDwrAttempts ());
	_maxCERSize = getIntProperty (DiameterIOH.CONF_CER_MAX_SIZE, props, engine.getCERMaxSize ());
	_maxAppSize = getIntProperty (DiameterIOH.CONF_APP_MAX_SIZE, props, engine.getAppMaxSize ());
	_clientState = new ClientState ();
	_sendDpr = getBooleanProperty (DiameterIOH.CONF_DPR, props, engine.getSendDpr ());
	_dprReasonCode = getIntProperty (DiameterIOH.CONF_DPR_REASON, props, engine.getDprReasonCode ());
	_dprReasonCodeInternal = getIntProperty (DiameterIOH.CONF_DPR_REASON_INTERNAL, props, engine.getDprReasonCodeInternal ());
	_dprTimeout = getIntProperty (DiameterIOH.CONF_TIMER_DPR, props, engine.getDprTimeout ());
	_clientReqTimeout = getIntProperty (DiameterIOH.CONF_TIMER_CLIENT_REQ, props, engine.getClientReqTimeout ());
	_closeOnDpaDelay = getIntProperty (DiameterIOH.CONF_DELAY_DPA_CLOSE, props, engine.getCloseOnDPADelay ());
	_closeOnDpaTimeout = getIntProperty (DiameterIOH.CONF_TIMEOUT_DPA_CLOSE, props, engine.getCloseOnDPATimeout ());
	_hostIPAddrCEAPolicy = (String) props.get (DiameterIOH.CONF_CEA_HOST_IP_ADDRESS_POLICY);
	if (_hostIPAddrCEAPolicy == null) _hostIPAddrCEAPolicy = engine.getCEAHostIPAddrPolicy ();
	_hostIPAddrCEAContent = (String) props.get (DiameterIOH.CONF_CEA_HOST_IP_ADDRESS_CONTENT);
	if (_hostIPAddrCEAContent == null) _hostIPAddrCEAContent = engine.getCEAHostIPAddrContent ();
	_hostIPAddrCERPolicy = (String) props.get (DiameterIOH.CONF_CER_HOST_IP_ADDRESS_POLICY);
	if (_hostIPAddrCERPolicy == null) _hostIPAddrCERPolicy = engine.getCERHostIPAddrPolicy ();
	_hostIPAddrCERContent = (String) props.get (DiameterIOH.CONF_CER_HOST_IP_ADDRESS_CONTENT);
	if (_hostIPAddrCERContent == null) _hostIPAddrCERContent = engine.getCERHostIPAddrContent ();
	_diamMeters = new DiameterIOHMeters (new StringBuilder ().append (_id).append (':').append (engine.fullName ()).toString (), "");
	_diamMeters.initClient (_metering, engine.getDiameterIOHMeters ());
	engine.fillAppMeters (_diamMeters, _diamMeters);
    }
    public String toString (){
	return new StringBuilder ().append (_id).append (_state).toString ();
    }
    protected void upgradeToSecure (boolean upgradable, boolean required){
	_upgradeToTLS = upgradable;
	_upgradeToTLSRequired = required;
    }
    protected void init (){
	if (_incoming)
	    _handshakingRState.enter (EVENT_STARTING);
	else
	    _handshakingIState.enter (EVENT_STARTING);
    }
    protected void setDprTimeout (long delay){ _dprTimeout = delay;}
    protected DiameterIOHMeters getDiameterIOHMeters (){ return _diamMeters;}
    protected DiameterIOHMeters getDiameterIOHMeters (MuxClient agent){ return ((DiameterMuxClient)agent).getDiameterIOHMeters ();}
    protected DiameterIOHEngine getDiameterIOHEngine (){ return (DiameterIOHEngine) _client.getIOHEngine ();}
    public String getOriginHost (){ return _originH;}
    public String getOriginRealm (){ return _originR;}

    public TYPE getType (){ return _tcp ? TYPE.TCP : TYPE.SCTP;}
    
    public AgentState getAgentState (MuxClient agent) { return _agentStates.get (agent);}
    public AgentState removeAgentState (MuxClient agent) { return _agentStates.remove (agent);}
    public boolean agentConnected (MuxClient agent, MuxClientState mcs){
	AgentState state = new AgentState (this, agent, mcs);
	_agentStates.put (agent, state);
	_state.agentConnected (agent, state);
	return true;
    }
    public boolean agentClosed (MuxClient agent){
	_state.agentClosed (agent, removeAgentState (agent));
	return true;
    }
    public boolean agentStopped (MuxClient agent){
	_state.agentStopped (agent, getAgentState (agent));
	return true;
    }
    public boolean agentUnStopped (MuxClient agent){
	_state.agentUnStopped (agent, getAgentState (agent));
	return true;
    }

    public boolean incoming (){ return _incoming;}
    public IOHChannel getIOHChannel (){ return _client;}
    public <T extends AsyncChannel> T getChannel (){ return _client.getChannel ();}
    public Logger getLogger (){ return _client.getLogger ();}
    public PlatformExecutor getPlatformExecutor (){ return _client.getPlatformExecutor ();}
    public DiameterUtils.Avp getIOHOriginHost (){ return _myoriginH;}
    public DiameterUtils.Avp getIOHOriginRealm (){ return _myoriginR;}

    // this is the public API for DiameterIOHRouter
    public boolean sendOut (boolean checkBuffer, DiameterMessage msg){
	if (_state.maySendOut (msg)) // only the API call checks the state - else the state is in charge of sending out properly
	    return sendToClient (checkBuffer, msg);
	return false;
    }
    public MuxClient pickAgent (String group, Object preferenceHint){
	MuxClientList list = _activeAgents.getMuxClientList (group);
	if (_routeLocal){
	    // local agent is picked like a remote agent
	    int size = list.sizeOfActive ();
	    int rnd = preferenceHint != null ? (preferenceHint.hashCode () & 0X7FFFFFFF)%size : ThreadLocalRandom.current ().nextInt (size);
	    if (rnd == 0){
		MuxClient client = list.pickLocalAgent (preferenceHint);
		if (client != null) return client;
	    }
	}
	return list.pick (preferenceHint);
    }
    public void attach (Object attachment){ _client.attach (attachment);}
    public <T> T attachment (){ return (T) _client.attachment ();}
    public MuxClientMap getActiveAgents (){ return _activeAgents;}
    public void close (){ close ("Connection closed by IOH"); }
    public void close (Object reason){
	_client.closeReason (reason);
	_client.close ();
    }
    public String getDiameterId (){ return _id; }

    public boolean isOpen (){ return _state == _activeState; };
    public Map<String, Object> getProperties (){ return _props; }
    public SimpleMonitorable getMonitorable (){ return _diamMeters;}
    
    private void sendToClient (DiameterMessage msg){
	sendToClient (true, msg);
    }
    private boolean sendToClient (boolean checkBuffer, DiameterMessage msg){
	if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : sending to client : "+msg);
	byte[] bytes = msg.getBytes ();
	if (_client.sendOut (null, null, checkBuffer, false, ByteBuffer.wrap (bytes))){
	    _diamMeters.incClientSendMeter (msg);
	    _engine.calcEngineLatency (msg);
	    return true;
	}
	return false;
    }

    // this is the public API call
    public boolean sendAgent (MuxClient agent, DiameterMessage msg, long sessionId){
	if (_state.maySendAgent (msg)) // only the API call checks the state - else the state is in charge of sending out properly
	    return sendAgent (agent, msg, msg.isRequest () ?
			      getDiameterIOHMeters (agent).getSendAppReqMeter (msg) :
			      getDiameterIOHMeters (agent).getSendAppRespMeter (msg),
			      sessionId);
	return false;
    }

    private boolean sendAgent (MuxClient agent, DiameterMessage msg, Meter meter){
	return sendAgent (agent, msg, meter, 0L);
    }
    private boolean sendAgent (MuxClient agent, DiameterMessage msg, Meter meter, long sessionId){
	if (agent != null){
	    // note : we may check if we already sent a dpa to the agent : but it is too costly for the benefit
	    //if (getAgentState (agent).getTerminatingState == TERMINATING_STATE.SENT_DPA) return false;
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : sending to agent : "+agent+" : "+msg+" sessionId="+sessionId);
	    if (msg.timestamp1 () == 0L ||
		!((DiameterMuxClient) agent).latencySupported ()
		){
		ByteBuffer buff = ByteBuffer.wrap (msg.getBytes ());
		_client.sendAgent (agent, null, false, sessionId, false, buff);
	    } else {
		ByteBuffer timestamp = ByteBuffer.wrap (msg.timestamp1B ());
		ByteBuffer buff = ByteBuffer.wrap (msg.getBytes ());
		_client.sendAgent (agent, null, false, sessionId, false, timestamp, buff);
	    }
	    meter.inc (1);
	    if (!msg.isRequest ())
		getDiameterIOHMeters (agent).incSendAppRespMeterByResult (msg);
	    return true;
	} else {
	    if (LOGGER.isDebugEnabled ()) LOGGER.debug (this+" : cannot send to agent : dropping : "+msg);
	    return false;
	}
    }

    public List<MuxClient> sendAgents (final DiameterMessage msg, final boolean activeAgentsOnly, boolean checkBuffer){
	final List<MuxClient> agents = new ArrayList<MuxClient> (_client.getAgents ().size ());
	MuxClientList.Iterator<Boolean> it = new MuxClientList.Iterator<Boolean> (){
		public Boolean next (MuxClient agent, Boolean ctx){
		    if (ctx == false) return Boolean.FALSE;
		    boolean doSend = true;
		    if (activeAgentsOnly){
			doSend = getAgentState (agent).isActive ();
		    }
		    if (doSend){
			if (checkBuffer){
			    boolean ok = _router.checkAgentOverload (agent, msg) == 0;
			    if (!ok) return Boolean.FALSE;
			}
			sendAgent (agent, msg, getDiameterIOHMeters (agent).getSendMeter (msg));
			agents.add (agent);
		    }
		    return Boolean.TRUE;
		}
	    };
	Boolean ok = (Boolean) _client.getAgents ().iterate (it, Boolean.TRUE);
	if (!ok){
	    //if (LOGGER.isInfoEnabled ())
	    LOGGER.warn (this+" : agent overload in control message : closing client connection");
	    close ("Agent overloaded in control message");
	}
	return agents;
    }
    
    public void clientMessage (DiameterMessage msg){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (this+" : received from client : "+msg);
	_clientState.alive ();
	if (msg.isRequest ()){
	    switch (msg.getType ()){
	    case Watchdog:
		_diamMeters._readDWRMeter.inc (1);
		_diamMeters._parent._readDWRMeter.inc (1);
		if (_maxCERSize > 0 && msg.getBytes ().length > _maxCERSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max DWR size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max DWR size exceeded");
		    return;
		}		
		_state.clientDWR (msg); return;
	    case Disconnection:
		_diamMeters._readDPRMeter.inc (1);
		_diamMeters._parent._readDPRMeter.inc (1);
		if (_maxCERSize > 0 && msg.getBytes ().length > _maxCERSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max DPR size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max DPR size exceeded");
		    return;
		}
		_state.clientDPR (msg); return;
	    case Capabilities:
		if (msg.getApplicationID () == 0){
		    _diamMeters._readCERMeter.inc (1);
		    _diamMeters._parent._readCERMeter.inc (1);
		} else {
		    _diamMeters._readCURMeter.inc (1);
		    _diamMeters._parent._readCURMeter.inc (1);
		}
		if (_maxCERSize > 0 && msg.getBytes ().length > _maxCERSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max CER size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max CER size exceeded");
		    return;
		}
		_state.clientCER (msg);	return;
	    case Application:
		_diamMeters.getReadAppReqMeter (msg).inc (1);
		_diamMeters._parent.getReadAppReqMeter (msg).inc (1);
		if (_maxAppSize > 0 && msg.getBytes ().length > _maxAppSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max App Request size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max application request size exceeded");
		    return;
		}
		_state.clientRequest (msg); return;
	    }
	} else {
	    switch (msg.getType ()){
	    case Watchdog:
		_diamMeters._readDWAMeter.inc (1);  // no state call for DWA : just update read timeout
		_diamMeters._parent._readDWAMeter.inc (1);
		return;
	    case Disconnection:
		_diamMeters._readDPAMeter.inc (1);
		_diamMeters._parent._readDPAMeter.inc (1);
		if (_maxCERSize > 0 && msg.getBytes ().length > _maxCERSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max DPA size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max DPA size exceeded");
		    return;
		}
		_state.clientDPA (msg);
		return;
	    case Capabilities:
		_diamMeters._readCEAMeter.inc (1);
		_diamMeters._parent._readCEAMeter.inc (1);
		if (_maxCERSize > 0 && msg.getBytes ().length > _maxCERSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max CEA size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max CEA size exceeded");
		    return;
		}
		_state.clientCEA (msg);
		return;
	    case Application:
		_diamMeters.getReadAppRespMeter (msg).inc (1);
		_diamMeters._parent.getReadAppRespMeter (msg).inc (1);
		_diamMeters.incReadAppRespMeterByResult (msg); // _parent handled in it
		if (_maxAppSize > 0 && msg.getBytes ().length > _maxAppSize){
		    if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : max App Response size exceeded : "+msg.getBytes ().length+" : closing");
		    close ("Max application response size exceeded");
		    return;
		}		
		_state.clientResponse (msg);
		return;
	    }
	}
    }
    
    public void clientTimeout (){
	if (_clientState.disabled ()) return; // we are in DPR exchange
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client inactivity detected");
	if (_clientState.timeout (getChannel (), "client"))
	    sendToClient (DiameterUtils.updateIdentifiers (_myClientDwr));
	else
	    _client.closeReason ("Inactivity timeout");
    }

    protected void clientClosed (){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : client closed");
	_state.clientClosed ();
    }

    protected void closeClient (Object reason){ // need to schedule in this exec, called from server exec
	getPlatformExecutor ().execute (() -> {
		if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : close client");
		_state.closeClient (reason);
	    });
    }

    protected void agentMessage (MuxClient agent, DiameterMessage msg){
	if (msg.isRequest ()){
	    switch (msg.getType ()){
	    case Disconnection:
		getDiameterIOHMeters (agent)._readDPRMeter.inc (1);
		_state.agentDPR (agent, msg); return;
	    case Capabilities:
		if (msg.getApplicationID () == 0L)
		    getDiameterIOHMeters (agent)._readCERMeter.inc (1);
		else
		    getDiameterIOHMeters (agent)._readCURMeter.inc (1);
		_state.agentCER (agent, msg); return;
	    case Application:
		getDiameterIOHMeters (agent).getReadAppReqMeter (msg).inc (1);
		_state.agentRequest (agent, msg); return;
	    case Watchdog:
		getDiameterIOHMeters (agent)._readDWRMeter.inc (1);
		_state.agentDWR (agent, msg);
		return;
	    }
	} else {
	    switch (msg.getType ()){
	    case Application:
		getDiameterIOHMeters (agent).getReadAppRespMeter (msg).inc (1);
		getDiameterIOHMeters (agent).incReadAppRespMeterByResult (msg);
		_state.agentResponse (agent, msg); return;
	    case Disconnection:
		getDiameterIOHMeters (agent)._readDPAMeter.inc (1);
		_state.agentDPA (agent, msg);return;
	    case Capabilities:
		if (msg.getApplicationID () == 0L)
		    getDiameterIOHMeters (agent)._readCEAMeter.inc (1);
		else
		    getDiameterIOHMeters (agent)._readCUAMeter.inc (1);
		_state.agentCEA (agent, msg); return;
	    case Watchdog:
		getDiameterIOHMeters (agent)._readDWAMeter.inc (1); // no state call for DWA
		LOGGER.warn (this+" : unexpected DWA from "+agent);
		return;
	    }
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
    private static final Event EVENT_QUORUM_LOST = new Event ("EVENT_QUORUM_LOST", Level.INFO);
    private static final Event EVENT_QUORUM_FAILED = new Event ("EVENT_QUORUM_FAILED", Level.WARN);
    private static final Event EVENT_CLEAN = new Event ("EVENT_CLEAN", Level.DEBUG);
    private static final Event EVENT_SENT_DPA_TO_CLIENT = new Event ("EVENT_SENT_DPA_TO_CLIENT", Level.INFO);
    private static final Event EVENT_SENT_DPA_TO_AGENT = new Event ("EVENT_SENT_DPA_TO_AGENT", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_REQUEST = new Event ("EVENT_UNEXPECTED_CLIENT_REQUEST", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_RESPONSE = new Event ("EVENT_UNEXPECTED_CLIENT_RESPONSE", Level.INFO);
    private static final Event EVENT_UNEXPECTED_AGENT_CER = new Event ("EVENT_UNEXPECTED_AGENT_CER", Level.WARN);
    private static final Event EVENT_UNEXPECTED_AGENT_CEA = new Event ("EVENT_UNEXPECTED_AGENT_CEA", Level.WARN);
    private static final Event EVENT_UNEXPECTED_AGENT_DPR = new Event ("EVENT_UNEXPECTED_AGENT_DPR", Level.WARN);
    private static final Event EVENT_UNEXPECTED_AGENT_DPA = new Event ("EVENT_UNEXPECTED_AGENT_DPA", Level.WARN);
    private static final Event EVENT_UNEXPECTED_AGENT_DWR = new Event ("EVENT_UNEXPECTED_AGENT_DWR", Level.WARN);
    private static final Event EVENT_UNEXPECTED_CLIENT_CER = new Event ("EVENT_UNEXPECTED_CLIENT_CER", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_CEA = new Event ("EVENT_UNEXPECTED_CLIENT_CEA", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DWR = new Event ("EVENT_UNEXPECTED_CLIENT_DWR", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DPR = new Event ("EVENT_UNEXPECTED_CLIENT_DPR", Level.INFO);
    private static final Event EVENT_UNEXPECTED_CLIENT_DPA = new Event ("EVENT_UNEXPECTED_CLIENT_DPA", Level.INFO);
    private static final Event EVENT_SENT_CEA_TO_CLIENT = new Event ("EVENT_SENT_CEA_TO_CLIENT", Level.DEBUG);
    private static final Event EVENT_SENT_CEA_ERROR__TO_CLIENT = new Event ("EVENT_SENT_CEA_ERROR__TO_CLIENT", Level.INFO);
    private static final Event EVENT_SENT_CEA_TO_AGENT = new Event ("EVENT_SENT_CEA_TO_AGENT", Level.DEBUG);
    private static final Event EVENT_RECEIVED_DPA_FROM_CLIENT = new Event ("EVENT_RECEIVED_DPA_FROM_CLIENT", Level.DEBUG);
    private static final Event EVENT_RECEIVED_CEA_ERROR_FROM_CLIENT = new Event ("EVENT_RECEIVED_CEA_ERROR_FROM_CLIENT", Level.INFO);
    private static final Event EVENT_ADMIN_DPR = new Event ("EVENT_ADMIN_DPR", Level.WARN);
    private static final Event EVENT_INVALID_INBAND_SECURITY = new Event ("EVENT_INVALID_INBAND_SECURITY", Level.INFO);
    
    private class State {
	protected void logEnter (Event event){
	    if (LOGGER.isEnabledFor (event.getLogLevel ()))
		LOGGER.log (event.getLogLevel (),
			    ClientContext.this+" : changing state : from : "+_state+" to : "+this+" event : "+event);
	}
	protected void enter (Event event){};
	protected boolean maySendOut (DiameterMessage msg){ return false;}
	protected boolean maySendAgent (DiameterMessage msg){ return false;}
	protected void clientClosed (){ _terminatedState.enter (EVENT_CLIENT_CLOSED); }
	protected void clientRequest (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_REQUEST); }
	protected void clientResponse (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_RESPONSE); }
	protected void clientCER (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_CER); }
	protected void clientCEA (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_CEA); }
	protected void clientDWR (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DWR); }
	protected void clientDPR (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DPR); }
	protected void clientDPA (DiameterMessage msg){ _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_DPA); }
	protected void closeClient (Object reason){ close (reason); }
	protected void agentConnected (MuxClient agent, AgentState state){}
	protected void agentClosed (MuxClient agent, AgentState state){}
	protected void agentStopped (MuxClient agent, AgentState state){ state.stopped ();}
	protected void agentUnStopped (MuxClient agent, AgentState state){ state.unstopped ();}
	protected void agentRequest (MuxClient agent, DiameterMessage msg){}
	protected void agentResponse (MuxClient agent, DiameterMessage msg){}
	protected void agentCER (MuxClient agent, DiameterMessage msg){_terminatedState.enter (EVENT_UNEXPECTED_AGENT_CER);}
	protected void agentCEA (MuxClient agent, DiameterMessage msg){_terminatedState.enter (EVENT_UNEXPECTED_AGENT_CEA);}
	protected void agentDWR (MuxClient agent, DiameterMessage msg){}
	protected void agentDPR (MuxClient agent, DiameterMessage msg){_terminatedState.enter (EVENT_UNEXPECTED_AGENT_DPR);}
	protected void agentDPA (MuxClient agent, DiameterMessage msg){_terminatedState.enter (EVENT_UNEXPECTED_AGENT_DPA);}
    }
    private State _initState = new State (){
	    public String toString (){ return "[Initial]";}
	};
    private State _handshakingRState = new State (){
	    protected boolean _sentToLocalAgents;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
	    }
	    protected void clientCER (final DiameterMessage msg){
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
		_myAgentDwr = DiameterUtils.makeDwr (msg); // if cer is crap, dwr is crap : consistent
		_myAgentDwa = DiameterUtils.makeDwa (msg);
		int inbandSecId = msg.getIntAvp (299, 0, -1);
		if (inbandSecId != -1){
		    _inbandSecId = new byte[12];
		    DiameterUtils.setIntValue (299, _inbandSecId, 0);
		    DiameterUtils.setIntValue (12, _inbandSecId, 4);
		    DiameterUtils.setIntValue (inbandSecId, _inbandSecId, 8);
		    _inbandSecId[4] = 0x40;
		}
		if (_upgradeToTLS){
		    if (inbandSecId == 1){
			// we will activate TLS after the CEA
			if (LOGGER.isDebugEnabled ()) LOGGER.debug (ClientContext.this+" : will Upgrade to TLS after handshake");
		    } else {
			if (_upgradeToTLSRequired){
			    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : Inband-Security-Id not set to 1 while required : closing");
			    _terminatedState.enter (EVENT_INVALID_INBAND_SECURITY);
			    return;
			}
			_upgradeToTLS = false;
		    }
		}
		_cer = msg;
		// check all sendBuffers first
		MuxClientList.Iterator<Boolean> itSB = new MuxClientList.Iterator<Boolean> (){
			public Boolean next (MuxClient agent, Boolean ctx){
			    return ctx && _router.checkAgentOverload (agent, msg) == 0;
			}
		    };
		boolean bufferok = (Boolean) _client.getAgents ().iterate (itSB, Boolean.TRUE);
		if (!bufferok){
		    LOGGER.warn (this+" : cannot handle CER : excessive buffer needed : "+msg.getBytes ().length);
		    close ("Not enough mux buffer to handle CER");
		    return;
		}
		int minremote = getDiameterIOHEngine ().getIntProperty (DiameterIOH.CONF_AGENT_REMOTE_MIN, 0);
		if (minremote > 0){
		    int remote = _client.getAgents ().sizeOfRemoteAgents (false);
		    if (remote < minremote){
			if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : not enough remote agents : "+remote+" : adding Disconnect Cause to CER");
			getDiameterIOHEngine ().getDiameterIOHMeters ()._errorNoRemoteReadCER.inc (1);
			_client.ignoreAgents ();
			// add a disconnect cause AVP (to mark the pb)
			byte[] avp = new byte[12];
			DiameterUtils.setIntValue (273, avp, 0);
			DiameterUtils.setIntValue (12, avp, 4);
			avp[4] = (byte) 0x40;
			DiameterUtils.setIntValue (1, avp, 8);
			_cer.insertValue (_cer.getBytes ().length, avp, 0, avp.length);
		    }
		}
		if (_routeLocal){
		    sendCERToLocalAgents ();
		    sendCERToRemoteAgents ();
		} else {
		    int sentMuxAgents = sendCERToRemoteAgents ();
		    if (sentMuxAgents == 0)
			sendCERToLocalAgents ();
		}
	    }
	    protected void agentConnected (MuxClient agent, AgentState state){
		if (_cer != null){
		    sendAgent (agent, _cer, getDiameterIOHMeters (agent)._writeCERMeter);
		}
	    }
	    protected void agentClosed (MuxClient agent, AgentState state){
		state.closed ();
		if (_client.getAgents ().size () == 0){
		    _terminatedState.enter (EVENT_QUORUM_FAILED);
		} else {
		    if (_sentToLocalAgents == false &&
			_cer != null &&
			_client.getAgents ().sizeOfRemoteAgents (false) == 0)
			sendCERToLocalAgents ();
		}
	    }
	    protected void agentCEA (MuxClient agent, DiameterMessage msg){
		int result = msg.getResultCode ();
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (ClientContext.this+" : agentCEA : result-code="+result);
		if (result != 2001){
		    if (!_sentToLocalAgents)
			sendCERToLocalAgents (); // this is useless, but csd manages a counter on this
		    sendToClient (transformCEHostIPAddress (msg, false)); // not sure if needed but lets be coherent
		    _terminatedState.enter (EVENT_SENT_CEA_ERROR__TO_CLIENT);
		    return;
		}
		AgentState state = getAgentState (agent);
		state.active ();
		_myClientDwr = DiameterUtils.makeDwr (msg);
		_myClientDwa = DiameterUtils.makeDwa (msg);
		if (!_sentToLocalAgents)
		    sendCERToLocalAgents ();
		if (_upgradeToTLS)
		    setInbandSecurityToTLS (msg);
		sendToClient (transformCEHostIPAddress (msg, false));
		if (_upgradeToTLS)
		    upgradeToSecure ();
		byte[] avp = msg.getAvp (264, 0);
		_myoriginH = new DiameterUtils.Avp (264, 0, true, avp);
		avp = msg.getAvp (296, 0);
		_myoriginR = new DiameterUtils.Avp (296, 0, true, avp);
		_activeState.enter (EVENT_SENT_CEA_TO_CLIENT);
	    }
	    protected int sendCERToRemoteAgents (){
		MuxClientList.Iterator it = new MuxClientList.Iterator (){
			public Object next (MuxClient agent, Object ctx){
			    if (agent.isLocalAgent ()){
				return ctx;
			    } else {
				sendAgent (agent, _cer, getDiameterIOHMeters (agent).getSendMeter (_cer));
				return ((Integer)ctx)+1;
			    }
			}
		    };
		return (Integer) _client.getAgents ().iterate (it, 0);
	    }
	    protected void sendCERToLocalAgents (){
		MuxClientList.Iterator it = new MuxClientList.Iterator (){
			public Object next (MuxClient agent, Object ctx){
			    if (agent.isLocalAgent ())
				sendAgent (agent, _cer, getDiameterIOHMeters (agent).getSendMeter (_cer));
			    return null;
			}
		    };
		_client.getAgents ().iterate (it, null);
		_sentToLocalAgents = true;
	    }
	    public String toString (){ return "[HandshakingR]";}
	};
    private State _handshakingIState = new State (){
	    Map<MuxClient, DiameterMessage> _cers;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
	    }
	    protected void agentCER (MuxClient agent, DiameterMessage msg){
		if (_cers == null){
		    _cers = new HashMap<> ();
		    _myClientDwr = DiameterUtils.makeDwr (msg);
		    _myClientDwa = DiameterUtils.makeDwa (msg);
		    byte[] avp = msg.getAvp (264, 0);
		    _myoriginH = new DiameterUtils.Avp (264, 0, true, avp);
		    avp = msg.getAvp (296, 0);
		    _myoriginR = new DiameterUtils.Avp (296, 0, true, avp);
		    if (_upgradeToTLS)
			setInbandSecurityToTLS (msg);
		    sendToClient (transformCEHostIPAddress (msg, true));
		}
		_cers.put (agent, msg);
	    }
	    protected void clientCEA (DiameterMessage msg){
		if (_cers == null){
		    _terminatedState.enter (EVENT_UNEXPECTED_CLIENT_CEA);
		    return;
		}
		int result = msg.getResultCode ();
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info (ClientContext.this+" : clientCEA : result-code="+result);
		boolean success = (result == 2001);
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
		if (success){
		    _myAgentDwr = DiameterUtils.makeDwr (msg); // if cea is crap, dwr is crap : consistent
		    _myAgentDwa = DiameterUtils.makeDwa (msg);
		    int inbandSecId = msg.getIntAvp (299, 0, -1);
		    if (inbandSecId != -1){
			_inbandSecId = new byte[12];
			DiameterUtils.setIntValue (299, _inbandSecId, 0);
			DiameterUtils.setIntValue (12, _inbandSecId, 4);
			DiameterUtils.setIntValue (inbandSecId, _inbandSecId, 8);
			_inbandSecId[4] = 0x40;
		    }
		    if (_upgradeToTLS){
			if (inbandSecId == 1){
			    upgradeToSecure ();
			} else {
			    if (_upgradeToTLSRequired){
				if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : Inband-Security-Id not set to 1 while required : closing");
				_terminatedState.enter (EVENT_INVALID_INBAND_SECURITY);
				return;
			    }			
			    _upgradeToTLS = false;
			}
		    }
		}
		_cea = msg;
		for (MuxClient agent : _cers.keySet ()){
		    DiameterMessage cer = _cers.get (agent);
		    sendAgent (agent, DiameterUtils.cloneCea (_cea, cer), getDiameterIOHMeters (agent)._writeCEAMeter);
		    AgentState state = getAgentState (agent);
		    state.active ();
		}
		_cers = null;
		if (success){
		    _activeState.enter (EVENT_SENT_CEA_TO_AGENT);
		}else{
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : received non-2001 CEA : closing");
		    _terminatedState.enter (EVENT_RECEIVED_CEA_ERROR_FROM_CLIENT);
		}
	    }
	    protected void agentClosed (MuxClient agent, AgentState state){
		state.closed ();
		if (_client.getAgents ().size () == 0){
		    _terminatedState.enter (EVENT_QUORUM_FAILED);
		} else {
		    if (_cers != null)
			_cers.remove (agent);
		}
	    }
	    public String toString (){ return "[HandshakingI]";}
	};
    private State _activeState = new State (){
	    protected int _dprReasonCodeFromAgent = -1;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientReqTimeoutFuture = getPlatformExecutor ().scheduleAtFixedRate (_clientReqTimeoutTask,
										      _clientReqTimeout,
										      _clientReqTimeout,
										      java.util.concurrent.TimeUnit.MILLISECONDS);
		_diamMeters.setDescription (new StringBuilder ().append (_originH).append ('@').append (_originR).toString ());
		_router.clientOpened (ClientContext.this);
		_diamMeters.start (_osgi); // the router may add some meters
		getDiameterIOHEngine ().diameterChannelOpened (_tcp, _incoming);
		_clientState.active (_dwrMaxClient);
	    }
	    protected boolean maySendOut (DiameterMessage msg){ return true;}
	    protected boolean maySendAgent (DiameterMessage msg){ return true;}
	    protected void clientRequest (DiameterMessage msg){ _router.doClientRequest (ClientContext.this, msg); }
	    protected void clientResponse (DiameterMessage msg){ _router.doClientResponse (ClientContext.this, msg); }	
	    protected void clientCER (DiameterMessage msg){
		_cer = msg;
		if (_cer.getApplicationID () != 0L){
		    // this is a CUR : make it a CER
		    byte[] bytes = msg.getBytes ();
		    if (_inbandSecId != null)
			msg.insertValue (bytes.length, _inbandSecId, 0, _inbandSecId.length);
		    // replace appId & code
		    msg.updateCommand (0, 257);
		}
		_pendingClientReqs.add (new ClientRequest (msg, 1));
		sendAgents (msg, true, true);
	    }
	    protected void clientCEA (DiameterMessage msg){
		 // handle as a request for now
		clientResponse (msg);
	    }
	    protected void clientDWR (DiameterMessage msg){
		sendToClient (DiameterUtils.makeDwa (msg, _myClientDwa));
	    }
	    protected void clientDPR (DiameterMessage msg){
		_terminatingByClientState.enter (new Event ("EVENT_RECEIVED_CLIENT_DPR", Level.INFO, msg));
	    }
	    protected void agentConnected (MuxClient agent, AgentState state){
		if (_incoming) sendAgent (agent, _cer, getDiameterIOHMeters (agent)._writeCERMeter);
	    }
	    protected void agentClosed (MuxClient agent, AgentState state){
		state.closed ();
		if (_client.getAgents ().size () == 0){
		    if (_sendDpr){
			sendToClient (DiameterUtils.makeDpr (_myClientDwr, _dprReasonCodeFromAgent != -1 ? _dprReasonCodeFromAgent : _dprReasonCode));
			_terminatingByAgentState.enter (EVENT_QUORUM_LOST);
		    } else {
			_terminatedState.enter (EVENT_QUORUM_LOST);
		    }
		}
	    }
	    protected void agentDWR (MuxClient agent, DiameterMessage msg){
		sendAgent (agent, DiameterUtils.makeDwa (msg, _myAgentDwa), getDiameterIOHMeters (agent)._writeDWAMeter);
	    }
	    protected void agentCER (MuxClient agent, DiameterMessage msg){
		 AgentState state = getAgentState (agent);
		 if (state.isActive ()){
		     // handle as a request for now
		     agentRequest (agent, msg);
		 } else { // normally the agent cannot be terminating
		     // incoming = false
		     sendAgent (agent, DiameterUtils.cloneCea (_cea, msg), getDiameterIOHMeters (agent)._writeCEAMeter);
		     state.active ();
		     if (_cer != null){
			 sendAgent (agent, _cer, getDiameterIOHMeters (agent)._writeCERMeter);
		     }
		 }
	    }
	    protected void agentCEA (MuxClient agent, DiameterMessage msg){
		AgentState state = getAgentState (agent);
		if (state.isActive () == false){
		    state.active ();
		    if (!msg.isTwin (_cer)){
			sendAgent (agent, _cer, getDiameterIOHMeters (agent)._writeCERMeter);
		    }
		}
		ClientRequest ctx = getClientRequest (msg);
		if (ctx == null || ctx.ping () == false) return;
		_pendingClientReqs.remove (ctx);
		if (ctx._request.getApplicationID () == 0L) // this was a CUR
		    msg.updateCommand (10, 328);
		sendToClient (transformCEHostIPAddress (msg, false));
	    }
	    protected void agentRequest (MuxClient agent, DiameterMessage msg){
		_router.doAgentRequest (ClientContext.this, agent, msg);
	    }
	    protected void agentResponse (MuxClient agent, DiameterMessage msg){
		_router.doAgentResponse (ClientContext.this, agent, msg);
	    }
	    protected void agentDPR (final MuxClient agent, DiameterMessage dpr){
		// we handle the case where agent == null : this is an admin command to disconnect
		if (agent != null){
		    _dprReasonCodeFromAgent = dpr.getIntAvp (273, 0, -1);
		    AgentState state = getAgentState (agent);
		    sendAgent (agent, DiameterUtils.makeDpa (_myAgentDwr, dpr), getDiameterIOHMeters (agent)._writeDPAMeter);
		    state.closed ();
		} else {
		    DiameterMessage newDpr = DiameterUtils.makeDpr (_myClientDwr,_dprReasonCode);
		    sendToClient (newDpr);
		    _terminatingByAgentState.enter (EVENT_ADMIN_DPR);
		}
	    }
	    protected void closeClient (Object reason){
		sendToClient (DiameterUtils.makeDpr (_myClientDwr, _dprReasonCodeFromAgent != -1 ? _dprReasonCodeFromAgent : _dprReasonCode));
		if (_dprReasonCodeInternal != -1){
		    DiameterMessage dpr = DiameterUtils.makeDpr (_myClientDwr, _dprReasonCodeInternal);		    
		    sendAgents (dpr, true, false);
		}
		_terminatingByAgentState.enter (EVENT_CLEAN);
	    }
	    public String toString (){
		return "[Active]";
	    }
	};
    private State _terminatingByClientState = new State (){
	    private DiameterMessage _dpr, _dpa;
	    private int _nbDpa = 0;
	    private Future _cleaningFuture;
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientState.disable ();
		_client.ignoreAgents ();
		for (MuxClient agent : sendAgents (_dpr = (DiameterMessage)event.attachment(), true, true)){
		    AgentState state = getAgentState (agent);
		    state.terminating ();
		    _nbDpa++;
		}
		Runnable r = new Runnable (){
			public void run (){
			    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture triggered : "+_dpa);
			    if (_dpa != null){
				sendToClient (_dpa);
				_terminatedState.enter (EVENT_SENT_DPA_TO_CLIENT);
			    } else {
				_terminatedState.enter (new Event ("EVENT_NO_DPA_FROM_AGENT", Level.INFO));
			    }
			}
		    };
		_cleaningFuture = getPlatformExecutor ().schedule (r, _dprTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	    }
	    protected boolean maySendOut (DiameterMessage msg){ return true;}
	    protected boolean maySendAgent (DiameterMessage msg){ return true;}
	    private void processAgentDPA (DiameterMessage dpa){ // dpa is null in case of agent close
		if (dpa != null) _dpa = dpa; // we store the DPA
		if (--_nbDpa == 0){
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture cancelled");
		    _cleaningFuture.cancel (false);
		    if (_dpa != null){
			sendToClient (_dpa);
			_terminatedState.enter (EVENT_SENT_DPA_TO_CLIENT);
		    } else {
			_terminatedState.enter (new Event ("EVENT_NO_DPA_FROM_AGENT", Level.INFO));
		    }
		}
	    }
	    protected void clientClosed (){
		if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : clientFuture cancelled");
		_cleaningFuture.cancel (false);
		_terminatedState.enter (EVENT_CLIENT_CLOSED);
	    }
	    protected void agentClosed (MuxClient agent, AgentState state){
		if (state.isTerminating ()){
		    if (LOGGER.isInfoEnabled ()) LOGGER.info (ClientContext.this+" : agent closed in state WAITING_DPA");
		    processAgentDPA (null);
		}
		state.closed ();
	    }
	    protected void agentCER (MuxClient agent, DiameterMessage msg){} // ignore
	    protected void agentCEA (MuxClient agent, DiameterMessage msg){
		AgentState state = getAgentState (agent);
		if (state.isActive () == false){
		    // the agent is just joining - we know _incoming = true
		    state.active ();
		    sendAgent (agent, _dpr, getDiameterIOHMeters (agent)._writeDPRMeter);
		    state.terminating ();
		    _nbDpa++;
		}
		ClientRequest ctx = getClientRequest (msg);
		if (ctx == null || ctx.ping () == false) return;
		_pendingClientReqs.remove (ctx);
		sendToClient (transformCEHostIPAddress (msg, false));
	    }
	    protected void agentRequest (MuxClient agent, DiameterMessage msg){
		_router.doAgentRequest (ClientContext.this, agent, msg);
	    }
	    protected void agentResponse (MuxClient agent, DiameterMessage msg){
		_router.doAgentResponse (ClientContext.this, agent, msg);
	    }
	    protected void agentDPR (MuxClient agent, DiameterMessage msg){} // ignored
	    protected void agentDPA (MuxClient agent, DiameterMessage msg){
		AgentState state = getAgentState (agent);
		state.closed ();
		// TODO ? notifyClose to agent ? if yes, it will call agentClosed --> do it before processAgentDPA which may change _state
		// but routing code should then be checked to avoid also sending to the agent (for ex on client response)
		processAgentDPA (msg);
	    }
	    protected void clientRequest (DiameterMessage msg){
		_router.doClientRequest (ClientContext.this, msg); // not expected though
	    }
	    protected void clientResponse (DiameterMessage msg){
		_router.doClientResponse (ClientContext.this, msg);
	    }
	    protected void clientCER (DiameterMessage msg){} // ignored
	    protected void clientCEA (DiameterMessage msg){} // ignored
	    protected void clientDWR (DiameterMessage msg){
		sendToClient (DiameterUtils.makeDwa (msg, _myClientDwa));
	    }
	    protected void clientDPR (DiameterMessage msg){} // ignored
	    protected void clientDPA (DiameterMessage msg){} // ignored
	    protected void closeClient (Object reason){} // ignored
	    public String toString (){ return "[TerminatingByClient]"; }
	};
    
    private State _terminatingByAgentState = new State (){
	    private Future _cleaningFuture;
	    private Future _closeFuture; // may be null
	    protected void enter (Event event){
		logEnter (event);
		_state = this;
		_clientState.disable ();
		_client.ignoreAgents ();
		Runnable r = new Runnable (){
			public void run (){
			    _terminatedState.enter (new Event ("EVENT_NO_DPA_FROM_CLIENT", Level.INFO));
			}
		    };
		_cleaningFuture = getPlatformExecutor ().schedule (r, _dprTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	    }
	    protected boolean maySendOut (DiameterMessage msg){ return !msg.isRequest ();} // not sure if allowed...
	    protected boolean maySendAgent (DiameterMessage msg){ return true;}
	    protected void clientClosed (){
		_cleaningFuture.cancel (false);
		if (_closeFuture != null) _closeFuture.cancel (false);
		_terminatedState.enter (EVENT_CLIENT_CLOSED);
	    }
	    protected void agentClosed (MuxClient agent, AgentState state){
		state.closed ();
	    }
	    protected void agentRequest (MuxClient agent, DiameterMessage msg){} // cannot send it
	    protected void agentResponse (MuxClient agent, DiameterMessage msg){
		// not sure if allowed....
		_router.doAgentResponse (ClientContext.this, agent, msg);
	    }
	    protected void agentCER (MuxClient agent, DiameterMessage msg){} // ignore for now
	    protected void agentCEA (MuxClient agent, DiameterMessage msg){} // ignore for now
	    protected void agentDPR (MuxClient agent, DiameterMessage msg){} // ignore for now
	    protected void agentDPA (MuxClient agent, DiameterMessage msg){} // ignore (a DPR may have been sent to it)
	    protected void agentDWR (MuxClient agent, DiameterMessage msg){
		sendAgent (agent, DiameterUtils.makeDwa (msg, _myAgentDwa), getDiameterIOHMeters (agent)._writeDWAMeter);
	    }
	    protected void clientRequest (DiameterMessage msg){
		_router.doClientRequest (ClientContext.this, msg);
	    }
	    protected void clientResponse (DiameterMessage msg){
		_router.doClientResponse (ClientContext.this, msg);
	    }
	    protected void clientCER (DiameterMessage msg){}
	    protected void clientCEA (DiameterMessage msg){}
	    protected void clientDWR (DiameterMessage msg){
		sendToClient (DiameterUtils.makeDwa (msg, _myClientDwa));
	    }
	    protected void clientDPR (DiameterMessage msg){} // ignore it
	    protected void clientDPA (DiameterMessage msg){
		_cleaningFuture.cancel (false);
		if (_closeOnDpaDelay > 0L){
		    Runnable r = new Runnable (){
			    public void run (){
				_terminatedState.enter (EVENT_RECEIVED_DPA_FROM_CLIENT);
			    }
			};
		    _closeFuture = getPlatformExecutor ().schedule (r, _closeOnDpaDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
		} else {
		    _terminatedState.enter (EVENT_RECEIVED_DPA_FROM_CLIENT);
		}
	    }
	    protected void closeClient (Object reason){} // ignored
	    public String toString (){ return "[TerminatingByAgent]"; }
	};
    private State _terminatedState = new State (){
	    private Future _clientCloseFuture;
	    public void enter (Event event){
		if (_state == this) return; // make it idempotent for cleaning and for closed callbacks
		_client.ignoreAgents ();
		boolean wasActive = (_state == _activeState ||
				     _state == _terminatingByClientState ||
				     _state == _terminatingByAgentState);
		logEnter (event);
		_state = this;
		if (event != EVENT_CLIENT_CLOSED && event != EVENT_SENT_DPA_TO_CLIENT) close (event);
		if (event == EVENT_SENT_DPA_TO_CLIENT){
		    Runnable r = new Runnable (){
			    public void run (){
				close ("CLOSE_ON_DPA Timeout");
				_clientCloseFuture = null;
			    }};
		    _clientCloseFuture = getPlatformExecutor ().schedule (r, _closeOnDpaTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
		_diamMeters.stop ();
		if (wasActive){
		    getDiameterIOHEngine ().diameterChannelClosed (_tcp, _incoming);
		    _clientReqTimeoutFuture.cancel (false);
		    _router.clientClosed (ClientContext.this);
		}
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
	    protected void clientCEA (DiameterMessage msg){}
	    protected void clientDWR (DiameterMessage msg){}
	    protected void clientDPR (DiameterMessage msg){}
	    protected void clientDPA (DiameterMessage msg){}
	    protected void agentClosed (MuxClient agent, AgentState state){}
	    protected void agentRequest (MuxClient agent, DiameterMessage msg){}
	    protected void agentResponse (MuxClient agent, DiameterMessage msg){}
	    protected void agentCER (MuxClient agent, DiameterMessage msg){}
	    protected void agentCEA (MuxClient agent, DiameterMessage msg){}
	    protected void agentDPR (MuxClient agent, DiameterMessage msg){}
	    protected void agentDPA (MuxClient agent, DiameterMessage msg){}
	    protected void agentDWR (MuxClient agent, DiameterMessage msg){}
	    protected void closeClient (Object reason){}
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
    
    private class ClientState {
	private boolean _active, _disabled;
	private int _ping, _max = 0;
	private ClientState (){
	}
	private void active (int max){
	    _max = max;
	    _active = true;
	}
	private void disable (){ // deactivate all monitoring
	    _disabled = true;
	}
	private boolean disabled (){ return _disabled; }
	private void alive (){ _ping = 0; }
	private boolean timeout (AsyncChannel channel, Object target){
	    if (++_ping > _max){
		LOGGER.warn (ClientContext.this+" : "+target+" inactivity timeout");
		channel.shutdown ();
		return false;
	    }
	    return _active;
	}
    }
    
    private DiameterMessage transformCEHostIPAddress (DiameterMessage msg, boolean isCer){
	if (isCer){
	    if (_hostIPAddrCERPolicy.equalsIgnoreCase ("IGNORE")) return msg;
	} else
	    if (_hostIPAddrCEAPolicy.equalsIgnoreCase ("IGNORE")) return msg;
	Set<InetAddress> ips = new HashSet<> ();
	boolean endpointContent = isCer ?
	    _hostIPAddrCERContent.equalsIgnoreCase ("ENDPOINT"):
	    _hostIPAddrCEAContent.equalsIgnoreCase ("ENDPOINT");
	boolean groupContent = isCer ?
	    _hostIPAddrCERContent.equalsIgnoreCase ("GROUP"):
	    _hostIPAddrCEAContent.equalsIgnoreCase ("GROUP");
	if (isCer){
	    Object o = _props.get (DiameterIOH.CONF_CER_HOST_IP_ADDRESS);
	    if (o != null){
		try{
		    if (o instanceof String){
			ips.add (InetAddress.getByName ((String)o));
		    } else if (o instanceof List){
			List<String> list = (List<String>)o;
			for (String s: list) ips.add (InetAddress.getByName (s));
		    }
		}catch (Throwable t){
		    LOGGER.warn (this+" : failed to transformCEHostIPAddress : cannot parse configured addresses", t);
		}
	    }
	}else{
	    boolean manualContent = _hostIPAddrCEAContent.equalsIgnoreCase ("MANUAL");
	    if (!manualContent){
		String[] endpointIps = (String[]) _props.get (DiameterIOHEngine.ENDPOINT_ADDRESS);
		if (endpointIps != null){
		    try{
			for (String s: endpointIps) ips.add (InetAddress.getByName (s));
		    }catch (Throwable t){
			LOGGER.warn (this+" : failed to transformCEHostIPAddress : invalid address", t);
		    }
		}
	    }
	    for (Map<String, Object> props : endpointContent ? new Map[]{_props} :
		     new Map[]{_props, getDiameterIOHEngine ().getProperties ()}){
		Object o = props.get (DiameterIOH.CONF_CEA_HOST_IP_ADDRESS);
		if (o != null){
		    try{
			if (o instanceof String){
			    ips.add (InetAddress.getByName ((String)o));
			} else if (o instanceof List){
			    List<String> list = (List<String>)o;
			    for (String s: list) ips.add (InetAddress.getByName (s));
			}
		    }catch (Throwable t){
			LOGGER.warn (this+" : failed to transformCEHostIPAddress : cannot parse configured addresses", t);
		    }
		}
	    }
	}
	if (groupContent){
	    for (InetAddress addr : getDiameterIOHEngine ().getHostIPAddresses ())
		ips.add (addr);
	}
	// handle 0.0.0.0 case
	if (ips.remove (ALL_V4) | ips.remove (ALL_V6)){ // note | and not ||
	    try{
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
		    NetworkInterface ni = interfaces.nextElement();
		    Enumeration<InetAddress> addresses = ni.getInetAddresses();
		    while (addresses.hasMoreElements()) {
			InetAddress address = addresses.nextElement();
			ips.add(address);
		    }
		}
	    }catch(Exception e){
		LOGGER.warn (this+" : failed to resolve 0.0.0.0", e);
	    }
	}
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (this+" : transformCEHostIPAddress : ips="+ips);
	int insertIndex = -1;
	if (_hostIPAddrCEAPolicy.equalsIgnoreCase ("REPLACE")){
	    // remove existing HostIPAddresses
	    int offset = 0;
	    loop : while (true){
		byte[] data = msg.getBytes (); // always refresh the pointer
		int[] index = DiameterMessage.indexOf (257, 0, data, offset, data.length - offset, offset == 0);
		if (index == null) break;
		if (insertIndex == -1) insertIndex = index[0];
		msg.removeValue (index[0], index[1]);
		offset = index[0];
	    }
	}
	// if no HostIPAddresses removed : find the best place to insert the new HostIPAddresses
	if (insertIndex == -1){
	    int[] index = DiameterMessage.indexOf (296, 0, msg.getBytes (), 0, msg.getBytes ().length, true); // look for origin-realm
	    if (index != null) insertIndex = index[0] + index[1];
	    else insertIndex = msg.getBytes ().length;
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
	    msg.insertValue (insertIndex, avp, 0, avp.length);
	    insertIndex += avp.length;
	}
	return msg;
    }

    public void setInbandSecurityToTLS (DiameterMessage msg){
	int[] inbandSecAVP = msg.indexOf (299, 0);
	if (inbandSecAVP == null){
	    byte[] avp = new byte[12];
	    DiameterUtils.setIntValue (299, avp, 0);
	    DiameterUtils.setIntValue (12, avp, 4);
	    avp[4] = (byte) 0x40; // TODO check the flags
	    DiameterUtils.setIntValue (1, avp, 8);
	    msg.insertValue (msg.getBytes ().length, avp, 0, avp.length);
	} else {
	    DiameterUtils.setIntValue (1, msg.getBytes (), inbandSecAVP[2]);
	}
    }

    public void upgradeToSecure (){
	_client.history ("Upgrade to TLS");
	if (LOGGER.isInfoEnabled ())
	    LOGGER.info (ClientContext.this+" : Upgrade to TLS");
	((TcpChannel) _client.getChannel ()).upgradeToSecure ();
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
