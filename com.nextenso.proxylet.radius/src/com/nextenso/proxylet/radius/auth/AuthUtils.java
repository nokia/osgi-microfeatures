package com.nextenso.proxylet.radius.auth;

import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.RadiusAttribute;

import java.util.Hashtable;
import java.util.Map;

/**
 * This class gathers useful access constants and methods.
 * 
 * <BR>
 * See RFC 2865
 */
public class AuthUtils
		extends RadiusUtils {

	private static Map<Integer, Integer> POLICIES = new Hashtable<Integer, Integer>();

	/**
	 * The Access-Request message code (1).
	 */
	public static final int CODE_ACCESS_REQUEST = 1;

	/**
	 * The Access-Accept message code (2).
	 */
	public static final int CODE_ACCESS_ACCEPT = 2;
	/**
	 * The Access-Reject message code (3).
	 */
	public static final int CODE_ACCESS_REJECT = 3;
	/**
	 * The Access-Challenge message code (11).
	 */
	public static final int CODE_ACCESS_CHALLENGE = 11;

	/**
	 * The standard radius access port (1812).
	 */
	public static final int AUTH_PORT = 1812;

	/**
	 * The old deprecated radius access port (1645).
	 */
	public static final int AUTH_PORT_DEPRECATED = 1645;

	/**
	 * Specifies if a given Radius attribute is valid in an Access request.
	 * 
	 * <BR>
	 * See RFC 2865 section 5.44 for details.
	 * 
	 * @param attribute the Radius attribute to check.
	 * @return true if valid in an Access request, false otherwise.
	 */
	public static final boolean isValidAccessRequestAttribute(RadiusAttribute attribute) {
		Integer i = POLICIES.get(attribute.getType());
		if (i == null) {
			// custom Attribute
			return true;
		}

		return isValidAttribute(attribute, (i >> 24) & 0xFF);
	}

	/**
	 * Specifies if a given Radius Attribute is valid in an Access Response.
	 * 
	 * <BR>
	 * See RFC 2865 section 5.44 for details.
	 * 
	 * @param attribute the Radius attribute to check.
	 * @return true if valid in an Access Response, false otherwise.
	 */
	public static final boolean isValidAccessResponseAttribute(RadiusAttribute attribute, int responseCode) {
		Integer i = POLICIES.get(attribute.getType());
		if (i == null) {
			// custom Attribute
			return true;
		}

		switch (responseCode) {
			case CODE_ACCESS_ACCEPT:
				return isValidAttribute(attribute, (i >> 16) & 0xFF);
			case CODE_ACCESS_REJECT:
				return isValidAttribute(attribute, (i >> 8) & 0xFF);
			case CODE_ACCESS_CHALLENGE:
				return isValidAttribute(attribute, i & 0xFF);
			default:
				throw new IllegalArgumentException("Illegal response code: " + responseCode);
		}
	}

	/** PRIVATE PART */
	private static final boolean isValidAttribute(RadiusAttribute attribute, int policy) {
		if (policy == ZERO) {
			return false;
		} else if (policy == ZERO_PLUS) {
			return true;
		} else if (policy == ZERO_ONE) {
			return (attribute.getValueSize() <= 1);
		} else if (policy == ONE) {
			return (attribute.getValueSize() == 1);
		}

		return false;
	}

	private static void put(Integer code, int req, int acc, int rej, int cha) {
		POLICIES.put(code, (req << 24) | (acc << 16) | (rej << 8) | cha);
	}

	static {
		put(USER_NAME, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(USER_PASSWORD, ZERO_ONE, ZERO, ZERO, ZERO);
		put(CHAP_PASSWORD, ZERO_ONE, ZERO, ZERO, ZERO);
		put(NAS_IP_ADDRESS, ZERO_ONE, ZERO, ZERO, ZERO);
		put(NAS_PORT, ZERO_ONE, ZERO, ZERO, ZERO);
		put(SERVICE_TYPE, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_PROTOCOL, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IP_ADDRESS, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IP_NETMASK, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_ROUTING, ZERO, ZERO_ONE, ZERO, ZERO);
		put(FILTER_ID, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_MTU, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_COMPRESSION, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_IP_HOST, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_SERVICE, ZERO, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_TCP_PORT, ZERO, ZERO_ONE, ZERO, ZERO);
		put(REPLY_MESSAGE, ZERO, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS);
		put(CALLBACK_NUMBER, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(CALLBACK_ID, ZERO, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_ROUTE, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPX_NETWORK, ZERO, ZERO_ONE, ZERO, ZERO);
		put(STATE, ZERO_ONE, ZERO_ONE, ZERO, ZERO_ONE);
		put(CLASS, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(VENDOR_SPECIFIC, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO_PLUS);
		put(SESSION_TIMEOUT, ZERO, ZERO_ONE, ZERO, ZERO_ONE);
		put(IDLE_TIMEOUT, ZERO, ZERO_ONE, ZERO, ZERO_ONE);
		put(TERMINATE_ACTION, ZERO, ZERO_ONE, ZERO, ZERO);
		put(CALLED_STATION_ID, ZERO_ONE, ZERO, ZERO, ZERO);
		put(CALLING_STATION_ID, ZERO_ONE, ZERO, ZERO, ZERO);
		put(NAS_IDENTIFIER, ZERO_ONE, ZERO, ZERO, ZERO);
		put(PROXY_STATE, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS);
		put(LOGIN_LAT_SERVICE, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_NODE, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_GROUP, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_APPLETALK_LINK, ZERO, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_APPLETALK_NETWORK, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_APPLETALK_ZONE, ZERO, ZERO_ONE, ZERO, ZERO);
		put(CHAP_CHALLENGE, ZERO_ONE, ZERO, ZERO, ZERO);
		put(NAS_PORT_TYPE, ZERO_ONE, ZERO, ZERO, ZERO);
		put(PORT_LIMIT, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_PORT, ZERO_ONE, ZERO_ONE, ZERO, ZERO);

		// RFC 2868
		put(TUNNEL_TYPE, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_MEDIUM_TYPE, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_CLIENT_ENDPOINT, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_SERVER_ENDPOINT, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_PASSWORD, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_PRIVATE_GROUP_ID, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_ASSIGNMENT_ID, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_PREFERENCE, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_CLIENT_AUTH_ID, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_SERVER_ENDPOINT, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);

		// RFC 2869
		put(ARAP_PASSWORD, ZERO_ONE, ZERO, ZERO, ZERO);
		put(ARAP_FEATURES, ZERO, ZERO_ONE, ZERO, ZERO_ONE);
		put(ARAP_ZONE_ACCESS, ZERO, ZERO_ONE, ZERO, ZERO_ONE);
		put(ARAP_SECURITY, ZERO_ONE, ZERO, ZERO, ZERO_ONE);
		put(ARAP_SECURITY_DATA, ZERO_PLUS, ZERO, ZERO, ZERO);
		put(PASSWORD_RETRY, ZERO, ZERO, ZERO_ONE, ZERO);
		put(PROMPT, ZERO, ZERO, ZERO, ZERO_ONE);
		put(CONNECT_INFO, ZERO_ONE, ZERO, ZERO, ZERO);
		put(CONFIGURATION_TOKEN, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(EAP_MESSAGE, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS);
		put(MESSAGE_AUTHENTICATOR, ZERO_ONE, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(ARAP_CHALLENGE_RESPONSE, ZERO, ZERO_ONE, ZERO, ZERO_ONE);
		put(ACCT_INTERIM_INTERVAL, ZERO, ZERO_ONE, ZERO, ZERO);
		put(NAS_PORT_ID, ZERO_ONE, ZERO, ZERO, ZERO);
		put(FRAMED_POOL, ZERO, ZERO_ONE, ZERO, ZERO);

		// RFC 3162
		put(NAS_IPv6_ADDRESS, ZERO_ONE, ZERO, ZERO, ZERO);
		put(FRAMED_INTERFACE_ID, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IPV6_PREFIX, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_IPV6_HOST, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPV6_ROUTE, ZERO, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPV6_POOL, ZERO, ZERO_ONE, ZERO, ZERO);

		// RFC  4372
		put(CHARGEABLE_USER_IDENTITY, ZERO_ONE, ZERO_ONE, ZERO, ZERO);

		// RFC 4675
		put(EGRESS_VLANID, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(INGRESS_FILTERS, ZERO_ONE, ZERO_ONE, ZERO, ZERO);
		put(EGRESS_VLAN_NAME, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);
		put(USER_PRIORITY_TABLE, ZERO, ZERO_ONE, ZERO, ZERO);

		// RFC 4818
		put(DELEGATED_IPV6_PREFIX, ZERO_PLUS, ZERO_PLUS, ZERO, ZERO);

		// RFC 4849
		put(NAS_FILTER_RULE, ZERO, ZERO_PLUS, ZERO, ZERO);
	}

}
