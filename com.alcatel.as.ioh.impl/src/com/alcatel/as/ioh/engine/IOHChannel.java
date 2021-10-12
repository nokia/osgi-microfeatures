// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;
import alcatel.tess.hometop.gateways.reactor.util.FlowController;

import org.apache.log4j.Logger;

import com.alcatel.as.ioh.engine.IOHEngine.*;
import com.alcatel.as.ioh.tools.*;
import com.nextenso.mux.util.MuxIdentification;

import static com.alcatel.as.util.helper.AnsiFormatter.*;

public abstract class IOHChannel implements IOHEngine.MuxClientListener {

    protected MuxClient _agent;
    protected long _connectionId = 0L;
    protected Logger _logger;
    protected int _id;
    protected boolean _shared, _secure;
    protected IOHEngine _engine;
    protected PlatformExecutor _exec;
    protected AsyncChannel _channel;
    protected String _toString;
    protected MuxClientList _agentsList;
    protected Object _attachment;
    protected boolean _closed = false;
    protected ChannelWriter.SendBufferMonitor _sendOutBufferMonitor;
    protected Meter _sendMeter, _readMeter, _sendDroppedMeter;
    protected FlowController _flowController;
    protected boolean _isText;
    protected boolean _ignoreAgents = false; // new agents are not taken into account
    protected IOHHistory _history;
    protected Object _closeReason;
    
    public IOHChannel (IOHEngine engine, boolean shared){
	_engine = engine;
	_shared = shared;
	_isText = engine.isTextProtocol ();
	_history = new IOHHistory (_engine, _engine.historyChannels ());
	history ("created");
    }
    public IOHChannel (MuxClient agent, boolean shared){
	this (agent.getIOHEngine (), shared);
	_agent = agent;
    }
    // Must be called in engine thread
    public int makeId (){
	return _id = _engine.reserveSocketId (this, _shared);
    }
    
    /// to be overridden if needed, will be scheduled in the channel executor
    protected void applyParams (Map<String, String> params){};
    
    //to be overriden if needed, by default it schedule a call to applyParams 
    //in the queue
    protected void applyParamsNow(Map<String, String> params) {
    	schedule(() -> { applyParams(params); });
    };

    public IOHChannel closeReason (Object reason){ _closeReason = reason; return this;}
    public IOHChannel defaultCloseReason (Object reason){ // to set a reason by def is not explicitly set elsewhere
	if (_closeReason == null)
	    _closeReason = reason;
	return this;
    }
    public Object closeReason (){ return _closeReason;}
    
    public int getSockId (){ return _id; }
    public long getConnectionId (){ return _connectionId;}
    public Logger getLogger (){ return _logger;}
    public boolean shared (){ return _shared;}
    public boolean secure (){ return _secure;}
    public boolean closed (){ return _closed;}
    public boolean isText (){ return _isText;}
    public void ignoreAgents (){ _ignoreAgents = true;}
    public <T extends AsyncChannel> T getChannel (){ return (T) _channel;}
    public FlowController getFlowController (){ return _flowController;}
    
    public InetSocketAddress getLocalAddress (){ return _channel.getLocalAddress ();}

    // the methods called when requested by an agent
    public boolean close (MuxClient agent){
	if (_closeReason == null) _closeReason = "Close requested by agent : "+agent;
	_channel.close ();
	return true;
    }
    public boolean shutdown (MuxClient agent){
	if (_closeReason == null) _closeReason = "Shutdown requested by agent : "+agent;
	_channel.shutdown ();
	return true;
    }
    public void enableRead (MuxClient agent){ _channel.enableReading ();};
    public void disableRead (MuxClient agent){ _channel.disableReading ();}
    public abstract boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs);
    public boolean sendSctpOut (MuxClient agent, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean checkBuffer, boolean copy, ByteBuffer... data){ return false;} // for sctp channel only
    public abstract boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs);
    
    public void attach (Object attachment){ _attachment = attachment;}
    public <T> T attachment (){ return (T) _attachment;}

    public void schedule (Runnable r){
	_exec.execute (r);
    }
    public PlatformExecutor getPlatformExecutor (){ return _exec;}
    public <T extends IOHEngine> T getIOHEngine (){ return (T) _engine;}

    public void setChannelInputExecutor (){
	_channel.setInputExecutor (_exec = _engine.createQueueExecutor ());
    }
    
    public MuxClientList getAgents (){ return _agentsList;}

    protected boolean join (MuxClient agent, MuxClientState state){ return agentJoined (agent, state);} // may be useful to override
    protected void notifyOpenToAgent (MuxClient agent){
	notifyOpenToAgent (agent, agent == _agent ? _connectionId : 0L); // only the initial requester gets the connectionId
    }
    protected abstract void notifyOpenToAgent (MuxClient agent, long connectionId);
    protected void renotifyOpenToAgent (MuxClient agent, long delay){
	// for inbound connection, optional - to renotify after an agent closed - CSFAR-1352
	_engine.getPlatformExecutor ().schedule (() -> {
		if (_engine.getMuxClientList ().contains (agent)){
		    MuxClientState state = new MuxClientState ().stopped (_engine.getMuxClientList ().isDeactivated (agent));
		    schedule (() -> {
			    agentConnected (agent, state);
			});
		}
	    },
	    delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }    
    protected abstract void notifyCloseToAgent (MuxClient agent);
    public void close (){ close (true, true);} // public method to close the channel
    protected abstract void close (boolean notifyAgent, boolean closeChannel);
    public void reject (){} // used for accepted sockets : when it is rejected in the constructor - so the registration is not performed
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_closed) return false;
	if (_ignoreAgents) return false;
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentConnected : "+agent);
	notifyOpenToAgent (agent);
	if (!_agentsList.iterating ()) _agentsList.add (agent, state);
	return true;
    }
    public boolean agentJoined (MuxClient agent, MuxClientState state){
	if (_closed) return false;
	if (_ignoreAgents) return false;
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentJoined : "+agent+" / "+state.connectionId ());
	notifyOpenToAgent (agent, state.connectionId ());
	_agentsList.add (agent, state);
	return true;
    }
    public boolean agentClosed (MuxClient agent){
	if (_closed) return false;
	if (_shared){
	    if (_agentsList.remove (agent)){
		if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
		if (_history.active ())
		    history ("agentClosed : "+agent);
		return true;
	    } else
		return false;
	} else {
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentClosed : "+agent);
	    close (false, true);
	    return true;
	}
    }
    public boolean agentStopped (MuxClient agent){
	if (_closed) return false;
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentStopped : "+agent);
	if (_shared){
	    return _agentsList.deactivate (agent);
	} else
	    return true;
    }
    public boolean agentUnStopped (MuxClient agent){
	if (_closed) return false;
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : agentUnStopped : "+agent);
	if (_shared){
	    return _agentsList.reactivate (agent);
	} else
	    return true;
    }

    protected void iterateAgentConnected (MuxClientList list){
	list.iterate (new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    agentConnected (agent, new MuxClientState ().stopped (list.isDeactivated (agent)));
		    return null;}
	    }, null);
    }
    protected void iterateNotifyClose (){
	_agentsList.iterate (new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    notifyCloseToAgent (agent);
		    return null;}
	    }, null);
    }
    
    public void connectionAborted (){ // may be used sometimes - ex : RemoteIOHEngine
	connectionClosed ();
    }
    public void connectionClosed (){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : connectionClosed");
	close (true, false);
    }
    public void receiveTimeout(){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : receiveTimeout");
	_channel.shutdown ();
    }
    public String toString (){
	return _toString;
    }

    public boolean disabled (ByteBuffer buff){
	if (_closed){
	    // may happen if we called socket.close() but callback not yet arrived
	    buff.position (buff.limit ());
	    return true;
	}	
	return false;
    }
    public void logReceived (Object arg, ByteBuffer... buffs){
	if (_logger.isDebugEnabled ()){
	    StringBuilder sb = new StringBuilder ().append (this.toString ());
	    if (arg == null)
		arg = "RECEIVED";
	    sb.append (" : ").append (arg);
	    sb.append ("\n").append (toString (buffs));
	    _logger.debug (sb.toString ());
	}
    }
    public boolean checkSendBufferOut (Object argument){
	if (ChannelWriter.check (_channel, _sendOutBufferMonitor, argument) == false){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : Buffer Full : "+_channel.getSendBufferSize ()+" bytes : cannot send message");
	    return false;
	}
	return true;
    }
    public void logSendOut (MuxClient agent, Object arg, ByteBuffer... buffs){
	if (_logger.isDebugEnabled ()){
	    StringBuilder sb = new StringBuilder ().append (this.toString ());
	    if (arg == null)
		arg = "SEND";
	    if (agent != null)
		sb.append (" : ").append (agent.toString ());
	    sb.append (" : ").append (arg);
	    sb.append ("\n").append (toString (buffs));
	    _logger.debug (sb.toString ());
	}
    }
    public boolean checkSendBufferAgent (MuxClient agent, Object argument){
	if (agent.checkSendBuffer (argument) == false){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : Agent Buffer Full : cannot send message to : "+agent);
	    return false;
	}
	return true;
    }
    public void logSendAgent (MuxClient agent, Object arg, ByteBuffer... buffs){
	if (_logger.isDebugEnabled ()){
	    StringBuilder sb = new StringBuilder ().append (this.toString ());
	    if (arg == null)
		arg = "FORWARD";
	    sb.append (" : ").append (agent.toString ());
	    sb.append (" : ").append (arg);
	    sb.append ("\n").append (toString (buffs));
	    _logger.debug (sb.toString ());
	}
    }
    protected String toString (ByteBuffer[] buffs){
	return _isText ? "["+ByteBufferUtils.toUTF8String (false, buffs)+"]" : "<"+ByteBufferUtils.remaining (buffs)+" bytes>";
    }
    protected String toString (ByteBuffer buff){
	return _isText ? "["+ByteBufferUtils.getUTF8 (buff, false)+"]" : "<"+buff.remaining ()+" bytes>";
    }

    public void history (String s){
	_history.history (s);
    }

    public String dump (boolean history){
	return dump (history, false);
    }
    public String dump (boolean history, boolean pretty){
	final StringBuilder sb = new StringBuilder ();
	sb.append (toString ()).append ('\n');
	if (_closed) {
	    if (pretty)
		sb.append (BRIGHT_RED).append ("CLOSED").append (RESET).append ('\n');
	    else
		sb.append ("CLOSED\n");
	} else {
	    if (_shared){
		_agentsList.iterate (new MuxClientList.Iterator (){
			public Object next (MuxClient agent, Object ctx){
			    sb.append ("Agent : ");
			    IOHGogoCommands.print (agent, sb, pretty);
			    return null;}
		    }, null);
	    } else {
		sb.append ("Agent : ");
		IOHGogoCommands.print (_agent, sb, pretty);
	    }
	    if (history) _history.getHistory (true, sb, pretty);
	}
	return sb.toString ();
    }
}
