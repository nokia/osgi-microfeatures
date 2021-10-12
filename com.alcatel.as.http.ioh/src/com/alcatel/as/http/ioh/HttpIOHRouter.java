// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.service.concurrent.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

public class HttpIOHRouter {

    protected Logger _logger;

    protected HttpIOHRouterFactory _factory;
    
    public static final int PRIORITY_INITIAL = 0;
    public static final int PRIORITY_SUBSEQUENT = 1;
    public static final int PRIORITY_RESPONSE = 2;
    public static final int PRIORITY_BODY = 4;
    public ChannelWriter.SendBufferMonitor CHANNEL_WRITER_REMOTE_AGENT, CHANNEL_WRITER_REMOTE_IOH, CHANNEL_WRITER_LOCAL_AGENT;

    protected Meter _routedReqMeter, _overloadMeter, _respondMeter, _respondOverloadMeter, _unavailableMeter, _respondUnavailableMeter, _notFoundMeter, _respondNotFoundMeter;
    protected Error _errorOverload, _errorUnavailable, _errorNotFound;
    
    protected boolean _test = false;
    protected boolean _warning = false;
    protected TimerService _timerService;
    protected byte[] _retryAfterHeader, _serverHeader;
    protected String _toString;
    protected int _agentLoadSelect = 2;
    
    public HttpIOHRouter (HttpIOHRouterFactory factory, Logger logger){
	_factory = factory;
	_logger = logger;
	_timerService = factory.getTimerService ();
	_toString = new StringBuilder ().append ("HttpIOHRouter[").append ('-').append (']').toString ();
    }

    @Override
    public String toString (){
	return _toString;
    }
    
    /************************* The public methods called by the HttpIOH ********************************/
    
    public void init (IOHEngine engine){
	_toString = new StringBuilder ().append ("HttpIOHRouter[").append (engine.name ()).append (']').toString ();
	Map<String, Object> props = engine.getProperties ();
	_test = getBooleanProperty ("http.ioh.router.test", props, false);
	
	CHANNEL_WRITER_REMOTE_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("http.ioh.router.mux.writer.lowWM", props, 1000000),
												    getIntProperty ("http.ioh.router.mux.writer.highWM", props, 2000000),
												    5);
	CHANNEL_WRITER_REMOTE_IOH = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("http.ioh.router.remote.writer.lowWM", props, 5000000),
												  getIntProperty ("http.ioh.router.remote.writer.highWM", props, 10000000),
												  5);
	CHANNEL_WRITER_LOCAL_AGENT = new ChannelWriter.ProgressiveSendBufferMonitorWithPriorities (getIntProperty ("http.ioh.router.local.writer.lowWM", props, 10000),
												   getIntProperty ("http.ioh.router.local.writer.highWM", props, 20000),
												   5);
	_warning = getBooleanProperty ("http.ioh.router.Warning", props, false);
	_retryAfterHeader = getStringProperty ("http.ioh.router.Retry-After", props, null);
	_serverHeader = getStringProperty ("http.ioh.router.Server", props, null);

	_routedReqMeter = engine.getIOHMeters ().createIncrementalMeter ("router.routed.req", null);
	_respondMeter = engine.getIOHMeters ().createIncrementalMeter ("router.respond", null);
	
	_unavailableMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.unavailable", null);
	_respondUnavailableMeter = engine.getIOHMeters ().createIncrementalMeter ("router.respond.unavailable", _respondMeter);
	_overloadMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.overload", null);
	_respondOverloadMeter = engine.getIOHMeters ().createIncrementalMeter ("router.respond.overload", _respondMeter);
	_notFoundMeter = engine.getIOHMeters ().createIncrementalMeter ("router.error.notfound", null);
	_respondNotFoundMeter = engine.getIOHMeters ().createIncrementalMeter ("router.respond.notfound", _respondMeter);
	
	_errorUnavailable = new Error (STATUS_503_UNAVAILABLE, _respondUnavailableMeter);
	_errorOverload = new Error (STATUS_503_OVERLOAD, _respondOverloadMeter);
	_errorNotFound = new Error (STATUS_404, _respondNotFoundMeter);

	_agentLoadSelect = getIntProperty (IOHEngine.PROP_AGENT_LOAD_SELECT, props, _agentLoadSelect);
    }

    public void initMuxClient (MuxClient agent){
	agent.setContext (newAgentContext ().init (agent));
    }
    public AgentContext newAgentContext (){ return new AgentContext ();} // may be overriden
    public class AgentContext {
	public Meter _routedReqMeter, _routedWSMeter, _overloadMeter, _latencyMeter, _dismissMeter;
	protected AgentContext init (MuxClient agent){
	    _routedReqMeter = agent.getIOHMeters ().createIncrementalMeter ("router.routed.req", HttpIOHRouter.this._routedReqMeter);
	    _routedWSMeter = agent.getIOHMeters ().createIncrementalMeter ("router.routed.ws", null);
	    _overloadMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.overload", HttpIOHRouter.this._overloadMeter);
	    _dismissMeter = agent.getIOHMeters ().createIncrementalMeter ("router.error.dismiss", null);
	    _latencyMeter = agent.getIOHMeters ().getMeter ("resp.latency"); // direct pointer for perfs - set by HttpIOHEngine
	    return this;
	}
    }

    // may be overridden
    public void agentConnected (HttpIOHChannel channel, MuxClient agent, MuxClientState state){
    }
    public void agentClosed (HttpIOHChannel channel, MuxClient agent){
    }
    public void agentStopped (HttpIOHChannel channel, MuxClient agent){
    }
    public void agentUnStopped (HttpIOHChannel channel, MuxClient agent){
    }

    public void handleError (HttpIOHChannel channel, HttpMessage msg, int code, String warning){
	Error error = null;
	switch (code){ case 404 : error = _errorNotFound; break;}
	returnError (channel, msg, error, warning);
    }

    public void handleRequestHeaders (HttpIOHChannel channel, HttpMessage msg){
	// overridden routing
	if (routeCustom (channel, msg)) return;

	routeToAnyAgent (channel, msg);
    }

    public void handleRequestBody (HttpIOHChannel channel, HttpMessage msg, boolean done){
	if (msg.attachment () instanceof Error){
	    if (msg.isLast ()){
		writeError (channel, msg, (Error) msg.attachment ());
	    }
	    return;
	}
	MuxClient agent = msg.getAgent ();
	if (agent == null){
	    return;
	}
	sendToAgent (channel, agent, msg, PRIORITY_BODY);
    }
    
    public boolean handleWebSocketData (HttpIOHChannel channel, ByteBuffer buff){
	MuxClient agent = channel.agentAttached ();
	if (agent == null){
	    // this is an --http2-prior-knowledge case
	    // (or we attached null before closing, but it is ok
	    if (_logger.isDebugEnabled ())
		_logger.debug (channel+" : no attached agent -> picking one");
	    agent = channel.getIOHChannel ().getAgents ().pickRemoteAgent (null);
	    if (agent == null)
		agent = channel.getIOHChannel ().getAgents ().pickLocalAgent (null);
	    else {
		long load = agent.getLoadMeter ().getValue ();
		if (load != 0L){
		    MuxClient agent2 = channel.getIOHChannel ().getAgents ().pickRemoteAgent (null);
		    if (agent != agent2){
			long load2 = agent2.getLoadMeter ().getValue ();
			if (load2 < load){
			    AgentContext ctx = agent.getContext ();
			    ctx._dismissMeter.inc (1);
			    agent = agent2;		    
			}
		    }
		}
	    }
	}
	if (agent == null){
	    if (_logger.isInfoEnabled ())
		_logger.info (channel+" : all agents are stopped : closing");
	    _unavailableMeter.inc (1);
	    channel.close (null);
	    return false;
	}
	channel.attachAgent (agent);
	ChannelWriter.SendBufferMonitor monitor = agent.isRemoteIOHEngine () ? CHANNEL_WRITER_REMOTE_IOH :
	    (agent.isLocalAgent () ? CHANNEL_WRITER_LOCAL_AGENT : CHANNEL_WRITER_REMOTE_AGENT);
	boolean ok = checkBuffer (agent, monitor, PRIORITY_SUBSEQUENT);
	if (ok){
	    AgentContext ctx = agent.getContext ();
	    ctx._routedWSMeter.inc (buff.remaining ());
	    channel.getIOHChannel ().sendAgent (agent, null, false, 0L, true, buff);
	    return true;
	} else {
	    channel.attachAgent (null);
	    channel.close (null);
	    return false;
	}
    }
    
    /****************************************************************************************************/
    
    /************************* routing methods ********************************/

    // PLACEHOLDER TO BE OVERRIDEN
    protected boolean routeCustom (HttpIOHChannel channel, HttpMessage msg){
	return false;
    }
    
    protected boolean routeToAnyAgent (HttpIOHChannel channel, HttpMessage msg){
	MuxClient agent = channel.getIOHChannel ().getAgents ().pickRemoteAgent (null);
	if (agent == null){
	    // we assume that there is a single local agent - whatever it is
	    agent = channel.getIOHChannel ().getAgents ().pickLocalAgent (null);
	}
	if (agent == null){
	    // if all agents are stopped
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : all agents are stopped : 503 Unavailable");
	    _unavailableMeter.inc (1);
	    return returnError (channel, msg, _errorUnavailable, _warning ? "All agents stopped" : null);
	}
	MuxClientList agents = channel.getIOHChannel ().getAgents ();
	if (!agent.isLocalAgent () && agents.sizeOfRemoteAgents () > 1){
	    long load = agent.getLoadMeter ().getValue ();
	    if (load != 0L){
		for (int i=1; i<_agentLoadSelect; i++){
		    MuxClient agent2 = agents.pickRemoteAgent (null);
		    if (agent2 == agent){
			continue;
		    }
		    long load2 = agent2.getLoadMeter ().getValue ();
		    if (load2 < load){
			AgentContext ctx = agent.getContext ();
			ctx._dismissMeter.inc (1);
			agent = agent2;
			load = load2;
			if (load == 0L) break;
		    }
		}
	    }
	}
    	sendToAgent (channel, agent, msg, PRIORITY_INITIAL);
	return true;
    }
    
    /****************************************************************************************************/
    
    /************************* utilities ********************************/

    protected MuxClient pickAgent (HttpIOHChannel channel, Object preferenceHint){
	return channel.pickAgent (preferenceHint);
    }

    protected boolean sendToAgent (final HttpIOHChannel channel, MuxClient agent, final HttpMessage msg, int priority){
	ChannelWriter.SendBufferMonitor monitor = agent.isRemoteIOHEngine () ? CHANNEL_WRITER_REMOTE_IOH :
	    (agent.isLocalAgent () ? CHANNEL_WRITER_LOCAL_AGENT : CHANNEL_WRITER_REMOTE_AGENT);
	if (_test){
	    monitor = new ChannelWriter.BoundedSendBufferMonitor (10){
		    public boolean check (int buffSize, Object attachment){
			buffSize = msg.getHeaderValueAsInt ("test-send-buffer-size", buffSize);
			return buffSize < 10;
		    }
		};
	}
	boolean ok = checkBuffer (agent, monitor, priority);
	if (ok){
	    ok = channel.sendAgent (agent, msg);
	}
	if (ok){
	    AgentContext ctx = agent.getContext ();
	    if (msg.isFirst ())
		ctx._routedReqMeter.inc (1);
	} else {
	    if (msg.isFull ()){
		// we can send an error 503 now
		writeError (channel, msg, _errorOverload, _warning ? "Agent "+agent+" overloaded" : null);
	    } else if (msg.isFirst ()){
		// we can send an error 503 later
		msg.attach (_errorOverload.newInstance (_warning ? "Agent "+agent+" overloaded" : null));
	    } else {
		// a subsequent body is rejected -> close channel - cannot send a 503 since the agent may already be responding
		channel.close (msg);
	    }
	}
	return ok;
    }

     protected boolean checkBuffer (MuxClient agent, ChannelWriter.SendBufferMonitor sendMonitor, Object sendMonitorArg){
	boolean ok = ChannelWriter.check (agent.getSendBufferSize (), sendMonitor, sendMonitorArg);
	if (!ok){
	    AgentContext ctx = agent.getContext ();
	    ctx._overloadMeter.inc (1);
	    if (agent.getLogger ().isInfoEnabled ())
		agent.getLogger ().info (agent+" : Buffer Full : cannot send message (sendBufferSize="+agent.getSendBufferSize ()+")");
	}
	return ok;
    }
    
    protected static class Error {
	protected byte[][] _msg;
	protected Meter _sentMeter;
	protected String _warning;
	protected Error (byte[][] msg, Meter sentMeter){
	    _msg = msg;
	    _sentMeter = sentMeter;
	}
	protected Error newInstance (String warning){
	    if (warning == null) return this;
	    Error err = new Error (_msg, _sentMeter);
	    err._warning = warning;
	    return err;
	}
	protected void sent (){
	    _sentMeter.inc (1);
	}
	protected byte[] getMessage (int version){
	    if (version == 2) version = 1;
	    return _msg[version];
	}
	protected String getWarning (){ return _warning;}
    }
    protected boolean returnError (HttpIOHChannel channel, HttpMessage msg, Error error, String warning){
	if (msg.isLast ()){
	    writeError (channel, msg, error, warning);
	} else {
	    msg.attach ((_warning && warning != null) ? error.newInstance (warning) : error);
	}
	return true;
    }
    protected void writeError (HttpIOHChannel channel, HttpMessage message, Error error){
	writeError (channel, message, error, error.getWarning ());
    }
    protected void writeError (HttpIOHChannel channel, HttpMessage message, Error error, String warning){
	ByteBuffer firstLine = ByteBuffer.wrap (error.getMessage (message.getVersion ()));
	message.removeHeaders ();
	message.addHeader (CONTENT_LENGTH, CONTENT_LENGTH_0);
	if (_warning && warning != null)
	    message.addHeader (WARNING, warning);
	if (_retryAfterHeader != null)
	    message.addHeader (RETRY_AFTER, _retryAfterHeader);
	if (_serverHeader != null)
	    message.addHeader (SERVER, _serverHeader);
	message.setBody (null);
	ByteBuffer[] msgBuffs = message.toByteBuffers (false, true, false);
	if (channel.sendOut (message, true, ByteBufferUtils.prepend (firstLine, msgBuffs))){
	    error.sent ();
	    if (message.getVersion () != 2) // in http1 we must control the connection / no agent to help
		channel.close (message); // if sendOut failed --> close already called
	}
    }

    protected static final byte[] CONTENT_LENGTH = getUTF8 ("Content-Length");
    protected static final byte[] CONTENT_LENGTH_0 = getUTF8 ("0");
    protected static final byte[] WARNING = getUTF8 ("Warning");
    protected static final byte[] RETRY_AFTER = getUTF8 ("Retry-After");
    protected static final byte[] SERVER = getUTF8 ("Server");
    protected static final byte[][] STATUS_400_BAD_REQUEST = {getUTF8 ("HTTP/1.0 400 Bad Request\r\n"), getUTF8 ("HTTP/1.1 400 Bad Request\r\n")};
    protected static final byte[][] STATUS_503_OVERLOAD = {getUTF8 ("HTTP/1.0 503 Overload Detected\r\n"), getUTF8 ("HTTP/1.1 503 Overload Detected\r\n")};
    protected static final byte[][] STATUS_503_UNAVAILABLE = {getUTF8 ("HTTP/1.0 503 Service Unavailable\r\n"), getUTF8 ("HTTP/1.1 503 Service Unavailable\r\n")};
    protected static final byte[][] STATUS_404 = {getUTF8 ("HTTP/1.0 404 Not Found\r\n"), getUTF8 ("HTTP/1.1 404 Not Found\r\n")};
    
    protected static int getIntProperty (String name, Map<String, Object> props, int def){
	try{
	    String v = (String) props.get (name);
	    if (v != null) return Integer.parseInt (v);
	}catch(Throwable t){}
	return def;
    }
    protected static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def){
	try{
	    String v = (String) props.get (name);
	    if (v != null) return Boolean.parseBoolean (v);
	}catch(Throwable t){}
	return def;
    }
    protected static List<String> getStringListProperty (String name, Map<String, Object> props){
	Object o = props.get (name);
	if (o == null) return new ArrayList<String> (1);
	if (o instanceof String){
	    List<String> res = new ArrayList<String> (1);
	    res.add ((String) o);
	    return res;
	}
	return (List<String>) o;
    }
    protected static byte[] getStringProperty (String name, Map props, String def){
	String s = getStringPropertyAsString (name, props, def);
	return s != null ? getUTF8 (s) : null;
    }
    protected static String getStringPropertyAsString (String name, Map props, String def){
	String value = (String) props.get (name);
	if (value != null){
	    value = value.trim ();
	    return value.length () > 0 ? value : null;
	}
	return def != null ? def : null;
    }
}
