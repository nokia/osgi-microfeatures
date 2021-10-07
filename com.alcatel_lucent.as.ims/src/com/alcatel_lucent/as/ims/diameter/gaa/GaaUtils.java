package com.alcatel_lucent.as.ims.diameter.gaa;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;

/**
 * The Generic Authentication Architecture (GAA) utils.
 */
public class GaaUtils {

	public final static long THREEGPP_VENDOR_ID = 10415;

	private final static Version VERSION_8_1 = new Version(8, 1);

	private final static long ZH_APPLICATION_ID = 16777221;
	private final static long ZN_APPLICATION_ID = 16777220;

	/**
	 * Gets the Zh application id.
	 * 
	 * @param version The version of the 3GPP 29.109 document
	 * @return The application id.
	 */
	public static long getZhApplicationId(Version version) {
		return ZH_APPLICATION_ID;
	}

	/**
	 * Gets the Zn application id.
	 * 
	 * @param version The version of the 3GPP 29.109 document
	 * @return The application id.
	 */
	public static long getZnApplicationId(Version version) {
		return ZN_APPLICATION_ID;
	}

	private final static DiameterAVPDefinition AVP_GBA_USERSECSETTINGS = new DiameterAVPDefinition("GBA-UserSecSettings", 400, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the GBA-UserSecSettings AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGBAUserSecSettingsAVP(Version version) {
		return AVP_GBA_USERSECSETTINGS;
	}

	private final static DiameterAVPDefinition AVP_TRANSACTION_IDENTIFIER = new DiameterAVPDefinition("Transaction-Identifier", 401, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Transaction-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTransactionIdentifierAVP(Version version) {
		return AVP_TRANSACTION_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_NAF_ID = new DiameterAVPDefinition("NAF-Id", 402, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the NAF-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNAFIdAVP(Version version) {
		return AVP_NAF_ID;
	}

	private final static DiameterAVPDefinition AVP_GAA_SERVICE_IDENTIFIER = new DiameterAVPDefinition("GAA-Service-Identifier", 403, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the GAA-Service-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGAAServiceIdentifierAVP(Version version) {
		return AVP_GAA_SERVICE_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_KEY_EXPIRY_TIME = new DiameterAVPDefinition("Key-ExpiryTime", 404, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Key-ExpiryTime AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getKeyExpiryTimeAVP(Version version) {
		return AVP_KEY_EXPIRY_TIME;
	}

	private final static DiameterAVPDefinition AVP_ME_KEY_MATERIAL = new DiameterAVPDefinition("ME-Key-Material", 405, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the ME-Key-Material AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMEKeyMaterialAVP(Version version) {
		return AVP_ME_KEY_MATERIAL;
	}

	private final static DiameterAVPDefinition AVP_UICC_KEY_MATERIAL = new DiameterAVPDefinition("UICC-Key-Material", 406, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the UICC-Key-Material AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUICCKeyMaterialAVP(Version version) {
		return AVP_UICC_KEY_MATERIAL;
	}

	private final static DiameterAVPDefinition AVP_GBA_U_AWARENESS_INDICATOR = new DiameterAVPDefinition("GBA_U-Awareness-Indicator", 407, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the GBA_U-Awareness-Indicator AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGBAUAwarenessIndicatorAVP(Version version) {
		return AVP_GBA_U_AWARENESS_INDICATOR;
	}

	private final static DiameterAVPDefinition AVP_BOOTSTRAP_INFO_CREATION_TIME = new DiameterAVPDefinition("BootstrapInfoCreationTime", 408, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the BootstrapInfoCreationTime AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getBootstrapInfoCreationTimeAVP(Version version) {
		return AVP_BOOTSTRAP_INFO_CREATION_TIME;
	}

	private final static DiameterAVPDefinition AVP_GUSS_TIMESTAMP = new DiameterAVPDefinition("GUSS-Timestamp", 409, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the GUSS-Timestamp AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGUSSTimestampAVP(Version version) {
		return AVP_GUSS_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_GBA_TYPE = new DiameterAVPDefinition("GBA-Type", 410, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the GBA-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGBATypeAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_GBA_TYPE;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_UE_ID = new DiameterAVPDefinition("UE-Id", 411, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the UE-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUEIdAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_UE_ID;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_UE_ID_TYPE = new DiameterAVPDefinition("UE-Id-Type", 412, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the UE-Id-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUEIdTypeAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_UE_ID_TYPE;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_UICC_APP_LABEL = new DiameterAVPDefinition("UICC-App-Label", 413, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the UICC-App-Label AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUICCAppLabelAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_UICC_APP_LABEL;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_UICC_ME = new DiameterAVPDefinition("UICC-ME", 414, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the UICC-ME AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUICCMEAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_UICC_ME;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_REQUESTED_KEY_LIFETIME = new DiameterAVPDefinition("Requested-Key-Lifetime", 415, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Requested-Key-Lifetime AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRequestedKeyLifetimeAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_REQUESTED_KEY_LIFETIME;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_PRIVATE_IDENTITY_REQUEST = new DiameterAVPDefinition("Private-Identity-Request", 416, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Private-Identity-Request AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPrivateIdentityRequestAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_PRIVATE_IDENTITY_REQUEST;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_GBA_PUSH_INFO = new DiameterAVPDefinition("GBA-Push-Info", 417, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the GBA-Push-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGBAPushInfoAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_GBA_PUSH_INFO;
		}
		return null;
	}

	private final static DiameterAVPDefinition AVP_NAF_SA_IDENTIFIER = new DiameterAVPDefinition("NAF-SA-Identifier", 418, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the NAF-SA-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 29.109 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNAFSAIdentifierAVP(Version version) {
		if (VERSION_8_1.compareTo(version) <= 0) {
			return AVP_NAF_SA_IDENTIFIER;
		}
		return null;
	}

}
