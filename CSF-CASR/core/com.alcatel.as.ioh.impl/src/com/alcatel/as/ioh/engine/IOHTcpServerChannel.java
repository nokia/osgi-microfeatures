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

import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.engine.IOHEngine.*;
import com.alcatel.as.ioh.tools.*;
import com.nextenso.mux.util.MuxIdentification;

public class IOHTcpServerChannel extends IOHChannel implements Runnable {

    protected TcpServer _server;
    protected TcpServerChannel _serverChannel;
    
    // called in the server thread
    protected IOHTcpServerChannel (IOHEngine engine, TcpServer server){
	super (engine, true);
	_server = server;
	_serverChannel = server.getServerChannel ();
	_secure = _serverChannel.isSecure ();
	_exec = engine.getCurrentExecutor ();
	_logger = _engine.getTcpLogger ();
    }
    // called just after the constructor (which has time to finish)
    public IOHTcpServerChannel opened (){
	_engine.schedule (this);
	return this;
    }
    public void run (){
	register ();
    }
    // called in the engine thread
    protected void register (){
	_id = _engine.registerTcpServer (this);
	_toString = new StringBuilder ()
	    .append ("TcpServerChannel[id=").append (_id).append (", ").append (_serverChannel.getLocalAddress ()).append (']').toString ();
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : listening");
	_agentsList = new MuxClientList ();
	iterateAgentConnected (_engine.getMuxClientList ());
    }
    protected TcpServer getTcpServer (){ return _server;}
    @Override
    public InetSocketAddress getLocalAddress (){ return _serverChannel.getLocalAddress ();}
    
    // called in the engine thread
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.notifyTcpListen () || agent.isRemoteIOHEngine ()){ // we notify spontaneously for remoteIOH (same for udp servers)
	    return super.agentConnected (agent, state);
	}
	return false;
    }
    // called in the engine thread
    protected void notifyOpenToAgent (MuxClient agent, long listenId){
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().tcpSocketListening (agent, _id, getLocalAddress ().getAddress ().getHostAddress (), getLocalAddress ().getPort (), _secure, listenId, 0);
    }
    // called in the engine thread
    protected void notifyCloseToAgent (MuxClient agent){
	// TODO : check if agent is overloaded --> not useful !
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
    }
    @Override
    // called in the engine thread
    protected void close (boolean notifyAgent, boolean closeChannel){
	if (_closed) return;
	_closed = true;
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : close :"+notifyAgent+"/"+closeChannel);
	_engine.unregisterTcpServer (this);
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
