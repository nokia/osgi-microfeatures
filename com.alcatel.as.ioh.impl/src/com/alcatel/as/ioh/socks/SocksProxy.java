// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.socks;


import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.log4j.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.ioh.impl.conf.Property;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.management.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.reporter.api.CommandScopes;

import com.alcatel.as.service.discovery.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.ioh.tools.*;

@Component(service = { TcpServerProcessor.class }, property = { "processor.id=ioh.socks"}, immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SocksProxy implements TcpServerProcessor {
    
    public static final Logger LOGGER = Logger.getLogger ("as.ioh.socks");

    public static final String PROP_CLIENT_READ_TIMEOUT = "socks.client.read.timeout";
    public static final String PROP_CLIENT_WRITE_BUFFER_MAX = "socks.client.write.buffer.max";
    
    public static final String PROP_DEST_READ_TIMEOUT = "socks.dest.read.timeout";
    public static final String PROP_DEST_WRITE_BUFFER_MAX = "socks.dest.write.buffer.max";
    
    public static final String PROP_SOCKS_V4 = "socks.v4";
    public static final String PROP_SOCKS_V5 = "socks.v5";
    
    public static final String PROP_INIT_TIMEOUT = "socks.init.timeout";

    public static final String PROP_V4_USER_ID = "socks.v4.userid";    

    public static final int PROP_V4_USER_ID_MAX = Integer.getInteger ("socks.v4.userid.max", 128);
    
    // TODO one day : transparent proxying

    private PlatformExecutors _executors;
    private MeteringService _metering;
    private ServerFactory _serverFactory;
    private BundleContext _osgi;
    private ReactorProvider _reactorP;

    @FileDataProperty(title="SocksProxy",
		      fileData="defTcpSocksPxServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening endpoints.")
		      public final static String CONF_TCP_SERVERS = "socks.tcp.servers";
    
    @Reference()
    public void setExecutors(PlatformExecutors executors){
	LOGGER.info ("@Reference setExecutors");
	_executors = executors;
    }
    @Reference()
    public void setMetering(MeteringService metering){
	LOGGER.info ("@Reference setMetering");
	_metering = metering;
    }
    @Reference
    public void setServerFactory (ServerFactory factory) throws Exception {
	LOGGER.info ("@Reference setServerFactory");
	_serverFactory = factory;
    }
    @Reference()
    public void setReactorProvider(ReactorProvider provider){
	LOGGER.info ("@Reference setReactorProvider");
	_reactorP = provider;
    }
    public PlatformExecutors getPlatformExecutors (){ return _executors;}
    public PlatformExecutor createQueueExecutor (){ return _executors.createQueueExecutor (_executors.getProcessingThreadPoolExecutor ());}
    public ReactorProvider getReactorProvider (){ return _reactorP;}
    
    @Activate
    public void activate (BundleContext ctx, Map<String, String> conf) throws Exception {
	LOGGER.info ("@Activate");
	updated (conf);
	_osgi = ctx;
    }

    @Modified
    public void updated (Map<String, String> conf) throws Exception {
	LOGGER.info ("Configuration : "+conf);
	_serverFactory.newTcpServerConfig ("socks", conf.get (CONF_TCP_SERVERS));
    }

    @Deactivate
    public void deactivate (){
	LOGGER.warn ("@Deactivate");
    }

    /**********************************************
     *           TcpServer open/update/close      *
     **********************************************/
    
    public void serverCreated (TcpServer server){
	LOGGER.info ("serverCreated : "+server);
	Map<String, Object> props = server.getProperties ();
	props.put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	String name = (String) props.get (Server.PROP_SERVER_NAME);
	Property.getProperty (PROP_DEST_WRITE_BUFFER_MAX, props, "10000000", true); // 10Meg per dest
	Property.getProperty (PROP_CLIENT_WRITE_BUFFER_MAX, props, "10000000", true); // 10Meg per client
	if (props.get (PROP_CLIENT_READ_TIMEOUT) != null) // this is an alias for read.timeout
	    props.put (TcpServer.PROP_READ_TIMEOUT, props.get (PROP_CLIENT_READ_TIMEOUT));
	TcpServerContext ctx = new TcpServerContext ();
	ctx.load (props);
	ctx._name = name;
	ctx._meters = new Meters ("as.ioh.socks."+name, "SocksProxy for server : "+name, _metering);
	props.put ("TcpServerContext", ctx);
	ctx._meters.init ();
	ctx._meters.start (_osgi);
    }
    public void serverOpened (TcpServer server){
	LOGGER.info ("serverStarted : "+server);
    }
    public void serverFailed (TcpServer server, Object cause){
	LOGGER.debug ("serverFailed : "+server);
    }
    public void serverUpdated (TcpServer server){
	LOGGER.info ("serverUpdated : "+server);
    }
    public void serverClosed (TcpServer server){
	LOGGER.info ("serverClosed : "+server);
    }
    public void serverDestroyed (TcpServer server){
	LOGGER.info ("serverDestroyed : "+server);
	getContext (server)._meters.stop ();
    }
    public TcpServerContext getContext (TcpServer server){
	return (TcpServerContext) server.getProperties ().get ("TcpServerContext");
    }
	
    /**********************************************
     *           connection mgmt                  *
     **********************************************/

    // called in the server Q
    public void connectionAccepted(TcpServer server,
				   TcpChannel channel,
				   Map<String, Object> props){
	channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	TcpClientContext ctx = new TcpClientContext (this, getContext (server), channel, props);
	channel.attach (ctx);
	ctx.start ();
    }
    
    public TcpChannelListener getChannelListener (TcpChannel cnx){
	return (TcpChannelListener) cnx.attachment ();
    }

    public static class TcpServerContext {
	public Meters _meters;
	public String _name;
	public Reactor _reactor;
	public int _clientMaxSendBuffer, _destMaxSendBuffer;
	public int _destReadTimeout;
	public long _initTimeout;
	public boolean _allow4, _allow5;
	public List<String> _userIds;
	public Map<ReactorProvider.TcpClientOption, Object> _opts = new HashMap<> ();
	public void load (Map<String, Object> props){
	    _reactor = (Reactor) props.get (Server.PROP_SERVER_REACTOR);
	    _opts.put (ReactorProvider.TcpClientOption.TIMEOUT,
		       Property.getLongProperty (TcpClient.PROP_CONNECT_TIMEOUT, props, 3000L, true));
	    String from = (String) props.get (TcpClient.PROP_CONNECT_FROM);
	    try{
		if (from != null)
		    _opts.put (ReactorProvider.TcpClientOption.FROM_ADDR, InetAddress.getByName (from));
	    }catch(Exception e){ throw new RuntimeException ("Invalid FROM address : "+from);}
	    _opts.put (ReactorProvider.TcpClientOption.TCP_NO_DELAY, true);
	    _opts.put (ReactorProvider.TcpClientOption.USE_DIRECT_BUFFER, true);
	    _destMaxSendBuffer = Property.getIntProperty (PROP_DEST_WRITE_BUFFER_MAX, props, 10000000, true); // 10Meg
	    _clientMaxSendBuffer = Property.getIntProperty (PROP_CLIENT_WRITE_BUFFER_MAX, props, 10000000, true); // 10Meg

	    _allow4 = Property.getBooleanProperty (PROP_SOCKS_V4, props, true, true);
	    _allow5 = Property.getBooleanProperty (PROP_SOCKS_V5, props, true, true);

	    _destReadTimeout = Property.getIntProperty (PROP_DEST_READ_TIMEOUT, props, -1, false);
	    _initTimeout = Property.getLongProperty (PROP_INIT_TIMEOUT, props, 2000L, true);

	    _userIds = Property.getStringListProperty (PROP_V4_USER_ID, props);
	}
	public boolean checkUserId (String user){
	    if (_userIds == null) return true;
	    return _userIds.contains (user);
	}
    }
}
