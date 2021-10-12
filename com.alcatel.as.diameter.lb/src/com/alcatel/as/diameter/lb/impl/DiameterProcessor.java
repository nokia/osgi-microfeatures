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

import java.util.concurrent.atomic.*;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;

import com.alcatel_lucent.as.management.annotation.config.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;

public class DiameterProcessor implements TcpServerProcessor, SctpServerProcessor {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb");
    private static final AtomicInteger seed = new AtomicInteger (0);
    
    private DiameterLoadBalancer _lb;
    private ClientFactory _clientFactory;
    private DiameterRouter _router;
    
    public DiameterProcessor (DiameterLoadBalancer lb, DiameterRouter router){
	_lb = lb;
	_router = router;
    }
    
    public DiameterRouter getDiameterRouter (){
	return _router;
    }
    
    public DiameterLoadBalancer getDiameterLoadBalancer (){
	return _lb;
    }
    
    /**********************************************
     *           Server open/update/close         *
     **********************************************/
    
    public void serverCreated (TcpServer server){
	LOGGER.info ("serverCreated : "+server);
    }
    public void serverCreated (SctpServer server){
	LOGGER.info ("serverCreated : "+server);
    }
    public void serverOpened (TcpServer server){
	// NOTE : it is unwise to close the server at this stage
	LOGGER.info ("serverStarted : "+server);
	_lb.activated ();
	initServer (server);
    }    
    public void serverOpened (SctpServer server){
	// NOTE : it is unwise to close the server at this stage
	LOGGER.info ("serverStarted : "+server);
	_lb.activated ();
	initServer (server);
    }
    public void serverFailed (TcpServer server, Object cause){
	LOGGER.debug ("serverFailed : "+server);
    }
    public void serverFailed (SctpServer server, Object cause){
	LOGGER.debug ("serverFailed : "+server);
    }
    public void serverUpdated (TcpServer server){
	LOGGER.info ("serverUpdated : "+server);
    }
    public void serverUpdated (SctpServer server){
	LOGGER.info ("serverUpdated : "+server);
    }
    public void serverClosed (TcpServer server){
	LOGGER.info ("serverClosed : "+server);
	stopCounters (server.getProperties ());
    }
    public void serverClosed (SctpServer server){
	LOGGER.info ("serverClosed : "+server);
	stopCounters (server.getProperties ());
    }
    public void serverDestroyed (TcpServer server){
	LOGGER.info ("serverDestroyed : "+server);
    }
    public void serverDestroyed (SctpServer server){
	LOGGER.info ("serverDestroyed : "+server);
    }
    private void initServer (Server server){
	Object reactor = server.getProperties ().get ("diameter.lb.client.reactor");
	if (reactor != null){
	    server.getProperties ().put ("diameter.lb.client.reactor", Integer.parseInt ((String)reactor));
	}
	initCounters (server.getProperties ());
    }
    private void initCounters (Map<String, Object> props){
	Counters counters = new Counters ("diameter.lb.listen:"+props.get ("server.name"), Counters.AGGREGATED);
	counters.start ("Listening server : "+props.get ("server.name"));
	props.put ("diameter.lb.counters", counters);
    }
    private void stopCounters (Map<String, Object> props){
	Counters counters = (Counters) props.get ("diameter.lb.counters");
	counters.stop ();
    }

    // a gogo command to dump info
    public boolean infoCommand (TcpServer server, String arg, Map map){
	return getServerInfo (server, server.getProperties (), map, true);
    }
    public boolean infoCommand (SctpServer server, String arg, Map map){
	return getServerInfo (server, server.getProperties (), map, false);
    }

    private boolean getServerInfo (Object server, Map serverProps, Map commandMap, boolean tcp){
	Counters counters = (Counters) serverProps.get ("diameter.lb.counters");
	Map info = (Map) commandMap.get (server);
	for (Meter meter : counters.METERS_ALL) info.put (Counters.getName (meter), String.valueOf (meter.getValue ()));
	return true;
    }
    
    /**********************************************
     *           connection mgmt                  *
     **********************************************/
    
    public void connectionAccepted(TcpServer server,
				   TcpChannel acceptedChannel,
				   Map<String, Object> props){
	init (acceptedChannel, props, new TcpClientContext (this, acceptedChannel, setContextProperties (props)));
    }
    public void connectionAccepted(SctpServer server,
				   SctpChannel acceptedChannel,
				   Map<String,Object> props){
	init (acceptedChannel, props, new SctpClientContext (this, acceptedChannel, setContextProperties (props)));
    }
    private Map<String, Object> setContextProperties (Map<String, Object> props){
	Integer reactor = (Integer) props.get ("diameter.lb.client.reactor");
	if (reactor != null){
	    int i = (Math.abs (seed.getAndIncrement ()))%reactor;
	    props.put (Client.PROP_CLIENT_REACTOR, "r-"+i);
	}
	return props;
    }
    private void init (AsyncChannel channel, Map<String,Object> props, Runnable context){
	channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	channel.attach (context);
	PlatformExecutor exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
	exec.execute (context, ExecutorPolicy.SCHEDULE);
    }
    
    public TcpChannelListener getChannelListener (TcpChannel cnx){
	return (TcpChannelListener) cnx.attachment ();
    }
    public SctpChannelListener getChannelListener (SctpChannel cnx){
	return (SctpChannelListener) cnx.attachment ();
    }
}
