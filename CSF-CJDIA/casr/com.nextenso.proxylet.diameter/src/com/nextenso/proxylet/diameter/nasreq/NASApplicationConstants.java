package com.nextenso.proxylet.diameter.nasreq;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * This class wraps constants defined in RFC 4005.
 */
public class NASApplicationConstants
		extends DiameterBaseConstants {

	/**
	 * Constructor used by classes wishing to inherit the constants.
	 */
	public NASApplicationConstants() {}

    /**
     * Call this to ensure the class is loaded and the static declarations 
     * are called and inserted in the Dictionary
     */
    public static void init() {
    	
    }
    
	/**
	 * The NASREQ Application Identifier.
	 */
	public static final long APPLICATION_NASREQ = 1L;

	/**
	 * The AVP Definition for NAS_PORT.
	 */
	public static final DiameterAVPDefinition AVP_NAS_PORT = new DiameterAVPDefinition("NAS-Port", 5L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for NAS_PORT_ID.
	 */
	public static final DiameterAVPDefinition AVP_NAS_PORT_ID = new DiameterAVPDefinition("NAS-Port-Id", 87L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for NAS_PORT_TYPE.
	 */
	public static final DiameterAVPDefinition AVP_NAS_PORT_TYPE = new DiameterAVPDefinition("NAS-Port-Type", 61L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for CALLED_STATION_ID.
	 */
	public static final DiameterAVPDefinition AVP_CALLED_STATION_ID = new DiameterAVPDefinition("Called-Station-Id", 30L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for CALLING_STATION_ID.
	 */
	public static final DiameterAVPDefinition AVP_CALLING_STATION_ID = new DiameterAVPDefinition("Calling-Station-Id", 31L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for CONNECT_INFO.
	 */
	public static final DiameterAVPDefinition AVP_CONNECT_INFO = new DiameterAVPDefinition("Connect-Info", 77L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for ORIGINATING_LINE_INFO.
	 */
	public static final DiameterAVPDefinition AVP_ORIGINATING_LINE_INFO = new DiameterAVPDefinition("Originating-Line-Info", 94L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for REPLY_MESSAGE.
	 */
	public static final DiameterAVPDefinition AVP_REPLY_MESSAGE = new DiameterAVPDefinition("Reply-Message", 18L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	// ??? public static final DiameterAVPDefinition AVP_TERMINATION_ACTION = new DiameterAVPDefinition ("Termination-Action", 29L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for USER_PASSWORD.
	 */
	public static final DiameterAVPDefinition AVP_USER_PASSWORD = new DiameterAVPDefinition("User-Password", 2L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for PASSWORD_RETRY.
	 */
	public static final DiameterAVPDefinition AVP_PASSWORD_RETRY = new DiameterAVPDefinition("Password-Retry", 75L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for PROMPT.
	 */
	public static final DiameterAVPDefinition AVP_PROMPT = new DiameterAVPDefinition("Prompt", 76L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for CHAP_AUTH.
	 */
	public static final DiameterAVPDefinition AVP_CHAP_AUTH = new DiameterAVPDefinition("CHAP-Auth", 402L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, GroupedFormat.INSTANCE);

	/**
	 * The AVP Definition for CHAP_ALGORITHM.
	 */
	public static final DiameterAVPDefinition AVP_CHAP_ALGORITHM = new DiameterAVPDefinition("CHAP-Algorithm", 403L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for CHAP_IDENT.
	 */
	public static final DiameterAVPDefinition AVP_CHAP_IDENT = new DiameterAVPDefinition("CHAP-Ident", 404L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for CHAP_RESPONSE.
	 */
	public static final DiameterAVPDefinition AVP_CHAP_RESPONSE = new DiameterAVPDefinition("CHAP-Response", 405L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for CHAP_CHALLENGE.
	 */
	public static final DiameterAVPDefinition AVP_CHAP_CHALLENGE = new DiameterAVPDefinition("CHAP-Challenge", 60L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ARAP_PASSWORD.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_PASSWORD = new DiameterAVPDefinition("ARAP-Password", 70L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ARAP_CHALLENGE_RESPONSE.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_CHALLENGE_RESPONSE = new DiameterAVPDefinition("ARAP-Challenge-Response", 84L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ARAP_SECURITY.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_SECURITY = new DiameterAVPDefinition("ARAP-Security", 73L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for ARAP_SECURITY_DATA.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_SECURITY_DATA = new DiameterAVPDefinition("ARAP-Security-Data", 74L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for SERVICE_TYPE.
	 */
	public static final DiameterAVPDefinition AVP_SERVICE_TYPE = new DiameterAVPDefinition("Service-Type", 6L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for CALLBACK_NUMBER.
	 */
	public static final DiameterAVPDefinition AVP_CALLBACK_NUMBER = new DiameterAVPDefinition("Callback-Number", 19L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for CALLBACK_ID.
	 */
	public static final DiameterAVPDefinition AVP_CALLBACK_ID = new DiameterAVPDefinition("Callback-Id", 20L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for IDLE_TIMEOUT.
	 */
	public static final DiameterAVPDefinition AVP_IDLE_TIMEOUT = new DiameterAVPDefinition("Idle-Timeout", 28L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for PORT_LIMIT.
	 */
	public static final DiameterAVPDefinition AVP_PORT_LIMIT = new DiameterAVPDefinition("Port-Limit", 62L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for NAS_FILTER_RULE.
	 */
	public static final DiameterAVPDefinition AVP_NAS_FILTER_RULE = new DiameterAVPDefinition("NAS-Filter-Rule", 400L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, IPFilterRuleFormat.INSTANCE);

	/**
	 * The AVP Definition for FILTER_ID.
	 */
	public static final DiameterAVPDefinition AVP_FILTER_ID = new DiameterAVPDefinition("Filter-Id", 11L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for CONFIGURATION_TOKEN.
	 */
	public static final DiameterAVPDefinition AVP_CONFIGURATION_TOKEN = new DiameterAVPDefinition("Configuration-Token", 78L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_PROTOCOL.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_PROTOCOL = new DiameterAVPDefinition("Framed-Protocol", 7L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_ROUTING.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_ROUTING = new DiameterAVPDefinition("Framed-Routing", 10L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_MTU.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_MTU = new DiameterAVPDefinition("Framed-MTU", 12L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_COMPRESSION.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_COMPRESSION = new DiameterAVPDefinition("Framed-Compression", 13L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IP_ADDRESS.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IP_ADDRESS = new DiameterAVPDefinition("Framed-IP-Address", 8L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IP_NETMASK.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IP_NETMASK = new DiameterAVPDefinition("Framed-IP-Netmask", 9L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_ROUTE.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_ROUTE = new DiameterAVPDefinition("Framed-Route", 22L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_POOL.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_POOL = new DiameterAVPDefinition("Framed-Pool", 88L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_INTERFACE_ID.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_INTERFACE_ID = new DiameterAVPDefinition("Framed-Interface-Id", 96L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IPV6_PREFIX.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IPV6_PREFIX = new DiameterAVPDefinition("Framed-IPv6-Prefix", 97L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IPV6_ROUTE.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IPV6_ROUTE = new DiameterAVPDefinition("Framed-IPv6-Route", 99L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IPV6_POOL.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IPV6_POOL = new DiameterAVPDefinition("Framed_IPv6_Pool", 100L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_IPX_NETWORK.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_IPX_NETWORK = new DiameterAVPDefinition("Framed-IPX-Network", 23L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_APPLETALK_LINK.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_APPLETALK_LINK = new DiameterAVPDefinition("Framed-Appletalk-Link", 37L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_APPLETALK_NETWORK.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_APPLETALK_NETWORK = new DiameterAVPDefinition("Framed-Appletalk-Network", 38L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for FRAMED_APPLETALK_ZONE.
	 */
	public static final DiameterAVPDefinition AVP_FRAMED_APPLETALK_ZONE = new DiameterAVPDefinition("Framed-Appletalk-Zone", 39L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ARAP_FEATURES.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_FEATURES = new DiameterAVPDefinition("ARAP-Features", 71L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ARAP_ZONE_ACCESS.
	 */
	public static final DiameterAVPDefinition AVP_ARAP_ZONE_ACCESS = new DiameterAVPDefinition("ARAP-Zone-Access", 72L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_IP_HOST.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_IP_HOST = new DiameterAVPDefinition("Login-IP-Host", 14L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_IPV6_HOST.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_IPV6_HOST = new DiameterAVPDefinition("Login-IPv6-Host", 98L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_SERVICE.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_SERVICE = new DiameterAVPDefinition("Login-Service", 15L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_TCP_PORT.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_TCP_PORT = new DiameterAVPDefinition("Login-TCP-Port", 16L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_LAT_SERVICE.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_LAT_SERVICE = new DiameterAVPDefinition("Login-LAT-Service", 34L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_LAT_NODE.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_LAT_NODE = new DiameterAVPDefinition("Login-LAT-Node", 35L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_LAT_GROUP.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_LAT_GROUP = new DiameterAVPDefinition("Login-LAT-Group", 36L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for LOGIN_LAT_PORT.
	 */
	public static final DiameterAVPDefinition AVP_LOGIN_LAT_PORT = new DiameterAVPDefinition("Login-LAT-Port", 63L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_TYPE.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_TYPE = new DiameterAVPDefinition("Tunnel-Type", 64L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_MEDIUM_TYPE.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_MEDIUM_TYPE = new DiameterAVPDefinition("Tunnel-Medium-Type", 65L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_CLIENT_ENDPOINT.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_CLIENT_ENDPOINT = new DiameterAVPDefinition("Tunnel-Client-Endpoint", 66L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_SERVER_ENDPOINT.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_SERVER_ENDPOINT = new DiameterAVPDefinition("Tunnel-Server-Endpoint", 67L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_PASSWORD.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_PASSWORD = new DiameterAVPDefinition("Tunnel-Password", 69L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_PRIVATE_GROUP_ID.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_PRIVATE_GROUP_ID = new DiameterAVPDefinition("Tunnel-Private-Group-Id", 81L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_ASSIGNMENT_ID.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_ASSIGNMENT_ID = new DiameterAVPDefinition("Tunnel-Assignment-Id", 82L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_PREFERENCE.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_PREFERENCE = new DiameterAVPDefinition("Tunnel-Preference", 83L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_CLIENT_AUTH_ID.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_CLIENT_AUTH_ID = new DiameterAVPDefinition("Tunnel-Client-Auth-Id", 90L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for TUNNEL_SERVER_AUTH_ID.
	 */
	public static final DiameterAVPDefinition AVP_TUNNEL_SERVER_AUTH_ID = new DiameterAVPDefinition("Tunnel-Server-Auth-Id", 91L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for TUNNELING.
	 */
	public static final DiameterAVPDefinition AVP_TUNNELING = new DiameterAVPDefinition("Tunneling", 401L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, GroupedFormat.INSTANCE);

	/**
	 * The AVP Definition for ACCOUNTING_INPUT_OCTETS.
	 */
	public static final DiameterAVPDefinition AVP_ACCOUNTING_INPUT_OCTETS = new DiameterAVPDefinition("Accounting-Input-Octets", 363L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

	/**
	 * The AVP Definition for ACCOUNTING_OUTPUT_OCTETS.
	 */
	public static final DiameterAVPDefinition AVP_ACCOUNTING_OUTPUT_OCTETS = new DiameterAVPDefinition("Accounting-Output-Octets", 364L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

	/**
	 * The AVP Definition for ACCOUNTING_INPUT_PACKETS.
	 */
	public static final DiameterAVPDefinition AVP_ACCOUNTING_INPUT_PACKETS = new DiameterAVPDefinition("Accounting-Input-Packets", 365L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

	/**
	 * The AVP Definition for ACCOUNTING_OUTPUT_PACKETS.
	 */
	public static final DiameterAVPDefinition AVP_ACCOUNTING_OUTPUT_PACKETS = new DiameterAVPDefinition("Accounting-Output-Packets", 366L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

	/**
	 * The AVP Definition for ACCT_SESSION_TIME.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_SESSION_TIME = new DiameterAVPDefinition("Acct-Session-Time", 46L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for ACCT_AUTHENTIC.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_AUTHENTIC = new DiameterAVPDefinition("Acct-Authentic", 45L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	//public static final DiameterAVPDefinition AVP_ACOUNTING_AUTH_METHOD = new DiameterAVPDefinition ("Acounting-Auth-Method", TBDL, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

	/**
	 * The AVP Definition for ACCT_DELAY_TIME.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_DELAY_TIME = new DiameterAVPDefinition("Acct-Delay-Time", 41L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for ACCT_LINK_COUNT.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_LINK_COUNT = new DiameterAVPDefinition("Acct-Link-Count", 51L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for ACCT_TUNNEL_CONNECTION.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_TUNNEL_CONNECTION = new DiameterAVPDefinition("Acct-Tunnel-Connection", 68L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for ACCT_TUNNEL_PACKETS_LOST.
	 */
	public static final DiameterAVPDefinition AVP_ACCT_TUNNEL_PACKETS_LOST = new DiameterAVPDefinition("Acct-Tunnel-Packets-Lost", 86L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

	/**
	 * The AVP Definition for NAS_IDENTIFIER.
	 */
	public static final DiameterAVPDefinition AVP_NAS_IDENTIFIER = new DiameterAVPDefinition("NAS-Identifier", 32L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

	/**
	 * The AVP Definition for NAS_IP_ADDRESS.
	 */
	public static final DiameterAVPDefinition AVP_NAS_IP_ADDRESS = new DiameterAVPDefinition("NAS-IP-Address", 4L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for NAS_IPV6_ADDRESS.
	 */
	public static final DiameterAVPDefinition AVP_NAS_IPV6_ADDRESS = new DiameterAVPDefinition("NAS-IPv6-Address", 95L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The AVP Definition for STATE.
	 */
	public static final DiameterAVPDefinition AVP_STATE = new DiameterAVPDefinition("State", 24L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

	/**
	 * The Command Code for AAR.
	 */
	public static final int COMMAND_AAR = 265;

	/**
	 * The Command Code for AAA.
	 */
	public static final int COMMAND_AAA = COMMAND_AAR;

}
