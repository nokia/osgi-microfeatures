package com.nokia.as.k8s.sless.fwk.controller.ioh;

import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.tools.ChannelWriter.SendBufferMonitor;

import org.osgi.framework.BundleContext;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;
import com.nokia.as.k8s.sless.fwk.*;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.alcatel_lucent.as.management.annotation.config.*;

import com.nokia.as.k8s.sless.fwk.controller.*;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerIOH {
    
    @FileDataProperty(title="Sless Tcp Server",
		      fileData="defSlessTcpServer.txt",
		      required=true,
		      dynamic=false,
		      section="Server",
		      help="Describes the Sless controller listening endpoints.")
    public final static String CONF_SLESS_TCP_SERVERS = "sless.tcp.servers";
    

    protected Logger _logger = Logger.getLogger("sless.controller.ioh");
    protected MuxProcessor _processor;
    protected IOHServices _services;
    protected Controller _controller;
    
    public ControllerIOH (){
    }

    @Reference
    public void setController (Controller controller){
	_controller = controller;
    }
    @Reference
    public void setServices (IOHServices services){
	_services = services;
    }
    @Modified
    public void updated (Map<String, String> conf){
    }
    @Activate
    public void init (BundleContext ctx, Map<String, String> conf){
	_processor = new MuxProcessor (this).register (ctx);
	_services.getServerFactory ().newTcpServerConfig ("sless", conf.get (CONF_SLESS_TCP_SERVERS));
    }
    
    protected static class MuxProcessor implements TcpServerProcessor {

	protected ControllerIOH _ioh;
	protected Logger _logger;
	protected BundleContext _osgi;
	protected String _toString;
	
	protected MuxProcessor (ControllerIOH ioh){
	    _ioh = ioh;
	    _toString = "SlessMuxProcessor";
	    _logger = ioh._logger;
	}
	public String toString (){ return _toString;}
	protected MuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", "sless.ioh.mux");
	    props.put ("processor.advertize.id", String.valueOf (Mux.CONTROLLER_ID[0]));
	    props.put ("processor.advertize.name", "SlessIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("sless.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : sless.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "sless";
	    final ControllerIOHEngine engine = new ControllerIOHEngine (engineId, _ioh._services, _ioh._controller);
	    server.attach (engine.init (server));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});

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
	    ControllerIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : sless.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close MetersExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    ControllerIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }
}
