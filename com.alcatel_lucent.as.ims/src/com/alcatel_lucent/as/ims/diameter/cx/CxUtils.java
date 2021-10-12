// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.cx;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.URIFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * Some utils for Cx.
 */
public class CxUtils {

	public final static long THREEGPP_VENDOR_ID = 10415;
	public final static long ETSI_VENDOR_ID = 13019;
	private static final long RADIUS_VENDOR_ID = 0;

	private final static Version VERSION_6_2 = new Version(6, 2);
	private final static Version VERSION_6_6 = new Version(6, 6);
	private final static Version VERSION_7_3 = new Version(7, 3);
	private final static Version VERSION_7_4 = new Version(7, 4);
	private final static Version VERSION_7_8 = new Version(7, 8);
	private final static Version VERSION_8_0 = new Version(8, 0);
	private final static Version VERSION_8_1 = new Version(8, 1);
	private final static Version VERSION_8_3 = new Version(8, 3);
	private final static Version VERSION_8_5 = new Version(8, 5);

	private final static DiameterAVPDefinition AVP_VISITED_NETWORK_IDENTIFIER_V5 = new DiameterAVPDefinition("Visited-Network-Identifier", 1, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_VISITED_NETWORK_IDENTIFIER = new DiameterAVPDefinition("Visited-Network-Identifier", 600, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Visited-Network-Identifier AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getVisitedNetworkIdentifierAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_VISITED_NETWORK_IDENTIFIER;
		}

		return AVP_VISITED_NETWORK_IDENTIFIER_V5;
	}

	private final static DiameterAVPDefinition AVP_PUBLIC_IDENTITY_V5 = new DiameterAVPDefinition("Public-Identity", 2, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_PUBLIC_IDENTITY = new DiameterAVPDefinition("Public-Identity", 601, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Public-Identity AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPublicIdentityAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_PUBLIC_IDENTITY;
		}

		return AVP_PUBLIC_IDENTITY_V5;
	}

	private final static DiameterAVPDefinition AVP_SERVER_NAME_V5 = new DiameterAVPDefinition("Server-Name", 3, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SERVER_NAME = new DiameterAVPDefinition("Server-Name", 602, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Server-Name AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServerNameAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SERVER_NAME;
		}

		return AVP_SERVER_NAME_V5;
	}

	private final static DiameterAVPDefinition AVP_SERVER_CAPABILITIES_V5 = new DiameterAVPDefinition("Server-Capabilities", 4, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SERVER_CAPABILITIES = new DiameterAVPDefinition("Server-Capabilities", 603, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Server-Capabilities AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServerCapabilitiesAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SERVER_CAPABILITIES;
		}

		return AVP_SERVER_CAPABILITIES_V5;
	}

	private final static DiameterAVPDefinition AVP_MANDATORY_CAPABILITY_V5 = new DiameterAVPDefinition("Mandatory-Capability", 5, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_MANDATORY_CAPABILITY = new DiameterAVPDefinition("Mandatory-Capability", 604, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Mandatory-Capability AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMandatoryCapabilityAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_MANDATORY_CAPABILITY;
		}

		return AVP_MANDATORY_CAPABILITY_V5;
	}

	private final static DiameterAVPDefinition AVP_OPTIONAL_CAPABILITY_V5 = new DiameterAVPDefinition("Optional-Capability", 6, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_OPTIONAL_CAPABILITY = new DiameterAVPDefinition("Optional-Capability", 605, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Optional-Capability AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOptionalCapabilityAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_OPTIONAL_CAPABILITY;
		}

		return AVP_OPTIONAL_CAPABILITY_V5;
	}

	private final static DiameterAVPDefinition AVP_USER_DATA_V5 = new DiameterAVPDefinition("User-Data", 7, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_USER_DATA = new DiameterAVPDefinition("User-Data", 606, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the User-Data AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserDataAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_USER_DATA;
		}

		return AVP_USER_DATA_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_NUMBER_AUTH_ITEMS_V5 = new DiameterAVPDefinition("Sip-Number-Auth-Items", 8, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_NUMBER_AUTH_ITEMS = new DiameterAVPDefinition("Sip-Number-Auth-Items", 607, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Sip-Number-Auth-Items AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipNumberAuthItemsAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_NUMBER_AUTH_ITEMS;
		}

		return AVP_SIP_NUMBER_AUTH_ITEMS_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATION_SCHEME_V5 = new DiameterAVPDefinition("Sip-Authentication-Scheme", 9, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATION_SCHEME = new DiameterAVPDefinition("Sip-Authentication-Scheme", 608, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Sip-Authentication-Scheme AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipAuthenticationSchemeAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_AUTHENTICATION_SCHEME;
		}

		return AVP_SIP_AUTHENTICATION_SCHEME_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATE_V5 = new DiameterAVPDefinition("Sip-Authenticate", 10, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATE = new DiameterAVPDefinition("Sip-Authenticate", 609, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Sip-Authenticate AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipAuthenticateAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_AUTHENTICATE;
		}

		return AVP_SIP_AUTHENTICATE_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_AUTHORIZATION_V5 = new DiameterAVPDefinition("Sip-Authorization", 11, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_AUTHORIZATION = new DiameterAVPDefinition("Sip-Authorization", 610, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Sip-Authorization AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipAuthorizationAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_AUTHORIZATION;
		}

		return AVP_SIP_AUTHORIZATION_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATION_CONTEXT_V5 = new DiameterAVPDefinition("Sip-Authentication-Context", 12, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_AUTHENTICATION_CONTEXT = new DiameterAVPDefinition("Sip-Authentication-Context", 611, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Sip-Authentication-Context AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipAuthenticationContextAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_AUTHENTICATION_CONTEXT;
		}

		return AVP_SIP_AUTHENTICATION_CONTEXT_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_AUTH_DATA_ITEM_V5 = new DiameterAVPDefinition("Sip-Auth-Data-Item", 13, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_AUTH_DATA_ITEM = new DiameterAVPDefinition("Sip-Auth-Data-Item", 612, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Sip-Auth-Data-Item AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipAuthDataItemAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_AUTH_DATA_ITEM;
		}

		return AVP_SIP_AUTH_DATA_ITEM_V5;
	}

	private final static DiameterAVPDefinition AVP_SIP_ITEM_NUMBER_V5 = new DiameterAVPDefinition("Sip-Item-Number", 14, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SIP_ITEM_NUMBER = new DiameterAVPDefinition("Sip-Item-Number", 613, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Sip-Item-Number AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipItemNumberAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SIP_ITEM_NUMBER;
		}

		return AVP_SIP_ITEM_NUMBER_V5;
	}

	private final static DiameterAVPDefinition AVP_SERVER_ASSIGNMENT_TYPE_V5 = new DiameterAVPDefinition("Server-Assignment-Type", 15, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SERVER_ASSIGNMENT_TYPE = new DiameterAVPDefinition("Server-Assignment-Type", 614, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Server-Assignment-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServerAssignmentTypeAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SERVER_ASSIGNMENT_TYPE;
		}

		return AVP_SERVER_ASSIGNMENT_TYPE_V5;
	}

	private final static DiameterAVPDefinition AVP_DEREGISTRATION_REASON_V5 = new DiameterAVPDefinition("Deregistration-Reason", 16, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_DEREGISTRATION_REASON = new DiameterAVPDefinition("Deregistration-Reason", 615, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Deregistration-Reason AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDeregistrationReasonAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_DEREGISTRATION_REASON;
		}

		return AVP_DEREGISTRATION_REASON_V5;
	}

	private final static DiameterAVPDefinition AVP_REASON_CODE_V5 = new DiameterAVPDefinition("Reason-Code", 17, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_REASON_CODE = new DiameterAVPDefinition("Reason-Code", 616, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Reason-Code AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReasonCodeAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_REASON_CODE;
		}

		return AVP_REASON_CODE_V5;
	}

	private final static DiameterAVPDefinition AVP_REASON_INFO_V5 = new DiameterAVPDefinition("Reason-Info", 18, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_REASON_INFO = new DiameterAVPDefinition("Reason-Info", 617, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Reason-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReasonInfoAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_REASON_INFO;
		}

		return AVP_REASON_INFO_V5;
	}

	private final static DiameterAVPDefinition AVP_CHARGING_INFORMATION_V5 = new DiameterAVPDefinition("Charging-Information", 19, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_CHARGING_INFORMATION = new DiameterAVPDefinition("Charging-Information", 618, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Charging-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getChargingInformationAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_CHARGING_INFORMATION;
		}

		return AVP_CHARGING_INFORMATION_V5;
	}

	private final static DiameterAVPDefinition AVP_PRIMARY_EVENT_CHARGING_FUNCTION_NAME_V5 = new DiameterAVPDefinition("Primary-Event-Charging-Function-Name", 20, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_PRIMARY_EVENT_CHARGING_FUNCTION_NAME = new DiameterAVPDefinition("Primary-Event-Charging-Function-Name", 619, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, true);

	/**
	 * Gets the Primary-Event-Charging-Function-Name AVP definition according to
	 * the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPrimaryEventChargingFunctionNameAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_PRIMARY_EVENT_CHARGING_FUNCTION_NAME;
		}

		return AVP_PRIMARY_EVENT_CHARGING_FUNCTION_NAME_V5;
	}

	private final static DiameterAVPDefinition AVP_SECONDARY_EVENT_CHARGING_FUNCTION_NAME_V5 = new DiameterAVPDefinition("Secondary-Event-Charging-Function-Name", 21, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SECONDARY_EVENT_CHARGING_FUNCTION_NAME = new DiameterAVPDefinition("Secondary-Event-Charging-Function-Name", 620, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, true);

	/**
	 * Gets the Secondary-Event-Charging-Function-Name AVP definition according to
	 * the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSecondaryEventChargingFunctionNameAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SECONDARY_EVENT_CHARGING_FUNCTION_NAME;
		}

		return AVP_SECONDARY_EVENT_CHARGING_FUNCTION_NAME_V5;
	}

	private final static DiameterAVPDefinition AVP_PRIMARY_CHARGING_COLLECTION_FUNCTION_NAME_V5 = new DiameterAVPDefinition("Primary-Charging-Collection-Function-Name", 22, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_PRIMARY_CHARGING_COLLECTION_FUNCTION_NAME = new DiameterAVPDefinition("Primary-Charging-Collection-Function-Name", 621, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, true);

	/**
	 * Gets the Primary-Charging-Collection-Function-Name AVP definition according
	 * to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPrimaryChargingFunctionNameAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_PRIMARY_CHARGING_COLLECTION_FUNCTION_NAME;
		}

		return AVP_PRIMARY_CHARGING_COLLECTION_FUNCTION_NAME_V5;
	}

	private final static DiameterAVPDefinition AVP_SECONDARY_CHARGING_COLLECTION_FUNCTION_NAME_V5 = new DiameterAVPDefinition("Secondary-Charging-Collection-Function-Name", 23, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_SECONDARY_CHARGING_COLLECTION_FUNCTION_NAME = new DiameterAVPDefinition("Secondary-Charging-Collection-Function-Name", 622, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, URIFormat.INSTANCE, true);

	/**
	 * Gets the Secondary-Charging-Collection-Function-Name AVP definition
	 * according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSecondaryChargingFunctionNameAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SECONDARY_CHARGING_COLLECTION_FUNCTION_NAME;
		}

		return AVP_SECONDARY_CHARGING_COLLECTION_FUNCTION_NAME_V5;
	}

	private final static DiameterAVPDefinition AVP_USER_AUTHORIZATION_TYPE_V5 = new DiameterAVPDefinition("User-Authorization-Type", 24, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_USER_AUTHORIZATION_TYPE = new DiameterAVPDefinition("User-Authorization-Type", 623, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the User-Authorization-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserAuthorizationTypeAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_USER_AUTHORIZATION_TYPE;
		}

		return AVP_USER_AUTHORIZATION_TYPE_V5;
	}

	private final static DiameterAVPDefinition AVP_USER_DATA_REQUEST_TYPE_V5 = new DiameterAVPDefinition("User-Data-Request-Type", 25, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	/**
	 * Gets the User-Data-Request-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserDataRequestTypeAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return null;
		}

		return AVP_USER_DATA_REQUEST_TYPE_V5;
	}

	private final static DiameterAVPDefinition AVP_USER_DATA_ALREADY_AVAILABLE_V5 = new DiameterAVPDefinition("User-Data-Already-Available", 26, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_USER_DATA_ALREADY_AVAILABLE = new DiameterAVPDefinition("User-Data-Already-Available", 624, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the User-Data-Already-Available AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserDataAlreadyAvailableAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_USER_DATA_ALREADY_AVAILABLE;
		}

		return AVP_USER_DATA_ALREADY_AVAILABLE_V5;
	}

	private final static DiameterAVPDefinition AVP_CONFIDENTIALITY_KEY_V5 = new DiameterAVPDefinition("Confidentiality-Key", 27, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_CONFIDENTIALITY_KEY = new DiameterAVPDefinition("Confidentiality-Key", 625, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Confidentiality-Key AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getConfidentialityKeyAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_CONFIDENTIALITY_KEY;
		}

		return AVP_CONFIDENTIALITY_KEY_V5;
	}

	private final static DiameterAVPDefinition AVP_INTEGRITY_KEY_V5 = new DiameterAVPDefinition("Integrity-Key", 28, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_INTEGRITY_KEY = new DiameterAVPDefinition("Integrity-Key", 626, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Integrity-Key AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getIntegrityKeyAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_INTEGRITY_KEY;
		}

		return AVP_INTEGRITY_KEY_V5;
	}

	//	private final static DiameterAVPDefinition AVP_UNKNOWN = new DiameterAVPDefinition("???", 627, THREEGPP_VENDOR_ID,
	//			DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false,
	//			GroupedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SUPPORTED_FEATURES = new DiameterAVPDefinition("Supported-Features", 628, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Supported-Features AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSupportedFeaturesAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SUPPORTED_FEATURES;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_FEATURE_LIST_ID = new DiameterAVPDefinition("Feature-List-ID", 629, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Feature-List-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFeatureListIDAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_FEATURE_LIST_ID;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_FEATURE_LIST = new DiameterAVPDefinition("Feature-List", 630, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Feature-List AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFeatureListAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_FEATURE_LIST;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_SUPPORTED_APPLICATIONS = new DiameterAVPDefinition("Supported-Applications", 631, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Supported-Applications AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSupportedApplicationsAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SUPPORTED_APPLICATIONS;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_ASSOCIATED_IDENTITIES = new DiameterAVPDefinition("Associated-Identities", 632, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Associated-Identities AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAssociatedIdentitiesAVP(Version version) {
		if (VERSION_6_6.compareTo(version) <= 0) {
			return AVP_ASSOCIATED_IDENTITIES;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATING_REQUEST = new DiameterAVPDefinition("Originating-Request", 633, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Originating-Request AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatingRequestAVP(Version version) {
		if (VERSION_7_3.compareTo(version) <= 0) {
			return AVP_ORIGINATING_REQUEST;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_WILDCARDED_PSI = new DiameterAVPDefinition("Wildcarded-PSI", 634, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Wildcarded-PSI AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWildcardedPSIAVP(Version version) {
		if (VERSION_7_4.compareTo(version) <= 0) {
			return AVP_WILDCARDED_PSI;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_SIP_DIGEST_AUTHENTICATE = new DiameterAVPDefinition("SIP-Digest-Authenticate", 635, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the SIP-Digest-Authenticate AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSIPDigestAuthenticateAVP(Version version) {
		if (VERSION_8_0.compareTo(version) <= 0) {
			return AVP_SIP_DIGEST_AUTHENTICATE;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_DIGEST_REALM = new DiameterAVPDefinition("Digest-Realm", 104, RADIUS_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Digest-Realm AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDigestRealmAVP(Version version) {
		return AVP_DIGEST_REALM;
	}

	private final static DiameterAVPDefinition AVP_DIGEST_ALGORITHM = new DiameterAVPDefinition("Digest-Algorithm", 111, RADIUS_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Digest-Algorithm AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDigestAlgorithmAVP(Version version) {
		return AVP_DIGEST_ALGORITHM;
	}

	private final static DiameterAVPDefinition AVP_DIGEST_QOP = new DiameterAVPDefinition("Digest-Qop", 110, RADIUS_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Digest-Qop AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDigestQoPAVP(Version version) {
		return AVP_DIGEST_QOP;
	}

	private final static DiameterAVPDefinition AVP_DIGEST_HA1 = new DiameterAVPDefinition("Digest-HA1", 121, RADIUS_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Digest-HA1 AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDigestHA1AVP(Version version) {
		return AVP_DIGEST_HA1;
	}

	private final static DiameterAVPDefinition AVP_WILDCARDED_IMPU = new DiameterAVPDefinition("Wildcarded-IMPU", 636, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Wildcarded-IMPU AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWildcardedIMPUAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_WILDCARDED_IMPU;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_UAR_FLAGS = new DiameterAVPDefinition("UAR-Flags", 637, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the UAR-Flags AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUARFlagsAVP(Version version) {
		if (VERSION_7_8.compareTo(version) <= 0) {
			return AVP_UAR_FLAGS;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_LOOSE_ROUTE_INDICATION = new DiameterAVPDefinition("Loose-Route-Indication", 638, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Loose-Route-Indication AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLooseRouteIndicationAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_LOOSE_ROUTE_INDICATION;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_SCSCF_RESTORATION_INFO = new DiameterAVPDefinition("SCSCF-Restauration-Info", 639, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the SCSCF-Restauration-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSCSCFRestaurationInfoAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_SCSCF_RESTORATION_INFO;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_PATH = new DiameterAVPDefinition("Path", 640, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Path AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPathAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_PATH;
		}

		return null;
	}
	private final static DiameterAVPDefinition AVP_CONTACT = new DiameterAVPDefinition("Contact", 641, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Contact AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContactAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_CONTACT;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_SUBSCRIPTION_INFO = new DiameterAVPDefinition("Subscription-Info", 642, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Subscription-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSubscriptionInfoAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_SUBSCRIPTION_INFO;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_CALL_ID_SIP_HEADER = new DiameterAVPDefinition("Call-ID-SIP-Header", 643, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Call-ID-SIP-Header AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCallIDSIPHeaderAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_CALL_ID_SIP_HEADER;
		}

		return null;
	}
	private final static DiameterAVPDefinition AVP_FROM_SIP_HEADER = new DiameterAVPDefinition("From-SIP-Header", 644, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the From-SIP-Header AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFromSIPHeaderAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_FROM_SIP_HEADER;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_TO_SIP_HEADER = new DiameterAVPDefinition("To-SIP-Header", 645, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the To-SIP-Header AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getToSIPHeaderAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_TO_SIP_HEADER;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_RECORD_ROUTE = new DiameterAVPDefinition("Record-Route", 646, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Record-Route AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRecordRoutevAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_RECORD_ROUTE;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_ASSOCIATED_REGISTERED_IDENTITIES = new DiameterAVPDefinition("Associated-Registered-Identities", 647, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Associated-Registered-Identities AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAssociatedRegisteredIdentitiesAVP(Version version) {
		if (VERSION_8_3.compareTo(version) <= 0) {
			return AVP_ASSOCIATED_REGISTERED_IDENTITIES;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_MULTIPLE_REGISTRATION_INDICATION = new DiameterAVPDefinition("Multiple-Registration-Indication", 648, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Multiple-Registration-Indication AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMultipleRegistrationIndicationAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_MULTIPLE_REGISTRATION_INDICATION;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_RESTORATION_INFO = new DiameterAVPDefinition("Restoration-Info", 649, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Restoration-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRestorationInfoAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_RESTORATION_INFO;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_LINE_IDENTIFIER = new DiameterAVPDefinition("Line-Identifier", 500, ETSI_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Line-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLineIdentifierAVP(Version version) {
		if (VERSION_8_0.compareTo(version) <= 0) {
			return AVP_LINE_IDENTIFIER;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_FRAMED_IP_ADDRESS = new DiameterAVPDefinition("Framed-IP-Address", 8, ETSI_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Framed-IP-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFramedIPAddressAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_FRAMED_IP_ADDRESS;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_FRAMED_IPV6_PREFIX = new DiameterAVPDefinition("Framed-IPv6-Prefix", 97, ETSI_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Framed-IPv6-Prefix AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFramedIPv6PrefixAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_FRAMED_IPV6_PREFIX;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_FRAMED_INTERFACE_ID = new DiameterAVPDefinition("Framed-Interface-Id", 96, ETSI_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the Framed-Interface-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.229 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFramedInterfaceIdAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_FRAMED_INTERFACE_ID;
		}

		return null;
	}

}
