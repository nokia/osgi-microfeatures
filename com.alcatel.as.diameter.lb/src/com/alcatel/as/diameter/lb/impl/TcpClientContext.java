// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;

import com.alcatel.as.diameter.lb.*;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.reporter.api.AlarmService;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.tools.*;

public class TcpClientContext extends TcpMessageProcessor<DiameterMessage> implements Runnable {

    public static Logger LOGGER = Logger.getLogger ("as.diameter.lb.tcp");
    
    // the final below guarantees the memory barrier between the constructor and the run()
    private final ClientContext<TcpChannel> _clientContext;
    private DiameterProcessor _processor;

    protected TcpClientContext (DiameterProcessor processor, TcpChannel channel, Map<String, Object> props){
	super (new DiameterParser ());
	_processor = processor;
	String id = new StringBuilder ()
	    .append ("tcp.")
	    .append (channel.getRemoteAddress ().getAddress ().toString ().replace ("/", ""))
	    .append ('.')
	    .append (channel.getRemoteAddress ().getPort ())
	    .toString ();
	_clientContext = new ClientContext<TcpChannel> (id, channel, processor, props);
	_clientContext.getCounters ().CLIENT_TCP_CONNECTIONS.inc (1);
    }
    public void run (){
	_clientContext.init ();
    }
    
    /************************************************
     ** TcpMessageProcessor for client connections **
     ************************************************/
	
    public void messageReceived (TcpChannel channel, DiameterMessage msg){
	_clientContext.clientMessage (msg);
    }
    
    public void receiveTimeout (TcpChannel channel){
	_clientContext.clientTimeout ();
    }

    public void writeBlocked (TcpChannel channel){
	_clientContext.clientBlocked ();
    }

    public void writeUnblocked (TcpChannel channel){
	_clientContext.clientUnblocked ();
    }

    public void connectionClosed(TcpChannel cnx){
	_clientContext.getCounters ().CLIENT_TCP_CONNECTIONS.inc (-1);
	_clientContext.getCounters ().CLIENT_TCP_DISCONNECTIONS.inc (1);
	_clientContext.clientClosed ();
	if (_processor.getDiameterLoadBalancer ().sendTcpDisconnectedAlarm ()){
	    String prefix = (String) _clientContext.getProperties ().get (Server.PROP_SERVER_NAME);
	    prefix = prefix != null ? prefix+" : " : "";
	    long alarmId = DiameterLoadBalancer.ALARM_SEED.getAndIncrement ();
	    _clientContext.getLogger ().warn (_clientContext+" : connectionClosed : sending alarm "+DiameterLoadBalancer.TCP_DISCONNECTED_ALARM_CODE+" : id="+alarmId);
	    try{
		_processor.getDiameterLoadBalancer ().getAlarmService ().sendAlarm(DiameterClient.APP_NAME+"-"+alarmId, DiameterLoadBalancer.TCP_DISCONNECTED_ALARM_CODE, prefix+_clientContext+" : tcp client disconnected");
	    }catch(Exception e){
		LOGGER.error ("tcpClientClosed : failed to send alarm", e);
	    }
	}
    }

    // a gogo command to dump info
    public boolean infoCommand (TcpChannel channel, String arg, Map map){
	return _clientContext.getClientInfo ((Map) map.get (channel));
    }

    // a gogo command to dump cer
    public boolean cerCommand (TcpChannel channel, String arg, Map map){
	String cer = _clientContext.dumpCER ();
	map.put ("System.out", cer != null ? cer : "-unknown-");
	return true;
    }

    // a gogo command to get the aliases of a given channel
    public boolean aliasCommand (TcpChannel channel, List<String> aliases){
	return _clientContext.getClientAliases (aliases);
    }
}
