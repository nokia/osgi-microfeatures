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

public class SctpClientContext extends SctpMessageProcessor<DiameterMessage> implements Runnable {

    public static Logger LOGGER = Logger.getLogger ("as.diameter.lb.sctp");
    
    // the final below guarantees the memory barrier between the constructor and the run()
    private final ClientContext<SctpChannel> _clientContext;
    private Map<SocketAddress, Long> _addrUnreachableAlarms;
    private DiameterProcessor _processor;
    
    protected SctpClientContext (DiameterProcessor processor, SctpChannel channel, Map<String, Object> props){
	super (new DiameterParser ());
	_processor = processor;
	if (_processor.getDiameterLoadBalancer ().sendSctpAddressUnreachableAlarm ()) _addrUnreachableAlarms = new HashMap<> ();
	String id = null;
	try{
	    // the iterator below sometimes fails....
	    InetSocketAddress sockaddr = (InetSocketAddress) channel.getRemoteAddresses ().iterator ().next ();
	    id = new StringBuilder ()
		.append ("sctp.")
		.append (sockaddr.getAddress ().toString ().replace ("/", ""))
		.append ('.')
		.append (sockaddr.getPort ())
		.toString ();
	}catch(Throwable t){
	    LOGGER.warn ("Unexpected Exception while instanciating SctpClientContext", t);
	}
	_clientContext = id != null ? new ClientContext<SctpChannel> (id, channel, processor, props) : null;
	if (_clientContext != null) _clientContext.getCounters ().CLIENT_SCTP_CONNECTIONS.inc (1);
	else channel.shutdown ();
    }
    public void run (){
	if (_clientContext != null) {
	    _clientContext.init ();
	}
    }
    
    /************************************************
     ** SctpMessageProcessor for client connections **
     ************************************************/
	
    public void messageReceived (SctpChannel channel, DiameterMessage msg){
	_clientContext.clientMessage (msg);
    }
    
    public void receiveTimeout (SctpChannel channel){
	_clientContext.clientTimeout ();
    }

    public void writeBlocked (SctpChannel channel){
	_clientContext.clientBlocked ();
    }

    public void writeUnblocked (SctpChannel channel){
	_clientContext.clientUnblocked ();
    }

    public void connectionClosed(SctpChannel cnx, Throwable t){
	if (_clientContext == null) return; // maybe null if shutdown was called in the constructor
	_clientContext.getCounters ().CLIENT_SCTP_CONNECTIONS.inc (-1);
	_clientContext.getCounters ().CLIENT_SCTP_DISCONNECTIONS.inc (1);
	_clientContext.clientClosed ();
	if (_addrUnreachableAlarms != null){
	    for (SocketAddress addr : _addrUnreachableAlarms.keySet ()){
		long alarmId = _addrUnreachableAlarms.get (addr);
		String prefix = (String) _clientContext.getProperties ().get (Server.PROP_SERVER_NAME);
		prefix = prefix != null ? prefix+" : " : "";
		_clientContext.getLogger ().warn (_clientContext+" : connectionClosed : "+addr+" : clearing alarm "+DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE+" : id="+alarmId);
		try{
		    _processor.getDiameterLoadBalancer ().getAlarmService ().clearAlarm(DiameterClient.APP_NAME+"-"+alarmId, DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE, prefix+_clientContext+" : "+addr+" COMM_CLOSED");
		}catch(Exception e){
		    LOGGER.error ("sctpAddressUnreachable : failed to clear alarm", e);
		}
	    }
	    _addrUnreachableAlarms.clear ();
	}
	if (_processor.getDiameterLoadBalancer ().sendSctpDisconnectedAlarm ()){
	    String prefix = (String) _clientContext.getProperties ().get (Server.PROP_SERVER_NAME);
	    prefix = prefix != null ? prefix+" : " : "";
	    long alarmId = DiameterLoadBalancer.ALARM_SEED.getAndIncrement ();
	    _clientContext.getLogger ().warn (_clientContext+" : connectionClosed : sending alarm "+DiameterLoadBalancer.SCTP_DISCONNECTED_ALARM_CODE+" : id="+alarmId);
	    try{
		_processor.getDiameterLoadBalancer ().getAlarmService ().sendAlarm(DiameterClient.APP_NAME+"-"+alarmId, DiameterLoadBalancer.SCTP_DISCONNECTED_ALARM_CODE, prefix+_clientContext+" : sctp client disconnected");
	    }catch(Exception e){
		LOGGER.error ("sctpClientClosed : failed to send alarm", e);
	    }
	}
    }

    public void peerAddressChanged(SctpChannel cnx, SocketAddress addr, SctpChannelListener.AddressEvent event) {
	if (_clientContext == null) return;
	if (_clientContext.getLogger ().isDebugEnabled ())
	    _clientContext.getLogger ().debug (_clientContext+" : sctp address event : "+addr+" : "+event);
	switch (event){	    
	case ADDR_UNREACHABLE:
	    if (_addrUnreachableAlarms == null) break;
	    if (_addrUnreachableAlarms.containsKey (addr)) break; // already sent
	    _clientContext.getCounters ().CLIENT_SCTP_UNREACHABLE_EVTS.inc (1);
	    String prefix = (String) _clientContext.getProperties ().get (Server.PROP_SERVER_NAME);
	    prefix = prefix != null ? prefix+" : " : "";
	    long alarmId = DiameterLoadBalancer.ALARM_SEED.getAndIncrement ();
	    _addrUnreachableAlarms.put (addr, alarmId);
	    _clientContext.getLogger ().warn (_clientContext+" : sctp address "+addr+" unreachable : sending alarm "+DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE+" : id="+alarmId);
	    try{
		_processor.getDiameterLoadBalancer ().getAlarmService ().sendAlarm(DiameterClient.APP_NAME+"-"+alarmId, DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE, prefix+_clientContext+" : "+addr+" "+event);
	    }catch(Exception e){
		LOGGER.error ("sctpAddressUnreachable : failed to send alarm", e);
	    }
	    break;
	case ADDR_AVAILABLE:
	case ADDR_CONFIRMED:
	case ADDR_REMOVED:
	    if (_addrUnreachableAlarms == null) break;
	    if (!_addrUnreachableAlarms.containsKey (addr)) break; // no alarm sent
	    alarmId = _addrUnreachableAlarms.remove (addr);
	    prefix = (String) _clientContext.getProperties ().get (Server.PROP_SERVER_NAME);
	    prefix = prefix != null ? prefix+" : " : "";
	    _clientContext.getLogger ().warn (_clientContext+" : "+event+" : "+addr+" : clearing alarm "+DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE+" : id="+alarmId);
	    try{
		_processor.getDiameterLoadBalancer ().getAlarmService ().clearAlarm(DiameterClient.APP_NAME+"-"+alarmId, DiameterLoadBalancer.SCTP_UNREACHABLE_ALARM_CODE, prefix+_clientContext+" : "+addr+" "+event);
	    }catch(Exception e){
		LOGGER.error ("sctpAddressUnreachable : failed to clear alarm", e);
	    }
	    break;
	}
    }
    
    // a gogo command to dump info
    public boolean infoCommand (SctpChannel channel, String arg, Map map){
	return _clientContext.getClientInfo ((Map) map.get (channel));
    }

    // a gogo command to dump cer
    public boolean cerCommand (SctpChannel channel, String arg, Map map){
	String cer = _clientContext.dumpCER ();
	map.put ("System.out", cer != null ? cer : "-unknown-");
	return true;
    }

    // a gogo command to get the aliases of a given channel
    public boolean aliasCommand (SctpChannel channel, List<String> aliases){
	return _clientContext.getClientAliases (aliases);
    }
}
