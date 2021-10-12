// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

public class TcpClientContext extends ClientContext implements TcpChannelListener {

    public static Logger LOGGER = Logger.getLogger ("as.ioh.lb.tcp");
    
    private Parser _clientParser;
    private boolean _ignoreData = false;
    
    protected TcpClientContext (LoadBalancer.MulticastProcessor proc, TcpChannel channel, Parser clientParser, Parser serverParser, Router router, Logger logger, Meters meters, Map<String, Object> props){
	super (proc, channel, router, logger, meters, props);
	_clientParser = clientParser;
	_serverParser = serverParser;
	_toString = new StringBuilder ().append ("TcpClient[").append (channel.getRemoteAddress ()).append (']').toString ();
	_meters.getOpenTcpChannelsMeter ().inc (1);
    }
    protected TcpClientContext (LoadBalancer.UnicastProcessor proc, TcpChannel channel, Parser clientParser, UnicastRouter router, Logger logger, Meters meters, Set<java.net.InetSocketAddress> adverts, Map<String, Object> props){
	super (proc, channel, router, logger, meters, adverts, props);
	_clientParser = clientParser;
	_toString = new StringBuilder ().append ("TcpClient[").append (channel.getRemoteAddress ()).append (']').toString ();
	_meters.getOpenTcpChannelsMeter ().inc (1);
    }

    /************************************************
     ** TcpChannelListener for client connections **
     ************************************************/
	
    public int messageReceived (TcpChannel channel, ByteBuffer data){
	if (_ignoreData){
	    data.position (data.limit ());
	    return 0;
	}
	_meters.getReadTcpMeter ().inc (data.remaining ());
	try{
	    if (_logTrafficLogger.isEnabledFor (_logTrafficLevel)){
		StringBuilder sb = new StringBuilder ()
		    .append (this+" : RECV [");
		LoadBalancer.dumpData (sb, data);
		sb.append (']');
		_logTrafficLogger.log (_logTrafficLevel, sb.toString ());
	    }
	    Chunk chunk;
	    while ((chunk = _clientParser.parse (data)) != null){
		if (_logger.isDebugEnabled ())
		    _logger.debug (this+" : messageReceived from client : "+chunk);
		if (chunk.newMessage ())
		    _meters.getReadTcpMessageMeter ().inc (1);
		clientMessage (chunk);
	    }
	}catch(Exception e){
	    _logger.warn (this+" : exception while handling client data", e);
	    data.position (data.limit ());
	    channel.close ();
	    _ignoreData = true; // disable future reads until closed is called back
	}
	return 0;
    }
    
    public void receiveTimeout (TcpChannel channel){
	clientTimeout ();
    }

    public void writeBlocked (TcpChannel channel){
	clientBlocked ();
    }

    public void writeUnblocked (TcpChannel channel){
	clientUnblocked ();
    }

    public void connectionClosed(TcpChannel cnx){
	_meters.getOpenTcpChannelsMeter ().inc (-1);
	_meters.getClosedTcpChannelsMeter ().inc (1);
	clientClosed ();
    }
}
