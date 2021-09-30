package com.nextenso.proxylet.radius;

import java.util.Hashtable;
import java.util.Locale;

/**
 * A Utility class that gathers useful constants and methods.
 */
public class RadiusUtils {

	protected static final int ZERO = 0;
	protected static final int ZERO_PLUS = 1;
	protected static final int ZERO_ONE = 2;
	protected static final int ONE = 3;
	private static Hashtable ATTRIBUTES = new Hashtable();

	/**
	 * The default Constructor.
	 */
	public RadiusUtils() {}

	/**
	 * Gets the usual name of the Radius Attribute of the given type. <br/>
	 * Returns <code>null</code> if the type is unknown. Only the registered
	 * attributes can be recognized. <br/>
	 * All the attributes defined in RFC2865 (Authentication) and RFC2866
	 * (Accounting) are registered by default.
	 * 
	 * @param type The attribute type.
	 * @return The usual attribute name or <code>null</code> if unknown.
	 */
	public static String getRadiusAttributeName(int type) {
		return (String) ATTRIBUTES.get(Integer.valueOf(type));
	}

	/**
	 * Gets the type of the named Radius attribute.<br/>
	 * Returns -1 if the name is unknown. Only the registered attributes can be
	 * recognized. <br/>
	 * All the attributes defined in RFC2865 (Authentication) and RFC2866
	 * (Accounting) are registered by default.
	 * 
	 * @param name The attribute name.
	 * @return The attribute type or -1 if unknown.
	 */
	public static int getRadiusAttributeType(String name) {
		Object o = ATTRIBUTES.get(name.toLowerCase(Locale.getDefault()));
		return (o != null) ? ((Integer) o).intValue() : -1;
	}

	/**
	 * Gets the data type of the Radius Attribute of the given type. <br/>
	 * Returns -1 if the type is unknown. Only the registered attributes can be
	 * recognized. <br/>
	 * All the attributes defined in RFC2865 (Authentication) and RFC2866
	 * (Accounting) are registered by default.
	 * 
	 * @param type The attribute type.
	 * @return DATA_TYPE_TEXT, DATA_TYPE_ADDRESS, DATA_TYPE_INTEGER,
	 *         DATA_TYPE_DATE, DATA_TYPE_BIN or -1 if unknown
	 */
	public static int getRadiusAttributeDataType(int type) {
		Object o = ATTRIBUTES.get(Integer.valueOf(type << 16));
		return (o != null) ? ((Integer) o).intValue() : -1;
	}

	/**
	 * Registers a new Radius attribute. <br/>
	 * It enables calling <code>getRadiusAttributeName(int type)</code>,
	 * <code>getRadiusAttributeType(String name)</code> and
	 * <code>getRadiusAttributeDataType(int type)</code> on it. <br/>
	 * All the attributes defined in RFC2865 (Authentication) and RFC2866
	 * (Accounting) are registered by default.
	 * 
	 * @param type The attribute type.
	 * @param name The attribute name.
	 * @param dataType The attribute data type: DATA_TYPE_TEXT, DATA_TYPE_ADDRESS,
	 *          DATA_TYPE_INTEGER, DATA_TYPE_DATE or DATA_TYPE_BIN.
	 */
	public static void registerRadiusAttribute(int type, String name, int dataType) {
		if (type < 0 || type > 255) {
			throw new IllegalArgumentException("Invalid radius attribute type: " + type);
		}
		Integer i = Integer.valueOf(type);
		ATTRIBUTES.put(i, name);
		ATTRIBUTES.put(name.toLowerCase(Locale.getDefault()), i);
		int myDataType = dataType;
		if (myDataType == -1) {
			myDataType = DATA_TYPE_BIN;
		}
		switch (myDataType) {
			case DATA_TYPE_TEXT:
			case DATA_TYPE_BIN:
			case DATA_TYPE_ADDRESS:
			case DATA_TYPE_INTEGER:
			case DATA_TYPE_DATE:
				ATTRIBUTES.put(Integer.valueOf(type << 16), Integer.valueOf(myDataType));
				break;
			default:
				throw new IllegalArgumentException("Invalid radius attribute data type: " + myDataType);
		}
	}

	/**
	 * The "text" data type in RFC 2865 paragraph 5.0 : UTF-8 encoded characters.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int DATA_TYPE_TEXT = 256;
	/**
	 * The "string" data type in RFC 2865 paragraph 5.0 : binary data
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int DATA_TYPE_BIN = 257;
	/**
	 * The "address" data type in RFC 2865 paragraph 5.0 : 32 bit value
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int DATA_TYPE_ADDRESS = 258;
	/**
	 * The "integer" data type in RFC 2865 paragraph 5.0 : 32 bit unsigned value
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int DATA_TYPE_INTEGER = 259;
	/**
	 * The "time" data type in RFC 2865 paragraph 5.0 : 32 bit unsigned value
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int DATA_TYPE_DATE = 260;

	/**
	 * The Radius attribute type for "User-Name".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int USER_NAME = 1;
	/**
	 * The Radius attribute type for "User-Password".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int USER_PASSWORD = 2;
	/**
	 * The Radius attribute type for "CHAP-Password".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CHAP_PASSWORD = 3;
	/**
	 * The Radius attribute type for "NAS-IP-Address".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_IP_ADDRESS = 4;
	/**
	 * The Radius attribute type for "NAS-Port".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT = 5;
	/**
	 * The Radius attribute type for "Service-Type".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE = 6;
	/**
	 * The Radius attribute type for "Framed-Protocol".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL = 7;
	/**
	 * The Radius attribute type for "Framed-IP-Address".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_IP_ADDRESS = 8;
	/**
	 * The Radius Attribute type for "Framed-IP-Netmask".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_IP_NETMASK = 9;
	/**
	 * The Radius Attribute type for "Framed-Routing".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTING = 10;
	/**
	 * The Radius attribute type for "Filter-Id".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FILTER_ID = 11;
	/**
	 * The Radius attribute type for "Framed-MTU".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_MTU = 12;
	/**
	 * The Radius attribute type for "Framed-Compression".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_COMPRESSION = 13;
	/**
	 * The Radius attribute type for "Login-IP-Host".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_IP_HOST = 14;
	/**
	 * The Radius attribute type for "Login-Service".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE = 15;
	/**
	 * The Radius attribute type for "Login-TCP-Port".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_TCP_PORT = 16;
	/**
	 * The Radius attribute type for "Reply-Message".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int REPLY_MESSAGE = 18;
	/**
	 * The Radius attribute type for "Callback-Number".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CALLBACK_NUMBER = 19;
	/**
	 * The Radius attribute type for "Callback-Id".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CALLBACK_ID = 20;
	/**
	 * The Radius attribute type for "Framed-Route".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTE = 22;
	/**
	 * The Radius attribute type for "Framed-IPX-Network".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_IPX_NETWORK = 23;
	/**
	 * The Radius attribute type for "State".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int STATE = 24;
	/**
	 * The Radius attribute type for "Class".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CLASS = 25;
	/**
	 * The Radius attribute type for "Vendor-Specific".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int VENDOR_SPECIFIC = 26;
	/**
	 * The Radius attribute type for "Session-Timeout".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SESSION_TIMEOUT = 27;
	/**
	 * The Radius attribute type for "Idle-Timeout".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int IDLE_TIMEOUT = 28;
	/**
	 * The Radius attribute type for "Terminate-Action".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int TERMINATE_ACTION = 29;
	/**
	 * The Radius attribute type for "Called-Station-Id".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CALLED_STATION_ID = 30;
	/**
	 * The Radius attribute type for "Calling-Station-Id".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CALLING_STATION_ID = 31;
	/**
	 * The Radius attribute type for "NAS-Identifier".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_IDENTIFIER = 32;
	/**
	 * The Radius attribute type for "Proxy-State".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int PROXY_STATE = 33;
	/**
	 * The Radius attribute type for "Login-LAT-Service".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_LAT_SERVICE = 34;
	/**
	 * The Radius attribute type for "Login-LAT-Node".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_LAT_NODE = 35;
	/**
	 * The Radius attribute type for "Login-LAT-Group".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_LAT_GROUP = 36;
	/**
	 * The Radius attribute type for "Framed-Appletalk-Link".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_APPLETALK_LINK = 37;
	/**
	 * The Radius attribute type for "Framed-Appletalk-Network".
	 */
	public static final int FRAMED_APPLETALK_NETWORK = 38;
	/**
	 * The Radius attribute type for "Framed-Appletalk-Zone".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_APPLETALK_ZONE = 39;
	/**
	 * The Radius attribute type for "CHAP-Challenge".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int CHAP_CHALLENGE = 60;
	/**
	 * The Radius attribute type for "NAS-Port-Type".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE = 61;
	/**
	 * The Radius attribute type for "Port-Limit".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int PORT_LIMIT = 62;
	/**
	 * The Radius attribute type for "Login-LAT-Port".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_LAT_PORT = 63;

	/**
	 * The "Login" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_LOGIN = 1;
	/**
	 * The "Framed" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_FRAMED = 2;
	/**
	 * The "Callback Login" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_CALLBACK_LOGIN = 3;
	/**
	 * The "Callback Framed" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_CALLBACK_FRAMED = 4;
	/**
	 * The "Outbound" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_OUTBOUND = 5;
	/**
	 * The "Administrative" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_ADMINISTRATIVE = 6;
	/**
	 * The "NAS Prompt" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_NAS_PROMPT = 7;
	/**
	 * The "Authenticate Only" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_AUTHENTICATE_ONLY = 8;
	/**
	 * The "Callback NAS Prompt" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_CALLBACK_NAS_PROMPT = 9;
	/**
	 * The "Call Check" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_CALL_CHECK = 10;
	/**
	 * The "Callback Administrative" Service-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int SERVICE_TYPE_CALLBACK_ADMINISTRATIVE = 11;

	/**
	 * The "PPP" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_PPP = 1;
	/**
	 * The "SLIP" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_SLIP = 2;
	/**
	 * The "Appletalk Remote Access Protocol (ARAP)" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_ARAP = 3;
	/**
	 * The "Gandalf Proprietary SingleLink/MultiLink protocol" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_GANDALF = 4;
	/**
	 * The "Xylogics proprietary IPX/SLIP" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_XYLOGICS = 5;
	/**
	 * The "X.75 Synchronous" Framed-Protocol.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_PROTOCOL_X75 = 6;

	/**
	 * Int value used by Framed-IPX-Network
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FFFFFFFE = 0xFFFFFFFE;

	/**
	 * The Framed-Routing method "None".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTING_NONE = 0;
	/**
	 * The Framed-Routing method "Send routing packets".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTING_SEND = 1;
	/**
	 * The Framed-Routing method "Listen for routing packets".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTING_LISTEN = 2;
	/**
	 * The Framed-Routing method "Send and Listen".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_ROUTING_SEND_LISTEN = 3;

	/**
	 * The Framed-Compression protocol "None".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_COMPRESSION_NONE = 0;
	/**
	 * The Framed-Compression protocol "VJ TCP/IP header compression".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_COMPRESSION_VJ_TCP_IP = 1;
	/**
	 * The Framed-Compression protocol "IPX header compression".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_COMPRESSION_IPX = 2;
	/**
	 * The Framed-Compression protocol "Stac-LZS compression".
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int FRAMED_COMPRESSION_STAC_LZS = 3;

	/**
	 * The "Telnet" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_TELNET = 0;
	/**
	 * The "Rlogin" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_RLOGIN = 1;
	/**
	 * The "TCP Clear" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_TCP_CLEAR = 2;
	/**
	 * The "PortMaster" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_PORTMASTER = 3;
	/**
	 * The "LAT" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_LAT = 4;
	/**
	 * The "X25-PAD" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_X25_PAD = 5;
	/**
	 * The "X25-T3POS" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_X25_T3POS = 6;
	/**
	 * The "TCP Clear Quiet" Login-Service.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int LOGIN_SERVICE_TCP_CLEAR_QUIET = 8;

	/**
	 * The "Default" Termination-Action.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int TERMINATION_ACTION_DEFAULT = 0;
	/**
	 * The "RADIUS-Request" Termination-Action.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int TERMINATION_ACTION_RADIUS_REQUEST = 1;

	/**
	 * The "Async" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ASYNC = 0;
	/**
	 * The "Sync" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_SYNC = 1;
	/**
	 * The "ISDN Sync" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ISDN_SYNC = 2;
	/**
	 * The "ISDN Async V.120" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ISDN_ASYNC_V120 = 3;
	/**
	 * The "ISDN Async V.110" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ISDN_ASYNC_V110 = 4;
	/**
	 * The "Virtual" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_VIRTUAL = 5;
	/**
	 * The "PIAFS" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_PIAFS = 6;
	/**
	 * The "HDLC Clear Channel" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_HDLC = 7;
	/**
	 * The "X.25" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_X25 = 8;
	/**
	 * The "X.75" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_X75 = 9;
	/**
	 * The "G.3 Fax" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_G3 = 10;
	/**
	 * The "SDSL" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_SDSL = 11;
	/**
	 * The "ADSL-CAP" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ADSL_CAP = 12;
	/**
	 * The "ADSL-DMT" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ADSL_DMT = 13;
	/**
	 * The "IDSL" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_IDSL_ISDN = 14;
	/**
	 * The "Ethernet" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_ETHERNET = 15;
	/**
	 * The "xDSL" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_XDSL = 16;
	/**
	 * The "Cable" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_CABLE = 17;
	/**
	 * The "Wireless-Other" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_WIRELESS_OTHER = 18;
	/**
	 * The "Wireless-IEEE 802.11" NAS-Port-Type.
	 * 
	 * <BR>
	 * See RFC 2865
	 */
	public static final int NAS_PORT_TYPE_WIRELESS = 19;

	static {
		registerRadiusAttribute(USER_NAME, "User-Name", DATA_TYPE_BIN);
		registerRadiusAttribute(USER_PASSWORD, "User-Password", DATA_TYPE_BIN);
		registerRadiusAttribute(CHAP_PASSWORD, "CHAP-Password", DATA_TYPE_BIN);
		registerRadiusAttribute(NAS_IP_ADDRESS, "NAS-IP-Address", DATA_TYPE_ADDRESS);
		registerRadiusAttribute(NAS_PORT, "NAS-Port", DATA_TYPE_INTEGER);
		registerRadiusAttribute(SERVICE_TYPE, "Service-Type", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FRAMED_PROTOCOL, "Framed-Protocol", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FRAMED_IP_ADDRESS, "Framed-IP-Address", DATA_TYPE_ADDRESS);
		registerRadiusAttribute(FRAMED_IP_NETMASK, "Framed-IP-Netmask", DATA_TYPE_ADDRESS);
		registerRadiusAttribute(FRAMED_ROUTING, "Framed-Routing", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FILTER_ID, "Filter-Id", DATA_TYPE_TEXT);
		registerRadiusAttribute(FRAMED_MTU, "Framed-MTU", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FRAMED_COMPRESSION, "Framed-Compression", DATA_TYPE_INTEGER);
		registerRadiusAttribute(LOGIN_IP_HOST, "Login-IP-Host", DATA_TYPE_ADDRESS);
		registerRadiusAttribute(LOGIN_SERVICE, "Login-Service", DATA_TYPE_INTEGER);
		registerRadiusAttribute(LOGIN_TCP_PORT, "Login-TCP-Port", DATA_TYPE_INTEGER);
		registerRadiusAttribute(REPLY_MESSAGE, "Reply-Message", DATA_TYPE_TEXT);
		registerRadiusAttribute(CALLBACK_NUMBER, "Callback-Number", DATA_TYPE_BIN);
		registerRadiusAttribute(CALLBACK_ID, "Callback-Id", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_ROUTE, "Framed-Route", DATA_TYPE_TEXT);
		registerRadiusAttribute(FRAMED_IPX_NETWORK, "Framed-IPX-Network", DATA_TYPE_INTEGER);
		registerRadiusAttribute(STATE, "State", DATA_TYPE_BIN);
		registerRadiusAttribute(CLASS, "Class", DATA_TYPE_BIN);
		registerRadiusAttribute(VENDOR_SPECIFIC, "Vendor-Specific", DATA_TYPE_BIN);
		registerRadiusAttribute(SESSION_TIMEOUT, "Session-Timeout", DATA_TYPE_INTEGER);
		registerRadiusAttribute(IDLE_TIMEOUT, "Idle-Timeout", DATA_TYPE_INTEGER);
		registerRadiusAttribute(TERMINATE_ACTION, "Terminate-Action", DATA_TYPE_INTEGER);
		registerRadiusAttribute(CALLED_STATION_ID, "Called-Station-Id", DATA_TYPE_BIN);
		registerRadiusAttribute(CALLING_STATION_ID, "Calling-Station-Id", DATA_TYPE_BIN);
		registerRadiusAttribute(NAS_IDENTIFIER, "NAS-Identifier", DATA_TYPE_BIN);
		registerRadiusAttribute(PROXY_STATE, "Proxy-State", DATA_TYPE_BIN);
		registerRadiusAttribute(LOGIN_LAT_SERVICE, "Login-LAT-Service", DATA_TYPE_BIN);
		registerRadiusAttribute(LOGIN_LAT_NODE, "Login-LAT-Node", DATA_TYPE_BIN);
		registerRadiusAttribute(LOGIN_LAT_GROUP, "Login-LAT-Group", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_APPLETALK_LINK, "Framed-Appletalk-Link", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FRAMED_APPLETALK_NETWORK, "Framed-Appletalk-Network", DATA_TYPE_INTEGER);
		registerRadiusAttribute(FRAMED_APPLETALK_ZONE, "Framed-Appletalk-Zone", DATA_TYPE_BIN);
		registerRadiusAttribute(CHAP_CHALLENGE, "CHAP-Challenge", DATA_TYPE_BIN);
		registerRadiusAttribute(NAS_PORT_TYPE, "NAS-Port-Type", DATA_TYPE_INTEGER);
		registerRadiusAttribute(PORT_LIMIT, "Port-Limit", DATA_TYPE_INTEGER);
		registerRadiusAttribute(LOGIN_LAT_PORT, "Login-LAT-Port", DATA_TYPE_BIN);
	}

	// ----------------------------------------------------------------------
	//   RFC 2867 RADIUS Accounting Modifications for Tunnel Protocol Support
	// ----------------------------------------------------------------------
	/**
	 * The Radius attribute type for "Tunnel-Type".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_TYPE = 64;
	/**
	 * The Radius attribute type for "Tunnel-Medium-Type".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_MEDIUM_TYPE = 65;
	/**
	 * The Radius attribute type for "Tunnel-Client-Endpoint".
	 */
	public static final int TUNNEL_CLIENT_ENDPOINT = 66;
	/**
	 * The Radius attribute type for "Tunnel-Server-Endpoint".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_SERVER_ENDPOINT = 67;
	/**
	 * The Radius attribute type for "Acct-Tunnel-Connection".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int ACCT_TUNNEL_CONNECTION = 68;
	/**
	 * The Radius attribute type for "Tunnel-Password".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_PASSWORD = 69;
	/**
	 * The Radius attribute type for "Tunnel-Private-Group-ID".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_PRIVATE_GROUP_ID = 81;
	/**
	 * The Radius attribute type for "Tunnel-Assignment-ID".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_ASSIGNMENT_ID = 82;
	/**
	 * The Radius attribute type for "Tunnel-Preference".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int TUNNEL_PREFERENCE = 83;
	/**
	 * The Radius attribute type for "Acct-Tunnel-Packets-Lost".
	 * 
	 * <BR>
	 * See RFC 2867
	 */
	public static final int ACCT_TUNNEL_PACKETS_LOST = 86;
	static {
		registerRadiusAttribute(TUNNEL_TYPE, "Tunnel-Type", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_MEDIUM_TYPE, "Tunnel-Medium-Type", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_CLIENT_ENDPOINT, "Tunnel-Client-Endpoint", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_SERVER_ENDPOINT, "Tunnel-Server-Endpoint", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_PASSWORD, "Tunnel-Password", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_PRIVATE_GROUP_ID, "Tunnel-Private-Group-ID", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_ASSIGNMENT_ID, "Tunnel-Assignment-ID", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_PREFERENCE, "Tunnel-Preference", DATA_TYPE_BIN);
	}

	// ----------------------------------------------------------------------
	//   RFC 2868 RADIUS Attributes for Tunnel Protocol Support
	// ----------------------------------------------------------------------
	/**
	 * The Radius attribute type for "Tunnel-Client-Auth-ID".
	 * 
	 * <BR>
	 * See RFC 2868
	 */
	public static final int TUNNEL_CLIENT_AUTH_ID = 90;
	/**
	 * The Radius attribute type for "Tunnel-Server-Auth-ID".
	 * 
	 * <BR>
	 * See RFC 2868
	 */
	public static final int TUNNEL_SERVER_AUTH_ID = 91;

	static {
		registerRadiusAttribute(TUNNEL_CLIENT_AUTH_ID, "Tunnel-Client-Auth-ID", DATA_TYPE_BIN);
		registerRadiusAttribute(TUNNEL_SERVER_AUTH_ID, "Tunnel-Server-Auth-ID", DATA_TYPE_BIN);
	}

	// ----------------------------------------------------------------------
	//   RFC 2869 RADIUS Extensions
	// ----------------------------------------------------------------------
	/**
	 * The Radius attribute type for "Acct-Input-Gigawords".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ACCT_INPUT_GIGAWORDS = 52;
	/**
	 * The Radius attribute type for "Acct-Output-Gigawords".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ACCT_OUTPUT_GIGAWORDS = 53;
	/**
	 * The Radius attribute type for "Event-Timestamp".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int EVENT_TIMESTAMP = 55;
	/**
	 * The Radius attribute type for "ARAP-Password".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_PASSWORD = 70;
	/**
	 * The Radius attribute type for "ARAP-Features".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_FEATURES = 71;
	/**
	 * The Radius attribute type for "ARAP-Zone-Access".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_ZONE_ACCESS = 72;
	/**
	 * The Radius attribute type for "ARAP-Security".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_SECURITY = 73;
	/**
	 * The Radius attribute type for "ARAP-Security-Data".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_SECURITY_DATA = 74;
	/**
	 * The Radius attribute type for "Password-Retry".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int PASSWORD_RETRY = 75;
	/**
	 * The Radius attribute type for "Prompt".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int PROMPT = 76;
	/**
	 * The Radius attribute type for "Connect-Info".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int CONNECT_INFO = 77;
	/**
	 * The Radius attribute type for "Configuration-Token".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int CONFIGURATION_TOKEN = 78;
	/**
	 * The Radius attribute type for "EAP-Message".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int EAP_MESSAGE = 79;
	/**
	 * The Radius attribute type for "Message-Authenticator".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int MESSAGE_AUTHENTICATOR = 80;
	/**
	 * The Radius attribute type for "ARAP-Challenge-Response".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ARAP_CHALLENGE_RESPONSE = 84;
	/**
	 * The Radius attribute type for "Acct-Interim-Interval".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int ACCT_INTERIM_INTERVAL = 85;
	/**
	 * The Radius attribute type for "NAS-Port-Id".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int NAS_PORT_ID = 87;
	/**
	 * The Radius attribute type for "Framed-Pool".
	 * 
	 * <BR>
	 * See RFC 2869
	 */
	public static final int FRAMED_POOL = 88;

	static {
		registerRadiusAttribute(ACCT_INPUT_GIGAWORDS, "Acct-Input-Gigawords", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_OUTPUT_GIGAWORDS, "Acct-Ouput-Gigawords", DATA_TYPE_INTEGER);
		registerRadiusAttribute(EVENT_TIMESTAMP, "Event-Timestamp", DATA_TYPE_DATE);
		registerRadiusAttribute(ARAP_PASSWORD, "ARAP-Password", DATA_TYPE_BIN);
		registerRadiusAttribute(ARAP_FEATURES, "ARAP-Features", DATA_TYPE_BIN);
		registerRadiusAttribute(ARAP_ZONE_ACCESS, "ARAP-Zone-Access", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ARAP_SECURITY, "ARAP-Security", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ARAP_SECURITY_DATA, "ARAP-Security-Data", DATA_TYPE_BIN);
		registerRadiusAttribute(PASSWORD_RETRY, "Password-Retry", DATA_TYPE_INTEGER);
		registerRadiusAttribute(PROMPT, "Prompt", DATA_TYPE_INTEGER);
		registerRadiusAttribute(CONNECT_INFO, "Connect-Info", DATA_TYPE_TEXT);
		registerRadiusAttribute(CONFIGURATION_TOKEN, "Configuration-Token", DATA_TYPE_BIN);
		registerRadiusAttribute(EAP_MESSAGE, "EAP-Message", DATA_TYPE_BIN);
		registerRadiusAttribute(MESSAGE_AUTHENTICATOR, "Message-Authenticator", DATA_TYPE_BIN);
		registerRadiusAttribute(ARAP_CHALLENGE_RESPONSE, "ARAP-Challenge-Response", DATA_TYPE_BIN);
		registerRadiusAttribute(ACCT_INTERIM_INTERVAL, "Acct-Interim-Interval", DATA_TYPE_INTEGER);
		registerRadiusAttribute(NAS_PORT_ID, "NAS-Port-Id", DATA_TYPE_TEXT);
		registerRadiusAttribute(FRAMED_POOL, "Framed-Pool", DATA_TYPE_BIN);
	}

	// ----------------------------------------------------------------------
	//   RFC 3162
	// ----------------------------------------------------------------------
	/**
	 * The Radius attribute type for "NAS-IPv6-Address".
	 */
	public static final int NAS_IPv6_ADDRESS = 95;
	/**
	 * The Radius attribute type for "Framed-Interface-Id".
	 * 
	 * <BR>
	 * See RFC 3162
	 */
	public static final int FRAMED_INTERFACE_ID = 96;
	/**
	 * The Radius attribute type for "Framed-IPv6-Prefix".
	 * 
	 * <BR>
	 * See RFC 3162
	 */
	public static final int FRAMED_IPV6_PREFIX = 97;
	/**
	 * The Radius attribute type for "Login-IPv6-Host".
	 * 
	 * <BR>
	 * See RFC 3162
	 */
	public static final int LOGIN_IPV6_HOST = 98;
	/**
	 * The Radius attribute type for "Framed-IPv6-Route".
	 * 
	 * <BR>
	 * See RFC 3162
	 */
	public static final int FRAMED_IPV6_ROUTE = 99;
	/**
	 * The Radius attribute type for "Framed-IPv6-Pool".
	 * 
	 * <BR>
	 * See RFC 3162
	 */
	public static final int FRAMED_IPV6_POOL = 100;

	static {
		registerRadiusAttribute(NAS_IPv6_ADDRESS, "NAS-IPv6-Address", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_INTERFACE_ID, "Framed-Interface-Id", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_IPV6_PREFIX, "Framed-IPv6-Prefix", DATA_TYPE_BIN);
		registerRadiusAttribute(LOGIN_IPV6_HOST, "Login-IPv6-Host", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_IPV6_ROUTE, "Framed-IPv6-Route", DATA_TYPE_BIN);
		registerRadiusAttribute(FRAMED_IPV6_POOL, "Framed-IPv6-Pool", DATA_TYPE_BIN);
	}

	// RFC 4005
	/**
	 * The Radius attribute type for "Originating-Line-Info".
	 * 
	 * <BR>
	 * See RFC 4005
	 */
	public static final int ORIGINATING_LINE_INFO = 94;

	static {
		registerRadiusAttribute(ORIGINATING_LINE_INFO, "Originating-Line-Info", DATA_TYPE_BIN);
	}

	// RFC  4372
	/**
	 * The Radius attribute type for "Chargeable-User-Identity".
	 * 
	 * <BR>
	 * See RFC 4372
	 */
	public static final int CHARGEABLE_USER_IDENTITY = 89;
	static {
		registerRadiusAttribute(CHARGEABLE_USER_IDENTITY, "Chargeable-User-Identity", DATA_TYPE_BIN);
	}

	// RFC 3576
	/**
	 * The Radius attribute type for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE = 101;

	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_RESIDUAL_SESSION_CONTEXT_REMOVAL = 201;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_INVALID_EAP_PACKET = 202;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_UNSUPPORTED_ATTRIBUTE = 401;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_MISSING_ATTRIBUTE = 402;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_NAS_IDENTIFICATION_MISMATCH = 403;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_INVALID_REQUEST = 404;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_UNSUPPORTED_SERVICE = 405;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_UNSUPPORTED_EXTENSION = 406;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_UNSUPPORTED_ATTRIBUTE_VALUE = 407;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_ADMINISTRATIVELY_PROHIBITED = 501;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_REQUEST_NOT_ROUTABLE = 502;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_SESSION_CONTEXT_NOT_FOUND = 503;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_SESSION_CONTEXT_NOT_REMOVABLE = 504;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_OTHER_PROXY_PROCESSING_ERROR = 505;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_RESOURCES_UNAVAILABLE = 506;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_REQUEST_INITIATED = 507;
	/**
	 * The Radius attribute value for "Error-Cause".
	 * 
	 * <BR>
	 * See RFC 3576 section3.5
	 */
	public static final int ERROR_CAUSE_MULTIPLE_SESSION_SELECTION_UNSUPPORTED = 508;

	static {
		registerRadiusAttribute(ERROR_CAUSE, "Error-Cause", DATA_TYPE_INTEGER);
	}

	//  RFC 4675
	/**
	 * The Radius attribute type for "Egress-VLANID".
	 * 
	 * <BR>
	 * See RFC 4675
	 */
	public static final int EGRESS_VLANID = 56;

	/**
	 * The Radius attribute type for "Ingress-Filters".
	 * 
	 * <BR>
	 * See RFC 4675
	 */
	public static final int INGRESS_FILTERS = 57;

	/**
	 * The Radius attribute type for "Egress-VLAN-Name".
	 * 
	 * <BR>
	 * See RFC 4675
	 */
	public static final int EGRESS_VLAN_NAME = 58;

	/**
	 * The Radius attribute type for "User-Priority-Table".
	 * 
	 * <BR>
	 * See RFC 4675
	 */
	public static final int USER_PRIORITY_TABLE = 59;

	static {
		registerRadiusAttribute(EGRESS_VLANID, "Egress-VLANID", DATA_TYPE_BIN);
		registerRadiusAttribute(INGRESS_FILTERS, "Ingress-Filters", DATA_TYPE_INTEGER);
		registerRadiusAttribute(EGRESS_VLAN_NAME, "Egress-VLAN-Name", DATA_TYPE_TEXT);
		registerRadiusAttribute(USER_PRIORITY_TABLE, "User-Priority-Table", DATA_TYPE_BIN);
	}

	// RFC 4818
	/**
	 * The Radius attribute type for "Delegated-IPv6-Prefix".
	 * 
	 * <BR>
	 * See RFC 4818
	 */
	public static final int DELEGATED_IPV6_PREFIX = 123;
	static {
		registerRadiusAttribute(DELEGATED_IPV6_PREFIX, "Delegated-IPv6-Prefix", DATA_TYPE_BIN);
	}

	// RFC 4849
	/**
	 * The Radius attribute type for "NAS-Filter-Rule".
	 * 
	 * <BR>
	 * See RFC 4849
	 */
	public static final int NAS_FILTER_RULE = 92;

	static {
		registerRadiusAttribute(NAS_FILTER_RULE, "NAS-Filter-Rule", DATA_TYPE_BIN);
	}
}
