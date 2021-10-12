// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh.impl;

import com.alcatel.as.radius.parser.*;
import com.alcatel.as.radius.ioh.*;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentFactory;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import com.alcatel_lucent.as.management.annotation.config.*;

import com.nextenso.mux.*;
import com.nextenso.mux.util.MuxIdentification;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class RadiusIOH {

    public static final String APP_NAME = "RadiusIOH";
    
    protected Logger _logger = Logger.getLogger("as.ioh.radius.proc");
    public static Dictionary<String, String> _system;
    
    protected BundleContext _osgi;
    protected Map<String, RadiusMuxProcessor> _procs = new HashMap<String, RadiusMuxProcessor> ();
    protected IOHServices _services;
    
    public RadiusIOH (){
    }
    @Reference
    public void setServices (IOHServices services){
	_services = services;
    }
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
    public void setSystemConfig(Dictionary<String, String> system){
	_logger.info ("@Reference setSystemConfig");
        _system = system;
    }
    
    public void unsetSystemConfig(Dictionary<String, String> system){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]	
    }

    @FileDataProperty(title="Radius Udp Server",
		      fileData="defRadiusUdpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening udp endpoints.")
    public final static String CONF_RADIUS_UDP_SERVERS = "radius.udp.servers";
    @FileDataProperty(title="Radius Tcp Server",
		      fileData="defRadiusTcpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening tcp endpoints.")
    public final static String CONF_RADIUS_TCP_SERVERS = "radius.tcp.servers";
    
    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
	_osgi = ctx;
	updated (conf);
	for (RadiusMuxProcessor proc : _procs.values ())
	    proc.register (_osgi);
    }
    @Modified
    public void updated (Map<String, String> conf){
	_logger.info ("Configuration : "+conf);
	_services.getServerFactory ().newUdpServerConfig ("radius", conf.get (CONF_RADIUS_UDP_SERVERS));
	_services.getServerFactory ().newTcpServerConfig ("radius", conf.get (CONF_RADIUS_TCP_SERVERS));
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setRadiusIOHRouterFactory (RadiusIOHRouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	_logger.warn ("@Reference setRadiusIOHRouterFactory : "+id+" : "+routerF);
	RadiusMuxProcessor proc = new RadiusMuxProcessor (this, id = "radius.ioh.mux."+id, routerF, properties);
	_procs.put (id, proc);
	if (_osgi != null){
	    proc.register (_osgi);
	}
    }
    public synchronized void unsetRadiusIOHRouterFactory (RadiusIOHRouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]		
    }
    
    protected static class RadiusMuxProcessor implements TcpServerProcessor {

	protected RadiusIOH _ioh;
	protected Logger _logger;
	protected Map<String, String> _props;
	protected String _id, _toString;
	protected RadiusIOHRouterFactory _routerFactory;
	protected BundleContext _osgi;
	
	protected RadiusMuxProcessor (RadiusIOH ioh, String id, RadiusIOHRouterFactory routerFactory, Map<String, String> props){
	    _ioh = ioh;
	    _id = id;
 	    _routerFactory = routerFactory;
	    _props = props;
	    _toString = "RadiusMuxProcessor["+id+"]";
	    _logger = Logger.getLogger (id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}
	protected RadiusMuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    props.put ("processor.advertize.id", "296");
	    props.put ("processor.advertize.name", "RadiusIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    for (String key : _props.keySet ()) props.put (key, _props.get (key));
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("radius.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : radius.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "radius";
	    String extProcId = id != null ? "radius.ioh.ext."+id : "radius.ioh.ext";
	    final RadiusIOHEngine engine = new RadiusIOHEngine (engineId, _ioh._services, _routerFactory);
	    server.attach (engine.init (_ioh, server, _osgi, _ioh._system));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});
	    RadiusExtProcessor ioProc = new RadiusExtProcessor (extProcId, engine);
	    ioProc.register (_osgi);
	    // set local factory
	    IOHLocalMuxFactory localFactory = new IOHLocalMuxFactory (engineId, engine);
	    localFactory.setMeteringService(_ioh._services.getMeteringService(), _osgi);
	    localFactory.register (_osgi);
	    server.getProperties ().put ("advertize.mux.factory.local", engineId);
	    server.getProperties ().put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){
	    RadiusIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : radius.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close RadiusExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    RadiusIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }

    protected static class RadiusExtProcessor implements UdpServerProcessor {
	protected RadiusIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	protected BundleContext _osgi;
	
	protected RadiusExtProcessor (String id, RadiusIOHEngine engine){
	    _id = id;
	    _engine = engine;
	    _toString = "RadiusExtProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public RadiusExtProcessor register (BundleContext ctx){
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    ctx.registerService (new String[]{UdpServerProcessor.class.getName ()}, this, props);
	    return this;
	}

	public void serverCreated (UdpServer server){
	    server.getProperties ().put (UdpServer.PROP_READ_BUFFER_DIRECT, true);
	}
	
	public void serverOpened (UdpServer server){
	    server.getServerChannel ().attach (_engine.serverOpened (server));
	}

	public void serverFailed (UdpServer server, Object cause){
	}

	public void serverUpdated (UdpServer server){
	}
	
	public void serverClosed (UdpServer server){
	}

	public void serverDestroyed (UdpServer server){}

	public UdpChannelListener getChannelListener (UdpChannel cnx){
	    return (UdpChannelListener) cnx.attachment ();
	}

    }
    
    public static int getIntProperty (String name, Map<String, Object> props, Integer def){
	Object o = props.get (name);
	if (o == null){
	    props.put (name, def);
	    return def.intValue ();
	}
	if (o instanceof String){
	    int i = Integer.parseInt (((String)o).trim ());
	    props.put (name, i);
	    return i;
	}
	if (o instanceof Integer){
	    return ((Integer) o).intValue ();
	}
	props.put (name, def);
	return def.intValue ();
    }
    public static boolean getBooleanProperty (String name, Map<String, Object> props, boolean def){
	Object o = props.get (name);
	if (o == null){
	    props.put (name, def);
	    return def;
	}
	if (o instanceof String){
	    boolean b = Boolean.parseBoolean (((String)o).trim ());
	    props.put (name, b);
	    return b;
	}
	if (o instanceof Boolean){
	    return ((Boolean) o).booleanValue ();
	}
	props.put (name, def);
	return def;
    }
    public static String getStringProperty (String name, Map<String, Object> props, String def){
	Object o = props.get (name);
	if (o == null){
	    props.put (name, def);
	    return def;
	}
	return o.toString ();
    }
}
