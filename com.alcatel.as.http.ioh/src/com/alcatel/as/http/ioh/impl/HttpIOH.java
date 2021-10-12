// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.ioh.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;

import com.alcatel.as.http.ioh.HttpIOHRouterFactory;
import com.alcatel.as.http2.ConnectionFactory;
import com.alcatel.as.ioh.engine.IOHEngine;
import com.alcatel.as.ioh.engine.IOHLocalMuxFactory;
import com.alcatel.as.ioh.engine.IOHServices;
import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.service.coordinator.Callback;
import com.alcatel.as.service.coordinator.Coordination;
import com.alcatel.as.service.coordinator.Participant;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE, property={"coordination=ACTIVATION"})
public class HttpIOH implements Participant {

    public static final String PROP_REMOTE_IMMEDIATE = "http.ioh.remote.immediate";
    public static final String PROP_MODE_SERVER = "http.ioh.server";
    public static final String PROP_MODE_PROXY = "http.ioh.proxy";
    
    @FileDataProperty(title="Http Server",
		      fileData="defHttpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening endpoints.")
    public final static String CONF_HTTP_SERVERS = "http.tcp.servers";
    
    protected static AtomicInteger ID = new AtomicInteger(1);    

    protected Logger _logger = Logger.getLogger("as.ioh.http.proc");
    public static Dictionary<String, String> _system;
    
    protected BundleContext _osgi;
    protected Map<String, HttpMuxProcessor> _procs = new HashMap<String, HttpMuxProcessor> ();
    protected Callback _activationCB;
    protected boolean _activated;
    protected IOHServices _services;
    protected ConnectionFactory _connF;
    
    public HttpIOH (){
    }
    @Reference
    public void setServices (IOHServices services){
	_services = services;
    }
    @Reference
    public void setConnectionF (ConnectionFactory cf){
	_connF = cf;
    }
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
    public void setSystemConfig(Dictionary<String, String> system){
	_logger.info ("@Reference setSystemConfig");
        _system = system;
    }
    public void unsetSystemConfig(Dictionary<String, String> system){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
    	_osgi = ctx;
	for (HttpMuxProcessor proc : _procs.values ())
	    proc.register (_osgi);
	update (conf);
    }

    @Modified
    public synchronized void update (Map<String, String> conf){
	_services.getServerFactory ().newTcpServerConfig ("http", conf.get (CONF_HTTP_SERVERS));
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setHttpIOHRouterFactory (HttpIOHRouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	_logger.warn ("@Reference setHttpIOHRouterFactory : "+id+" : "+routerF);
	HttpMuxProcessor proc = new HttpMuxProcessor (this, id = "http.ioh.mux."+id, routerF, properties);
	_procs.put (id, proc);
	if (_osgi != null){
	    proc.register (_osgi);
	}
    }
    public synchronized void unsetHttpIOHRouterFactory (HttpIOHRouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    public void join (Coordination coordination, Callback cb){
	synchronized (this){
	    _logger.info (this+" : join coordination ACTIVATION");
	    _activationCB = cb;
	    if (_activated == false) return;
	    _activationCB.joined (null);
	    _logger.info (this+" : joined coordination ACTIVATION");
	    _activationCB = null;
	}
    }
    public void activated (){
	synchronized (this){
	    if (_activated) return;
	    _logger.info (this+" : activated");
	    _activated = true;
	    if (_activationCB == null) return;
	    _logger.info (this+" : joined coordination ACTIVATION");
	    _activationCB.joined (null);
	    _activationCB = null;
	}
    }

    public String toString () { return "HttpIOH";}
    
    protected static class HttpMuxProcessor implements TcpServerProcessor {

	protected HttpIOH _ioh;
	protected Logger _logger;
	protected Map<String, String> _props;
	protected String _id, _toString;
	protected HttpIOHRouterFactory _routerFactory;
	protected BundleContext _osgi;
	
	protected HttpMuxProcessor (HttpIOH ioh, String id, HttpIOHRouterFactory routerFactory, Map<String, String> props){
	    _ioh = ioh;
	    _id = id;
 	    _routerFactory = routerFactory;
	    _props = props;
	    _toString = "HttpMuxProcessor["+id+"]";
	    _logger = Logger.getLogger ("as.ioh."+id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	protected HttpMuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    props.put ("processor.advertize.id", "286");
	    props.put ("processor.advertize.name", "HttpIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    for (String key : _props.keySet ()) props.put (key, _props.get (key));
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("http.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : http.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "http";
	    String extProcId = id != null ? "http.ioh.ext."+id : "http.ioh.ext";
	    final HttpIOHEngine engine = new HttpIOHEngine (engineId, _ioh._services, _routerFactory, _ioh._connF);
	    server.attach (engine.init (server, _osgi, _ioh._system));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});
	    HttpExtProcessor ioProc = new HttpExtProcessor (extProcId, _ioh, engine);
	    ioProc.register (_osgi);
	    String remoteProcId = id != null ? "http.ioh.remote."+id : "http.ioh.remote";
	    HttpRemoteProcessor remoteProc = new HttpRemoteProcessor (remoteProcId, engine);
	    remoteProc.register (_osgi);
	    // set local factory
	    IOHLocalMuxFactory localFactory = new IOHLocalMuxFactory (engineId, engine);
	    localFactory.setMeteringService(_ioh._services.getMeteringService(), _osgi);
	    localFactory.register (_osgi);
	    server.getProperties ().put ("advertize.mux.factory.local", engineId);
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){
	    HttpIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : http.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close HttpExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    HttpIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }

    protected static class HttpExtProcessor implements TcpServerProcessor {
	protected HttpIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	protected HttpIOH _ioh;
	protected BundleContext _osgi;
	
	protected HttpExtProcessor (String id, HttpIOH ioh, HttpIOHEngine engine){
	    _id = id;
	    _ioh = ioh;
	    _engine = engine;
	    _toString = "HttpExtProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public HttpExtProcessor register (BundleContext ctx){
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}

	public void serverCreated (TcpServer server){}
	
	public void serverOpened (TcpServer server){
	    _engine.serverOpened (server);
	    _ioh.activated ();
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
    
    protected static class HttpRemoteProcessor implements TcpServerProcessor {
	protected HttpIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	
	protected HttpRemoteProcessor (String id, HttpIOHEngine engine){
	    _id = id;
	    _engine = engine;
	    _toString = "HttpRemoteProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public HttpRemoteProcessor register (BundleContext ctx){
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    props.put ("processor.advertize.id", "292");
	    props.put ("processor.advertize.name", "RemoteHttpIOH");
	    props.put ("advertize.application.name", _engine.name ()); // we'll track remote ioh with the same id
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}

	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("http.ioh.remote.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	}
	public void serverOpened (TcpServer server){}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){}
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    acceptedChannel.attach (_engine.muxClientAccepted (acceptedChannel, props, true));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }
}
