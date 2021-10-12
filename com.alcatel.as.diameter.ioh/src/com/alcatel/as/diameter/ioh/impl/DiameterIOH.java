// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.alcatel.as.diameter.ioh.DiameterIOHRouterFactory;
import com.alcatel.as.ioh.engine.IOHEngine;
import com.alcatel.as.ioh.engine.IOHLocalMuxFactory;
import com.alcatel.as.ioh.engine.IOHServices;
import com.alcatel.as.ioh.server.SctpServer;
import com.alcatel.as.ioh.server.SctpServerProcessor;
import com.alcatel.as.ioh.server.TcpServer;
import com.alcatel.as.ioh.server.TcpServerProcessor;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.SelectProperty;

import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;

@Component(service={}, immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DiameterIOH {

    public static final String APP_NAME = "DiameterIOH";

    public static final String PROP_METER_APP = "diameter.ioh.meter.app";
    public static final String PROP_METER_APP_RESP = "diameter.ioh.meter.app.resp";
    
    @IntProperty(title="Maximum CER Size",
		 defval=0,
		 min=0,
		 required=false,
		 dynamic=false,
		 section="General",
		 help="Indicates the maximum size for an incoming control message (CER / DPR / DWR). When a larger message comes in, the connection is closed : DoS suspected. Set to 0 to disable the check.")
    public final static String CONF_CER_MAX_SIZE = "diameter.ioh.cer.max.size";
    @IntProperty(title="Maximum Application Message Size",
		 defval=0,
		 min=0,
		 required=false,
		 dynamic=false,
		 section="General",
		 help="Indicates the maximum size for an application message. When a larger message comes in, the connection is closed : DoS suspected. Set to 0 to disable the check.")
    public final static String CONF_APP_MAX_SIZE = "diameter.ioh.app.max.size";
    @IntProperty(title="DeviceWatchdogRequest Timeout",
		 defval=3000,
		 min=0,
		 required=false,
		 dynamic=false,
		 section="General",
		 help="Indicates the timeout in milliseconds to trigger a DWR. 0 stands for no timeout (infinite).")
    public final static String CONF_DWR_TIMEOUT = "diameter.ioh.dwr.timeout";
    @IntProperty(title="DeviceWatchdogRequest Attempts",
		 defval=2,
		 min=0,
		 required=true,
		 dynamic=true,
		 section="General",
		 help="Indicates the number of DWR to send upon read timeout before declaring a peer disconnected.")
    public final static String CONF_DWR_ATTEMPTS = "diameter.ioh.dwr.attempts";
    @IntProperty(title="Client Requests Timeout",
		 defval=3000,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="Timers",
		 help="Indicates the maximum duration for a client-originated request context to be kept in memory before timing out. This is applicable to subsequent CER only.")
    public final static String CONF_TIMER_CLIENT_REQ = "diameter.ioh.client-req.timeout";
    @IntProperty(title="DisconnectPeerRequest Timeout",
		 defval=1000,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="Timers",
		 help="Indicates the delay for final cleanup upon DPR. This is a safeguard in case normal disconnections do not occur in the meantime.")
    public final static String CONF_TIMER_DPR = "diameter.ioh.dpr.timeout";
    @BooleanProperty(title="Graceful Shutdown",
		     defval=true,
		     required=true,
		     dynamic=true,
		     section="General",
		     help="Indicates if a DisconnectPeerRequest should be sent when closing the client connection.")
    public final static String CONF_DPR = "diameter.ioh.dpr";
    @SelectProperty(title="CEA Host-IP-Address Transformation Policy",
		    defval="REPLACE",
		    required=true,
		    dynamic=true,
		    range = { "IGNORE", "ADD", "REPLACE" },
		    section="General",
		    help="Indicates if the Host-IP-Address in the CEA should be modified."
		    + "<ul>"
		    + "<li><b>IGNORE:</b> Leave the CEA Host-IP-Address AVP(s) untouched.</li>"
		    + "<li><b>ADD:</b> Leave the existing Host-IP-Address AVP(s) and add the configured ones.</li>"
		    + "<li><b>REPLACE:</b> Remove the existing Host-IP-Address AVP(s) and add the configured ones.</li>"
		    + "</ul>")
    public final static String CONF_CEA_HOST_IP_ADDRESS_POLICY = "diameter.ioh.cea.host-ip-address.policy";
    @SelectProperty(title="CEA Host-IP-Address Transformation Content",
		     defval="GROUP",
		     required=true,
		     dynamic=true,
		     range = { "ENDPOINT", "GROUP" },
		     section="General",
		     help="Indicates the addresses to insert into the CEA when the policy is ADD or REPLACE."
		     + "<ul>"
		     + "<li><b>ENDPOINT:</b>The listening address(es) of the connected endpoint.</li>"
		     + "<li><b>GROUP:</b>The listening addresses of all the endpoints belonging to the same group.</li>"
		     + "</ul>")
    public final static String CONF_CEA_HOST_IP_ADDRESS_CONTENT = "diameter.ioh.cea.host-ip-address.content";
    @SelectProperty(title="CER Host-IP-Address Transformation Policy",
		    defval="REPLACE",
		    required=true,
		    dynamic=true,
		    range = { "IGNORE", "ADD", "REPLACE" },
		    section="General",
		    help="Indicates if the Host-IP-Address in the CER should be modified."
		    + "<ul>"
		    + "<li><b>IGNORE:</b> Leave the CER Host-IP-Address AVP(s) untouched.</li>"
		    + "<li><b>ADD:</b> Leave the existing Host-IP-Address AVP(s) and add the configured ones.</li>"
		    + "<li><b>REPLACE:</b> Remove the existing Host-IP-Address AVP(s) and add the configured ones.</li>"
		    + "</ul>")
    public final static String CONF_CER_HOST_IP_ADDRESS_POLICY = "diameter.ioh.cer.host-ip-address.policy";
    @SelectProperty(title="CER Host-IP-Address Transformation Content",
		     defval="GROUP",
		     required=true,
		     dynamic=true,
		     range = { "ENDPOINT", "GROUP" },
		     section="General",
		     help="Indicates the addresses to add to the CER when the policy is ADD or REPLACE."
		     + "<ul>"
		     + "<li><b>ENDPOINT:</b>The configured address(es) of the local peer.</li>"
		     + "<li><b>GROUP:</b>All the listening addresses of the endpoints belonging to the same group.</li>"
		     + "</ul>")
    public final static String CONF_CER_HOST_IP_ADDRESS_CONTENT = "diameter.ioh.cer.host-ip-address.content";
    
    public final static String CONF_CER_HOST_IP_ADDRESS = "diameter.ioh.cer.host-ip-address";
    public final static String CONF_CEA_HOST_IP_ADDRESS = "diameter.ioh.cea.host-ip-address";
    
    public final static String REBOOTING = "0"; 
    public final static String BUSY = "1"; 
    public final static String DO_NOT_WANT_TO_TALK_TO_YOU = "2"; 

    @SelectProperty(title="DPR Disconnect Reason Code",
        	required=true,
        	dynamic=true,
        	range = { REBOOTING, BUSY, DO_NOT_WANT_TO_TALK_TO_YOU },
        	defval=REBOOTING,
		    section="General",
        	help="<p>DPR reason code to be used when Graceful Shutdown."
          	+ "<ul>"
          	+ "<li><b>0:</b> REBOOTING,</li>"
          	+ "<li><b>1:</b> BUSY</li>"
          	+ "<li><b>2:</b> DO_NOT_WANT_TO_TALK_TO_YOU</li>"
          	+ "</ul>")
    public final static String CONF_DPR_REASON = "diameter.ioh.dpr.reason";

    @FileDataProperty(title="Diameter Tcp Server",
		      fileData="defDiameterTcpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening tcp endpoints.")
    public final static String CONF_DIAMETER_TCP_SERVERS = "diameter.tcp.servers";
    @FileDataProperty(title="Diameter Sctp Server",
		      fileData="defDiameterSctpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening sctp endpoints.")
    public final static String CONF_DIAMETER_SCTP_SERVERS = "diameter.sctp.servers";

    public final static String CONF_DIAMETER_SCTP_ORDERED = "diameter.ioh.sctp.ordered";
    public final static String CONF_DIAMETER_SCTP_MSTREAMS = "diameter.ioh.sctp.multistreams";
    public final static String CONF_DIAMETER_SCTP_CONNECT_STREAM_OUT = "diameter.ioh.sctp.connect.stream.out";

    public final static String CONF_DIAMETER_LATENCY_PROC = "diameter.ioh.latency.proc";
    public final static String CONF_DIAMETER_LATENCY_PROC_SAMPLING = "diameter.ioh.latency.proc.sampling";
    public final static String CONF_DIAMETER_LATENCY_PROC_AGENT = "diameter.ioh.latency.proc.agent"; 

    public final static String CONF_DIAMETER_SECURE_DELAYED_REQUIRED_R = "diameter.ioh.tcp.secure.delayed.required";
    public final static String CONF_DIAMETER_SECURE_DELAYED_REQUIRED_I = "diameter.ioh.tcp.connect.secure.delayed.required";

    public final static String CONF_DELAY_DPA_CLOSE = "diameter.ioh.dpa.close.delay";
    public final static String CONF_TIMEOUT_DPA_CLOSE = "diameter.ioh.dpa.close.timeout";

    public final static String CONF_AGENT_REMOTE_MIN = "diameter.ioh.agent.remote.min";

    public final static String CONF_DPR_REASON_INTERNAL = "diameter.ioh.dpr.reason.internal";
    
    protected Logger _logger = Logger.getLogger("as.ioh.diameter.proc");
    public static Dictionary<String, String> _system;
    
    protected BundleContext _osgi;
    protected Map<String, DiameterMuxProcessor> _procs = new HashMap<String, DiameterMuxProcessor> ();
    protected IOHServices _services;
        
    public DiameterIOH (){
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

    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
	_osgi = ctx;
	updated (conf);
	for (DiameterMuxProcessor proc : _procs.values ())
	    proc.register (_osgi);
    }
    @Modified
    public void updated (Map<String, String> conf){
	_logger.info ("Configuration : "+conf);
	_dwrTimeout = Integer.parseInt (conf.get (CONF_DWR_TIMEOUT));
	_dwrAttempts = Integer.parseInt (conf.get (CONF_DWR_ATTEMPTS));
	_clientReqTimeout = Integer.parseInt (conf.get (CONF_TIMER_CLIENT_REQ));
	_dprTimeout = Integer.parseInt (conf.get (CONF_TIMER_DPR));
	_sendDpr = Boolean.parseBoolean (conf.get (CONF_DPR));
	_dprReasonCode = Integer.parseInt (conf.get (CONF_DPR_REASON));
	if (conf.get (CONF_CER_MAX_SIZE) != null) // added in a patch - may not be set
	    _maxCERSize = Integer.parseInt (conf.get (CONF_CER_MAX_SIZE));
	if (conf.get (CONF_APP_MAX_SIZE) != null) // added in a patch - may not be set
	    _maxAppSize = Integer.parseInt (conf.get (CONF_APP_MAX_SIZE));
	_hostIPAddrCEAPolicy = conf.get (CONF_CEA_HOST_IP_ADDRESS_POLICY);
	if (_hostIPAddrCEAPolicy == null) _hostIPAddrCEAPolicy = "IGNORE"; // by safety
	_hostIPAddrCEAContent = conf.get (CONF_CEA_HOST_IP_ADDRESS_CONTENT);
	if (_hostIPAddrCEAContent == null) _hostIPAddrCEAContent = "GROUP"; // by safety
	_hostIPAddrCERPolicy = conf.get (CONF_CER_HOST_IP_ADDRESS_POLICY);
	if (_hostIPAddrCERPolicy == null) _hostIPAddrCERPolicy = "IGNORE"; // by safety
	_hostIPAddrCERContent = conf.get (CONF_CER_HOST_IP_ADDRESS_CONTENT);
	if (_hostIPAddrCERContent == null) _hostIPAddrCERContent = "GROUP"; // by safety
	_services.getServerFactory ().newTcpServerConfig ("diameter", conf.get (CONF_DIAMETER_TCP_SERVERS));
	_services.getServerFactory ().newSctpServerConfig ("diameter", conf.get (CONF_DIAMETER_SCTP_SERVERS));
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setDiameterIOHRouterFactory (DiameterIOHRouterFactory routerF, Map<String, String> properties){
	String id = properties.get ("router.id");
	_logger.warn ("@Reference setDiameterIOHRouterFactory : "+id+" : "+routerF);
	DiameterMuxProcessor proc = new DiameterMuxProcessor (this, id = "diameter.ioh.mux."+id, routerF, properties);
	_procs.put (id, proc);
	if (_osgi != null){
	    proc.register (_osgi);
	}
    }
    public synchronized void unsetDiameterIOHRouterFactory (DiameterIOHRouterFactory routerF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]		
    }
    
    protected static class DiameterMuxProcessor implements TcpServerProcessor {

	protected DiameterIOH _ioh;
	protected Logger _logger;
	protected Map<String, String> _props;
	protected String _id, _toString;
	protected DiameterIOHRouterFactory _routerFactory;
	protected BundleContext _osgi;
	
	protected DiameterMuxProcessor (DiameterIOH ioh, String id, DiameterIOHRouterFactory routerFactory, Map<String, String> props){
	    _ioh = ioh;
	    _id = id;
 	    _routerFactory = routerFactory;
	    _props = props;
	    _toString = "DiameterMuxProcessor["+id+"]";
	    _logger = Logger.getLogger (id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}
	protected DiameterMuxProcessor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    // the following props will be set in the opened TcpServers, all the processor.advertize in particular will be used in advertizing
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    props.put ("processor.advertize.id", "289");
	    props.put ("processor.advertize.name", "DiameterIOH");
	    props.put ("advertize.mux.factory.remote", "ioh");
	    for (String key : _props.keySet ()) props.put (key, _props.get (key));
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    String target = (String) server.getProperties ().get ("diameter.ioh.agent.group"); // it is an alias to "processor.advertize.group.target"
	    if (target != null) server.getProperties ().put ("advertize.group.target", target);
	    String id = (String) server.getProperties ().get (IOHEngine.PROP_APP_NAME);
	    _logger.info (this+" : MUX serverCreated : "+server+" : diameter.ioh.id=["+id+"]");
	    String engineId = id != null ? id : "diameter";
	    String extProcId = id != null ? "diameter.ioh.ext."+id : "diameter.ioh.ext";
	    final DiameterIOHEngine engine = new DiameterIOHEngine (engineId, _ioh._services, _routerFactory);
	    server.attach (engine.init (_ioh, server, _osgi, _ioh._system));
	    engine.schedule (new Runnable (){
		    public void run (){ engine.start (_osgi);}
		});
	    DiameterExtProcessor ioProc = new DiameterExtProcessor (extProcId, engine);
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
	    DiameterIOHEngine engine = server.attachment ();
	    _logger.info (this+" : MUX serverClosed : "+server+" : diameter.id="+engine.name ());
	    //TODO ???? what to do ?
	    engine.stop ();
	    //TODO close DiameterExtProcessor service
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    DiameterIOHEngine engine = server.attachment ();
	    acceptedChannel.attach (engine.muxClientAccepted (acceptedChannel, props, false));
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}
    }

    protected static class DiameterExtProcessor implements TcpServerProcessor, SctpServerProcessor {
	protected DiameterIOHEngine _engine;
	protected String _toString;
	protected Logger _logger;
	protected String _id;
	protected BundleContext _osgi;
	
	protected DiameterExtProcessor (String id, DiameterIOHEngine engine){
	    _id = id;
	    _engine = engine;
	    _toString = "DiameterExtProcessor["+_id+"]";
	    _logger = Logger.getLogger ("as.ioh."+_id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}

	public DiameterExtProcessor register (BundleContext ctx){
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    ctx.registerService (new String[]{TcpServerProcessor.class.getName (), SctpServerProcessor.class.getName ()}, this, props);
	    return this;
	}

	public void serverCreated (TcpServer server){
	    if (server.getProperties ().get (TcpServer.PROP_READ_TIMEOUT) == null)
		server.getProperties ().put (TcpServer.PROP_READ_TIMEOUT, String.valueOf (_engine.getDwrTimeout ()));
	    server.getProperties ().put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	}
	
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

	public void closeConnection (TcpChannel cnx){
	    _engine.closeClientConnection (cnx);
	}

	public void serverCreated (SctpServer server){
	    if (server.getProperties ().get (SctpServer.PROP_READ_TIMEOUT) == null)
		server.getProperties ().put (SctpServer.PROP_READ_TIMEOUT, String.valueOf (_engine.getDwrTimeout ()));
	    server.getProperties ().put (SctpServer.PROP_READ_BUFFER_DIRECT, true);
	}
	
	public void serverOpened (SctpServer server){
	    _engine.serverOpened (server);
	}

	public void serverFailed (SctpServer server, Object cause){
	}

	public void serverUpdated (SctpServer server){
	}
	
	public void serverClosed (SctpServer server){
	    _engine.serverClosed (server);
	}

	public void serverDestroyed (SctpServer server){}
	
	public void connectionAccepted(SctpServer server,
				       SctpChannel acceptedChannel,
				       Map<String, Object> props){
	    acceptedChannel.attach (_engine.connectionAccepted (server, acceptedChannel, props));
	}
	
	public SctpChannelListener getChannelListener (SctpChannel cnx){
	    return (SctpChannelListener) cnx.attachment ();
	}

	public void closeConnection (SctpChannel cnx){
	    _engine.closeClientConnection (cnx);
	}
    }
    
    private int _dwrTimeout, _dwrAttempts, _dprTimeout, _clientReqTimeout, _dprReasonCode = 0;
    private int _maxCERSize, _maxAppSize;
    private boolean _sendDpr;
    private String _hostIPAddrCEAPolicy, _hostIPAddrCEAContent, _hostIPAddrCERPolicy, _hostIPAddrCERContent;

    public int getCERMaxSize (){ return _maxCERSize;}
    public int getAppMaxSize (){ return _maxAppSize;}
    public long getDwrTimeout (){ return (long) _dwrTimeout;}
    public int getDwrAttempts (){ return _dwrAttempts; }
    public int getDprTimeout (){ return _dprTimeout; }
    public int getClientReqTimeout (){ return _clientReqTimeout; }
    public boolean getSendDpr (){ return _sendDpr; }
    public int getDprReasonCode (){ return _dprReasonCode; }
    public String getCEAHostIPAddrPolicy (){ return _hostIPAddrCEAPolicy;}
    public String getCEAHostIPAddrContent (){ return _hostIPAddrCEAContent;}
    public String getCERHostIPAddrPolicy (){ return _hostIPAddrCERPolicy;}
    public String getCERHostIPAddrContent (){ return _hostIPAddrCERContent;}
    
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
