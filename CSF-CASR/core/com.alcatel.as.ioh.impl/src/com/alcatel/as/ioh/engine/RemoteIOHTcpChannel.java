package com.alcatel.as.ioh.engine;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.impl.ioh.*;

import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.*;

public class RemoteIOHTcpChannel extends IOHChannel {

    protected RemoteIOHEngine _remoteEngine;
    protected MuxConnection _muxConnection;
    protected String _remoteIP, _localIP;
    protected int _remotePort, _localPort;
    protected boolean _accepted;
    protected int _remoteId;
    
    /*
     * A RemoteIOHTcpChannel can only be shared
     */

    // called in the remote ioh thread
    public RemoteIOHTcpChannel (RemoteIOHEngine engine){
	super (engine.getIOHEngine (), true);
	_exec = _engine.createQueueExecutor ();
	_remoteEngine = engine;
	_muxConnection = engine.getMuxConnection (); // since it may change, we need to keep a pointer on this one
	_channel = engine.getChannel ();
	_readMeter = _engine.getRemoteReadTcpMeter ();
	_sendMeter = _engine.getRemoteSendTcpMeter ();
	_sendOutBufferMonitor = _engine.getSendRemoteIOHBufferMonitor ();
	_flowController = engine.getFlowController ();
	_id = _engine.reserveSocketId (this, _remoteEngine.id (), true);
	_logger = engine.getIOHEngine ().getRemoteLogger ();
    }
    public RemoteIOHTcpChannel connected (int sockId, String remoteIP, int remotePort, String localIP, int localPort,
					  boolean secure, boolean accepted, long connectionId){
	_toString = new StringBuilder ()
	    .append ("RemoteTcpChannel[id=").append (_id).append (", remote=").append (remoteIP).append (':').append (remotePort).append (']')
	    .toString ();
	_remoteId = sockId;
	_remoteIP = remoteIP;
	_remotePort = remotePort;
	_localIP = localIP;
	_localPort = localPort;
	_secure = secure;
	_accepted = accepted;
	_connectionId = connectionId;
	_engine.getRemoteTcpChannelsMeter ().inc (1);
	_remoteEngine.getTcpChannelsByRemoteId ().put (_remoteId, this);
	_remoteEngine.getTcpChannelsByLocalId ().put (_id, this);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : connected");
	// dont close the socket if no agent
	_agentsList = _remoteEngine.copyMuxClientList ();
	iterateAgentConnected (_agentsList);
	return this;
    }

    @Override
    // called in remote ioh thread
    public void connectionClosed (){
	if (_remoteEngine.isOpen ()){ // else mux connection is closed --> iterating/cannot remove
	    _remoteEngine.getTcpChannelsByRemoteId ().remove (_remoteId);
	    _remoteEngine.getTcpChannelsByLocalId ().remove (_id);
	}
	Runnable r = new Runnable (){
		public void run (){
		    close (true, false);
		}};
	schedule (r);
    }
    
    @Override
    // called in remote ioh thread when tcp socket is aborted
    public void connectionAborted (){    
	_remoteEngine.getTcpChannelsByRemoteId ().remove (_remoteId);
	_remoteEngine.getTcpChannelsByLocalId ().remove (_id);
	Runnable r = new Runnable (){
		public void run (){
		    if (_closed) return;
		    _closed = true;
		    iterateNotifyAbort ();
		    _engine.releaseSocketId (_id);
		}};
	schedule (r);
    }

    @Override
    // must be called in this exec
    protected void close (boolean notifyAgent, boolean closeChannel){
	if (_closed) return;
	_closed = true;
	if (closeChannel){
	    _muxConnection.sendTcpSocketClose (_remoteId);
	    // we'll unregister from the remoteIOHEngine in connectionClosed callback
	}
	if (notifyAgent){
	    iterateNotifyClose ();
	    _agentsList.clear ();
	}
	_engine.releaseSocketId (_id);
	_engine.getRemoteTcpChannelsMeter ().inc (-1);
    }

    
    // called in remote ioh exec
    protected void received (final long sessionId, ByteBuffer data){
	int size = data.remaining ();
	_readMeter.inc (size);
	_flowController.acquireNow ();
	final ByteBuffer copied = ByteBuffer.allocate (size);
	copied.put (data);
	Runnable receivedRunnable = new Runnable (){
		public void run (){
		    try{
			if (_closed) return;
			copied.flip ();
			receivedData (sessionId, copied);
		    }finally{
			_flowController.release ();
		    }
		}};
	schedule (receivedRunnable);
    }
    protected void receivedData (long sessionId, ByteBuffer data){
	//TO BE OVERRIDDEN
	// since shared=true, not clear how to pick an agent w/o parsing
    }

    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	agent.getMuxHandler ().tcpSocketConnected (agent, _id, _remoteIP, _remotePort, _localIP, _localPort, null, 0, _secure, _accepted, connectionId, 0);
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
    }
    protected void notifyAbortToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyAbortToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	agent.getMuxHandler ().tcpSocketAborted (agent, _id);
    }

    protected void iterateNotifyAbort (){
	_agentsList.iterate (new MuxClientList.Iterator (){
		public Object next (MuxClient agent, Object ctx){
		    notifyAbortToAgent (agent);
		    return null;}
	    }, null);
    }

    @Override
    // called in agent thread
    public boolean close (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	return _muxConnection.sendTcpSocketClose (_remoteId);
    }
    @Override
    // called in agent thread
    public boolean shutdown (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	return _muxConnection.sendTcpSocketAbort (_remoteId);
    }
    @Override
    // called in agent thread
    public void enableRead (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	_muxConnection.enableRead (_remoteId);
    }
    @Override
    // called in agent thread
    public void disableRead (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	_muxConnection.disableRead (_remoteId);
    }
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferOut (null) == false){
		_channel.shutdown (); // when remote is overloaded --> we close the socket
		return false;
	    }
	}
	logSendOut (agent, null, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	_muxConnection.sendTcpSocketData (_remoteId, copy, buffs);
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
}
