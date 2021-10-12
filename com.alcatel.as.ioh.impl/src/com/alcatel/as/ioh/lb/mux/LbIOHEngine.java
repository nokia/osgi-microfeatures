// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.tools.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.ioh.impl.conf.Property;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;


import com.alcatel.as.ioh.lb.*;

public class LbIOHEngine extends IOHEngine {

    private LbIOH _lb;
    private IOHRouterFactory _routerF;
    private ParserFactory _parserF;
    private Object _routerC, _parserC;
    private com.alcatel.as.service.metering2.Meter _readTcpMessageMeter;
    private Level _logTrafficLevel;
    private Logger _logTrafficLogger;
    
    public LbIOHEngine (String name, IOHServices services, IOHRouterFactory rf, ParserFactory pf){
	super (name, services);
	_routerF = rf;
	_parserF = pf;
    }

    public IOHEngine init (TcpServer server){
	server.getProperties ().put (PROP_TCP_ACCEPT_SHARED, "true");
	if (server.getProperties ().get (PROP_TCP_ACCEPT_SHARED_CLOSE) == null)
	    server.getProperties ().put (PROP_TCP_ACCEPT_SHARED_CLOSE, "true");
	server.getProperties ().put (PROP_TCP_LISTEN_NOTIFY, "false");
	server.getProperties ().put (PROP_UDP, "false");
	server.getProperties ().put (PROP_SCTP, "false");

	if (server.getProperties ().get (PROP_HISTORY_CHANNELS) == null)
	    server.getProperties ().put (PROP_HISTORY_CHANNELS, "true");

	_logTrafficLevel = Level.toLevel (((String) Property.getProperty (LoadBalancer.PROP_CLIENT_LOG_TRAFFIC_LEVEL, server.getProperties (), "TRACE", false)).toUpperCase ());
	String loggerS = (String) Property.getProperty (LoadBalancer.PROP_CLIENT_LOG_TRAFFIC_LOGGER, server.getProperties (), null, false);
	if (loggerS != null) _logTrafficLogger = Logger.getLogger (loggerS);
	else _logTrafficLogger = _logger;
	
	super.init (server);

	_readTcpMessageMeter = _meters.createIncrementalMeter ("read.tcp.message", null);

	return this;
    }

    protected void setConfigs (Object routerC, Object parserC){
	_routerC = routerC;
	_parserC = parserC;
    }

    @Override
    protected IOHTcpChannel newTcpChannel (IOHEngine engine, TcpServer server, TcpChannel channel, Map<String, Object> props){
	IOHRouter router = _routerF.newIOHRouter (_routerC);
	return new IOHTcpClientContext (this,
					channel,
					_parserF.newClientParser (_parserC, router.neededBuffer ()),
					router.needServerData () ? _parserF.newServerParser (_parserC, router.neededBuffer ()) : null,
					router,
					(Logger) server.getProperties ().get ("server.logger"),
					props);
    }

    public com.alcatel.as.service.metering2.Meter getReadTcpMessageMeter (){ return _readTcpMessageMeter;}
    
    
    public void logTraffic (String prefix, ByteBuffer msg){
	if (_logTrafficLogger.isEnabledFor (_logTrafficLevel)){
	    StringBuilder sb = new StringBuilder ()
		.append (this.toString ()).append (prefix);
	    LoadBalancer.dumpData (sb, msg);
	    sb.append (']');
	    _logTrafficLogger.log (_logTrafficLevel, sb.toString ());
	}	
    }
}
