package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.impl.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.*;

public class IOHVirtualTcpChannel extends IOHChannel implements Runnable {
    
    protected final static AtomicLong SEED = new AtomicLong(0);

    protected long _createdTime;
    
    // called in any thread
    public IOHVirtualTcpChannel (IOHEngine engine, Map<String, Object> props){
	super (engine, engine.sharedTcpAccept ());
	_connectionId = SEED.getAndIncrement ();
	_createdTime = System.currentTimeMillis ();
	_exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	_logger = _engine.getTcpLogger ();
	_sendOutBufferMonitor = _engine.getSendTcpBufferMonitor ();
	_sendMeter = _engine.getIOHMeters ().getSendTcpMeter ();
	_readMeter = _engine.getIOHMeters ().getReadTcpMeter ();
	_sendDroppedMeter = _engine.getIOHMeters ().getSendDroppedTcpMeter ();
	_toString = new StringBuilder().append ("IOHVirtualTcpChannel[connectionId=").append (_connectionId).append (']').toString ();
    }
    // called just after the constructor (which has time to finish)
    public IOHVirtualTcpChannel created (){
	// no meters for now to count these channels
	_engine.schedule (this);
	return this;
    }
    public boolean shutdown (MuxClient agent){return close (agent);}
    public void enableRead (MuxClient agent){} // to be overridden
    public void disableRead (MuxClient agent){} // to be overridden
    
    public void run (){
	register ();
    }
    // called in the engine thread
    protected void register (){
	makeId ();
	if (_logger.isInfoEnabled ()){
	    _logger.info (this+" : registered : id="+_id);
	}
	_toString = new StringBuilder ()
	    .append ("VirtualTcpChannel[id=").append (_id).append (']')
	    .toString ();
	if (checkAgentsSize () == false)
	    return;
	if (_shared){
	    _engine.getTcpChannels ().put (_id, this);
	    _agentsList = _engine.copyMuxClientList ();
	    iterateAgentConnected (_agentsList);
	    Runnable registered = new Runnable (){
		    public void run (){
			registered ();
		    }};
	    schedule (registered);
	} else {
	    assignToAgent ();
	}
    }
    // called in the engine thread
    protected boolean checkAgentsSize (){
	// if _shared=false, we need at least 1 active agent
	// if _shared=true, we accept a deactivated agent (override if needed)
	int size = _shared ? _engine.getMuxClientList ().size () : _engine.getMuxClientList ().sizeOfActive ();
	if (size == 0){
	    if (_logger.isInfoEnabled ())
		_logger.info (this+" : closing : no active agent connected");
	    Runnable close = new Runnable (){
		    public void run (){
			close (false, true); // will trigger connectionClosed
		    }};
	    schedule (close);
	    return false;
	}
	return true;
    }
    // called in the engine thread
    protected void assignToAgent (){
	_agent = _engine.getMuxClientList ().pick (null);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : attaching to : "+_agent);
	Runnable r = new Runnable (){
		public void run (){
		    if (_agent.isOpened () == false){
			if (_logger.isInfoEnabled ())
			    _logger.info (this+" : cannot attach to : "+_agent+" : closed - retrying");
			Runnable r = new Runnable (){
				public void run (){
				    _agent = null; // reset to null for meters if close() is called
				    if (checkAgentsSize ())
					assignToAgent ();
				}};
			_engine.schedule (r);
			return;
		    }
		    _agent.getTcpChannels ().put (_id, IOHVirtualTcpChannel.this);
		    notifyOpenToAgent (_agent);
		    Runnable enableReading = new Runnable (){
			    public void run (){
				registered ();
			    }};
		    _exec.execute (enableReading);
		}};
	_agent.schedule (r);
    }
    protected void registered (){
	// to be overridden - same as enableReading - callback for readiness in _exec
    }
    
    @Override
    public boolean agentClosed (MuxClient agent){
	if (super.agentClosed (agent) == false) return false;
	if (_shared && _agentsList.size () == 0)
	    close (false, true);
	return true;
    }
    
    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	try{
	InetSocketAddress remote = new InetSocketAddress (InetAddress.getByName ("127.0.0.1"), 0);
	InetSocketAddress local = new InetSocketAddress (InetAddress.getByName ("127.0.0.1"), 0);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyOpenToAgent : "+agent+" : "+connectionId);
	agent.getMuxHandler ().tcpSocketConnected (agent, _id, remote.getAddress().getHostAddress(), remote.getPort(), local.getAddress().getHostAddress(), local.getPort (), null, 0, _secure, true, connectionId, 0);
	}catch(Exception e){
	    // getByName exception cannot happen
	}
    }
    
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	if (_engine.historyChannels ())
	    history ("notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
    }

    // called in agent thread
    @Override
    public boolean close (final MuxClient agent){
	if (_shared == false ||
	    _engine.sharedCloseTcpAccept ()){
	    Runnable r = new Runnable (){
		    public void run (){
			close (true, true);
		    }
		};
	    schedule (r);
	    return true;
	} else {
	    Runnable r = new Runnable (){
		    public void run (){
			if (_closed) return;
			notifyCloseToAgent (agent);
			agentClosed (agent);
		    }
		};
	    schedule (r);
	    return true;
	}
    }
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	// to be overridden
	return true;
    }
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	logSendAgent (agent, null, buffs);
	if (copy)
	    agent.getMuxHandler ().tcpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs));
	else
	    agent.getExtendedMuxHandler ().tcpSocketData (agent, _id, sessionId, buffs);
	return true;
    }
    
    public void close (boolean notifyAgent, boolean closeSocket){
	if (_closed) return;
	_closed = true;
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : close :"+notifyAgent+"/"+closeSocket);
	if (closeSocket){
	    Runnable r = new Runnable (){
		    public void run (){
			connectionClosed ();
		    }
		};
	    schedule (r);
	}
	if (notifyAgent){
	    if (_shared){
		iterateNotifyClose ();
		_agentsList.clear ();
	    } else {
		Runnable r = new Runnable (){
			public void run (){
			    if (_agent.isOpened ()){
				_agent.getTcpChannels ().remove (_id);
				notifyCloseToAgent (_agent);
			    }
			    _engine.releaseSocketId (_id);
			}};
		_agent.schedule (r);
	    }
	}
	// clean
	if (_shared){
	    Runnable r = new Runnable (){
		    public void run (){
			_engine.getTcpChannels ().remove (_id);
			_engine.releaseSocketId (_id);
		    }};
	    _engine.schedule (r);
	} else {
	    if (notifyAgent == false){
		_engine.releaseSocketId (_id);
	    }
	}
    }
    
}
