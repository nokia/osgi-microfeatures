package com.nextenso.diameter.agent;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.config.Visibility;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

@Config(name = "diameteragent", rootSnmpName = "alcatel.srd.a5350.DiameterAgent", rootOid = { 637, 71, 6, 1000 }, section = "Diameter Agent parameters")
public interface PropertiesDeclaration {
	public final static int DEF_OVERLOAD_LOWWM = 500;
	public final static int DEF_OVERLOAD_HIGHWM = 1000;
	public final static boolean DEF_OVERLOAD_ACTIVE = false;
	public final boolean DEF_MESSAGE_SYNCHRONIZED = true;
	public final boolean DEF_MESSAGE_SCHEDULED = false;

	@BooleanProperty(title = "Delay Mux Connections", help = "Specifies if the agent should suspend traffic by default when a mux connection is opened.", required = false, dynamic = false, defval = false, visibility=Visibility.HIDDEN)
	public static final String DELAY_CONNECTION = "diameteragent.delayConnection";	 
	
	@StringProperty(title = "Local Realm", help = "The local Realm.", oid = 4600, snmpName = "LocalRealm", required = true, dynamic = false, defval = "enter value")
	public static final String ORIGINREALM = "diameteragent.originRealm";

	@BooleanProperty(title = "Realm in OriginHost", help = "Specifies if the OriginHost should be ended with the OriginRealm.", oid = 4601, snmpName = "RealmInOriginHost", required = true, dynamic = false, defval = true)
	public static final String APPENDREALM = "diameteragent.appendRealm";

	@StringProperty(title = "Product Name", help = "The Product Name.", oid = 4602, snmpName = "ProductName", required = true, dynamic = false, defval = "Nokia CSF-CJDI")
	public static final String PRODUCTNAME = "diameteragent.productname";

	@IntProperty(min = -1, max = 10000, title = "Firmware revision", help = "The Firmware revision (-1 value means the associated AVP is not filled in the CER/CEA).", oid = 4603, snmpName = "FirmwareRevision", required = true, dynamic = false, defval = -1)
	public static final String FIRMWARE_REVISION = "diameteragent.firmwareRevision";

	@IntProperty(min = 0, max = 100000, title = "Vendor identifier", help = "The Vendor identifier.", oid = 4604, snmpName = "VendorIdentifier", required = true, dynamic = false, defval = 0)
	public static final String VENDOR_ID = "diameteragent.vendorId";

	@IntProperty(min = 0, max = 3600, title = "Session Lifetime", help = "The Session lifetime.", oid = 4605, snmpName = "SessionLifetime", required = true, dynamic = true, defval = 600)
	public static final String SESSION_LIFETIME = "diameteragent.sessionLifetime";

	@IntProperty(min = 1, max = 99, title = "Diameter Client Attempts", help = "Specifies the maximum number of times a request is sent before it is considered as timed out.The minimum value is 1.", oid = 4606, snmpName = "DiameterClientAttempts", required = true, dynamic = true, defval = 3)
	public static final String CLIENT_ATTEMPTS = "diameteragent.client.attempts";

	@IntProperty(min = 1, max = 60, title = "Diameter Client Timeout", help = "Specifies the timeout value <b>in seconds</b> of the Diameter Client.A value of N seconds indicates that the timeout of a request will occur randomly in the interval [N, 2*N] .Then, the request may be retransmitted if it has not yet exceeded the maximum number of retransmissions.", oid = 4607, snmpName = "DiameterClientTimeout", required = true, dynamic = true, defval = 4)
	public static final String CLIENT_TIMEOUT = "diameteragent.client.timeout";

	@IntProperty(min = 5, max = 3600, title = "Diameter Connection Timeout", help = "Specifies the Tc timeout value <b>in seconds</b> used for defined static peers.See RFC 3588 section 2.1.", oid = 4608, snmpName = "DiameterConnectionTimeout", required = true, dynamic = false, defval = 30)
	public static final String TIMER_TC = "diameteragent.timer.tc";

	@FileDataProperty(fileData = "capabilities.diameterAgent", title = "Capabilities", help = "The Advertised Capabilities.", oid = 4610, snmpName = "Capabilities", required = true, dynamic = false)
	public static final String CAPABILITIES = "diameteragent.capabilities";

	@FileDataProperty(fileData = "peers.diameterAgent", title = "Peers", help = "The static Peers.", oid = 4611, snmpName = "Peers", required = false, dynamic = false)
	public static final String PEERS = "diameteragent.peers";

	@FileDataProperty(fileData = "routes.diameterAgent", title = "Routes", help = "The Routes.", oid = 4612, snmpName = "Routes", required = false, dynamic = true)
	public static final String ROUTES = "diameteragent.routes";

	@BooleanProperty(title = "High Availability activated", help = "Activates the High Availability mode. This mode may require a registry to be started at the level group.", oid = 4613, snmpName = "HighAvailability", required = true, dynamic = false, defval = false)
	public static final String HA = "diameteragent.ha";

	@StringProperty(title = "Local OriginHost", help = "The local OriginHost.", oid = 4614, snmpName = "LocalOriginHost", required = false, dynamic = false, section = "Diameter Agent Private Parameters")
	public static final String ORIGINHOST = "diameteragent.originHost";

	@StringProperty(title = "Client Local OriginHost", help = "The local OriginHost used when acting as a client. %LEGACY% means to use the old property. %AGENT% includes the agent name, %STACK% the diameter IO Handler name.", oid = 4615, snmpName = "ClientLocalOriginHost", required = false, dynamic = false, section = "Diameter Agent Private Parameters", defval = "%LEGACY%")
	public static final String ORIGINHOST_CLIENT = "diameteragent.originHost.client";

	@StringProperty(title = "Server Local OriginHost", help = "The local OriginHost. used when acting as a server. %LEGACY% means to use the old property. %STACK% the diameter IO Handler name. (%AGENT% is never taken into account for server)", oid = 4616, snmpName = "ServerLocalOriginHost", required = false, dynamic = false, section = "Diameter Agent Private Parameters", defval = "%LEGACY%")
	public static final String ORIGINHOST_SERVER = "diameteragent.originHost.server";

	@StringProperty(title = "Local Ip Address", help = "The IP Address", oid = 4619, snmpName = "LocalIpAddress", required = false, dynamic = false, section = "Diameter Agent Private Parameters")
	public static final String HOSTIPADDRESS = "diameteragent.hostIPAddress";

	@StringProperty(title = "Diameter Stack instance", help = "The stack instance.", oid = 4620, snmpName = "DiameterStackInstance", required = true, dynamic = true, section = "Diameter Agent Private Parameters", defval = "*")
	public static final String STACKINSTANCE = "diameteragent.stackInstance";

	@FileDataProperty(title="Diameter Proxylets", oid=4621, snmpName="DiameterProxyletContexts", required=true, dynamic=true, blueprintEditor="/bpadminpxleteditor/index.html", fileData="diameter-pxlets.xml")
	public static String PROXYLET_CONTEXTS = "diameteragent.proxylets";
	
	@BooleanProperty(title = "Scheduled Messages", help = "Sets this property to true if received messages should be scheduled in the processing threadpool.", required = false, dynamic = false, defval = DEF_MESSAGE_SCHEDULED, visibility=Visibility.HIDDEN)
	public static final String MESSAGE_SCHEDULED = "diameteragent.message.scheduled";
	
	@BooleanProperty(title = "Message Overload", help = "Sets this property to true if message overload controll should be enabled.", required = false, dynamic = false, defval = DEF_OVERLOAD_ACTIVE, visibility=Visibility.HIDDEN)
	public static final String MESSAGE_OVERLOAD = "diameteragent.overload.active";

	@IntProperty(min = 1000, max = 1000000, title = "Message Overload Hight Water Mark", help = "If messages are scheduled in the processing threadpool, then this parameter defines the high water mark for the number of scheduled messages.", required = false, dynamic = false, defval = DEF_OVERLOAD_HIGHWM, visibility=Visibility.HIDDEN)
	public static final String MESSAGE_OVERLOAD_HWM = "diameteragent.overload.highWM";

	@IntProperty(min = 1000, max = 1000000, title = "Message Overload Low Water Mark", help = "If messages are scheduled in the processing threadpool, then this parameter defines the lower water mark for the number of scheduled messages.", required = false, dynamic = false, defval = DEF_OVERLOAD_LOWWM, visibility=Visibility.HIDDEN)
	public static final String MESSAGE_OVERLOAD_LWM = "diameteragent.overload.lowWM";
	
	@BooleanProperty(title = "Synchronized Messages", help = "Protects against parallel access to AVPs list.", required = false, dynamic = false, defval = DEF_MESSAGE_SYNCHRONIZED, visibility=Visibility.HIDDEN)
	public static final String MESSAGE_SYNCHRONIZED = "diameteragent.message.synchronized";

	@BooleanProperty(title = "DiameterPeerTable immediate activation", help = "Indicates if the DiameterPeerTable should be activated immediately or only once the full container is ready.", required = false, dynamic = false, defval = false)
	public static final String PEER_TABLE_IMMEDIATE = "diameteragent.peertable.immediate";

	@BooleanProperty(title = "DiameterClientFactory immediate activation", help = "Indicates if the DiameterClientFactory should be activated immediately or only once the full container is ready.", required = false, dynamic = false, defval = false)
	public static final String CLIENT_FACTORY_IMMEDIATE = "diameteragent.clientfactory.immediate";
	  
	@BooleanProperty(title="Diameter Agent Auto Start Mode", help="set the property to false in case the agent should not accept traffic on start.", required=true, defval=true)			
	public static final String AUTOSTART = "diameteragent.autostart";
	
	@StringProperty(title = "Dictionary Path", help = "Path to the JSON Diameter Dictionary", required = false, dynamic = true, visibility=Visibility.HIDDEN)
	public static final String DICTIONARY_PATH = "diameteragent.dictionaryPath";

	@BooleanProperty(title="Global requests storage.", help="Indicates if the pending requests should be attached to the peer connections, or should be global (hence survive re-connections).", required=true, defval=false)
	public static final String REQUESTS_GLOBAL = "diameteragent.reqs.global";

	@IntProperty(min = 0, max = 100000, title = "Result Code on timeout", help = "Indicates the result code to set in case of timeout. A value of 0 means that the response is dropped.", required = false, dynamic = false, defval = (int)DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER)
	public static final String REQUESTS_TIMEOUT_RESULT = "diameteragent.reqs.timeout.result";
	
	@BooleanProperty(title = "Per-Socket Metrics", help = "Enable per-socket metrics on the agent. If true, a monitorable will be created for each opened socket. If false, metrics will only be kept per mux connection", required=false, defval=false)
	public static final String PER_SOCKET_METRICS = "diameteragent.metrics.perSocket";

	@BooleanProperty(title = "Enable DiameterPeer reconnection", help = "Indicates if method connect() can be called more than once for a DiameterPeer", required=false, defval=true)
	public static final String ENABLE_PEER_RECONNECT = "diameteragent.peer.reconnect";
}
