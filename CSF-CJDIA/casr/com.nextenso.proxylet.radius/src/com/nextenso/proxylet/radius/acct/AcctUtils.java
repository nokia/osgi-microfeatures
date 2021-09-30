package com.nextenso.proxylet.radius.acct;

import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.RadiusAttribute;

import java.util.Hashtable;
import java.util.Map;

/**
 * This class gathers useful accounting constants and methods.
 */
public class AcctUtils
		extends RadiusUtils {

	private static Map<Integer, Integer> POLICIES = new Hashtable<Integer, Integer>();

	/**
	 * Specifies if a given Radius attribute is valid in an Accounting request.
	 * 
	 * <BR>
	 * See RFC 2866 section 5.13 for details.
	 * 
	 * @param attribute The Radius attribute to check.
	 * @return true if valid in an Accounting request, false otherwise.
	 */
	public static final boolean isValidAccountingRequestAttribute(RadiusAttribute attribute) {
		Integer i = POLICIES.get(attribute.getType());
		if (i == null) {
			// custom Attribute
			return true;
		}
		if (i == ZERO) {
			return false;
		} else if (i == ZERO_PLUS) {
			return true;
		} else if (i == ZERO_ONE) {
			return (attribute.getValueSize() <= 1);
		} else if (i == ONE) {
			return (attribute.getValueSize() == 1);
		}
		return false;
	}

	/**
	 * Specifies if a given Radius attribute is valid in an Accounting response.
	 * 
	 * <BR>
	 * See RFC 2866 section 5.13 for details.
	 * 
	 * @param attribute The Radius attribute to check.
	 * @return true if valid in an Accounting response, false otherwise.
	 */
	public static final boolean isValidAccountingResponseAttribute(RadiusAttribute attribute) {
		int type = attribute.getType();
		Integer i = POLICIES.get(type);
		if (i == null) {
			// custom Attribute
			return true;
		}
		return (type == PROXY_STATE || type == VENDOR_SPECIFIC);
	}

	/**
	 * The Accounting-Request request code (4).
	 */
	public static final int CODE_ACCOUNTING_REQUEST = 4;

	/**
	 * The Accounting-Response message code (5).
	 */
	public static final int CODE_ACCOUNTING_RESPONSE = 5;

	/**
	 * The standard radius accounting port (1813).
	 */
	public static final int ACCT_PORT = 1813;
	/**
	 * The old deprecated radius accounting port (1646).
	 */
	public static final int ACCT_PORT_DEPRECATED = 1646;

	/**
	 * The Radius Attribute type for "Acct-Status-Type".
	 */
	public static final int ACCT_STATUS_TYPE = 40;
	/**
	 * The Radius Attribute type for "Acct-Delay-Time".
	 */
	public static final int ACCT_DELAY_TIME = 41;
	/**
	 * The Radius Attribute type for "Acct-Input-Octets".
	 */
	public static final int ACCT_INPUT_OCTETS = 42;
	/**
	 * The Radius Attribute type for "Acct-OutputOctets".
	 */
	public static final int ACCT_OUTPUT_OCTETS = 43;
	/**
	 * The Radius Attribute type for "Acct-Session-Id".
	 */
	public static final int ACCT_SESSION_ID = 44;
	/**
	 * The Radius Attribute type for "Acct-Authentic".
	 */
	public static final int ACCT_AUTHENTIC = 45;
	/**
	 * The Radius Attribute type for "Acct-Session-Time".
	 */
	public static final int ACCT_SESSION_TIME = 46;
	/**
	 * The Radius Attribute type for "Acct-Input-Packets".
	 */
	public static final int ACCT_INPUT_PACKETS = 47;
	/**
	 * The Radius Attribute type for "Acct-Output-Packets".
	 */
	public static final int ACCT_OUTPUT_PACKETS = 48;
	/**
	 * The Radius Attribute type for "Acct-Terminate-Cause".
	 */
	public static final int ACCT_TERMINATE_CAUSE = 49;
	/**
	 * The Radius Attribute type for "Acct-Multi-Session-Id".
	 */
	public static final int ACCT_MULTI_SESSION_ID = 50;
	/**
	 * The Radius Attribute type for "Acct-Link-Count".
	 */
	public static final int ACCT_LINK_COUNT = 51;

	/**
	 * The "Start" Acct-Status-Type.
	 */
	public static final int ACCT_STATUS_TYPE_START = 1;
	/**
	 * The "Stop" Acct-Status-Type.
	 */
	public static final int ACCT_STATUS_TYPE_STOP = 2;
	/**
	 * The "Interim-Update" Acct-Status-Type.
	 */
	public static final int ACCT_STATUS_TYPE_INTERIM_UPDATE = 3;
	/**
	 * The "Accounting-On" Acct-Status-Type.
	 */
	public static final int ACCT_STATUS_TYPE_ACCOUNTING_ON = 7;
	/**
	 * The "Accounting-On" Acct-Status-Type.
	 */
	public static final int ACCT_STATUS_TYPE_ACCOUNTING_OFF = 8;

	/**
	 * The "RADIUS" Acct-Authentic.
	 */
	public static final int ACCT_AUTHENTIC_RADIUS = 1;
	/**
	 * The "Local" Acct-Authentic.
	 */
	public static final int ACCT_AUTHENTIC_LOCAL = 2;
	/**
	 * The "Remote" Acct-Authentic.
	 */
	public static final int ACCT_AUTHENTIC_REMOTE = 3;

	/**
	 * The "User Request" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_USER_REQUEST = 1;
	/**
	 * The "Lost Carrier" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_LOST_CARRIER = 2;
	/**
	 * The "Lost Service" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_LOST_SERVICE = 3;
	/**
	 * The "Idle Timeout" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_IDLE_TIMEOUT = 4;
	/**
	 * The "Session Timeout" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_SESSION_TIMEOUT = 5;
	/**
	 * The "Admin Reset" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_ADMIN_RESET = 6;
	/**
	 * The "Admin Reboot" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_ADMIN_REBOOT = 7;
	/**
	 * The "Port Error" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_PORT_ERROR = 8;
	/**
	 * The "NAS Error" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_NAS_ERROR = 9;
	/**
	 * The "NAS Request" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_NAS_REQUEST = 10;
	/**
	 * The "NAS Reboot" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_NAS_REBOOT = 11;
	/**
	 * The "Port Unneeded" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_PORT_UNNEEDED = 12;
	/**
	 * The "Port Preempted" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_PORT_PREEMPTED = 13;
	/**
	 * The "Port Suspended" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_PORT_SUSPENDED = 14;
	/**
	 * The "Service Unavailable" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_SERVICE_UNAVAILABLE = 15;
	/**
	 * The "Callback" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_CALLBACK = 16;
	/**
	 * The "User Error" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_USER_ERROR = 17;
	/**
	 * The "Host Request" Acct-Terminate-Cause.
	 */
	public static final int ACCT_TERMINATE_CAUSE_HOST_REQUEST = 18;

	static {
		// RFC 2666 for RFC 2665 attributes
		POLICIES.put(USER_NAME, ZERO_ONE);
		POLICIES.put(USER_PASSWORD, ZERO);
		POLICIES.put(CHAP_PASSWORD, ZERO);
		POLICIES.put(NAS_IP_ADDRESS, ZERO_ONE);
		POLICIES.put(NAS_PORT, ZERO_ONE);
		POLICIES.put(SERVICE_TYPE, ZERO_ONE);
		POLICIES.put(FRAMED_PROTOCOL, ZERO_ONE);
		POLICIES.put(FRAMED_IP_ADDRESS, ZERO_ONE);
		POLICIES.put(FRAMED_IP_NETMASK, ZERO_ONE);
		POLICIES.put(FRAMED_ROUTING, ZERO_ONE);
		POLICIES.put(FILTER_ID, ZERO_PLUS);
		POLICIES.put(FRAMED_MTU, ZERO_ONE);
		POLICIES.put(FRAMED_COMPRESSION, ZERO_PLUS);
		POLICIES.put(LOGIN_IP_HOST, ZERO_PLUS);
		POLICIES.put(LOGIN_SERVICE, ZERO_ONE);
		POLICIES.put(LOGIN_TCP_PORT, ZERO_ONE);
		POLICIES.put(REPLY_MESSAGE, ZERO);
		POLICIES.put(CALLBACK_NUMBER, ZERO_ONE);
		POLICIES.put(CALLBACK_ID, ZERO_ONE);
		POLICIES.put(FRAMED_ROUTE, ZERO_PLUS);
		POLICIES.put(FRAMED_IPX_NETWORK, ZERO_ONE);
		POLICIES.put(STATE, ZERO);
		POLICIES.put(CLASS, ZERO_PLUS);
		POLICIES.put(VENDOR_SPECIFIC, ZERO_PLUS);
		POLICIES.put(SESSION_TIMEOUT, ZERO_ONE);
		POLICIES.put(IDLE_TIMEOUT, ZERO_ONE);
		POLICIES.put(TERMINATE_ACTION, ZERO_ONE);
		POLICIES.put(CALLED_STATION_ID, ZERO_ONE);
		POLICIES.put(CALLING_STATION_ID, ZERO_ONE);
		POLICIES.put(NAS_IDENTIFIER, ZERO_ONE);
		POLICIES.put(PROXY_STATE, ZERO_PLUS);
		POLICIES.put(LOGIN_LAT_SERVICE, ZERO_ONE);
		POLICIES.put(LOGIN_LAT_NODE, ZERO_ONE);
		POLICIES.put(LOGIN_LAT_GROUP, ZERO_ONE);
		POLICIES.put(FRAMED_APPLETALK_LINK, ZERO_ONE);
		POLICIES.put(FRAMED_APPLETALK_NETWORK, ZERO_ONE);
		POLICIES.put(FRAMED_APPLETALK_ZONE, ZERO_ONE);

		// RFC 2666
		POLICIES.put(ACCT_STATUS_TYPE, ONE);
		POLICIES.put(ACCT_DELAY_TIME, ZERO_ONE);
		POLICIES.put(ACCT_INPUT_OCTETS, ZERO_ONE);
		POLICIES.put(ACCT_OUTPUT_OCTETS, ZERO_ONE);
		POLICIES.put(ACCT_SESSION_ID, ONE);
		POLICIES.put(ACCT_AUTHENTIC, ZERO_ONE);
		POLICIES.put(ACCT_SESSION_TIME, ZERO_ONE);
		POLICIES.put(ACCT_INPUT_PACKETS, ZERO_ONE);
		POLICIES.put(ACCT_OUTPUT_PACKETS, ZERO_ONE);
		POLICIES.put(ACCT_TERMINATE_CAUSE, ZERO_ONE);
		POLICIES.put(ACCT_MULTI_SESSION_ID, ZERO_PLUS);
		POLICIES.put(ACCT_LINK_COUNT, ZERO_PLUS);
		POLICIES.put(CHAP_CHALLENGE, ZERO);
		POLICIES.put(NAS_PORT_TYPE, ZERO_ONE);
		POLICIES.put(PORT_LIMIT, ZERO_ONE);
		POLICIES.put(LOGIN_LAT_PORT, ZERO_ONE);

		// RFC 2867 section 5
		POLICIES.put(TUNNEL_TYPE, ZERO_ONE);
		POLICIES.put(TUNNEL_MEDIUM_TYPE, ZERO_ONE);
		POLICIES.put(TUNNEL_CLIENT_ENDPOINT, ZERO_ONE);
		POLICIES.put(TUNNEL_SERVER_ENDPOINT, ZERO_ONE);
		POLICIES.put(ACCT_TUNNEL_CONNECTION, ZERO_ONE);
		POLICIES.put(TUNNEL_PASSWORD, ZERO);
		POLICIES.put(TUNNEL_PRIVATE_GROUP_ID, ZERO_ONE);
		POLICIES.put(TUNNEL_ASSIGNMENT_ID, ZERO_ONE);
		POLICIES.put(TUNNEL_PREFERENCE, ZERO);
		POLICIES.put(ACCT_TUNNEL_PACKETS_LOST, ZERO_ONE);

		// RFC 2868 section 4
		POLICIES.put(TUNNEL_CLIENT_AUTH_ID, ZERO_ONE);
		POLICIES.put(TUNNEL_SERVER_AUTH_ID, ZERO_ONE);

		// RFC 2869
		POLICIES.put(ACCT_INPUT_GIGAWORDS, ZERO_ONE);
		POLICIES.put(ACCT_OUTPUT_GIGAWORDS, ZERO_ONE);
		POLICIES.put(EVENT_TIMESTAMP, ZERO_ONE);
		POLICIES.put(NAS_PORT_ID, ZERO_ONE);
		POLICIES.put(CONNECT_INFO, ZERO_PLUS);

		// RFC 3162
		POLICIES.put(NAS_IPv6_ADDRESS, ZERO_ONE);
		POLICIES.put(FRAMED_INTERFACE_ID, ZERO_ONE);
		POLICIES.put(FRAMED_IPV6_PREFIX, ZERO_PLUS);
		POLICIES.put(LOGIN_IPV6_HOST, ZERO_PLUS);
		POLICIES.put(FRAMED_IPV6_ROUTE, ZERO_PLUS);
		POLICIES.put(FRAMED_IPV6_POOL, ZERO_ONE);

		// RFC  4372
		POLICIES.put(CHARGEABLE_USER_IDENTITY, ZERO_ONE);

		// RFC 4675
		POLICIES.put(EGRESS_VLANID, ZERO_PLUS);
		POLICIES.put(INGRESS_FILTERS, ZERO_ONE);
		POLICIES.put(EGRESS_VLAN_NAME, ZERO_PLUS);
		POLICIES.put(USER_PRIORITY_TABLE, ZERO);

		// RFC 4818
		POLICIES.put(DELEGATED_IPV6_PREFIX, ZERO_PLUS);

		// RFC 4849
		POLICIES.put(NAS_FILTER_RULE, ZERO_PLUS);

	}

	static {
		registerRadiusAttribute(ACCT_STATUS_TYPE, "Acct-Status-Type", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_DELAY_TIME, "Acct-Delay-Time", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_INPUT_OCTETS, "Acct-Input-Octets", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_OUTPUT_OCTETS, "Acct-Output-Octets", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_SESSION_ID, "Acct-Session-Id", DATA_TYPE_TEXT);
		registerRadiusAttribute(ACCT_AUTHENTIC, "Acct-Authentic", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_SESSION_TIME, "Acct-Session-Time", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_INPUT_PACKETS, "Acct-Input-Packets", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_OUTPUT_PACKETS, "Acct-Output-Packets", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_TERMINATE_CAUSE, "Acct-Terminate-Cause", DATA_TYPE_INTEGER);
		registerRadiusAttribute(ACCT_MULTI_SESSION_ID, "Acct-Multi-Session-Id", DATA_TYPE_TEXT);
		registerRadiusAttribute(ACCT_LINK_COUNT, "Acct-Link-Count", DATA_TYPE_INTEGER);
	}

}
