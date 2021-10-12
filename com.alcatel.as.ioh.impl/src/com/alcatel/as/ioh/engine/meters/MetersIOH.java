// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine.meters;

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
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.alcatel_lucent.as.management.annotation.config.*;


@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class MetersIOH {

    protected Logger _logger = Logger.getLogger("as.ioh.meters");
    protected MuxProcessor _processor;
    protected IOHServices _services;
    
    public MetersIOH (){

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
	_processor = new MuxProcessor (this).register (ctx);
    }
    
    protected static class MuxProcessor implements TcpServerProcessor {

	protected MetersIOH _ioh;
	protected Logger _logger;
	protected BundleContext _osgi;
	protected String _toString;
	
	protected MuxProcessor (MetersIOH ioh){
	    _ioh = ioh;
	    _toString = "MetersMuxProcessor";
	    _logger = ioh._logger;
	}
	public String toString (){ return _toString;}
	protected MuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", "meters.ioh.mux");
	    props.put ("processor.advertize.id", "324");
	    props.put ("processor.advertize.name", "MetersIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("meters.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : meters.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "meters";
	    final MetersIOHEngine engine = new MetersIOHEngine (engineId, _ioh._services);
	    server.attach (engine.init (server));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){
	    MetersIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : meters.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close MetersExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    MetersIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }
}
