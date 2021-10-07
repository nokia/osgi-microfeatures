package com.nextenso.diameter.agent;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

import alcatel.tess.hometop.gateways.utils.IPAddr;

public class DiameterProperties {
    
	private static final Logger LOGGER = Logger.getLogger("agent.diameter.properties");
	public static final String NULL_HOST_IP_ADDRESS = "0.0.0.0";

	public static final String ORIGIN_STATE_ID_PROPERTY = "diameteragent.originstateid";
	private static final String FORCED_WATCHDOG_TIMEOUT_PROPERTY = "diameteragent.timer.forcedWatchdog";
	private static final String NO_ROUTE_RECORD_PROPERTY = "diameteragent.noRouteRecord";
	private static final String RESPONSES_ROUTE_RECORD_PROPERTY = "diameteragent.resps.RouteRecord";
	private static final String UNIQUE_HOST_ID_PROPERTY = "diameteragent.uniqueHostId";
	private static final String DEFAULT_QUARANTINE_DELAY_PROPERTY = "diameteragent.quarantine.delay";
	private static final String QUARANTINE_ENABLED_PROPERTY = "diameteragent.quarantine.enabled";
	private static final String ROUTING_POLICY_PROPERTY = "diameteragent.routing.policy";
	private static final String DIAMETER_CLIENT_SRC_PROPERTY = "diameteragent.client.src";
	private static final String DIAMETER_CLIENT_SRC_TCP_PROPERTY = "diameteragent.client.src.tcp";
	private static final String DIAMETER_CLIENT_SRC_SCTP_PROPERTY = "diameteragent.client.src.sctp";
	private static final String DIAMETER_CLIENT_SRC_PORT_PROPERTY = "diameteragent.client.src.port";
	private static final String DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY = "diameteragent.client.src.port.tcp";
	private static final String DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY = "diameteragent.client.src.port.sctp";
	private static final String SCTP_TTL_PROPERTY = "diameteragent.sctp.ttl";
	private static final String SCTP_UNORDERED_PROPERTY = "diameteragent.sctp.unordered";
	private static final String SCTP_SO_RCVBUF_PROPERTY = "diameteragent.sctp.so_rcvbuf";
	private static final String SCTP_SO_SNDBUF_PROPERTY = "diameteragent.sctp.so_sndbuf";
	private static final String CHECK_APP_ADVERT_PROPERTY = "diameteragent.check.appAdvert";
	private static final String CHECK_HOST_IP_ADDRESS_FAMILY_PROPERTY = "diameteragent.check.hostipaddress.family";
	private static final String UNIQUE_REMOTE_ORIGIN_HOST_PROPERTY = "diameteragent.uniqueRemoteOriginHost";
	private static final String DPR_TIMEOUT_PROPERTY = "diameteragent.dpr.timeout";
	private static final String DPR_REASON_PROPERTY = "diameteragent.dpr.reason";
	private static final String CHECK_PROXIABLE_PROPERTY = "diameteragent.check.proxiable";
	private static final String ROUTING_ABSOLUTE_PROXY_PROPERTY = "diameteragent.routing.proxy.absolute";
	private static final String ROUTING_ABSOLUTE_CLIENT_PROPERTY = "diameteragent.routing.client.absolute";
	
	public enum RoutingPolicy {
		DESTINATION_HOST,
		SESSION,
		ROUTES
	}

	private static List<RoutingPolicy> ROUTING_POLICIES = new ArrayList<DiameterProperties.RoutingPolicy>();
	private static long ORIGIN_STATE_ID = System.currentTimeMillis() / 1000L;
	private static final List<String> NO_ROUTE_RECORDS = new ArrayList<String>();

	private static int VENDOR_ID = 0;
	private static String PRODUCT_NAME = "A5350 Diameter Agent";
	private static long FIRMWARE_REVISION = -1L;

	private static boolean RESPONSES_ROUTE_RECORD = false;
	private static boolean CHECK_APP_ADVERT = true;
	private static boolean IS_REALM_APPENDED = true;
	private static boolean IS_HA = false;
	private static String XML_PEERS = null;
	private static String XML_CAPABILITIES = null;
	private static String XML_ROUTES = null;
	private static String ORIGIN_HOST = null;
	private static String ORIGIN_HOST_CLIENT = null;
	private static String ORIGIN_HOST_SERVER = null;
	private static String ORIGIN_REALM = null;
	private static String[] ADDRESSES = null;
	private static long TC_TIMER = 30L; // in seconds, default is 30 s (see RFC 3588 section 2.1)
	private static long FORCED_WATCHDOG_TIMER = 0L; // in milliseconds
	private static boolean IS_FORCED_WATCHDOG = false;
	private static boolean USE_UNIQUE_HOST_ID = false;
	private static boolean IS_QUARANTINE_ENABLED = false;
	private static boolean IS_MESSAGE_SYNCHRONIZED;
	private static boolean IS_MESSAGE_SCHEDULED;
	private static int DEFAULT_RETRY_TIMEOUT_IN_MS = 5000; // in milliseconds
	private static long SESSION_LIFETIME = 5L * 1000;
	// max nb of retransmissions - does not include the first send
	// by default we send client requests up to 3 times
	private static int MAX_NB_RETRANSMISSIONS = 2;
	private static long DEFAULT_QUARANTINE_DELAY = 60000; // in ms
	private static boolean IS_OVERLOAD_ACTIVE;
	private static int OVERLOAD_LOWWM;
	private static int OVERLOAD_HIGHWM;
	private static String DIAMETER_CLIENT_SRC_TCP = null;
	private static String[] DIAMETER_CLIENT_SRC_SCTP = new String[1];
	private static long SCTP_TTL = 0L;
	private static boolean SCTP_UNORDERED = false;
	private static boolean UNIQUE_REMOTE_ORIGIN_HOST = true;
	private static int DIAMETER_CLIENT_SRC_PORT_TCP = 0, DIAMETER_CLIENT_SRC_PORT_SCTP = 0;
	private static int DPR_TIMEOUT = 2000;
	private static int DPR_REASON = DiameterBaseConstants.VALUE_DISCONNECT_CAUSE_DO_NOT_WANT_TO_TALK_TO_YOU;
	private static boolean CHECK_PROXIABLE = true;
	private static boolean ROUTING_ABSOLUTE_CLIENT = false;
	private static boolean ROUTING_ABSOLUTE_PROXY = false;
	private static boolean PER_SOCKET_METRICS = false;
	private static boolean REQUESTS_GLOBAL = false;
	private static int REQUESTS_TIMEOUT_RESULT = (int) DiameterBaseConstants.RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER;
	private static boolean ENABLE_PEER_RECONNECT = true;
	private static boolean CHECK_HOST_IP_ADDRESS_FAMILY = true;

	private static DiameterAVP VENDOR_ID_AVP, PRODUCT_NAME_AVP, ORIGIN_STATE_ID_AVP, ORIGIN_REALM_AVP, FIRMWARE_REVISION_AVP;

	private static Dictionary _conf;
	private static Map<String, Map<String, Object>> _confByGroup = new HashMap<> ();
	public static void init(Dictionary cnf)
		throws Exception {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init with " + cnf);
		}
		_conf = cnf;

		updatePeersXml(cnf);
		updateRoutesXml(cnf);
		XML_CAPABILITIES = ConfigHelper.getString(cnf, PropertiesDeclaration.CAPABILITIES);
		IS_REALM_APPENDED = ConfigHelper.getBoolean(cnf, PropertiesDeclaration.APPENDREALM, true);
		ORIGIN_REALM = ConfigHelper.getString(cnf, PropertiesDeclaration.ORIGINREALM);

		Object o = cnf.get(PropertiesDeclaration.HOSTIPADDRESS);
		if (o == null || "".equals(o)) {
			ADDRESSES = new String[] { NULL_HOST_IP_ADDRESS };
		} else if (o instanceof String) {
			ADDRESSES = new String[] { (String) o };
		} else if (o instanceof String[]) {
			ADDRESSES = (String[]) o;
		} else {
			ADDRESSES = new String[] { NULL_HOST_IP_ADDRESS };
		}
		if (LOGGER.isInfoEnabled()) {
			for (String s : ADDRESSES) {
				LOGGER.info("init: address= " + s);
			}
		}

		ORIGIN_HOST = ConfigHelper.getString(cnf, PropertiesDeclaration.ORIGINHOST, null);
		ORIGIN_HOST_CLIENT = ConfigHelper.getString(cnf, PropertiesDeclaration.ORIGINHOST_CLIENT, "%LEGACY%");
		ORIGIN_HOST_SERVER = ConfigHelper.getString(cnf, PropertiesDeclaration.ORIGINHOST_SERVER, "%LEGACY%");
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("init: legacy Origin-Host= " + getOriginHostParam());
			LOGGER.info("init: client Origin-Host= " + getOriginHostClientParam());
			LOGGER.info("init: server Origin-Host= " + getOriginHostServerParam());
		}

		// Product info
		VENDOR_ID = ConfigHelper.getInt(cnf, PropertiesDeclaration.VENDOR_ID, VENDOR_ID);
		VENDOR_ID_AVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
		VENDOR_ID_AVP.addValue(Unsigned32Format.toUnsigned32(VENDOR_ID), false);

		PRODUCT_NAME = ConfigHelper.getString(cnf, PropertiesDeclaration.PRODUCTNAME, PRODUCT_NAME);
		PRODUCT_NAME_AVP = new DiameterAVP(DiameterBaseConstants.AVP_PRODUCT_NAME);
		PRODUCT_NAME_AVP.addValue(UTF8StringFormat.toUtf8String(PRODUCT_NAME), false);

		FIRMWARE_REVISION_AVP = null;
		FIRMWARE_REVISION = ConfigHelper.getLong(cnf, PropertiesDeclaration.FIRMWARE_REVISION, FIRMWARE_REVISION);
		if (FIRMWARE_REVISION >= 0) {
			FIRMWARE_REVISION_AVP = new DiameterAVP(DiameterBaseConstants.AVP_FIRMWARE_REVISION);
			FIRMWARE_REVISION_AVP.addValue(Unsigned32Format.toUnsigned32(FIRMWARE_REVISION), false);
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("init: vendorId AVP= " + getVendorIdAvp());
			LOGGER.info("init: Product-Name AVP= " + getProductNameAvp());
			LOGGER.info("init: Firmware-Revision AVP= " + getFirmwareRevisionAvp());
		}

		// OriginStateId
		setOriginStateId(cnf);

		if (ORIGIN_REALM.length() < 2) {
			throw new IllegalArgumentException("Invalid originRealm : " + ORIGIN_REALM);
		}
		if (ORIGIN_REALM.charAt(0) == '.') {
			ORIGIN_REALM = ORIGIN_REALM.substring(1);
		}

		ORIGIN_REALM_AVP = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
		ORIGIN_REALM_AVP.addValue(UTF8StringFormat.toUtf8String(ORIGIN_REALM), false);
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("init: originRealm is " + ORIGIN_REALM);
		}

		IS_HA = ConfigHelper.getBoolean(cnf, PropertiesDeclaration.HA, IS_HA);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: is HA=" + isHa());
		}

		TC_TIMER = ConfigHelper.getLong(cnf, PropertiesDeclaration.TIMER_TC, TC_TIMER);
		
		IS_MESSAGE_SYNCHRONIZED = ConfigHelper.getBoolean(cnf, PropertiesDeclaration.MESSAGE_SYNCHRONIZED, PropertiesDeclaration.DEF_MESSAGE_SYNCHRONIZED);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Diameter_Message_Synchronized: " + isMessageSynchronized());
		}
		IS_MESSAGE_SCHEDULED = ConfigHelper.getBoolean(cnf, PropertiesDeclaration.MESSAGE_SCHEDULED, PropertiesDeclaration.DEF_MESSAGE_SCHEDULED);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Diameter_Message_Scheduled: " + isMessageScheduled());
		}
		
		FORCED_WATCHDOG_TIMER = ConfigHelper.getLong(cnf, FORCED_WATCHDOG_TIMEOUT_PROPERTY, FORCED_WATCHDOG_TIMER);
		if (FORCED_WATCHDOG_TIMER > 0) {
			IS_FORCED_WATCHDOG = true;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Tc timer (in seconds)=" + getTcTimer());
			LOGGER.debug("init: Forced watchdog timer (in millseconds)=" + getForcedWatchdogTimer());
		}

		String noRouteRecordValue = ConfigHelper.getString(cnf, NO_ROUTE_RECORD_PROPERTY, null);
		if (noRouteRecordValue != null) {
			String[] values = noRouteRecordValue.split(",");
			for (String v : values) {
				String server = v.trim();
				if (server.length() > 0) {
					NO_ROUTE_RECORDS.add(server);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("init: add Route-Record for =" + server + "!");
					}
				}
			}
		}
		RESPONSES_ROUTE_RECORD = ConfigHelper.getBoolean(cnf, RESPONSES_ROUTE_RECORD_PROPERTY, RESPONSES_ROUTE_RECORD);

		USE_UNIQUE_HOST_ID = ConfigHelper.getBoolean(cnf, UNIQUE_HOST_ID_PROPERTY, USE_UNIQUE_HOST_ID);
		DEFAULT_QUARANTINE_DELAY = ConfigHelper.getLong(cnf, DEFAULT_QUARANTINE_DELAY_PROPERTY, DEFAULT_QUARANTINE_DELAY);
		IS_QUARANTINE_ENABLED = ConfigHelper.getBoolean(cnf, QUARANTINE_ENABLED_PROPERTY, IS_QUARANTINE_ENABLED);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: no Route-Record for =" + NO_ROUTE_RECORDS);
			LOGGER.debug("init: Route-Record in responses ="+RESPONSES_ROUTE_RECORD);
			LOGGER.debug("init: use unique host id =" + useUniqueHostId());
			LOGGER.debug("init: default quarantine delay (ms)=" + getDefaultQuarantineDelay());
			LOGGER.debug("init: quarantine enabled=" + isQuarantineEnabled());
		}

		setClientAttempts(cnf);
		setClientTimeout(cnf);
		setSessionLifetime(cnf);
		setRoutingPolicies(cnf);

		if (IS_OVERLOAD_ACTIVE = ConfigHelper.getBoolean (cnf, PropertiesDeclaration.MESSAGE_OVERLOAD, PropertiesDeclaration.DEF_OVERLOAD_ACTIVE)){
			OVERLOAD_HIGHWM = ConfigHelper.getInt (cnf, PropertiesDeclaration.MESSAGE_OVERLOAD_HWM, PropertiesDeclaration.DEF_OVERLOAD_HIGHWM);
			OVERLOAD_LOWWM = ConfigHelper.getInt (cnf, PropertiesDeclaration.MESSAGE_OVERLOAD_LWM, PropertiesDeclaration.DEF_OVERLOAD_LOWWM);
		}

		String src = ConfigHelper.getString(cnf, DIAMETER_CLIENT_SRC_PROPERTY, null);
		String[] srcSplit = src != null ? split (src) : new String[1];
		DIAMETER_CLIENT_SRC_TCP = ConfigHelper.getString(cnf, DIAMETER_CLIENT_SRC_TCP_PROPERTY, srcSplit[0]);
		String srcSctp = ConfigHelper.getString(cnf, DIAMETER_CLIENT_SRC_SCTP_PROPERTY, null);
		if (srcSctp != null) DIAMETER_CLIENT_SRC_SCTP = split (srcSctp);
		else DIAMETER_CLIENT_SRC_SCTP = srcSplit;

		int srcPort = ConfigHelper.getInt (cnf, DIAMETER_CLIENT_SRC_PORT_PROPERTY, 0);
		DIAMETER_CLIENT_SRC_PORT_TCP = ConfigHelper.getInt (cnf, DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, srcPort);
		DIAMETER_CLIENT_SRC_PORT_SCTP = ConfigHelper.getInt (cnf, DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, srcPort);

		SCTP_TTL = ConfigHelper.getLong(cnf, SCTP_TTL_PROPERTY, SCTP_TTL);
		SCTP_UNORDERED = ConfigHelper.getBoolean(cnf, SCTP_UNORDERED_PROPERTY, SCTP_UNORDERED);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: SCTP_TTL =" + SCTP_TTL);
			LOGGER.debug("init: SCTP_UNORDERED =" + SCTP_UNORDERED);
		}
		int buffer = ConfigHelper.getInt(cnf, SCTP_SO_SNDBUF_PROPERTY, -1);
		if (buffer != -1){
			LOGGER.debug ("init : reactor.sctp.so_sndbuf = "+buffer);
			System.setProperty ("reactor.sctp.so_sndbuf", String.valueOf (buffer));
		}
		buffer = ConfigHelper.getInt(cnf, SCTP_SO_RCVBUF_PROPERTY, -1);
		if (buffer != -1) {
			LOGGER.debug ("init : reactor.sctp.so_rcvbuf = "+buffer);
			System.setProperty ("reactor.sctp.so_rcvbuf", String.valueOf (buffer));
		}

		CHECK_APP_ADVERT = ConfigHelper.getBoolean(cnf, CHECK_APP_ADVERT_PROPERTY, true);
		LOGGER.debug (CHECK_APP_ADVERT_PROPERTY+" : "+CHECK_APP_ADVERT);

		UNIQUE_REMOTE_ORIGIN_HOST = ConfigHelper.getBoolean(cnf, UNIQUE_REMOTE_ORIGIN_HOST_PROPERTY, UNIQUE_REMOTE_ORIGIN_HOST);
		LOGGER.debug (UNIQUE_REMOTE_ORIGIN_HOST_PROPERTY+" : "+UNIQUE_REMOTE_ORIGIN_HOST);

		DPR_TIMEOUT = ConfigHelper.getInt(cnf, DPR_TIMEOUT_PROPERTY, DPR_TIMEOUT);
		if (DPR_TIMEOUT < 10) DPR_TIMEOUT *= DPR_TIMEOUT; // assume it is in seconds

		DPR_REASON = ConfigHelper.getInt(cnf, DPR_REASON_PROPERTY, DPR_REASON);
		LOGGER.debug (DPR_REASON_PROPERTY+" : "+DPR_REASON);

		CHECK_PROXIABLE = ConfigHelper.getBoolean (cnf, CHECK_PROXIABLE_PROPERTY, CHECK_PROXIABLE);
		LOGGER.debug (CHECK_PROXIABLE_PROPERTY+" : "+CHECK_PROXIABLE);

		ROUTING_ABSOLUTE_PROXY = ConfigHelper.getBoolean (cnf, ROUTING_ABSOLUTE_PROXY_PROPERTY, ROUTING_ABSOLUTE_PROXY);
		LOGGER.debug (ROUTING_ABSOLUTE_PROXY_PROPERTY+" : "+ROUTING_ABSOLUTE_PROXY);
		ROUTING_ABSOLUTE_CLIENT = ConfigHelper.getBoolean (cnf, ROUTING_ABSOLUTE_CLIENT_PROPERTY, ROUTING_ABSOLUTE_CLIENT);
		LOGGER.debug (ROUTING_ABSOLUTE_CLIENT_PROPERTY+" : "+ROUTING_ABSOLUTE_CLIENT);
		PER_SOCKET_METRICS = ConfigHelper.getBoolean(cnf, PropertiesDeclaration.PER_SOCKET_METRICS, false);
		REQUESTS_GLOBAL = ConfigHelper.getBoolean (cnf, PropertiesDeclaration.REQUESTS_GLOBAL, REQUESTS_GLOBAL);
		LOGGER.debug (PropertiesDeclaration.REQUESTS_GLOBAL+" : "+REQUESTS_GLOBAL);

		REQUESTS_TIMEOUT_RESULT = ConfigHelper.getInt (cnf, PropertiesDeclaration.REQUESTS_TIMEOUT_RESULT, REQUESTS_TIMEOUT_RESULT);
		LOGGER.debug (PropertiesDeclaration.REQUESTS_TIMEOUT_RESULT+" : "+REQUESTS_TIMEOUT_RESULT);

		ENABLE_PEER_RECONNECT = ConfigHelper.getBoolean (cnf, PropertiesDeclaration.ENABLE_PEER_RECONNECT, ENABLE_PEER_RECONNECT);
		LOGGER.debug (PropertiesDeclaration.ENABLE_PEER_RECONNECT+" : "+ENABLE_PEER_RECONNECT);

		CHECK_HOST_IP_ADDRESS_FAMILY = ConfigHelper.getBoolean(cnf, CHECK_HOST_IP_ADDRESS_FAMILY_PROPERTY, CHECK_HOST_IP_ADDRESS_FAMILY);
		LOGGER.debug (CHECK_HOST_IP_ADDRESS_FAMILY+" : "+CHECK_HOST_IP_ADDRESS_FAMILY);
	}
	private static String[] split (String s){
		java.util.StringTokenizer tok = new java.util.StringTokenizer (s.trim (), " ,", false);
		String[] ret = new String[tok.countTokens ()];
		for (int i=0; i<ret.length; i++) ret[i] = tok.nextToken ();
		return ret;
	}

	private static void setRoutingPolicies(Dictionary cnf) {
		String property = ConfigHelper.getString(cnf, ROUTING_POLICY_PROPERTY, "DESTINATION_HOST SESSION ROUTES");
		String[] policies = property.split("\\s+");
		for (String policy : policies) {
			RoutingPolicy p = RoutingPolicy.valueOf(policy);
			if (p != null) {
				ROUTING_POLICIES.add(p);
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("init: Routing policies=" + ROUTING_POLICIES);
		}	
	}

	public static List<RoutingPolicy> getRoutingPolicies() {
		return ROUTING_POLICIES;
	}

	public static boolean useUniqueHostId() {
		return USE_UNIQUE_HOST_ID;
	}

	public static boolean routeRecordInResponses() {
		return RESPONSES_ROUTE_RECORD;
	}

	public static boolean isSupportingNoRouteRecordForHost() {
		return !NO_ROUTE_RECORDS.isEmpty();
	}

	public static boolean hasNoRouteRecordForHost(String destinationHost) {
		if (destinationHost == null) {
			return false;
		}
		for (String server : NO_ROUTE_RECORDS) {
			if (destinationHost.equals(server)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isForcedWatchdog() {
		return IS_FORCED_WATCHDOG;
	}

	public static boolean isMessageSynchronized (){
		return IS_MESSAGE_SYNCHRONIZED;
	}

	public static boolean isMessageScheduled (){
		return IS_MESSAGE_SCHEDULED;
	}

	public static long getForcedWatchdogTimer() {
		return FORCED_WATCHDOG_TIMER;
	}

	public static void updateRoutesXml(Dictionary cnf) {
		XML_ROUTES = ConfigHelper.getString(cnf, PropertiesDeclaration.ROUTES);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("updateRoutesXml: routes=" + getRoutesXml());
		}
	}

	public static void updatePeersXml(Dictionary cnf) {
		XML_PEERS = ConfigHelper.getString(cnf, PropertiesDeclaration.PEERS);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("updatePeersXml: peers=" + getPeersXml());
		}
	}

	public static DiameterAVP getOriginRealmAvp() {
		return ORIGIN_REALM_AVP;
	}

	public static DiameterAVP getVendorIdAvp() {
		return VENDOR_ID_AVP;
	}

	public static DiameterAVP getProductNameAvp() {
		return PRODUCT_NAME_AVP;
	}

	public static String getProductName() {
		return PRODUCT_NAME;
	}

	public static DiameterAVP getOriginStateIdAvp() {
		return ORIGIN_STATE_ID_AVP;
	}

	public static long getOriginStateId() {
		return ORIGIN_STATE_ID;
	}

	public static DiameterAVP getFirmwareRevisionAvp() {
		return FIRMWARE_REVISION_AVP;
	}

	public static long getFirmwareRevision() {
		return FIRMWARE_REVISION;
	}

	public static String getOriginHostParam() {
		return ORIGIN_HOST;
	}

	public static String getOriginHostClientParam() {
		return ORIGIN_HOST_CLIENT;
	}

	public static String getOriginHostServerParam() {
		return ORIGIN_HOST_SERVER;
	}

	public static String getPeersXml() {
		return XML_PEERS;
	}

	public static String getCapabilitiesXml() {
		return XML_CAPABILITIES;
	}

	public static String getRoutesXml() {
		return XML_ROUTES;
	}

	public static boolean isRealmAppended() {
		return IS_REALM_APPENDED;
	}

	public static String getOriginRealm() {
		return ORIGIN_REALM;
	}

	protected static List<String> getIPlist (String ip) {
		try {
			return getIPlist (ip, true, true);
		} catch(Exception e){
			LOGGER.error ("Exception while parsing IP : "+ip, e);
			return new java.util.ArrayList<String> ();
		}
	}

    protected static List<String> getIPlist(String reactorIP, boolean ipv6Support, boolean resolve0000) throws Exception {
		List<String> list = new ArrayList<String>();
		if (reactorIP == null) {
			return list;
		}

		if (!resolve0000 || !reactorIP.equals("0.0.0.0")) {
			for (StringTokenizer tok = new StringTokenizer(reactorIP, " ,"); tok.hasMoreTokens();) {
				String s = tok.nextToken().trim();
				try {
					IPAddr addr = new IPAddr(s);
					// If the mux handler does not support ipv6, we must refuse
					// an ipv6 address.
					if (!ipv6Support && addr.isIPv6()) {
						throw new IllegalArgumentException("Agent does not support ipv6 address: " + s);
					}
					list.add(s);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("getIPlist: add ip (reactor.ip parsing)=" + s);
					}
				} catch (Exception e) {
					LOGGER.warn("The reactor.ip properties contains a not well formed IP address: " + s
							+ " -> the address is ignored", e);
				}
			}
		} else {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address instanceof Inet4Address || (ipv6Support && address instanceof Inet6Address)) {
						String s = address.getHostAddress();
						int index = s.indexOf("%");
						if (index > 0) {
							s = s.substring(0, index);
						}

						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("getIPlist: ip (from 0.0.0.0)=" + s);
						}
						list.add(s);
					}
				}
			}
		}

		return list;
    }

	public static Set<String> getHostIPAddresses(String group) {
		Set<String> res = new HashSet<> ();
		String addrs = getPropertyFromConf (PropertiesDeclaration.HOSTIPADDRESS, group);
		if (addrs == null)
			addrs = ConfigHelper.getString (_conf, PropertiesDeclaration.HOSTIPADDRESS, null); // may be populated by Launcher with content of reactor.ip
		if (addrs != null){
			for (StringTokenizer tok = new StringTokenizer(addrs, " ,"); tok.hasMoreTokens();) {
				String addr = tok.nextToken().trim();
				if (addr.length () > 0)
					for (String ip: getIPlist (addr)) res.add (ip); // resolve 0.0.0.0
			}
			if (res.size () > 0) return res;
		}
		for (String listen : getReactorListenProperties ("ip", group)){
			for (StringTokenizer tok = new StringTokenizer(listen, " ,"); tok.hasMoreTokens();) {
				String addr = tok.nextToken().trim();
				if (addr.length () > 0)
					for (String ip: getIPlist (addr)) res.add (ip);
			}
		}
		return res;
	}

	public static long getVendorId() {
		return VENDOR_ID;
	}

	public static boolean isHa() {
		return IS_HA;
	}

	/**
	 * Gets the Tc timer duration in seconds.
	 * 
	 * @return The Tc timer duration in seconds.
	 */
	public static long getTcTimer() {
		return TC_TIMER;
	}

	public static void setOriginStateId(Dictionary cnf) {
		long id = ConfigHelper.getLong(cnf, ORIGIN_STATE_ID_PROPERTY, ORIGIN_STATE_ID);
		if (id > 0) {
			ORIGIN_STATE_ID = id;
		}

		synchronized (DiameterProperties.class) {
			if (ORIGIN_STATE_ID_AVP == null) {
				ORIGIN_STATE_ID_AVP = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_STATE_ID);
			}
		}
		ORIGIN_STATE_ID_AVP.setValue(Unsigned32Format.toUnsigned32(ORIGIN_STATE_ID), false);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("setOriginStateId: Origin-State-Id is " + ORIGIN_STATE_ID + ", avp=" + ORIGIN_STATE_ID_AVP);
		}
	}

	public static void setClientAttempts(Dictionary cnf) {
		String s = ConfigHelper.getString(cnf, PropertiesDeclaration.CLIENT_ATTEMPTS, null);

		if (s != null) {
			try {
				int number = Integer.parseInt(s);
				if (number < 1) {
					number = 1;
				}
				setMaxNumberOfRetransmission(number - 1);
			}
			catch (Exception ex) {
				LOGGER.error("Failed to parse " + PropertiesDeclaration.CLIENT_ATTEMPTS + " : modification ignored", ex);
			}
		}
	}

	private static void setMaxNumberOfRetransmission(int n) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setMaxNumberOfRetransmission: maximum number of retransmission set to " + n);
		}

		MAX_NB_RETRANSMISSIONS = n;
	}

	public static int getMaxNumberOfRetransmission() {
		return MAX_NB_RETRANSMISSIONS;
	}

	private static void setClientTimeout(Dictionary cnf) {

		String s = ConfigHelper.getString(cnf, PropertiesDeclaration.CLIENT_TIMEOUT, null);
		if (s != null) {
			try {
				int seconds = Integer.parseInt(s);
				if (seconds < 1) {
					seconds = 1;
				}
				setDefaultRetryTimeout(seconds);
			}
			catch (Exception e) {
				LOGGER.error("Failed to parse " + PropertiesDeclaration.CLIENT_TIMEOUT + "property=" + s + " : using previous value (" + getDefaultRetryTimeout() + " s)", e);
			}
		}
	}

	private static void setDefaultRetryTimeout(int seconds) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setDefaultRetryTimeout: " + PropertiesDeclaration.CLIENT_TIMEOUT + " set to " + seconds + " seconds");
		}

		DEFAULT_RETRY_TIMEOUT_IN_MS = seconds * 1000;
	}

	public static int getDefaultRetryTimeout() {
		return DEFAULT_RETRY_TIMEOUT_IN_MS;
	}

	public static void setSessionLifetime(Dictionary cnf) {
		int lifetime = ConfigHelper.getInt(cnf, PropertiesDeclaration.SESSION_LIFETIME) * 1000;
		DiameterProperties.setSessionLifetime(lifetime);
	}

	private static void setSessionLifetime(int i) {
		SESSION_LIFETIME = i;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setSessionLifetime: Session Lifetime set to " + (getSessionLifetime() / 1000) + " seconds");
		}
	}

	public static long getSessionLifetime() {
		return SESSION_LIFETIME;
	}

	public static long getDefaultQuarantineDelay() {
		return DEFAULT_QUARANTINE_DELAY;
	}

	public static boolean isQuarantineEnabled() {
		return IS_QUARANTINE_ENABLED;
	}

	public static boolean isOverloadActive (){ return IS_OVERLOAD_ACTIVE; }
	public static int getOverloadLowWM (){ return OVERLOAD_LOWWM; }
	public static int getOverloadHighWM (){ return OVERLOAD_HIGHWM; }
	
	public static String getClientSrcTcp (){ return DIAMETER_CLIENT_SRC_TCP;}
	public static String[] getClientSrcSctp (){ return DIAMETER_CLIENT_SRC_SCTP;}
	public static int getClientSrcPortTcp (){ return DIAMETER_CLIENT_SRC_PORT_TCP;}
	public static int getClientSrcPortSctp (){ return DIAMETER_CLIENT_SRC_PORT_SCTP;}
	
	public static long getSctpTimeToLive (){ return SCTP_TTL;}
	public static boolean getSctpUnordered (){ return SCTP_UNORDERED;}

	public static boolean checkAppAdvert (){ return CHECK_APP_ADVERT;}
	
	public static boolean prohibitMultipleRemoteOriginHost (){ return UNIQUE_REMOTE_ORIGIN_HOST;}

	public static int getDprTimeout (){ return DPR_TIMEOUT;}
	public static int getDprReason (){ return DPR_REASON;}

	public static boolean checkProxiable (){ return CHECK_PROXIABLE;}

	public static boolean checkHostIPAddressFamily (){ return CHECK_HOST_IP_ADDRESS_FAMILY;}
	
	public static boolean perSocketMetrics() { return PER_SOCKET_METRICS; }

	// if set to true, the routes lookup is done through all local peers, to find the best absolute route - else only the local peer in use is taken into account
	public static boolean doAbsoluteProxyRouting (){ return ROUTING_ABSOLUTE_PROXY;}
	public static boolean doAbsoluteClientRouting (){ return ROUTING_ABSOLUTE_CLIENT;}

	public static boolean globalRequests (){ return REQUESTS_GLOBAL;}
	public static int getReqTimeoutResult (){ return REQUESTS_TIMEOUT_RESULT;}

	public static boolean peerReconnectEnabled (){ return ENABLE_PEER_RECONNECT;}

	public static String getClientSrcTcp (String group){
		if (group == null) return getClientSrcTcp (); // not sure if possible --> remain cautious
		String srcTcp = (String) getStoredGroupProperty (DIAMETER_CLIENT_SRC_TCP_PROPERTY, group, null); // check if already get / which stores the result as Object
		if (srcTcp == null){
			srcTcp = getPropertyFromConf (DIAMETER_CLIENT_SRC_TCP_PROPERTY, group); // check if groupName.diameteragent.client.src.tcp is set
			if (srcTcp == null){
				srcTcp = getPropertyFromConf (DIAMETER_CLIENT_SRC_PROPERTY, group); // check if groupName.diameteragent.client.src is set
				if (srcTcp != null)
					return storeGroupProperty (DIAMETER_CLIENT_SRC_TCP_PROPERTY, group, split(srcTcp) [0]);
				return storeGroupProperty (DIAMETER_CLIENT_SRC_TCP_PROPERTY, group, DIAMETER_CLIENT_SRC_TCP); // apply default diameteragent.client.src.tcp
			}
			return storeGroupProperty (DIAMETER_CLIENT_SRC_TCP_PROPERTY, group, split(srcTcp) [0]);
		}
		return srcTcp;
	}
	public static int getClientSrcPortTcp (String group){
		if (group == null) return getClientSrcPortTcp (); // not sure if possible --> remain cautious
		Integer port = (Integer) getStoredGroupProperty (DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, group, null); // check if already get / which stores the result as Object
		if (port != null) return port;
		String srcPortTcp = null;
		srcPortTcp = getPropertyFromConf (DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, group); // check if groupName.diameteragent.client.src.port.tcp is set
		if (srcPortTcp == null){
			srcPortTcp = getPropertyFromConf (DIAMETER_CLIENT_SRC_PORT_PROPERTY, group); // check if groupName.diameteragent.client.src.port is set
			if (srcPortTcp != null)
				return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, group, Integer.parseInt (srcPortTcp));
			return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, group, DIAMETER_CLIENT_SRC_PORT_TCP); // apply default diameteragent.client.src.tcp
		}
		return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_TCP_PROPERTY, group, Integer.parseInt (srcPortTcp));
	}
	public static String[] getClientSrcSctp (String group){
		if (group == null) return getClientSrcSctp (); // not sure if possible --> remain cautious
		String[] srcSctp = (String[]) getStoredGroupProperty (DIAMETER_CLIENT_SRC_SCTP_PROPERTY, group, null);
		if (srcSctp == null){
			String srcSctpS = getPropertyFromConf (DIAMETER_CLIENT_SRC_SCTP_PROPERTY, group);
			if (srcSctpS == null){
				srcSctpS = getPropertyFromConf (DIAMETER_CLIENT_SRC_PROPERTY, group);
				if (srcSctpS != null)
					return storeGroupProperty (DIAMETER_CLIENT_SRC_SCTP_PROPERTY, group, split(srcSctpS));
				return storeGroupProperty (DIAMETER_CLIENT_SRC_SCTP_PROPERTY, group, DIAMETER_CLIENT_SRC_SCTP);
			}
			return storeGroupProperty (DIAMETER_CLIENT_SRC_SCTP_PROPERTY, group, split(srcSctpS));
		}
		return srcSctp;
	}
	public static int getClientSrcPortSctp (String group){
		if (group == null) return getClientSrcPortSctp (); // not sure if possible --> remain cautious
		Integer port = (Integer) getStoredGroupProperty (DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, group, null); // check if already get / which stores the result as Object
		if (port != null) return port;
		String srcPortSctp = null;
		srcPortSctp = getPropertyFromConf (DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, group); // check if groupName.diameteragent.client.src.port.sctp is set
		if (srcPortSctp == null){
			srcPortSctp = getPropertyFromConf (DIAMETER_CLIENT_SRC_PORT_PROPERTY, group); // check if groupName.diameteragent.client.src.port is set
			if (srcPortSctp != null)
				return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, group, Integer.parseInt (srcPortSctp));
			return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, group, DIAMETER_CLIENT_SRC_PORT_SCTP); // apply default diameteragent.client.src.sctp
		}
		return storeGroupProperty (DIAMETER_CLIENT_SRC_PORT_SCTP_PROPERTY, group, Integer.parseInt (srcPortSctp));
	}
	public static String getPropertyFromConf (String propName, String groupName){
		return (String) _conf.get (new StringBuilder ().append (groupName).append (groupName.length () == 0 ? "" : ".").append (propName).toString ());
	}
	public static Map<String, Object> getGroupConf (String groupName){
		if (groupName == null) groupName = "";
		Map<String, Object> props = _confByGroup.get (groupName);
		if (props == null) _confByGroup.put (groupName, props = new HashMap<> ());
		return props;
	}
	public static Object getStoredGroupProperty (String propName, String groupName, Object def){
		Object value = getGroupConf (groupName).get (propName);
		return value != null ? value : def;
	}
	public static <T> T storeGroupProperty (String propName, String groupName, T value){
		getGroupConf (groupName).put (propName, value);
		return value;
	}

	
	public static Set<String> getReactorListenProperties (String prop, String group){
		Map<String, HashMap<String, String>> propsById = getReactorListenPropertiesById ();
		Set<String> ret = new HashSet<String> ();
		for (HashMap<String, String> map : propsById.values ()){
			String value = map.get (prop);
			if (value != null && group.equals (map.get ("group")))
				ret.add (value);
		}
		return ret;
	}
	public static Map<String, HashMap<String, String>> getReactorListenPropertiesById (){
		Map<String, HashMap<String, String>> ret = new HashMap<> ();
		Enumeration enumeration = _conf.keys();
		while (enumeration.hasMoreElements ()){
			String key = (String) enumeration.nextElement ();
			if (key.startsWith ("reactor.listen.")){
				String item = null;
				int index = key.indexOf ('.', "reactor.listen.".length ());
				if (index == -1){
					item = "port";
					index = key.length ();
				} else
					item = key.substring (index+1);
				String id = key.substring ("reactor.listen.".length (), index);
				HashMap<String, String> map = ret.get (id);
				if (map == null) ret.put (id, map = new HashMap<String, String> ());
				map.put (item, (String) _conf.get (key));
			}
		}
		for (HashMap<String, String> map : ret.values ())
			if (map.get ("group") == null) map.put ("group", "def");
		return ret;
	}
}
