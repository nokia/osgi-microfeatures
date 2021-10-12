// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.*;

import com.alcatel.as.diameter.lb.*;
import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.alcatel_lucent.as.management.annotation.alarm.*;
import com.alcatel.as.service.reporter.api.AlarmService;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

@Component(service={DiameterRouter.class}, property={"router.id=def"}, configurationPolicy=ConfigurationPolicy.REQUIRE)
@Stat
public class DefDiameterRouter extends DiameterRouter implements Runnable {
    
    public static final Logger LOGGER_OVERLOAD = Logger.getLogger ("as.diameter.lb.overload");

    public static final int DIAMETER_AVP_SESSION_PRIORITY_CODE = 650;
    public static final int DIAMETER_3GPP_VENDOR_ID = 10415;
    private static class Response3004 {
	protected DiameterUtils.Avp avp;
	protected String txt;
	protected int serverIndex = -1;
    }
    public static Charset UTF8 = null;
    static {
	try{ UTF8 = Charset.forName ("utf-8");
	}catch(Exception e){}// cannot happen
    }

    @Alarm(name="DiameterServerOveload",
           severity=AlarmSeverity.MAJOR,
           message="Server overload detected",
           description="This alarm notifies the operator that the Diameter LoadBalancer has detected a server overload. When this situation occurs, the Diameter LoadBalancer starts discarding client requests progressively to preserve the servers.",
           generatedBy="DiameterLB",
           correctiveAction="None on the Diameter LoadBalancer. Some analysis should be done on the server side.")
    protected static final int OVERLOAD_SERVER_ALARM_CODE = 150;
    @Alarm(name="DiameterClientOveload",
           severity=AlarmSeverity.MAJOR,
           message="Client overload detected",
           description="This alarm notifies the operator that the Diameter LoadBalancer has detected a client overload. When this situation occurs, the Diameter LoadBalancer starts discarding messages targeted to the client.",
           generatedBy="DiameterLB",
           correctiveAction="None on the Diameter LoadBalancer. Some analysis should be done on the client side.")
    protected static final int OVERLOAD_CLIENT_ALARM_CODE = 151;

    protected Map<String, AtomicLong> _overloadedServers = new HashMap<String, AtomicLong> ();
    protected Map<String, AtomicLong> _overloadedClients = new HashMap<String, AtomicLong> ();

    public boolean _do3004, _doTest, _doPriority;
    public long _serverCache;
    public int _serverLowWM, _serverHighWM, _serverDiffWM;
    public int _clientLowWM, _clientHighWM, _clientDiffWM;
    public int _defPriority, _nbPriorities;
    public AtomicLong _serverOK = new AtomicLong (0);
    public AtomicLong _serverKO = new AtomicLong (0);
    public AtomicLong _clientOK = new AtomicLong (0);
    public AtomicLong _clientKO = new AtomicLong (0);
    public AlertManager _serverAlert, _clientAlert;
    public AlarmService _alarmService;
    public String _instName;
    public Response3004 _localResponse3004;
    public MeteringService _metering;
    public RoutingMeters _routingMeters;
    public Sessions _sessions;
    public PlatformExecutors _execs;
    public EventAdmin _eventAdmin;
    
    @BooleanProperty(title="Respond 3004",
		     defval=true,
		     required=true,
		     dynamic=true,
		     section="Server Overload",
		     help="Indicates if a request, when discarded, should be responded to with 3004 (instead of being silently dropped).")
    public final static String CONF_OVERLOAD_3004 = "diameter.lb.overload.3004";
    @StringProperty(title="Respond 3004 Error-Message",
		    defval="",
		    required=false,
		    dynamic=true,
		    section="Server Overload",
		    help="Indicates the Error-Message to include in 3004 responses locally initiated. This message is intended to be human-readable.")
    public final static String CONF_OVERLOAD_3004_ERROR_MSG_LOCAL = "diameter.lb.overload.3004.errmsg.local";
    @IntProperty(title="Buffer Low Watermark",
		 defval=100000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates the low watermark in the overload detection mechanism. It corresponds to a number of bytes in the TCP send buffer.")
    public final static String CONF_OVERLOAD_SERVER_LOW = "diameter.lb.overload.low";
    @IntProperty(title="Buffer High Watermark",
		 defval=1000000,
		 min=1,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates the high watermark in the overload detection mechanism. It corresponds to a number of bytes in the TCP send buffer.")
    public final static String CONF_OVERLOAD_SERVER_HIGH = "diameter.lb.overload.high";
    @IntProperty(title="Buffer Low Watermark",
		 defval=100000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates the low watermark in the overload detection mechanism. It corresponds to a number of bytes in the TCP send buffer.")
    public final static String CONF_OVERLOAD_CLIENT_LOW = "diameter.lb.overload.client.low";
    @IntProperty(title="Buffer High Watermark",
		 defval=1000000,
		 min=1,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates the high watermark in the overload detection mechanism. It corresponds to a number of bytes in the TCP send buffer.")
    public final static String CONF_OVERLOAD_CLIENT_HIGH = "diameter.lb.overload.client.high";
    @IntProperty(title="Server Requests Timeout",
		 defval=3000,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="Timers",
		 help="Indicates the maximum duration for a server-originated request context to be kept in memory before timing out.")
    public final static String CONF_TIMER_CACHE = "diameter.lb.server-req.timeout";
    @IntProperty(title="Alarm Trigger Low Watermark",
		 defval=1,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates the low watermark in the overload alarm mechanism.")
    public final static String CONF_OVERLOAD_SERVER_ALARM_LOW = "diameter.lb.overload.alarm.low";
    @IntProperty(title="Alarm Trigger High Watermark",
		 defval=5,
		 min=1,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates the high watermark in the overload alarm mechanism.")
    public final static String CONF_OVERLOAD_SERVER_ALARM_HIGH = "diameter.lb.overload.alarm.high";
    @IntProperty(title="Alarm Send Delay",
		 defval=10000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates in milliseconds the overload duration that generates a send alarm. It avoids sending an alarm if the overload is short.")
    public final static String CONF_OVERLOAD_SERVER_ALARM_SEND = "diameter.lb.overload.alarm.send";
    @IntProperty(title="Alarm Clear Delay",
		 defval=30000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Server Overload",
		 help="Indicates in milliseconds the un-overload duration that generates a clear alarm. It avoids clearing an alarm if the overload disappears for only a short interval.")
    public final static String CONF_OVERLOAD_SERVER_ALARM_CLEAR = "diameter.lb.overload.alarm.clear";
    @BooleanProperty(title="Enable Session-Priority handling",
		     defval=false,
		     required=true,
		     dynamic=true,
		     section="Server Overload",
		     help="Activates the processing of the Session-Priority AVP in the overload control mechanism.")
    public final static String CONF_OVERLOAD_SERVER_SESSION_PRIORITY_ACTIVE = "diameter.lb.overload.priority.active";
    @SelectProperty(title = "Default Session-Priority AVP Policy",
		    range = { "-1", "0", "1" , "2", "3", "4", "5"},
		    defval="-1",
		    dynamic=false,
		    section="Server Overload",
		    help="Indicates the behavior when the incoming client request contains no Session-Priority AVP. This is relevant if the management of this AVP is activated.<br>-1 : keep the linear drop rate between the low and high watermarks.<br>0, 1, 2, 3, 4, 5 : assign the specific priority, 5 being a custom one, lower than 4 (the official range being from 0 to 4).")
    public final static String CONF_OVERLOAD_SERVER_SESSION_PRIORITY_DEF = "diameter.lb.overload.priority.def";
    @IntProperty(title="Alarm Trigger Low Watermark",
		 defval=1,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates the low watermark in the overload alarm mechanism.")
    public final static String CONF_OVERLOAD_CLIENT_ALARM_LOW = "diameter.lb.overload.client.alarm.low";
    @IntProperty(title="Alarm Trigger High Watermark",
		 defval=5,
		 min=1,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates the high watermark in the overload alarm mechanism.")
    public final static String CONF_OVERLOAD_CLIENT_ALARM_HIGH = "diameter.lb.overload.client.alarm.high";
    @IntProperty(title="Alarm Send Delay",
		 defval=10000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates in milliseconds the overload duration that generates a send alarm. It avoids sending an alarm if the overload is short.")
    public final static String CONF_OVERLOAD_CLIENT_ALARM_SEND = "diameter.lb.overload.client.alarm.send";
    @IntProperty(title="Alarm Clear Delay",
		 defval=30000,
		 min=0,
		 required=true,
		 dynamic=false,
		 section="Client Overload",
		 help="Indicates in milliseconds the un-overload duration that generates a clear alarm. It avoids clearing an alarm if the overload disappears for only a short interval.")
    public final static String CONF_OVERLOAD_CLIENT_ALARM_CLEAR = "diameter.lb.overload.client.alarm.clear";

    @FileDataProperty(title="Policies",
                      fileData="defSessionPolicies.txt",
                      required=true,
                      dynamic=false,
                      section="Sessions",
                      help="Indicates the session stickiness policies by diameter application.")
    public final static String CONF_SESSION_POLICIES = "diameter.lb.sessions.policies";

    @FileDataProperty(title="Encryption keys",
                      fileData="eapKeys.txt",
                      required=false,
                      dynamic=true,
                      section="EAP",
                      help="Indicates the encryption keys to decode the IMSIs in EAP application.")
    public final static String CONF_EAP_KEYS = "diameter.lb.eap.keys";

    @Reference()
    public void setMetering(MeteringService metering){
	_metering = metering;
    }

    @Reference()
    public void setExecutors(PlatformExecutors executors){
	(_execs = executors).getProcessingThreadPoolExecutor ("overload").scheduleWithFixedDelay (this, 1000, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Reference
    public void bindAlarmService(AlarmService alarmService){
        _alarmService = alarmService;
    }

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin){
	_eventAdmin = eventAdmin;
    }

    @Activate
    public void init (BundleContext ctx, Map<String, String> conf){
	SimpleMonitorable monitorable = new SimpleMonitorable ("diameter.lb.router.def", "Meters for DefDiameterRouter");
	_routingMeters = new RoutingMeters().init (monitorable, _metering, null);
	(_sessions = new Sessions ()).init (conf);
	_serverLowWM = Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_LOW));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_SERVER_LOW+" : "+_serverLowWM);
	_serverHighWM = Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_HIGH));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_SERVER_HIGH+" : "+_serverHighWM);
	_serverDiffWM = _serverHighWM - _serverLowWM;
	_serverAlert = new AlertManager (Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_ALARM_LOW)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_ALARM_HIGH)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_ALARM_SEND)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_ALARM_CLEAR))
					 );
	_clientLowWM = Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_LOW));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_CLIENT_LOW+" : "+_clientLowWM);
	_clientHighWM = Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_HIGH));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_CLIENT_HIGH+" : "+_clientHighWM);
	_clientDiffWM = _clientHighWM - _clientLowWM;
	_clientAlert = new AlertManager (Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_ALARM_LOW)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_ALARM_HIGH)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_ALARM_SEND)),
					 Integer.parseInt (conf.get (CONF_OVERLOAD_CLIENT_ALARM_CLEAR))
					 );
	_defPriority = Integer.parseInt (conf.get (CONF_OVERLOAD_SERVER_SESSION_PRIORITY_DEF));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_SERVER_SESSION_PRIORITY_DEF+" : "+_defPriority);
	_nbPriorities = _defPriority == 5 ? 6 : 5;
	_doTest = System.getProperty ("as.diameter.lb.test") != null;
	if (_doTest) LOGGER.warn (this+" : RUNNING IN TEST MODE");
	updated (conf);
	monitorable.start (ctx);
    }

    @Modified
    public void updated (Map<String, String> conf){
	_do3004 = ("true".equalsIgnoreCase (conf.get (CONF_OVERLOAD_3004)) ||
		   "yes".equalsIgnoreCase (conf.get (CONF_OVERLOAD_3004)));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_3004+" : "+_do3004);
	if (_do3004){
	    Response3004 localResponse3004 = new Response3004 ();
	    localResponse3004.txt = conf.get (CONF_OVERLOAD_3004_ERROR_MSG_LOCAL);
	    if (localResponse3004.txt != null){
		localResponse3004.txt = localResponse3004.txt.trim ();
		if (localResponse3004.txt.length () > 0){
		    localResponse3004.serverIndex = localResponse3004.txt.indexOf ("%server%");
		    if (localResponse3004.serverIndex == -1){
			localResponse3004.avp = new DiameterUtils.Avp (281, 0, false, localResponse3004.txt.getBytes (UTF8));
		    }
		} else
		    localResponse3004.txt = null;
	    }
	    if (localResponse3004.txt != null){
		_localResponse3004 = localResponse3004;
		LOGGER.warn (this+" : "+CONF_OVERLOAD_3004_ERROR_MSG_LOCAL+" : "+localResponse3004.txt);
	    } else {
		_localResponse3004 = null;
		LOGGER.warn (this+" : "+CONF_OVERLOAD_3004_ERROR_MSG_LOCAL+" : -not set-");
	    }
	} else {
	    _localResponse3004 = null;
	}
	_serverCache = Long.parseLong (conf.get (CONF_TIMER_CACHE));
	LOGGER.warn (this+" : "+CONF_TIMER_CACHE+" : "+_serverCache);
	_doPriority = Boolean.parseBoolean (conf.get (CONF_OVERLOAD_SERVER_SESSION_PRIORITY_ACTIVE));
	LOGGER.warn (this+" : "+CONF_OVERLOAD_SERVER_SESSION_PRIORITY_ACTIVE+" : "+_doPriority);

	ImsiDecoder.initKeys (conf.get (CONF_EAP_KEYS));
    }

    public static final int ATTACHMENTS_LEN = 2; // not so elegant... but helps for other routers
    @Override
    public void clientOpened (final DiameterClient client){
	final Cache<MessageKey, ServerRequest> serverRequests = new Cache <MessageKey, ServerRequest>();
	Runnable cleaner = new Runnable (){
		public void run (){
		    if (client.isOpen () == false) return;
		    int size = serverRequests.tick ();
		    if (size > 0 && client.getLogger ().isInfoEnabled ())
			client.getLogger ().info (client+" : dropping "+size+" server requests on timeout");
		    client.getExecutor ().schedule (this, _serverCache, TimeUnit.MILLISECONDS);
		}};
	client.getExecutor ().schedule (cleaner, _serverCache, TimeUnit.MILLISECONDS);
	client.getMonitorable ().createValueSuppliedMeter (_metering, "router:cache.server.reqs", () -> {return serverRequests.size ();});
	RoutingMeters meters = new RoutingMeters().init (client.getMonitorable (), _metering, _routingMeters);
	client.attach (new Object []{serverRequests, meters});
    }
    public RoutingMeters getRoutingMeters (DiameterClient client) { return (RoutingMeters) client.attachment (1);}
    
    @Override
    public void doClientRequest (DiameterClient client, DiameterMessage msg){
	doClientRequest (client, msg, client.getDestinationManager (null));
    }
    public boolean doClientRequest (DiameterClient client, DiameterMessage msg, DestinationManager mgr){
	return doClientRequest (client, msg, pickDestination (client, msg, mgr));
    }
    public boolean doClientRequest (DiameterClient client, DiameterMessage msg, Destination server){
	try{
	    return sendClientRequest (client, msg, server);
	}catch(DiameterException e){
	    getRoutingMeters (client).getErrorOtherMeter ().inc (1);
	    respondToClient (client, msg, e);
	    return false;
	}
    }

    public Destination pickDestination (DiameterClient client, DiameterMessage msg, DestinationManager mgr){
	if (mgr == null) return null;
	return _sessions.getDestination (client, msg, mgr);
    }

    public boolean sendClientRequest (DiameterClient client, DiameterMessage msg, Destination server){
	if (server == null){
	    // may happen when we lost the quorum and sent a DPR
	    // or with fancy routers that cannot find a destination
	    getRoutingMeters (client).getUnroutableMeter ().inc (1);
	    respondToClient (client, msg, DiameterException.DIAMETER_UNABLE_TO_DELIVER);
	    return false;
	}
	if (checkPartialOverload (server.getChannel (), msg, _serverLowWM, _serverHighWM, _serverDiffWM, _doPriority, getRoutingMeters (client).getServerOverloadMeter ())){
	    acceptClientRequest (client, msg, server);
	    return true;
	} else {
	    refuseClientRequest (client, msg, server);
	    return false;
	}
    }
    
    public void acceptClientRequest (DiameterClient client, DiameterMessage msg, Destination server){
	_serverOK.incrementAndGet ();
	client.sendToServer (server, msg);
    }

    public void refuseClientRequest (DiameterClient client, DiameterMessage msg, Destination server){
	// server overload
	_serverKO.incrementAndGet ();
	refuseClientRequest (client, msg, (String) server.getProperties ().get ("server.name"), server.getChannel ().getSendBufferSize ());
    }
    public void refuseClientRequest (DiameterClient client, DiameterMessage msg, String server, int overload){
	if (LOGGER_OVERLOAD.isDebugEnabled ()) LOGGER_OVERLOAD.debug (server+" : overload : "+overload);
	registerOverload (_overloadedServers, server, overload);
	if (_do3004){
	    DiameterUtils.Avp errMsgAvp = null;
	    Response3004 resp3004 = _localResponse3004;
	    if (resp3004 != null){
		if (resp3004.serverIndex != -1){
		    StringBuilder txt = new StringBuilder ();
		    txt.append (resp3004.txt.substring (0, resp3004.serverIndex));
		    txt.append (server);
		    txt.append (resp3004.txt.substring (resp3004.serverIndex+"%server%".length ()));
		    errMsgAvp = new DiameterUtils.Avp (281, 0, false, txt.toString ().getBytes (UTF8));
		} else {
		    errMsgAvp = resp3004.avp;
		}
	    }
	    DiameterMessage resp = errMsgAvp != null ?
		DiameterException.DIAMETER_TOO_BUSY.makeResponse (msg, errMsgAvp) :
		DiameterException.DIAMETER_TOO_BUSY.makeResponse (msg);
	    if (_doTest){
		byte[] bytes = resp.getBytes ();
		bytes[5] = (byte)0xFF;
		bytes[6] = (byte)0xFF;
		bytes[7] = (byte)0xFF;
	    }
	    sendResponseToClient (client, resp);
	} else {
	    // drop
	    client.sendToServer (null, msg);
	}
    }
    
    @Override
    public void doServerRequest (DiameterClient client, Destination server, DiameterMessage msg){
	if (checkPartialOverload (client.getClientChannel (), msg, _clientLowWM, _clientHighWM, _clientDiffWM, false, getRoutingMeters (client).getClientOverloadMeter ()))
	    acceptServerRequest (client, msg, server);
	else
	    refuseServerRequest (client, msg, server);
    }

    public void acceptServerRequest (DiameterClient client, DiameterMessage msg, Destination server){
	_clientOK.incrementAndGet ();
	Cache<MessageKey, ServerRequest> serverRequests = client.attachment (0);
	ServerRequest req = new ServerRequest (server, msg); // this will modify the msg hop id
	serverRequests.put (new MessageKey (msg), req);
	client.sendToClient (msg);
    }
    
    public void refuseServerRequest (DiameterClient client, DiameterMessage msg, Destination server){
	_clientKO.incrementAndGet ();
	registerOverload (_overloadedClients, client.getDiameterId (), client.getClientChannel ().getSendBufferSize ());
	if (LOGGER_OVERLOAD.isDebugEnabled ()) LOGGER_OVERLOAD.debug (client+" : overload : "+client.getClientChannel ().getSendBufferSize ());
	DiameterMessage resp = DiameterException.DIAMETER_TOO_BUSY.makeResponse (msg);
	sendResponseToServer (client, server, resp);
    }
    
    @Override
    public void doClientResponse (DiameterClient client, DiameterMessage msg){
	Cache<MessageKey, ServerRequest> serverRequests = client.attachment (0);
	ServerRequest ctx = serverRequests.remove (new MessageKey (msg));
	Destination server = ctx != null ? ctx.doResponse (msg) : null;
	sendResponseToServer (client, server, msg);
    }
    
    @Override
    public void doServerResponse (DiameterClient client, Destination server, DiameterMessage msg){
	sendResponseToClient (client, msg);
    }
    
    public void sendResponseToClient (DiameterClient client, DiameterMessage msg){
	if (checkClientOverload (client, msg))
	    client.sendToClient (msg);
    }
    public void sendResponseToServer (DiameterClient client, Destination server, DiameterMessage msg){
	if (server != null && checkServerOverload (client, server, msg))
	    client.sendToServer (server, msg);
    }
    
    public void respondToClient (DiameterClient client, DiameterMessage req, DiameterException exc){
	DiameterMessage resp = exc.makeResponse (req);
    	if (checkClientOverload (client, resp)){
	    client.sendToClient (resp);
	}
    }

    @Override
    public boolean checkClientOverload (DiameterClient client, DiameterMessage msg){
	if (checkFullOverload (client.getClientChannel (), msg, _clientHighWM, getRoutingMeters (client).getClientOverloadMeter ())){
	    _clientKO.incrementAndGet ();
	    registerOverload (_overloadedClients, client.getDiameterId (), client.getClientChannel ().getSendBufferSize ());
	    if (LOGGER_OVERLOAD.isDebugEnabled ()) LOGGER_OVERLOAD.debug (client+" : overload : "+client.getClientChannel ().getSendBufferSize ());
	    return false;
	} else {
	    _clientOK.incrementAndGet ();
	    return true;
	}
    }
    @Override
    public boolean checkServerOverload (DiameterClient client, Destination server, DiameterMessage msg){
	if (checkFullOverload (server.getChannel (), msg, _serverHighWM, getRoutingMeters (client).getServerOverloadMeter ())){
	    _serverKO.incrementAndGet ();
	    registerOverload (_overloadedServers, server.toString (), server.getChannel ().getSendBufferSize ());
	    if (LOGGER_OVERLOAD.isDebugEnabled ()) LOGGER_OVERLOAD.debug (server+" : overload : "+server.getChannel ().getSendBufferSize ());
	    return false;
	} else {
	    _serverOK.incrementAndGet ();
	    return true;
	}
    }

    public boolean checkPartialOverload (AsyncChannel channel, DiameterMessage msg, int lowWM, int highWM, int diffWM, boolean doPriority, Meter overloadMeter){
	int buffSize = channel.getSendBufferSize ();
	if (_doTest){
	    int value = msg.getIntAvp (5, 123, -1);
	    if (value != -1){
		buffSize = value;
	    }
	}
	if (buffSize > 0){
	    if (doPriority){
		int priority = msg.getIntAvp (DIAMETER_AVP_SESSION_PRIORITY_CODE, DIAMETER_3GPP_VENDOR_ID, -1);
		switch (priority){
		case -1:
		    if (_defPriority < 0) break; // _defPriority = -1 : keep normal linear drop
		    priority = _defPriority; // maybe 5
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		    diffWM = diffWM / _nbPriorities;
		    lowWM += diffWM * (_nbPriorities - priority - 1);
		    highWM = lowWM + diffWM;
		    break;
		}
	    }
	    if (buffSize > lowWM){
		if (buffSize > highWM){
		    overloadMeter.inc (1);
		    return false;
		}
		int overload = (100*(buffSize - lowWM))/diffWM;
		if (ThreadLocalRandom.current().nextInt (100) < overload){
		    overloadMeter.inc (1);
		    return false;
		}
	    }
	}
	return true;
    }
    public boolean checkFullOverload (AsyncChannel channel, DiameterMessage msg, int highWM, Meter overloadMeter){
	int buffSize = channel.getSendBufferSize ();
	if (_doTest){
	    int value = msg.getIntAvp (5, 123, -1);
	    if (value != -1){
		buffSize = value;
	    }
	}
	if (buffSize > highWM){
	    overloadMeter.inc (1);
	    return true;
	}
	return false;
    }
    
    @Override
    public String toString (){
	return "DefDiameterRouter";
    }

    public static class ServerRequest {
	private Destination _server;
	private int _id, _hopId;
	public ServerRequest (Destination server, DiameterMessage req){
	    _server = server;
	    _id = server.getHistory ()[0];
	    _hopId = req.getHopIdentifier ();
	    DiameterUtils.updateHopIdentifier (req);
	}
	public Destination doResponse (DiameterMessage resp){
	    Destination server = _server.getHistory ()[0] == _id && _server.isOpen () ? _server : null;
	    if (server != null) resp.updateHopIdentifier (_hopId);
	    return server;
	}
    }

    private void registerOverload (Map<String, AtomicLong> map, String target, int buffer){
	synchronized (map){
	    AtomicLong l = map.get (target);
	    if (l == null)
		map.put (target, new AtomicLong (buffer));
	    else {
		if (l.get () < buffer) l.set (buffer);
	    }
	}
    }

    private int _ratioCache = -1;
    public void run (){
	/************ server overload ****************/
	long ok = _serverOK.getAndSet (0);
	long ko = _serverKO.getAndSet (0);
	long total = ok + ko;
	int ratio = 0;
	if (total == 0L){
	    ratio = 0;
	} else {
	    ratio = (int) ((ko*10000L)/total);
	}
	if (ko > 0)
	    LOGGER_OVERLOAD.warn (this+" : Server Overload metrics in last second : KO="+ko+", OK="+ok+", ratio="+ratio);
	if (_ratioCache != ratio){
	    _ratioCache = ratio;
	    Map<String, Object> properties = new HashMap<> ();
	    properties.put ("server.overload.ratio", ratio);
	    _eventAdmin.sendEvent(new Event("diameter/lb/overload",
					    properties));
	}
	Object log = null;
	synchronized (_overloadedServers){
	    if (_overloadedServers.size () > 0){
		StringBuilder sb = new StringBuilder ();
		sb.append ("** Dumping Server Overload info **");
		for (Map.Entry<String, AtomicLong> entry : _overloadedServers.entrySet ())
		    sb.append ("\nServer Overloaded : ").append (entry.getKey ()).append (" : max send buffer : ").append (entry.getValue ());
		_overloadedServers.clear ();
		log = sb;
	    }
	}
	if (log != null) LOGGER_OVERLOAD.warn (log.toString ());
	switch (_serverAlert.process (ratio)){
	case SEND_ALERT: sendServerOverloadAlarm (); break;
	case CLEAR_ALERT: clearServerOverloadAlarm (); break;
	}
	/************ client overload ****************/
	ok = _clientOK.getAndSet (0);
	ko = _clientKO.getAndSet (0);
	total = ok + ko;
	ratio = 0;
	if (total == 0L){
	    ratio = 0;
	} else {
	    ratio = (int) ((ko*10000L)/total);
	}
	if (ko > 0)
	    LOGGER_OVERLOAD.warn (this+" : Client Overload metrics in last second : KO="+ko+", OK="+ok+", ratio="+ratio);
	log = null;
	synchronized (_overloadedClients){
	    if (_overloadedClients.size () > 0){
		StringBuilder sb = new StringBuilder ();
		sb.append ("** Dumping Client Overload info **");
		for (Map.Entry<String, AtomicLong> entry : _overloadedClients.entrySet ())
		    sb.append ("\nClient Overloaded : ").append (entry.getKey ()).append (" : max send buffer : ").append (entry.getValue ());
		_overloadedClients.clear ();
		log = sb;
	    }
	}
	if (log != null) LOGGER_OVERLOAD.warn (log.toString ());
	switch (_clientAlert.process (ratio)){
	case SEND_ALERT: sendClientOverloadAlarm (); break;
	case CLEAR_ALERT: clearClientOverloadAlarm (); break;
	}
    }

    public void sendServerOverloadAlarm (){
	LOGGER_OVERLOAD.warn (this+" : sendServerOverloadAlarm"); 
	try{
	    _alarmService.sendAlarm(DiameterClient.APP_NAME, OVERLOAD_SERVER_ALARM_CODE, "Server overloaded");
	}catch(Exception e){
	    LOGGER_OVERLOAD.error ("sendServerOverloadAlarm : failed to send alarm", e);
	}
    }
    public void clearServerOverloadAlarm (){
	LOGGER_OVERLOAD.warn (this+" : clearServerOverloadAlarm");
	try{
	    _alarmService.clearAlarm(DiameterClient.APP_NAME, OVERLOAD_SERVER_ALARM_CODE, "Server un-overloaded");
	}catch(Exception e){
	    LOGGER_OVERLOAD.error ("clearServerOverloadAlarm : failed to clear alarm", e);
	}
    }
    public void sendClientOverloadAlarm (){
	LOGGER_OVERLOAD.warn (this+" : sendClientOverloadAlarm"); 
	try{
	    _alarmService.sendAlarm(DiameterClient.APP_NAME, OVERLOAD_CLIENT_ALARM_CODE, "Client overloaded");
	}catch(Exception e){
	    LOGGER_OVERLOAD.error ("sendClientOverloadAlarm : failed to send alarm", e);
	}
    }
    public void clearClientOverloadAlarm (){
	LOGGER_OVERLOAD.warn (this+" : clearClientOverloadAlarm");
	try{
	    _alarmService.clearAlarm(DiameterClient.APP_NAME, OVERLOAD_CLIENT_ALARM_CODE, "Client un-overloaded");
	}catch(Exception e){
	    LOGGER_OVERLOAD.error ("clearClientOverloadAlarm : failed to clear alarm", e);
	}
    }

    protected static final class MessageKey {
	private int _hopId, _endId;
	protected MessageKey (DiameterMessage msg){
	    _hopId = msg.getHopIdentifier ();
	    _endId = msg.getEndIdentifier ();
	}
	@Override
	public int hashCode (){ return _hopId;}
	@Override
	public boolean equals (Object o){
	    if (o instanceof MessageKey){
		MessageKey other = (MessageKey) o;
		return _hopId == other._hopId && _endId == other._endId;
	    }
	    return false;
	}
    }

    public Meter asyncTaskStart (DiameterClient client, DiameterMessage req){
	Meter processingMeter = getRoutingMeters (client).getProcessingReqsMeter ();
	int processingCount = (int) processingMeter.getValue ();
	if (processingCount > 10000){ //TODO make it configurable
	    getRoutingMeters (client).getLocalOverloadMeter ().inc (1);
	    respondToClient (client, req, DiameterException.DIAMETER_TOO_BUSY);
	    return null;
	}
	processingMeter.inc (1);
	return processingMeter;
    }
}
