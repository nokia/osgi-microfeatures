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

public class RemoteIOHTcpServerChannel extends IOHChannel {

    protected RemoteIOHEngine _remoteEngine;
    protected MuxConnection _muxConnection;
    protected String _localIP;
    protected int _localPort;
    protected int _remoteId;
    
    /*
     * A RemoteIOHTcpServerChannel can only be shared
     * we keep it in the Remote ioh exec
     */

    // called in the remote ioh thread
    public RemoteIOHTcpServerChannel (RemoteIOHEngine engine){
	super (engine.getIOHEngine (), true);
	_exec = engine.getPlatformExecutor ();
	_remoteEngine = engine;
	_muxConnection = engine.getMuxConnection (); // since it may change, we need to keep a pointer on this one
	_id = _engine.reserveSocketId (this, _remoteEngine.id (), true);
	_logger = engine.getIOHEngine ().getRemoteLogger ();
    }
    public RemoteIOHTcpServerChannel listening (int sockId, String localIP, int localPort,
						boolean secure, long connectionId){
	_toString = new StringBuilder ()
	    .append ("RemoteTcpServerChannel[id=").append (_id).append (", ").append (localIP).append (':').append (localPort).append (']')
	    .toString ();
	_remoteId = sockId;
	_localIP = localIP;
	_localPort = localPort;
	_secure = secure;
	_connectionId = connectionId;
	_engine.getRemoteTcpServersMeter ().inc (1);
	_remoteEngine.getTcpServerChannelsByRemoteId ().put (_remoteId, this);
	if (_logger.isInfoEnabled ())
	    _logger.info (this+" : listening");
	// dont close the socket if no agent
	_agentsList = new MuxClientList ();
	iterateAgentConnected (_remoteEngine.getMuxClientList ());
	return this;
    }

    @Override
    // called in remote ioh thread
    public void connectionClosed (){
	if (_remoteEngine.isOpen ()){
	    _remoteEngine.getTcpServerChannelsByRemoteId ().remove (_remoteId);
	}
	close (true, false);
    }

    @Override
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
	_engine.getRemoteTcpServersMeter ().inc (-1);
    }
    
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	if (_engine.notifyRemoteTcpListen () || agent.isRemoteIOHEngine ()){ // we notify spontaneously for remoteIOH (same for udp servers)
	    return super.agentConnected (agent, state);
	}
	return false;
    }
    protected void notifyOpenToAgent (MuxClient agent, long connectionId){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyOpenToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketListening (agent, _id, _localIP, _localPort, _secure, 0L, 0);
    }
    protected void notifyCloseToAgent (MuxClient agent){
	if (_logger.isDebugEnabled ())
	    _logger.debug (this+" : notifyCloseToAgent : "+agent);
	agent.getMuxHandler ().tcpSocketClosed (agent, _id);
    }
    
    public boolean sendOut (MuxClient agent, InetSocketAddress to, boolean checkBuffer, boolean copy, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
    public boolean sendAgent (MuxClient agent, InetSocketAddress from, boolean checkBuffer, long sessionId, boolean copy, ByteBuffer... buffs){throw new IllegalStateException (toString ());}
}
