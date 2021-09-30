package com.nextenso.proxylet.radius.acct;

import java.util.Hashtable;
import java.util.Map;

import com.nextenso.proxylet.radius.RadiusAttribute;

/**
 * The Change-Of-Authorization Utils.
 * 
 * <BR>
 * See RFC 5176
 */
public class CoAUtils
		extends AcctUtils {

	private static Map<Integer, Map<Integer, Integer>> POLICIES = new Hashtable<Integer, Map<Integer, Integer>>();

	/**
	 * Code of a CoA request (43).
	 */
	public static final int CODE_COA_REQUEST = 43;
	/**
	 * Code of a CoA ACK response (44).
	 */
	public static final int CODE_COA_ACK = 44;
	/**
	 * Code of a CoA NAK response (45).
	 */
	public static final int CODE_COA_NAK = 45;

	/**
	 * The standard radius Change-Of-Authorization port (3799).
	 */
	public static final int COA_PORT = 3799;

	/**
	 * Indicates whether the attribute is valid for this message code.
	 * 
	 * @param code The message code.
	 * @param attribute The attribute to check.
	 * @return true if the attribute is valid.
	 */
	public static final boolean isValidAttribute(int code, RadiusAttribute attribute) {
		if (attribute == null) {
			return true;
		}

		Map<Integer, Integer> policy = POLICIES.get(code);
		if (policy == null) {
			return false;
		}

		Integer i = policy.get(attribute.getType());
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

	private static final void put(int attributeType, int requestPolicy, int ackPolicy, int nakPolicy) {
		Map<Integer, Integer> policy = POLICIES.get(CODE_COA_REQUEST);
		policy.put(attributeType, requestPolicy);

		policy = POLICIES.get(CODE_COA_ACK);
		policy.put(attributeType, ackPolicy);

		policy = POLICIES.get(CODE_COA_NAK);
		policy.put(attributeType, nakPolicy);
	}

	static {
		POLICIES.put(CODE_COA_REQUEST, new Hashtable<Integer, Integer>());
		POLICIES.put(CODE_COA_ACK, new Hashtable<Integer, Integer>());
		POLICIES.put(CODE_COA_NAK, new Hashtable<Integer, Integer>());

		// RFC 5176 section3.6
		put(USER_NAME, ZERO_ONE, ZERO, ZERO);
		put(NAS_IP_ADDRESS, ZERO_ONE, ZERO, ZERO);
		put(NAS_PORT, ZERO_ONE, ZERO, ZERO);
		put(SERVICE_TYPE, ZERO_ONE, ZERO, ZERO_ONE);
		put(FRAMED_PROTOCOL, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IP_ADDRESS, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IP_NETMASK, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_ROUTING, ZERO_ONE, ZERO, ZERO);
		put(FILTER_ID, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_MTU, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_COMPRESSION, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_IP_HOST, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_SERVICE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_TCP_PORT, ZERO_ONE, ZERO, ZERO);
		put(REPLY_MESSAGE, ZERO_PLUS, ZERO, ZERO);
		put(CALLBACK_NUMBER, ZERO_ONE, ZERO, ZERO);
		put(CALLBACK_ID, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_ROUTE, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPX_NETWORK, ZERO_ONE, ZERO, ZERO);
		put(STATE, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(CLASS, ZERO_PLUS, ZERO, ZERO);
		put(VENDOR_SPECIFIC, ZERO_PLUS, ZERO, ZERO);
		put(SESSION_TIMEOUT, ZERO_ONE, ZERO, ZERO);
		put(IDLE_TIMEOUT, ZERO_ONE, ZERO, ZERO);
		put(TERMINATE_ACTION, ZERO_ONE, ZERO, ZERO);
		put(CALLED_STATION_ID, ZERO_ONE, ZERO, ZERO);
		put(CALLING_STATION_ID, ZERO_ONE, ZERO, ZERO);
		put(NAS_IDENTIFIER, ZERO_ONE, ZERO, ZERO);
		put(PROXY_STATE, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS);
		put(LOGIN_LAT_SERVICE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_NODE, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_GROUP, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_APPLETALK_LINK, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_APPLETALK_NETWORK, ZERO_PLUS, ZERO, ZERO);
		put(ACCT_SESSION_ID, ZERO_ONE, ZERO, ZERO);
		put(ACCT_MULTI_SESSION_ID, ZERO_ONE, ZERO, ZERO);
		put(EVENT_TIMESTAMP, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(EGRESS_VLANID, ZERO_PLUS, ZERO, ZERO);
		put(INGRESS_FILTERS, ZERO_ONE, ZERO, ZERO);
		put(EGRESS_VLAN_NAME, ZERO_PLUS, ZERO, ZERO);
		put(USER_PRIORITY_TABLE, ZERO_ONE, ZERO, ZERO);
		put(NAS_PORT_TYPE, ZERO_ONE, ZERO, ZERO);
		put(PORT_LIMIT, ZERO_ONE, ZERO, ZERO);
		put(LOGIN_LAT_PORT, ZERO_ONE, ZERO, ZERO);
		put(TUNNEL_TYPE, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_MEDIUM_TYPE, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_CLIENT_ENDPOINT, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_SERVER_ENDPOINT, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_PASSWORD, ZERO_PLUS, ZERO, ZERO);
		put(ARAP_FEATURES, ZERO_ONE, ZERO, ZERO);
		put(ARAP_ZONE_ACCESS, ZERO_ONE, ZERO, ZERO);
		put(CONFIGURATION_TOKEN, ZERO_PLUS, ZERO, ZERO);
		put(EAP_MESSAGE, ZERO_PLUS, ZERO_ONE, ZERO);
		put(MESSAGE_AUTHENTICATOR, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(TUNNEL_PRIVATE_GROUP_ID, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_ASSIGNMENT_ID, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_PREFERENCE, ZERO_PLUS, ZERO, ZERO);
		put(ACCT_INTERIM_INTERVAL, ZERO_ONE, ZERO, ZERO);
		put(NAS_PORT_ID, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_POOL, ZERO_ONE, ZERO, ZERO);
		put(CHARGEABLE_USER_IDENTITY, ZERO_ONE, ZERO, ZERO);
		put(TUNNEL_CLIENT_AUTH_ID, ZERO_PLUS, ZERO, ZERO);
		put(TUNNEL_SERVER_AUTH_ID, ZERO_PLUS, ZERO, ZERO);
		put(NAS_FILTER_RULE, ZERO_ONE, ZERO, ZERO);
		put(ORIGINATING_LINE_INFO, ZERO, ZERO, ZERO);
		put(NAS_IPv6_ADDRESS, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_INTERFACE_ID, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_IPV6_PREFIX, ZERO_PLUS, ZERO, ZERO);
		put(LOGIN_IPV6_HOST, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPV6_ROUTE, ZERO_PLUS, ZERO, ZERO);
		put(FRAMED_IPV6_POOL, ZERO_ONE, ZERO, ZERO);
		put(ERROR_CAUSE, ZERO, ZERO, ZERO_PLUS);
		put(DELEGATED_IPV6_PREFIX, ZERO_PLUS, ZERO, ZERO);
	}

}
