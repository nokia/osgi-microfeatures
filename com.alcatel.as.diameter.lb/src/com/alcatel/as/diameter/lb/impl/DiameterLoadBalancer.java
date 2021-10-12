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

import org.osgi.framework.BundleContext;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.management.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel_lucent.as.management.annotation.alarm.*;
import com.alcatel.as.service.reporter.api.AlarmService;
import com.alcatel.as.service.coordinator.*;
import org.osgi.service.event.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE, property={"coordination=ACTIVATION",EventConstants.EVENT_TOPIC+"=diameter/lb/overload"})
public class DiameterLoadBalancer implements Participant, EventHandler {  // participes to ACTIVATION service for active/standby
    
    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb");
    public static AtomicLong ALARM_SEED = new AtomicLong (System.currentTimeMillis () & 0x174876e7ffL); // use 11 digits to init the seed
    
    public static final String PROP_CLIENT_ID = "diameter.lb.client.id";
    public static final int DEF_PRIORITY = 1;
    public static final int DEF_READ_TIMEOUT = 2000;

    private ClientFactory _clientFactory;
    private BundleContext _osgi;
    private Map<String, DiameterRouter> _routers = new HashMap<String, DiameterRouter> ();
    private String _instName, _quorum="1";
    private int _dwrAttempts, _dprTimeout, _clientReqTimeout, _dprReasonCode;
    private boolean _sendDpr, _useLoadFactor, _hostIPAddrTransformCER;
    private String _hostIPAddrTransformCEA;
    private Dictionary<String, String> _system;
    private PlatformExecutors _executors;
    private TimerService _timerService;
    private MeteringService _metering;
    private boolean _alarmTcpDisc, _alarmSctpDisc, _alarmSctpUnreach;
    private AlarmService _alarmService;
    private Callback _activationCB;
    private boolean _activated;
    private Gload _gload;
    private Meter _cpuMeter;
    private ServerFactory _serverFactory;
    
    @Override
    public String toString (){ return "DiameterLoadBalancer";}

    @FileDataProperty(title="Diameter TCP Endpoints",
		      fileData="defDiamLBTcpServer.txt",
		      required=true,
		      dynamic=true,
		      section="General",
		      help="Indicates the diameter TCP endpoints.")
		      public final static String CONF_TCP_SERVERS = "diameter.lb.tcp.servers";
    @FileDataProperty(title="Diameter SCTP Endpoints",
		      fileData="defDiamLBSctpServer.txt",
		      required=true,
		      dynamic=true,
		      section="General",
		      help="Indicates the diameter SCTP endpoints.")
		      public final static String CONF_SCTP_SERVERS = "diameter.lb.sctp.servers";
    @FileDataProperty(title="Diameter Destinations",
		      fileData="defDiamLBTcpClient.txt",
		      required=true,
		      dynamic=true,
		      section="General",
		      help="Indicates the backend Diameter servers.")
		      public final static String CONF_TCP_CLIENTS = "diameter.lb.tcp.clients";
    
    @IntProperty(title="DeviceWatchdogRequest Attempts",
		 defval=2,
		 min=0,
		 required=true,
		 dynamic=true,
		 section="General",
		 help="Indicates the number of DWR to send upon read timeout before declaring a peer disconnected.")
    public final static String CONF_DWR_ATTEMPTS = "diameter.lb.dwr";
    @StringProperty(title="Servers Quorum",
		    defval="1",
		    required=true,
		    dynamic=true,
		    section="General",
		    help="Indicates the minimum number of open server connections. If the value ends with '%', then it corresponds to the percentage of declared servers (with appropriate rounding).")
    public final static String CONF_QUORUM = "diameter.lb.quorum";
    @IntProperty(title="Client Requests Timeout",
		 defval=3000,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="Timers",
		 help="Indicates the maximum duration for a client-originated request context to be kept in memory before timing out. This is applicable to subsequent CER only.")
    public final static String CONF_TIMER_CLIENT_REQ = "diameter.lb.client-req.timeout";
    @IntProperty(title="DisconnectPeerRequest Timeout",
		 defval=1000,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="Timers",
		 help="Indicates the delay for final cleanup upon DPR. This is a safeguard in case normal disconnections do not occur in the meantime.")
    public final static String CONF_TIMER_DPR = "diameter.lb.dpr.timeout";
    @BooleanProperty(title="Graceful Shutdown",
		     defval=true,
		     required=true,
		     dynamic=true,
		     section="General",
		     help="Indicates if a DisconnectPeerRequest should be sent when closing the client connection.")
    public final static String CONF_DPR = "diameter.lb.dpr";
    @BooleanProperty(title="LoadFactor Monitoring",
		     defval=false,
		     required=true,
		     dynamic=true,
		     section="General",
		     help="Indicates if Servers may use the LoadFactor AVP (Nokia proprietary). If yes, then the Load Balancer will look for the AVP and apply the specified values in the load distribution algorithm.")
    public final static String CONF_LOADFACTOR = "diameter.lb.loadfactor";
    @BooleanProperty(title="CER Host-IP-Address Transformation",
		     defval=false,
		     required=true,
		     dynamic=true,
		     section="General",
		     help="Indicates if the Host-IP-Address in the CER should be transformed according to the SDM specifications.")
    public final static String CONF_CER_HOST_IP_ADDRESS_TRANSFORM = "diameter.lb.host-ip-address.transform"; // the property name was left unchanged while .cer. should be better
    @SelectProperty(title="CEA Host-IP-Address Transformation",
		    defval="REPLACE",
		    required=true,
		    dynamic=true,
		    range = { "IGNORE", "ADD", "REPLACE" },
		    section="General",
		    help="Indicates if the Host-IP-Address in the CEA should be enriched with the local endpoint IP address(es)."
		    + "<ul>"
		    + "<li><b>IGNORE:</b> Leave the CEA Host-IP-Address AVP(s) untouched.</li>"
		    + "<li><b>ADD:</b> Leave the existing Host-IP-Address AVP(s) and add the endpoint address(es)</li>"
		    + "<li><b>REPLACE:</b> Remove the existing Host-IP-Address AVP(s) and add the endpoint address(es)</li>"
		    + "</ul>")
    public final static String CONF_CEA_HOST_IP_ADDRESS_TRANSFORM = "diameter.lb.cea.host-ip-address.transform";
    
    public final static String CONF_REMOTE_LB = "diameter.lb";
    public final static String CONF_CEA_HOST_IP_ADDRESS = "diameter.lb.cea.host-ip-address";
    
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
    public final static String CONF_DPR_REASON = "diameter.lb.dpr.reason";
    
    @MSelectProperty(title="Client Connections Alarms",
		     required=false,
		     dynamic=true,
		     range = { "TCP-Disconnection", "SCTP-Disconnection", "SCTP-Address-Unreachable" },
		     defval="",
		     section="General",
		     help="<p>Indicates the client-side connection events that trigger an alarm:"
		     + "<ul>"
		     + "<li>TCP Disconnection : when a client-side TCP connection is closed</li>"
		     + "<li>SCTP Disconnection : when a client-side SCTP connection is closed</li>"
		     + "<li>SCTP Address Unreachable : when a client-side SCTP address becomes unreachable</li>"
		     + "</ul>")
    public final static String CONF_ALARM_CONNECTION = "diameter.lb.alarm.connection";
    
    @Alarm(name="SctpClientDisconnected",
           severity=AlarmSeverity.MAJOR,
           message="SCTP client disconnected",
           description="This alarm is triggered when a client-side SCTP connection is closed, regardless of the cause.",
           generatedBy="DiameterLB",
           correctiveAction="None on the Diameter LoadBalancer. Some analysis may be done on the client side.")
    protected static final int SCTP_DISCONNECTED_ALARM_CODE = 161;
    @Alarm(name="SctpAddressUnreachable",
           severity=AlarmSeverity.MAJOR,
           message="SCTP address unreachable",
           description="This alarm is triggered when an address of a client-side SCTP connection becomes unreachable.",
           generatedBy="DiameterLB",
           correctiveAction="None on the Diameter LoadBalancer. Some analysis may be done on the network elements.")
    protected static final int SCTP_UNREACHABLE_ALARM_CODE = 162;
    @Alarm(name="TcpClientDisconnected",
           severity=AlarmSeverity.MAJOR,
           message="TCP client disconnected",
           description="This alarm is triggered when a client-side TCP connection is closed, regardless of the cause.",
           generatedBy="DiameterLB",
           correctiveAction="None on the Diameter LoadBalancer. Some analysis may be done on the client side.")
    protected static final int TCP_DISCONNECTED_ALARM_CODE = 152;
    
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(service.pid=system)")
    public void setSystemConfig(Dictionary<String, String> system){
	LOGGER.info ("@Reference setSystemConfig");
        _system = system;
    }
    public void unsetSystemConfig(Dictionary<String, String> system){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]				
    }
    @Reference
    public void setServerFactory (ServerFactory factory){
	LOGGER.info ("@Reference setServerFactory");
	_serverFactory = factory;
    }
    @Reference()
    public void setExecutors(PlatformExecutors executors){
	LOGGER.info ("@Reference setExecutors");
	_executors = executors;
    }
    @Reference(target="(strict=false)")
    public void setTimerService(TimerService timer){
	LOGGER.info ("@Reference setTimerService");
	_timerService = timer;
    }
    @Reference()
    public void setMetering(MeteringService metering){
	LOGGER.info ("@Reference setMetering");
	_metering = metering;
    }
    @Reference
    public void setClientFactory (ClientFactory factory) throws Exception {
	LOGGER.info ("@Reference setClientFactory");
	_clientFactory = factory;
    }
    protected ClientFactory getClientFactory (){
	return _clientFactory;
    }
    public PlatformExecutors getPlatformExecutors (){ return _executors;}
    public Meter getCpuMeter (){ return _cpuMeter;}

     @Reference
    public void bindAlarmService(AlarmService alarmService){
        _alarmService = alarmService;
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setDiameterRouter (DiameterRouter router, Map<String, String> properties){
	String id = properties.get ("router.id");
	LOGGER.info ("@Reference setDiameterRouter : "+id+" : "+router);
	if (_osgi != null){
	    register (id, router);
	} else {
	    _routers.put (id, router);
	}
    }
    
    public synchronized void unsetDiameterRouter (DiameterRouter router, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }

    @Reference
    public void setMeteringService(MeteringService metering) {
	LOGGER.info ("@Reference setMeteringService");
    }
    @Reference(target="("+MeteringConstants.MONITORABLE_NAME+"="+MeteringConstants.SYSTEM+")")
    public void setSystemMetering (Monitorable monitorable){
	_cpuMeter = monitorable.getMeters ().get (MeteringConstants.SYSTEM_CPU_SYSTEM_LOAD);
    }
    
    @Activate
    public synchronized void activate (BundleContext ctx, Map<String, String> conf) throws Exception {
	LOGGER.info ("@Activate");
	LOGGER.info ("Alarm Seed is : "+ALARM_SEED.get ());
	Counters.init (_executors, _timerService, _metering, ctx);
	updated (conf);
	final String parent = _system.get(ConfigConstants.GROUP_NAME) + "__" + _system.get(ConfigConstants.INSTANCE_NAME);
        final String instance = parent + "-" + DiameterClient.APP_NAME;

	ctx.registerService(Object.class.getName(), new Counters.Instance (), new Hashtable() {
	    {
		put(ConfigConstants.MODULE_ID, "0");
		put(ConfigConstants.MODULE_NAME, DiameterClient.APP_NAME);
		put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
	    }
	});
	
	_osgi = ctx;
	for (String id: _routers.keySet ()) register (id, _routers.get (id));
	_routers.clear ();
	_routers = null;
    }

    @Modified
    public void updated (Map<String, String> conf) throws Exception {
	LOGGER.info ("Configuration : "+conf);
	String quorum = conf.get (CONF_QUORUM);
	boolean percentage = false;
	if (quorum == null) quorum = "";
	quorum = quorum.trim ().replace (" ", "");
	if (quorum.length () == 0) quorum = "1";
	else if (quorum.endsWith ("%")){
	    if (quorum.length () == 1) quorum = "1";
	    else percentage = true;
	}
	try{
	    int q = Integer.parseInt (percentage ? quorum.substring (0, quorum.length () - 1) : quorum);
	    if (q <= 0) throw new Exception ();
	    _quorum = quorum;
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug ("Set quorum to : "+_quorum);
	}catch(Throwable t){
	    LOGGER.error ("Invalid Quorum configuration : ["+conf.get (CONF_QUORUM)+"] : ignoring configured value");
	    _quorum = "1";
	}
	_dwrAttempts = Integer.parseInt (conf.get (CONF_DWR_ATTEMPTS));
	_clientReqTimeout = Integer.parseInt (conf.get (CONF_TIMER_CLIENT_REQ));
	_dprTimeout = Integer.parseInt (conf.get (CONF_TIMER_DPR));
	_sendDpr = Boolean.parseBoolean (conf.get (CONF_DPR));
	_dprReasonCode = Integer.parseInt (conf.get (CONF_DPR_REASON));
	_useLoadFactor = Boolean.parseBoolean (conf.get (CONF_LOADFACTOR));
	_hostIPAddrTransformCER = Boolean.parseBoolean (conf.get (CONF_CER_HOST_IP_ADDRESS_TRANSFORM));
	_hostIPAddrTransformCEA = conf.get (CONF_CEA_HOST_IP_ADDRESS_TRANSFORM);
	if (_hostIPAddrTransformCEA == null) _hostIPAddrTransformCEA = "IGNORE"; // by safety
	String alarms = conf.get (CONF_ALARM_CONNECTION);
	if (alarms == null) alarms = "";
	_alarmTcpDisc = alarms.indexOf ("TCP-Disconnection") > -1;
	_alarmSctpDisc = alarms.indexOf ("SCTP-Disconnection") > -1;
	_alarmSctpUnreach = alarms.indexOf ("SCTP-Address-Unreachable") > -1;
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug ("Send alarm on TcpDisc="+_alarmTcpDisc+", SctpDisc="+_alarmSctpDisc+", SctpAddrUnreach="+_alarmSctpUnreach);

	_serverFactory.newTcpServerConfig ("diameter.lb", conf.get (CONF_TCP_SERVERS));
	_serverFactory.newSctpServerConfig ("diameter.lb", conf.get (CONF_SCTP_SERVERS));
	_clientFactory.newTcpClientConfig (conf.get (CONF_TCP_CLIENTS));
    }

     @Deactivate
     public synchronized void deactivate (){
	 LOGGER.warn ("@Deactivate");
     }

    private void register (String id, DiameterRouter router){
	LOGGER.info ("Registering router : "+id);
	Dictionary props = new Hashtable ();
	props.put ("processor.id", "diameter.lb."+id);
	DiameterProcessor proc = new DiameterProcessor (this, router);
	_osgi.registerService (TcpServerProcessor.class.getName (), proc, props);
	_osgi.registerService (SctpServerProcessor.class.getName (), proc, props);
    }

    public void handleEvent(Event event){
	int ratio = (int) event.getProperty ("server.overload.ratio");
	Gload.set3004Ratio (ratio);
    }

    //////////////////////  Activation ////////////////////////
    public void join (Coordination coordination, Callback cb){
	synchronized (this){
	    LOGGER.info (this+" : join coordination ACTIVATION");
	    _activationCB = cb;
	    if (_activated == false) return;
	    _activationCB.joined (null);
	    LOGGER.info (this+" : joined coordination ACTIVATION");
	    _activationCB = null;
	}
	initGload ();
    }
    public void activated (){
	synchronized (this){
	    if (_activated) return;
	    LOGGER.info (this+" : activated");
	    _activated = true;
	    if (_activationCB == null) return;
	    LOGGER.info (this+" : joined coordination ACTIVATION");
	    _activationCB.joined (null);
	    _activationCB = null;
	}
	initGload ();
    }
    private void initGload (){
	_gload = new Gload (this, (int) Long.parseLong (_system.get (ConfigConstants.INSTANCE_ID))).start ();
    }
    //////////////////////////////////////////////

    public int getDwrAttempts (){ return _dwrAttempts; }
    public String getQuorum (){ return _quorum; }
    public int getDprTimeout (){ return _dprTimeout; }
    public int getClientReqTimeout (){ return _clientReqTimeout; }
    public boolean getSendDpr (){ return _sendDpr; }
    public int getDprReasonCode (){ return _dprReasonCode; }
    public boolean sendSctpAddressUnreachableAlarm (){ return _alarmSctpUnreach;}
    public boolean sendSctpDisconnectedAlarm (){ return _alarmSctpDisc;}
    public boolean sendTcpDisconnectedAlarm (){ return _alarmTcpDisc;}
    public AlarmService getAlarmService (){ return _alarmService;}
    public boolean getUseLoadFactor (){ return _useLoadFactor; }
    public boolean getCERHostIPAddrTransform (){ return _hostIPAddrTransformCER;}
    public String getCEAHostIPAddrTransform (){ return _hostIPAddrTransformCEA;}
}
