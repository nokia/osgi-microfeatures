package com.nextenso.proxylet.radius.acct;

import java.util.Hashtable;
import java.util.Map;

import com.nextenso.proxylet.radius.RadiusAttribute;

/**
 * The Disconnect Utils.
 * 
 * <BR>
 * See RFC 5176
 */
public class DisconnectUtils
		extends AcctUtils {

	private static Map<Integer, Map<Integer, Integer>> POLICIES = new Hashtable<Integer, Map<Integer, Integer>>();

	/**
	 * Code of a Disconnect request (40).
	 */
	public static final int CODE_DISCONNECT_REQUEST = 40;
	/**
	 * Code of a Disconnect ACK response (41).
	 */
	public static final int CODE_DISCONNECT_ACK = 41;
	/**
	 * Code of a Disconnect NAK response (42).
	 */
	public static final int CODE_DISCONNECT_NAK = 42;

	/**
	 * The standard radius Disconnect port (3799).
	 */
	public static final int DISCONNECT_PORT = 3799;

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
		Map<Integer, Integer> policy = POLICIES.get(CODE_DISCONNECT_REQUEST);
		policy.put(attributeType, requestPolicy);

		policy = POLICIES.get(CODE_DISCONNECT_ACK);
		policy.put(attributeType, ackPolicy);

		policy = POLICIES.get(CODE_DISCONNECT_NAK);
		policy.put(attributeType, nakPolicy);
	}

	static {
		POLICIES.put(CODE_DISCONNECT_REQUEST, new Hashtable<Integer, Integer>());
		POLICIES.put(CODE_DISCONNECT_ACK, new Hashtable<Integer, Integer>());
		POLICIES.put(CODE_DISCONNECT_NAK, new Hashtable<Integer, Integer>());

		// RFC 5176 section3.6
		put(USER_NAME, ZERO_ONE, ZERO, ZERO);
		put(NAS_IP_ADDRESS, ZERO_ONE, ZERO, ZERO);
		put(NAS_PORT, ZERO_ONE, ZERO, ZERO);
		put(SERVICE_TYPE, ZERO, ZERO, ZERO_ONE);
		put(FRAMED_IP_ADDRESS, ZERO, ZERO, ZERO);
		put(REPLY_MESSAGE, ZERO_PLUS, ZERO, ZERO);
		put(STATE, ZERO, ZERO, ZERO);
		put(CLASS, ZERO_PLUS, ZERO, ZERO);
		put(VENDOR_SPECIFIC, ZERO_PLUS, ZERO, ZERO);
		put(CALLED_STATION_ID, ZERO_ONE, ZERO, ZERO);
		put(CALLING_STATION_ID, ZERO_ONE, ZERO, ZERO);
		put(NAS_IDENTIFIER, ZERO_ONE, ZERO, ZERO);
		put(PROXY_STATE, ZERO_PLUS, ZERO_PLUS, ZERO_PLUS);
		put(ACCT_SESSION_ID, ZERO_ONE, ZERO, ZERO);
		put(ACCT_TERMINATE_CAUSE, ZERO_ONE, ZERO_ONE, ZERO);
		put(ACCT_MULTI_SESSION_ID, ZERO_ONE, ZERO, ZERO);
		put(EVENT_TIMESTAMP, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(NAS_PORT_TYPE, ZERO, ZERO, ZERO);
		put(EAP_MESSAGE, ZERO_PLUS, ZERO_ONE, ZERO);
		put(MESSAGE_AUTHENTICATOR, ZERO_ONE, ZERO_ONE, ZERO_ONE);
		put(NAS_PORT_ID, ZERO_ONE, ZERO, ZERO);
		put(CHARGEABLE_USER_IDENTITY, ZERO_ONE, ZERO, ZERO);
		put(NAS_IPv6_ADDRESS, ZERO_ONE, ZERO, ZERO);
		put(FRAMED_INTERFACE_ID, ZERO, ZERO, ZERO);
		put(FRAMED_IPV6_PREFIX, ZERO, ZERO, ZERO);
		put(ERROR_CAUSE, ZERO, ZERO, ZERO_PLUS);

	}

}
