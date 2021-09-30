package com.alcatel.as.ioh.engine.generic;

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
import com.alcatel_lucent.as.management.annotation.config.*;


@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class GenericIOH {

    protected Logger _logger = Logger.getLogger("as.ioh.generic");
    protected MuxProcessor _processor;
    protected IOHServices _services;
    
    public GenericIOH (){
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

	protected GenericIOH _ioh;
	protected Logger _logger;
	protected BundleContext _osgi;
	protected String _toString;
	
	protected MuxProcessor (GenericIOH ioh){
	    _ioh = ioh;
	    _toString = "GenericMuxProcessor";
	    _logger = ioh._logger;
	}
	public String toString (){ return _toString;}
	protected MuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", "generic.ioh.mux");
	    props.put ("processor.advertize.id", "287"); // MAYBE modified in the xml (with the same property name)
	    props.put ("processor.advertize.name", "GenericIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("generic.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : generic.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "generic";
	    String extProcId = id != null ? "generic.ioh.ext."+id : "generic.ioh.ext";
	    final GenericIOHEngine engine = new GenericIOHEngine (engineId, _ioh._services);
	    server.attach (engine.init (server));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});
	    GenericExtProcessor ioProc = new GenericExtProcessor (extProcId, engine);
	    ioProc.register (_osgi);
	    // set local factory
	    IOHLocalMuxFactory localFactory = new IOHLocalMuxFactory (engineId, engine);
	    localFactory.register (_osgi);
	    server.getProperties ().put ("advertize.mux.factory.local", engineId);
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){}
	public void serverDestroyed (TcpServer server){
	    GenericIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : generic.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close GenericExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    GenericIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }

    protected static class GenericExtProcessor implements TcpServerProcessor {
	protected GenericIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	protected BundleContext _osgi;
	
	protected GenericExtProcessor (String id, GenericIOHEngine engine){
	    _id = id;
	    _engine = engine;
	    _toString = "GenericExtProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public GenericExtProcessor register (BundleContext ctx){
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    _logger.info (this+" : registered");
	    return this;
	}

	public void serverCreated (TcpServer server){}
	
	public void serverOpened (TcpServer server){
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
