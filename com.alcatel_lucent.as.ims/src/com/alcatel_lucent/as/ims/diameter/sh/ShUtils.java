// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * Some Utils for Sh.
 */
public class ShUtils {

	private ShUtils() {}

	public final static long THREEGPP_VENDOR_ID = 10415;

	private final static Version VERSION_5_7 = new Version(5, 7);
	private final static Version VERSION_6_0 = new Version(6, 0);
	private final static Version VERSION_6_2 = new Version(6, 2);
	private final static Version VERSION_7_0 = new Version(7, 0);
	private final static Version VERSION_7_1 = new Version(7, 1);
	private final static Version VERSION_7_3 = new Version(7, 3);
	private final static Version VERSION_7_4 = new Version(7, 4);
	private final static Version VERSION_8_0 = new Version(8, 0);
	private final static Version VERSION_8_5 = new Version(8, 5);

	private final static DiameterAVPDefinition AVP_PUBLIC_IDENTITY = new DiameterAVPDefinition("Public-Identity", 601, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	private final static DiameterAVPDefinition AVP_PUBLIC_IDENTITY_V5_5 = new DiameterAVPDefinition("Public-Identity", 2, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_SERVER_NAME = new DiameterAVPDefinition("Server-Name", 602, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SERVER_NAME_V5_5 = new DiameterAVPDefinition("Server-Name", 3, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_USER_IDENTITY = new DiameterAVPDefinition("User-Identity", 700, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_USER_IDENTITY_V5_5 = new DiameterAVPDefinition("User-Identity", 100, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_MSISDN = new DiameterAVPDefinition("MSISDN", 701, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_MSISDN_V5_5 = new DiameterAVPDefinition("MSISDN", 101, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_USER_DATA = new DiameterAVPDefinition("User-Data", 702, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_USER_DATA_V5_5 = new DiameterAVPDefinition("User-Data", 102, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_DATA_REFERENCE = new DiameterAVPDefinition("Data-Reference", 703, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_DATA_REFERENCE_V5_5 = new DiameterAVPDefinition("Data-Reference", 103, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_SERVICE_INDICATION = new DiameterAVPDefinition("Service-Indication", 704, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SERVICE_INDICATION_V5_5 = new DiameterAVPDefinition("Service-Indication", 104, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_SUBS_REQUEST_TYPE = new DiameterAVPDefinition("Subs-Request-Type", 705, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SUBS_REQUEST_TYPE_V5_5 = new DiameterAVPDefinition("Subs-Request-Type", 105, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	private final static DiameterAVPDefinition REQUESTED_DOMAIN_AVPCODE = new DiameterAVPDefinition("Requested-Domain", 706, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition REQUESTED_DOMAIN_AVPCODE_V5_5 = new DiameterAVPDefinition("Requested-Domain", 106, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	private final static DiameterAVPDefinition AVP_CURRENT_LOCATION = new DiameterAVPDefinition("Current-Location", 707, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_CURRENT_LOCATION_V5_5 = new DiameterAVPDefinition("Current-Location", 107, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	/**
	 * Gets the Public-Identity AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Public-Identity AVP definition.
	 */
	public static DiameterAVPDefinition getPublicIdentityAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_PUBLIC_IDENTITY;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_PUBLIC_IDENTITY;
			}
		}

		return ShUtils.AVP_PUBLIC_IDENTITY_V5_5;
	}

	/**
	 * Gets the Server-Name AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Server-Name AVP definition.
	 */
	public static DiameterAVPDefinition getServerNameAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_SERVER_NAME;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_SERVER_NAME;
			}
		}
		return ShUtils.AVP_SERVER_NAME_V5_5;
	}

	/**
	 * Gets the User-Identity AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The User-Identity AVP definition.
	 */
	public static DiameterAVPDefinition getUserIdentityAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_USER_IDENTITY;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_USER_IDENTITY;
			}
		}

		return ShUtils.AVP_USER_IDENTITY_V5_5;
	}

	/**
	 * Gets the Msisdn AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Msisdn AVP definition.
	 */
	public static DiameterAVPDefinition getMsisdnAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_MSISDN;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_MSISDN;
			}
		}

		return ShUtils.AVP_MSISDN_V5_5;
	}

	/**
	 * Gets the User-Data AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The User-Data AVP definition.
	 */
	public static DiameterAVPDefinition getUserDataAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_USER_DATA;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_USER_DATA;
			}
		}

		return ShUtils.AVP_USER_DATA_V5_5;
	}

	/**
	 * Gets the Data-Reference AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Data-Reference AVP definition.
	 */
	public static DiameterAVPDefinition getDataReferenceAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_DATA_REFERENCE;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_DATA_REFERENCE;
			}
		}

		return ShUtils.AVP_DATA_REFERENCE_V5_5;
	}

	/**
	 * Gets the Service-Indication AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Service-Indication AVP definition.
	 */
	public static DiameterAVPDefinition getServiceIndicationAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_SERVICE_INDICATION;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_SERVICE_INDICATION;
			}
		}
		return ShUtils.AVP_SERVICE_INDICATION_V5_5;
	}

	/**
	 * Gets the Subs-Req-Type AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Subs-Req-Type AVP definition.
	 */
	public static DiameterAVPDefinition getSubsReqTypeAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_SUBS_REQUEST_TYPE;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_SUBS_REQUEST_TYPE;
			}
		}
		return ShUtils.AVP_SUBS_REQUEST_TYPE_V5_5;
	}

	/**
	 * Gets the Requested-Domain AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Requested-Domain AVP definition.
	 */
	public static DiameterAVPDefinition getRequestedDomainAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.REQUESTED_DOMAIN_AVPCODE;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.REQUESTED_DOMAIN_AVPCODE;
			}
		}
		return ShUtils.REQUESTED_DOMAIN_AVPCODE_V5_5;
	}

	/**
	 * Gets the Current-Location AVP definition according to the version.
	 * 
	 * @param version The version.
	 * @return The Current-Location AVP definition.
	 */
	public static DiameterAVPDefinition getCurrentLocationAvpDefinition(Version version) {
		if (version == null) {
			return null;
		}

		if (version.getMajor() == 5) {
			if (VERSION_5_7.compareTo(version) <= 0) {
				return ShUtils.AVP_CURRENT_LOCATION;
			}
		} else if (version.getMajor() >= 6) {
			if (VERSION_6_2.compareTo(version) <= 0) {
				return ShUtils.AVP_CURRENT_LOCATION;
			}
		}
		return ShUtils.AVP_CURRENT_LOCATION_V5_5;
	}

	private final static DiameterAVPDefinition AVP_SUPPORTED_FEATURES = new DiameterAVPDefinition("Supported-Features", 628, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Supported-Features AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
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
	 * @param version The 3GPP 29.329 document version.
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
	 * @param version The 3GPP 29.329 document version.
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
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSupportedApplicationsAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_SUPPORTED_APPLICATIONS;
		}

		return null;
	}

	private final static DiameterAVPDefinition AVP_IDENTITY_SET_V6 = new DiameterAVPDefinition("Identity-Set", 108, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);
	private final static DiameterAVPDefinition AVP_IDENTITY_SET = new DiameterAVPDefinition("Identity-Set", 708, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Identity-Set AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getIdentitySetAVP(Version version) {
		if (VERSION_6_2.compareTo(version) <= 0) {
			return AVP_IDENTITY_SET;
		} else if (VERSION_6_0.compareTo(version) <= 0) {
			return AVP_IDENTITY_SET_V6;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_EXPIRY_TIME = new DiameterAVPDefinition("Expiry-Time", 709, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Expiry-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getExpiryTimeAVP(Version version) {
		if (VERSION_7_0.compareTo(version) <= 0) {
			return AVP_EXPIRY_TIME;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_SEND_DATA_INDICATION = new DiameterAVPDefinition("Send-Data-Indication", 710, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Send-Data-Indication AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSendDataIndicationAVP(Version version) {
		if (VERSION_7_1.compareTo(version) <= 0) {
			return AVP_SEND_DATA_INDICATION;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_DSAI_TAG = new DiameterAVPDefinition("DSAI-Tag", 711, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the DSAI-Tag AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDSAITagAVP(Version version) {
		if (VERSION_7_3.compareTo(version) <= 0) {
			return AVP_DSAI_TAG;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_WILDCARDED_PSI = new DiameterAVPDefinition("Wildcarded-PSI", 634, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Wildcarded-PSI AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWildcardedPSIAVP(Version version) {
		if (VERSION_7_4.compareTo(version) <= 0) {
			return AVP_WILDCARDED_PSI;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_WILDCARDED_IMPU = new DiameterAVPDefinition("Wildcarded-IMPU", 636, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Wildcarded-IMPU AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWildcardedIMPUAVP(Version version) {
		if (VERSION_8_0.compareTo(version) <= 0) {
			return AVP_WILDCARDED_IMPU;
		}
		return null;
	}
	
	private final static DiameterAVPDefinition AVP_SESSION_PRIORITY = new DiameterAVPDefinition("Session-Priority", 650, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);
	
	/**
	 * Gets the Session-Priority AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.329 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSessionPriorityAVP(Version version) {
		if (VERSION_8_5.compareTo(version) <= 0) {
			return AVP_SESSION_PRIORITY;
		}
		return null;
	}

}
