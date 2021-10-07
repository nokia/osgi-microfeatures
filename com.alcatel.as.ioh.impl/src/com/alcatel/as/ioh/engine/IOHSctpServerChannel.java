package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.alcatel.as.service.concurrent.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;

import org.apache.log4j.Logger;

import com.alcatel.as.ioh.server.SctpServer;
import com.alcatel.as.ioh.engine.IOHEngine.*;
import com.alcatel.as.ioh.tools.*;
import com.nextenso.mux.util.MuxIdentification;

public class IOHSctpServerChannel extends IOHChannel implements Runnable {

    protected SctpServer _server;
    protected SctpServerChannel _serverChannel;
    protected String[] _localAddrs;
    protected InetSocketAddress _localAddr;
    
    // called in the server thread
    protected IOHSctpServerChannel (IOHEngine engine, SctpServer server){
	super (engine, true);
	_server = server;
	_serverChannel = server.getServerChannel ();
	_secure = _serverChannel.isSecure ();
	_exec = engine.getCurrentExecutor ();
	_logger = _engine.getSctpLogger ();
	try{
	    _localAddr = (InetSocketAddress) _serverChannel.getAllLocalAddresses ().iterator ().next ();
	    _localAddrs = IOHSctpChannel.getAddresses (_serverChannel.getAllLocalAddresses ());
	}catch(Throwable t){
	    _logger.warn (server+" : exception in init", t);
	    _serverChannel.close ();
	    _closed = true;
	}
    }
    // called just after the constructor (which has time to finish)
    public IOHSctpServerChannel opened (){
	if (_closed) return this; // exception in init
	_engine.schedule (this);
	return this;
    }
    public void run (){
	register ();
    }
    // called in the engine thread
    protected void register (){
	_id = _engine.registerSctpServer (this);
	_toString = new StringBuilder ()
	    .append ("SctpServerChannel[id=").append (_id).append (", ").append (_localAddr).append (']').toString ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : listening");
	_agentsList = new MuxClientList ();
	iterateAgentConnected (_engine.getMuxClientList ());
    }
    protected SctpServer getSctpServer (){ return _server;}
    @Override
    public InetSocketAddress getLocalAddress (){
	try {return (InetSocketAddress) _serverChannel.getAllLocalAddresses ().iterator ().next ();}
	catch(Throwable t){
	    _serverChannel.close ();
	    return new InetSocketAddress ("0.0.0.0", 0); // dummy
	}
    }
    public String[] getLocalAddresses (){ return _localAddrs;}
    
    // called in the engine thread
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.notifySctpListen () || agent.isRemoteIOHEngine ()){ // we notify spontaneously for remoteIOH (same for udp servers)
	    return super.agentConnected (agent, state);
	}
	return false;
    }
    // called in the engine thread
    protected void notifyOpenToAgent (MuxClient agent, long listenId){
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().sctpSocketListening (agent, _id, listenId, _localAddrs, _localAddr.getPort (), _secure, 0);
    }
    // called in the engine thread
    protected void notifyCloseToAgent (MuxClient agent){
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().sctpSocketClosed (agent, _id);
    }
    @Override
    // called in the engine thread
    protected void close (boolean notifyAgent, boolean closeChannel){
	if (_closed) return;
	_closed = true;
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : close :"+notifyAgent+"/"+closeChannel);
	_engine.unregisterSctpServer (this);
	if (closeChannel) _serverChannel.close ();
	if (notifyAgent){
	    iterateNotifyClose ();
	    _agentsList.clear ();
	}
    }
    public boolean close (MuxClient agent){throw new IllegalStateException (toString ());}
    public boolean shutdown (MuxClient agent){throw new IllegalStateException (toString ());}
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
    public boolean checkSendBufferOut (Object argument){throw new IllegalStateException (toString ());}
    public void logSendOut (MuxClient agent, Object arg, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
    public boolean checkSendBufferAgent (MuxClient agent, Object argument){throw new IllegalStateException (toString ());}
    public void logSendAgent (MuxClient agent, Object arg, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
    
}
