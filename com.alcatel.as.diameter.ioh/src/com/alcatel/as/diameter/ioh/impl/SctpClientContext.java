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

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.diameter.parser.*;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

import com.alcatel.as.diameter.ioh.impl.DiameterIOHEngine.DiameterIOHSctpChannel;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;
import com.alcatel.as.ioh.engine.IOHChannel;

public class SctpClientContext {
    
    protected static final AtomicLong SEED = new AtomicLong (1);

    public static Logger LOGGER = Logger.getLogger ("as.diameter.ioh.sctp");
    
    // the final below guarantees the memory barrier between the constructor and the run()
    private ClientContext<SctpChannel> _clientContext;
    private IOHChannel _channel;

    protected SctpClientContext (IOHChannel channel, SctpChannel sctp, Map<String, Object> props, boolean incoming){
	_channel = channel;
	try{
	    InetSocketAddress addr = (InetSocketAddress) sctp.getRemoteAddresses ().iterator ().next ();
	    String remote = addr.getHostString ();
	    String id = new StringBuilder ()
		.append (incoming ? "R." : "I.")
		.append ("sctp.")
		.append (remote.replace ("/", ""))
		.append ('.')
		.append (addr.getPort ())
		.append ('.')
		.append (SEED.getAndIncrement ())
		.toString ();
	    _clientContext = new ClientContext<SctpChannel> (id, channel, props, false, incoming);
	    _clientContext.init ();
	}catch(Throwable t){
	    LOGGER.info ("Met an exception while accepting an Sctp channel : "+t+" : rejecting it");
	    _clientContext = null;
	    channel.reject ();
	}
    }
    public boolean agentConnected (MuxClient agent, MuxClientState state){
	return _clientContext.agentConnected (agent, state);
    }
    public boolean agentClosed (MuxClient agent){
	return _clientContext.agentClosed (agent);
    }
    public boolean agentStopped (MuxClient agent){
	return _clientContext.agentStopped (agent);
    }
    public boolean agentUnStopped (MuxClient agent){
	return _clientContext.agentUnStopped (agent);
    }
    public boolean handleClientMessage (DiameterMessage msg){
	_clientContext.clientMessage (msg);
	return true;
    }
    public boolean handleAgentMessage (MuxClient agent, DiameterMessage msg){
	_clientContext.agentMessage (agent, msg);
	return true;
    }
    public void handleClientTimeout (){
	_clientContext.clientTimeout ();
    }
    public void handleClientClosed (){
	if (_clientContext == null) return; // exception in init
	_clientContext.clientClosed ();
    }
    public void closeClientConnection (Object reason){
	_clientContext.closeClient (reason);
    }

    protected void setDprTimeout (long delay){ _clientContext.setDprTimeout (delay);};

}
