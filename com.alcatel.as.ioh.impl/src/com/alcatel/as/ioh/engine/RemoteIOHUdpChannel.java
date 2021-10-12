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

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;

import alcatel.tess.hometop.gateways.reactor.*;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.impl.*;

import com.alcatel.as.ioh.engine.IOHEngine.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.tools.*;

public class RemoteIOHUdpChannel extends IOHChannel {

    protected RemoteIOHEngine _remoteEngine;
    protected MuxConnection _muxConnection;
    protected String _localIP;
    protected int _localPort;
    protected int _remoteId;
    
    /*
     * A RemoteIOHUdpChannel can only be shared
     */

    // called in the remote ioh thread
    public RemoteIOHUdpChannel (RemoteIOHEngine engine){
	super (engine.getIOHEngine (), true);
	_exec = _engine.createQueueExecutor ();
	_remoteEngine = engine;
	_muxConnection = engine.getMuxConnection (); // since it may change, we need to keep a pointer on this one
	_channel = engine.getChannel ();
	_readMeter = _engine.getRemoteReadUdpMeter ();
	_sendMeter = _engine.getRemoteSendUdpMeter ();
	_sendOutBufferMonitor = _engine.getSendRemoteIOHBufferMonitor ();
	_flowController = engine.getFlowController ();
	_id = _engine.reserveSocketId (this, _remoteEngine.id (), true);
	_logger = engine.getIOHEngine ().getRemoteLogger ();
    }
    public RemoteIOHUdpChannel bound (int sockId, String localIP, int localPort, long connectionId){
	_toString = new StringBuilder ()
	    .append ("RemoteUdpChannel[id=").append (_id).append (", local=").append (localIP).append (':').append (localPort).append (']')
	    .toString ();
	_remoteId = sockId;
	_localIP = localIP;
	_localPort = localPort;
	_connectionId = connectionId;
	_engine.getRemoteUdpChannelsMeter ().inc (1);
	_remoteEngine.getUdpChannelsByRemoteId ().put (_remoteId, this);
	_remoteEngine.getUdpChannelsByLocalId ().put (_id, this);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : opened");
	// dont close the socket if no agent
	_agentsList = new MuxClientList ();
	iterateAgentConnected (_remoteEngine.getMuxClientList ());
	return this;
    }
    
    @Override
    // called in remote ioh thread
    public void connectionClosed (){
	if (_remoteEngine.isOpen ()){ // else mux connection is closed --> iterating/cannot remove
	    _remoteEngine.getUdpChannelsByRemoteId ().remove (_remoteId);
	    _remoteEngine.getUdpChannelsByLocalId ().remove (_id);
	}
	Runnable r = new Runnable (){
		public void run (){
		    close (true, false);
		}};
	schedule (r);
    }
    
    @Override
    // must be called in this exec
    protected void close (boolean notifyAgent, boolean closeChannel){
	if (_closed) return;
	_closed = true;
	if (closeChannel){
	    _muxConnection.sendUdpSocketClose (_remoteId);
	    // we'll unregister from the remoteIOHEngine in connectionClosed callback
	}
	if (notifyAgent){
	    iterateNotifyClose ();
	    _agentsList.clear ();
	}
	_engine.releaseSocketId (_id);
	_engine.getRemoteUdpChannelsMeter ().inc (-1);
    }
    
    // called in remote ioh exec
    protected void received (final long sessionId, final String remoteIP, final int remotePort, ByteBuffer data){
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
			receivedData (sessionId, remoteIP, remotePort, copied);
		    }finally{
			_flowController.release ();
		    }
		}};
	schedule (receivedRunnable);
    }
    // called in this exec
    protected void receivedData (long sessionId, String remoteIP, int remotePort, ByteBuffer buff){
	MuxClient agent = _agentsList.pick (null);
	if (agent == null){
	    // we dont check it in disabled() since we may want in the case of SIP for ex to return a response 503
	    return;
	}
	sendAgent (agent, new InetSocketAddress (remoteIP, remotePort), true, 0L, false, buff);
    }

    @Override
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.notifyRemoteUdpBind () || agent.isRemoteIOHEngine ()) // remoteIOH engine dont request UDP ports - else UDP datagrams are not forwarded to remoteIOH
	    return super.agentConnected (agent, state);
	return false;
    }

    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	agent.getMuxHandler ().udpSocketBound (agent, _id, _localIP, _localPort, _shared, connectionId, 0);
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().udpSocketClosed (agent, _id);
    }
    
    @Override
    public boolean close (MuxClient agent){
	//TODO log
	return _muxConnection.sendUdpSocketClose (_remoteId);
    }
    @Override
    public boolean shutdown (MuxClient agent){
	//TODO log
	return _muxConnection.sendUdpSocketClose (_remoteId);
    }
    @Override
    public void enableRead (MuxClient agent){
	// TODO log
	_muxConnection.enableRead (_remoteId);
    }
    @Override
    public void disableRead (MuxClient agent){
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
	logSendOut (agent, to, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	_muxConnection.sendUdpSocketData (_remoteId, to.getAddress ().getHostAddress (), to.getPort (), null, 0, copy, buffs);
	return true;
    }
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	if (_logger.isDebugEnabled ())
	    logSendAgent (agent, "FORWARD from "+from, buffs);
	if (copy)
	    agent.getMuxHandler ().udpSocketData (agent, _id, sessionId, from.getAddress ().getHostAddress (), from.getPort (), null, 0, ByteBufferUtils.aggregate (true, true, buffs));
	else
	    agent.getExtendedMuxHandler ().udpSocketData (agent, _id, sessionId, from.getAddress ().getHostAddress (), from.getPort (), null, 0, buffs);
	return true;
    }

    @Override
    // arg is the remoteAddr
    public void logSendOut (MuxClient agent, Object arg, ByteBuffer... buffs){
	if (_logger.isDebugEnabled ())
	    super.logSendOut (agent, "SEND TO "+arg, buffs);
    }
}
