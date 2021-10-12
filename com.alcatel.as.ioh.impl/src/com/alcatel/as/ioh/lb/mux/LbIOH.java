// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.mux;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentFactory;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Modified;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.alcatel_lucent.as.management.annotation.config.*;

import com.alcatel.as.ioh.lb.*;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class LbIOH {

    public static final String PROP_AGENT_ID = "lb.ioh.agent.id";
    public static final String PROP_AGENT_GROUP = "lb.ioh.agent.group";
    
    protected static final Logger LOGGER = Logger.getLogger ("as.ioh.lb.mux");

    protected IOHServices _services;
    private BundleContext _osgi;
    private Map<String, IOHRouterFactory> _routers = new HashMap<> ();
    private Map<String, ParserFactory> _parsers = new HashMap<> ();
    private Map<String, MulticastMuxProcessor> _mprocs = new HashMap<> ();
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setRouterFactory (IOHRouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	LOGGER.info ("@Reference setRouterFactory : "+id+" : "+routerF);
	_routers.put (id, routerF);
	initProcs ();
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setParserFactory (ParserFactory parserF, Map<String, String> properties){
	String id = properties.get ("parser.id");
	LOGGER.info ("@Reference setParserFactory : "+id+" : "+parserF);
	_parsers.put (id, parserF);
	initProcs ();
    }
    public void unsetRouterFactory (IOHRouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    public void unsetParserFactory (ParserFactory parserF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    
    
    public LbIOH (){
    }
    @Reference
    public void setServices (IOHServices services){
	_services = services;
    }
    @Modified
    public void updated (Map<String, String> conf){
    }
    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
	_osgi = ctx;
	initProcs ();
    }

    private void initProcs (){
	if (_osgi == null) return;
	for (String rid: _routers.keySet ()){
	    for (String pid: _parsers.keySet ()){
		String id = (pid.equals (rid)) ? "lb.ioh.mux."+pid : "lb.ioh.mux."+pid+"."+rid;
		if (_mprocs.get (id) != null) continue;
		LOGGER.info ("Registering m-processor-mux : "+id);
		MulticastMuxProcessor proc = new MulticastMuxProcessor (this, id, _parsers.get (pid), _routers.get (rid));
		_mprocs.put (id, proc);
		proc.register ();
	    }
	}
    }
    
    protected static class MulticastMuxProcessor implements TcpServerProcessor {

	protected Logger _logger;
	private LbIOH _ioh;
	private IOHRouterFactory _routerF;
	private String _id, _toString;
	private ParserFactory _parserF;
	
	protected MulticastMuxProcessor (LbIOH ioh, String id, ParserFactory parserF, IOHRouterFactory routerF){
	    _ioh = ioh;
	    _id = id;
	    _parserF = parserF;
	    _routerF = routerF;
	    _logger = Logger.getLogger ("as."+id);
	    _toString = "LbIOH.MProcessor["+_id+"]";
	}
	public String toString (){ return _toString;}
	protected MulticastMuxProcessor register (){
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    props.put ("processor.advertize.name", "LbIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    _ioh._osgi.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get (PROP_AGENT_GROUP); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : lb.ioh.id=["+id+"]");
	    String agentid = (String) server.getProperties ().get (PROP_AGENT_ID);
	    String engineId = id != null ? id : "lb";
	    String extProcId = id != null ? "lb.ioh.ext."+id : "lb.ioh.ext";
	    final LbIOHEngine engine = new LbIOHEngine (engineId, _ioh._services, _routerF, _parserF);
	    server.attach (engine.init (server));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_ioh._osgi);}
		});
	    MulticastExtProcessor ioProc = new MulticastExtProcessor (extProcId, engine);
	    ioProc.register (_ioh._osgi);
	    // set local factory
	    IOHLocalMuxFactory localFactory = new IOHLocalMuxFactory (engineId, engine);
	    localFactory.setMeteringService(_ioh._services.getMeteringService(), _ioh._osgi);
	    localFactory.register (_ioh._osgi);
	    server.getProperties ().put ("advertize.mux.factory.local", engineId);
	    if (agentid != null)
		server.getProperties ().put ("processor.advertize.id", agentid);
	    else
		_logger.error (this+" : Missing property lb.ioh.agent.id in LB Mux endpoint : advertisement may be ineffective");
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){
	    LbIOHEngine engine = server.attachment ();
	    engine.setConfigs (_routerF.newIOHRouterConfig (server.getProperties ()),
			       _parserF.newParserConfig (server.getProperties ()));
	}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){
	    LbIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : lb.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close MulticastExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    LbIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }

    protected static class MulticastExtProcessor implements TcpServerProcessor {
	protected LbIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	protected BundleContext _osgi;
	
	protected MulticastExtProcessor (String id, LbIOHEngine engine){
	    _id = id;
	    _engine = engine;
	    _toString = "LbIOH.ExtProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public MulticastExtProcessor register (BundleContext ctx){
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    _logger.info (this+" : registered");
	    return this;
	}

	public void serverCreated (TcpServer server){}
	
	public void serverOpened (TcpServer server){
	    _engine.schedule (new Runnable (){
		    public void run (){_engine.start (_osgi);}
		});
	    _engine.serverOpened (server);
	}

	public void serverFailed (TcpServer server, Object cause){
	}

	public void serverUpdated (TcpServer server){
	}
	
	public void serverClosed (TcpServer server){
	    _engine.serverClosed (server);
	}

	public void serverDestroyed (TcpServer server){}
	
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    acceptedChannel.attach (_engine.connectionAccepted (server, acceptedChannel, props));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
	
    }
}
