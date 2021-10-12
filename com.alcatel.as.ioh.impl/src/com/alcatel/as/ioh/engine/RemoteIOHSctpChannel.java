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
import com.nextenso.mux.MuxHandler.*;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.mux.impl.ioh.*;

import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.*;

public class RemoteIOHSctpChannel extends IOHChannel {

    protected RemoteIOHEngine _remoteEngine;
    protected MuxConnection _muxConnection;
    protected String[] _remoteIPs, _localIPs;
    protected int _remotePort, _localPort, _maxOutStreams, _maxInStreams;
    protected boolean _accepted;
    protected int _remoteId;
    
    /*
     * A RemoteIOHSctpChannel can only be shared
     */

    // called in the remote ioh thread
    public RemoteIOHSctpChannel (RemoteIOHEngine engine){
	super (engine.getIOHEngine (), true);
	_exec = _engine.createQueueExecutor ();
	_remoteEngine = engine;
	_muxConnection = engine.getMuxConnection (); // since it may change, we need to keep a pointer on this one
	_channel = engine.getChannel ();
	_readMeter = _engine.getRemoteReadSctpMeter ();
	_sendMeter = _engine.getRemoteSendSctpMeter ();
	_sendOutBufferMonitor = _engine.getSendRemoteIOHBufferMonitor ();
	_flowController = engine.getFlowController ();
	_id = _engine.reserveSocketId (this, _remoteEngine.id (), true);
	_logger = engine.getIOHEngine ().getRemoteLogger ();
    }
    public RemoteIOHSctpChannel connected (int sockId, String[] remoteIPs, int remotePort, String[] localIPs, int localPort, int maxOutStreams, int maxInStreams, boolean accepted, long connectionId, boolean secure){
	_toString = new StringBuilder ()
	    .append ("RemoteSctpChannel[id=").append (_id).append (", remote=").append (remoteIPs[0]).append (':').append (remotePort).append (']')
	    .toString ();
	_remoteId = sockId;
	_remoteIPs = remoteIPs;
	_remotePort = remotePort;
	_localIPs = localIPs;
	_localPort = localPort;
	_maxOutStreams = maxOutStreams;
	_maxInStreams = maxInStreams;
	_accepted = accepted;
	_connectionId = connectionId;
	_secure = secure;
	_engine.getRemoteSctpChannelsMeter ().inc (1);
	_remoteEngine.getSctpChannelsByRemoteId ().put (_remoteId, this);
	_remoteEngine.getSctpChannelsByLocalId ().put (_id, this);
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
	    _remoteEngine.getSctpChannelsByRemoteId ().remove (_remoteId);
	    _remoteEngine.getSctpChannelsByLocalId ().remove (_id);
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
	    _muxConnection.sendSctpSocketClose (_remoteId);
	    // we'll unregister from the remoteIOHEngine in connectionClosed callback
	}
	if (notifyAgent){
	    iterateNotifyClose ();
	    _agentsList.clear ();
	}
	_engine.releaseSocketId (_id);
	_engine.getRemoteSctpChannelsMeter ().inc (-1);
    }

    
    // called in remote ioh exec
    protected void received(final long sessionId, ByteBuffer data, final String addr,
			    final boolean isUnordered, final boolean isComplete, final int ploadPID, final int streamNumber){
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
			receivedData (sessionId, copied, addr, isUnordered, isComplete, ploadPID, streamNumber);
		    }finally{
			_flowController.release ();
		    }
		}};
	schedule (receivedRunnable);
    }
    protected void receivedData (long sessionId, ByteBuffer data, String addr,
				 boolean isUnordered, boolean isComplete, int ploadPID, int streamNumber){
	//TO BE OVERRIDDEN
	// since shared=true, not clear how to pick an agent w/o parsing
    }
    protected void sendFailed (String addr, int streamNumber, ByteBuffer buf, int errcode){
	// not sure what agent to pick to notify ...
    }
    protected void peerAddressChanged(final String addr, final int port, final SctpAddressEvent event) {
	Runnable r = new Runnable (){
		public void run (){
		    if (_closed) return;
		    _agentsList.iterate (new MuxClientList.Iterator (){
			    public Object next (MuxClient agent, Object ctx){
				agent.getMuxHandler ().sctpPeerAddressChanged (agent, _id, addr, port, event);
				return null;}
			}, null);
		}};
	schedule (r);
    }

    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	agent.getMuxHandler ().sctpSocketConnected (agent, _id, connectionId, _remoteIPs, _remotePort, _localIPs, _localPort, _maxOutStreams, _maxInStreams, _accepted, _secure, 0);
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	// TODO : check if agent is overloaded --> not useful for now
	agent.getMuxHandler ().sctpSocketClosed (agent, _id);
    }
    
    @Override
    // called in agent thread
    public boolean close (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	return _muxConnection.sendSctpSocketClose (_remoteId);
    }
    @Override
    // called in agent thread
    public boolean shutdown (MuxClient agent){
	//TODO check buffer --> not useful for now
	// TODO log
	return _muxConnection.sendSctpSocketReset (_remoteId);
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

    @Override
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	throw new IllegalStateException ();
    }
    @Override
    public boolean sendSctpOut (MuxClient agent, String addr, boolean unordered, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean checkBuffer, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferOut (null) == false){
		_channel.shutdown (); // when remote is overloaded --> we close the socket
		return false;
	    }
	}
	logSendOut (agent, null, buffs);
	_sendMeter.inc (ByteBufferUtils.remaining (buffs));
	_muxConnection.sendSctpSocketData (_remoteId, addr,  unordered, complete, ploadPID, streamNumber, timeToLive, copy, buffs);
	return true;
    }
    @Override
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	throw new RuntimeException ("Method N/A in SCTP");
    }
    public boolean sendAgent (MuxClient agent, SocketAddress from, boolean isComplete, boolean isUnordered, int ploadPID, int streamNumber, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){
	if (checkBuffer){
	    if (checkSendBufferAgent (agent, null) == false){
		return false;
	    }
	}
	logSendAgent (agent, null, buffs);
	if (copy)
	    agent.getMuxHandler ().sctpSocketData (agent, _id, sessionId, ByteBufferUtils.aggregate (true, true, buffs), ((InetSocketAddress) from).getHostString (), isUnordered, isComplete, ploadPID, streamNumber);
	else
	    agent.getExtendedMuxHandler ().sctpSocketData (agent, _id, sessionId, buffs, ((InetSocketAddress) from).getHostString (), isUnordered, isComplete, ploadPID, streamNumber);
	return true;
    }
}
