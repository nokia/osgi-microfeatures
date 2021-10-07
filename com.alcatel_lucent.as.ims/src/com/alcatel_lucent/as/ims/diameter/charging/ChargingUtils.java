package com.alcatel_lucent.as.ims.diameter.charging;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.Integer64Format;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * The Charging Utils class.
 */
public class ChargingUtils {

	private final static Version VERSION_6_1 = new Version(6, 1);
	private final static Version VERSION_6_2 = new Version(6, 2);
	private final static Version VERSION_6_3 = new Version(6, 3);
	private final static Version VERSION_6_4 = new Version(6, 4);
	private final static Version VERSION_6_5 = new Version(6, 5);
	private final static Version VERSION_6_6 = new Version(6, 6);
	private final static Version VERSION_6_7 = new Version(6, 7);
	private final static Version VERSION_6_8 = new Version(6, 8);
	private final static Version VERSION_6_9 = new Version(6, 9);
	private final static Version VERSION_6_10 = new Version(6, 10);
	private final static Version VERSION_6_11 = new Version(6, 11);
	private final static Version VERSION_6_12 = new Version(6, 12);

	private final static Version VERSION_7_0 = new Version(7, 0);
	private final static Version VERSION_7_1 = new Version(7, 1);
	private final static Version VERSION_7_2 = new Version(7, 2);
	private final static Version VERSION_7_3 = new Version(7, 3);
	private final static Version VERSION_7_4 = new Version(7, 4);
	private final static Version VERSION_7_5 = new Version(7, 5);
	private final static Version VERSION_7_6 = new Version(7, 8);
	private final static Version VERSION_7_7 = new Version(7, 7);

	private final static Version VERSION_8_0 = new Version(8, 0);
	private final static Version VERSION_8_1 = new Version(8, 1);
	private final static Version VERSION_8_2 = new Version(8, 2);
	private final static Version VERSION_8_3 = new Version(8, 3);
	private final static Version VERSION_8_4 = new Version(8, 4);
	private final static Version VERSION_8_5 = new Version(8, 5);
	private final static Version VERSION_8_6 = new Version(8, 6);
	private final static Version VERSION_8_7 = new Version(8, 7);
	private final static Version VERSION_8_8 = new Version(8, 8);
	private final static Version VERSION_8_9 = new Version(8, 9);
	private final static Version VERSION_8_10 = new Version(8, 10);
	private final static Version VERSION_8_11 = new Version(8, 11);

	private final static Version VERSION_9_0 = new Version(9, 0);
	private final static Version VERSION_9_1 = new Version(9, 1);
	private final static Version VERSION_9_2 = new Version(9, 2);
	private final static Version VERSION_9_3 = new Version(9, 3);
	private final static Version VERSION_9_4 = new Version(9, 4);

	private final static long THREEGPP_VENDOR_ID = 10415;
	private final static long THREEGPP2_VENDOR_ID = 5135;

	private static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private static Map<Version, Version> TABLE_29_229 = new HashMap<Version, Version>(40);
	static {
		TABLE_29_229.put(VERSION_6_1, VERSION_6_3);
		TABLE_29_229.put(VERSION_6_2, VERSION_6_4);
		TABLE_29_229.put(VERSION_6_3, VERSION_6_5);
		TABLE_29_229.put(VERSION_6_4, VERSION_6_6);
		TABLE_29_229.put(VERSION_6_5, VERSION_6_7);
		TABLE_29_229.put(VERSION_6_6, VERSION_6_7);
		TABLE_29_229.put(VERSION_6_7, VERSION_6_8);
		TABLE_29_229.put(VERSION_6_8, VERSION_6_9);
		TABLE_29_229.put(VERSION_6_9, VERSION_6_9);
		TABLE_29_229.put(VERSION_6_10, VERSION_6_9);
		TABLE_29_229.put(VERSION_6_11, VERSION_6_9);
		TABLE_29_229.put(VERSION_6_12, VERSION_6_10);

		TABLE_29_229.put(VERSION_7_0, VERSION_7_0);
		TABLE_29_229.put(VERSION_7_1, VERSION_7_1);
		TABLE_29_229.put(VERSION_7_2, VERSION_7_2);
		TABLE_29_229.put(VERSION_7_3, VERSION_7_3);
		TABLE_29_229.put(VERSION_7_4, VERSION_7_4);
		TABLE_29_229.put(VERSION_7_5, VERSION_7_5);
		TABLE_29_229.put(VERSION_7_6, VERSION_7_5);
		TABLE_29_229.put(VERSION_7_7, VERSION_7_6);

		TABLE_29_229.put(VERSION_8_0, VERSION_8_0);
		TABLE_29_229.put(VERSION_8_1, VERSION_8_0);
		TABLE_29_229.put(VERSION_8_2, VERSION_8_1);
		TABLE_29_229.put(VERSION_8_3, VERSION_8_2);
		TABLE_29_229.put(VERSION_8_4, VERSION_8_3);
		TABLE_29_229.put(VERSION_8_5, VERSION_8_4);
		TABLE_29_229.put(VERSION_8_6, VERSION_8_5);
		TABLE_29_229.put(VERSION_8_7, VERSION_8_6);
		TABLE_29_229.put(VERSION_8_8, VERSION_8_7);
		TABLE_29_229.put(VERSION_8_9, VERSION_8_8);
		TABLE_29_229.put(VERSION_8_10, VERSION_8_9);
		TABLE_29_229.put(VERSION_8_11, VERSION_8_10);

		TABLE_29_229.put(VERSION_9_0, VERSION_9_0);
		TABLE_29_229.put(VERSION_9_1, VERSION_9_0);
		TABLE_29_229.put(VERSION_9_2, VERSION_9_0);
		TABLE_29_229.put(VERSION_9_3, VERSION_9_1);
		TABLE_29_229.put(VERSION_9_4, VERSION_9_2);
	}

	private static Map<Version, Version> TABLE_29_329 = new HashMap<Version, Version>(40);
	static {
		TABLE_29_329.put(VERSION_6_1, VERSION_6_3);
		TABLE_29_329.put(VERSION_6_2, VERSION_6_4);
		TABLE_29_329.put(VERSION_6_3, VERSION_6_5);
		TABLE_29_329.put(VERSION_6_4, VERSION_6_6);
		TABLE_29_329.put(VERSION_6_5, VERSION_6_6);
		TABLE_29_329.put(VERSION_6_6, VERSION_6_6);
		TABLE_29_329.put(VERSION_6_7, VERSION_6_6);
		TABLE_29_329.put(VERSION_6_8, VERSION_6_7);
		TABLE_29_329.put(VERSION_6_9, VERSION_6_7);
		TABLE_29_329.put(VERSION_6_10, VERSION_6_7);
		TABLE_29_329.put(VERSION_6_11, VERSION_6_7);
		TABLE_29_329.put(VERSION_6_12, VERSION_6_7);

		TABLE_29_329.put(VERSION_7_0, VERSION_7_0);
		TABLE_29_329.put(VERSION_7_1, VERSION_7_1);
		TABLE_29_329.put(VERSION_7_2, VERSION_7_2);
		TABLE_29_329.put(VERSION_7_3, VERSION_7_3);
		TABLE_29_329.put(VERSION_7_4, VERSION_7_3);
		TABLE_29_329.put(VERSION_7_5, VERSION_7_3);
		TABLE_29_329.put(VERSION_7_6, VERSION_7_3);
		TABLE_29_329.put(VERSION_7_7, VERSION_7_4);

		TABLE_29_329.put(VERSION_8_0, VERSION_8_0);
		TABLE_29_329.put(VERSION_8_1, VERSION_8_0);
		TABLE_29_329.put(VERSION_8_2, VERSION_8_0);
		TABLE_29_329.put(VERSION_8_3, VERSION_8_1);
		TABLE_29_329.put(VERSION_8_4, VERSION_8_1);
		TABLE_29_329.put(VERSION_8_5, VERSION_8_2);
		TABLE_29_329.put(VERSION_8_6, VERSION_8_3);
		TABLE_29_329.put(VERSION_8_7, VERSION_8_4);
		TABLE_29_329.put(VERSION_8_8, VERSION_8_4);
		TABLE_29_329.put(VERSION_8_9, VERSION_8_5);
		TABLE_29_329.put(VERSION_8_10, VERSION_8_6);
		TABLE_29_329.put(VERSION_8_11, VERSION_8_6);

		TABLE_29_329.put(VERSION_9_0, VERSION_9_0);
		TABLE_29_329.put(VERSION_9_1, VERSION_9_0);
		TABLE_29_329.put(VERSION_9_2, VERSION_9_0);
		TABLE_29_329.put(VERSION_9_3, VERSION_9_1);
		TABLE_29_329.put(VERSION_9_4, VERSION_9_2);
	}

	//private final static long ETSI_VENDOR_ID = 13019;

	/**
	 * Gets the 29.229 version according to the 32.299 version.
	 * 
	 * @param version The 32.299 version.
	 * @return The associated 29.229 version
	 */
	public static Version getVersion29229(Version version) {
		if (version == null) {
			return null;
		}

		return TABLE_29_229.get(version);
	}

	/**
	 * Gets the 29.329 version according to the 32.299 version.
	 * 
	 * @param version The 32.299 version.
	 * @return The associated 29.329 version
	 */
	public static Version getVersion29329(Version version) {
		if (version == null) {
			return null;
		}

		return TABLE_29_329.get(version);
	}

	private final static DiameterAVPDefinition AVP_ACCOUNTING_INPUT_OCTETS = new DiameterAVPDefinition("Accounting-Input-Octets", 363, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the Accounting-Input-Octets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getAccountingInputOctetsAVP() {
		return AVP_ACCOUNTING_INPUT_OCTETS;
	}

	private final static DiameterAVPDefinition AVP_ACCOUNTING_INPUT_PACKETS = new DiameterAVPDefinition("Accounting-Input-Packets", 365, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the Accounting-Input-Packets AVP definition according to the version.
	 * 
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAccountingInputPacketsAVP() {
		return AVP_ACCOUNTING_INPUT_PACKETS;
	}

	private final static DiameterAVPDefinition AVP_ACCOUNTING_OUTPUT_OCTETS = new DiameterAVPDefinition("Accounting-Output-Octets", 364, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the Accounting-Output-Octets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getAccountingOutputOctetsAVP() {
		return AVP_ACCOUNTING_OUTPUT_OCTETS;
	}

	private final static DiameterAVPDefinition AVP_ACCOUNTING_OUTPUT_PACKETS = new DiameterAVPDefinition("Accounting-Output-Packets", 366, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the Accounting-Output-Packets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getAccountingOutputPacketsAVP() {
		return AVP_ACCOUNTING_OUTPUT_PACKETS;
	}

	private final static DiameterAVPDefinition AVP_CALLED_STATION_ID = new DiameterAVPDefinition("Called-Station-Id", 30, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Called-Station-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCalledStationIdAVP() {
		return AVP_CALLED_STATION_ID;
	}

	private final static DiameterAVPDefinition AVP_CC_CORRELATION_ID = new DiameterAVPDefinition("CC-Correlation-Id", 411, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the CC-Correlation-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcCorrelationIdAVP() {
		return AVP_CC_CORRELATION_ID;
	}

	private final static DiameterAVPDefinition AVP_CC_INPUT_OCTETS = new DiameterAVPDefinition("CC-Input-Octets", 412, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the CC-Input-Octets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcInputOctetsAVP() {
		return AVP_CC_INPUT_OCTETS;
	}

	private final static DiameterAVPDefinition AVP_CC_MONEY = new DiameterAVPDefinition("CC-Money", 413, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the CC-Money AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcMoneyAVP() {
		return AVP_CC_MONEY;
	}

	private final static DiameterAVPDefinition AVP_CC_OUTPUT_OCTETS = new DiameterAVPDefinition("CC-Output-Octets", 414, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the CC-Output-Octets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcOutputOctetsAVP() {
		return AVP_CC_OUTPUT_OCTETS;
	}

	private final static DiameterAVPDefinition AVP_CC_REQUEST_NUMBER = new DiameterAVPDefinition("CC-Request-Number", 415, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the CC-Request-Number AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcRequestNumberAVP() {
		return AVP_CC_REQUEST_NUMBER;
	}

	private final static DiameterAVPDefinition AVP_CC_REQUEST_TYPE = new DiameterAVPDefinition("CC-Request-Type", 416, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the CC-Request-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcRequestTypeAVP() {
		return AVP_CC_REQUEST_TYPE;
	}

	private final static DiameterAVPDefinition AVP_CC_SERVICE_SPECIFIC_UNITS = new DiameterAVPDefinition("CC-Service-Specific-Units", 417, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the CC-Service-Specific-Units AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcServiceSpecificUnitsAVP() {
		return AVP_CC_SERVICE_SPECIFIC_UNITS;
	}

	private final static DiameterAVPDefinition AVP_CC_SESSION_FAILOVER = new DiameterAVPDefinition("CC-Session-Failover", 418, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the CC-Session-Failover AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcSessionFailoverAVP() {
		return AVP_CC_SESSION_FAILOVER;
	}

	private final static DiameterAVPDefinition AVP_CC_SUB_SESSION_ID = new DiameterAVPDefinition("CC-Sub-Session-Id", 419, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the CC-Sub-Session-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcSubSessionIdAVP() {
		return AVP_CC_SUB_SESSION_ID;
	}

	private final static DiameterAVPDefinition AVP_CC_TIME = new DiameterAVPDefinition("CC-Time", 420, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the CC-Time AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcTimeAVP() {
		return AVP_CC_TIME;
	}

	private final static DiameterAVPDefinition AVP_CC_TOTAL_OCTETS = new DiameterAVPDefinition("CC-Total-Octets", 421, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned64Format.INSTANCE, true);

	/**
	 * Gets the CC-Total-Octets AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcTotalOctetsAVP() {
		return AVP_CC_TOTAL_OCTETS;
	}

	private final static DiameterAVPDefinition AVP_CC_UNIT_TYPE = new DiameterAVPDefinition("CC-Unit-Type", 454, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the CC-Unit-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCcUnitTypeAVP() {
		return AVP_CC_UNIT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_CHECK_BALANCE_RESULT = new DiameterAVPDefinition("Check-Balance-Result", 422, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Check-Balance-Result AVP definition.
	 * 
	 * @return The AVP definition. .
	 */
	public final static DiameterAVPDefinition getCheckBalanceResultAVP() {
		return AVP_CHECK_BALANCE_RESULT;
	}

	private final static DiameterAVPDefinition AVP_COST_INFORMATION = new DiameterAVPDefinition("Cost-Information", 423, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Cost-Information AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCostInformationAVP() {
		return AVP_COST_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_COST_UNIT = new DiameterAVPDefinition("Cost-Unit", 424, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Cost-Unit AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCostUnitAVP() {
		return AVP_COST_UNIT;
	}

	private final static DiameterAVPDefinition AVP_CREDIT_CONTROL = new DiameterAVPDefinition("Credit-Control", 426, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Credit-Control AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCreditControlAVP() {
		return AVP_CREDIT_CONTROL;
	}

	private final static DiameterAVPDefinition AVP_CREDIT_CONTROL_FAILURE_HANDLING = new DiameterAVPDefinition("Credit-Control-Failure-Handling", 427, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Credit-Control-Failure-Handling AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCreditControlFailureHandlingAVP() {
		return AVP_CREDIT_CONTROL_FAILURE_HANDLING;
	}

	private final static DiameterAVPDefinition AVP_CURRENCY_CODE = new DiameterAVPDefinition("Currency-Code", 425, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Currency-Code AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getCurrencyCodeAVP() {
		return AVP_CURRENCY_CODE;
	}

	private final static DiameterAVPDefinition AVP_DIRECT_DEBITING_FAILURE_HANDLING = new DiameterAVPDefinition("Direct-Debiting-Failure-Handling", 428, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Direct-Debiting-Failure-Handling AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getDirectDebitingFailureHandlingAVP() {
		return AVP_DIRECT_DEBITING_FAILURE_HANDLING;
	}

	private final static DiameterAVPDefinition AVP_EXPONENT = new DiameterAVPDefinition("Exponent", 429, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer32Format.INSTANCE, true);

	/**
	 * Gets the Exponent AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getExponentAVP() {
		return AVP_EXPONENT;
	}

	private final static DiameterAVPDefinition AVP_FILTER_ID = new DiameterAVPDefinition("Filter-Id", 11, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Filter-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getFilterIdAVP() {
		return AVP_FILTER_ID;
	}

	private final static DiameterAVPDefinition AVP_FINAL_UNIT_ACTION = new DiameterAVPDefinition("Final-Unit-Action", 449, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Final-Unit-Action AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getFinalUnitActionAVP() {
		return AVP_FINAL_UNIT_ACTION;
	}

	private final static DiameterAVPDefinition AVP_FINAL_UNIT_INDICATION = new DiameterAVPDefinition("Final-Unit-Indication", 430, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Final-Unit-Indication AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getFinalUnitIndicationAVP() {
		return AVP_FINAL_UNIT_INDICATION;
	}

	private final static DiameterAVPDefinition AVP_GRANTED_SERVICE_UNIT = new DiameterAVPDefinition("Granted-Service-Unit", 431, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Granted-Service-Unit AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getGrantedServiceUnitAVP() {
		return AVP_GRANTED_SERVICE_UNIT;
	}

	private final static DiameterAVPDefinition AVP_G_S_U_POOL_IDENTIFIER = new DiameterAVPDefinition("G-S-U-Pool-Identifier", 453, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the G-S-U-Pool-Identifier AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getGsuPoolIdentifierAVP() {
		return AVP_G_S_U_POOL_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_G_S_U_POOL_REFERENCE = new DiameterAVPDefinition("G-S-U-Pool-Reference", 457, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the G-S-U-Pool-Reference AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getGsuPoolReferenceAVP() {
		return AVP_G_S_U_POOL_REFERENCE;
	}

	private final static DiameterAVPDefinition AVP_MULTIPLE_SERVICE_CREDIT_CONTROL = new DiameterAVPDefinition("Multiple-Service-Credit-Control", 456, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Multiple-Service-Credit-Control AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getMultipleServiceCreditControlAVP() {
		return AVP_MULTIPLE_SERVICE_CREDIT_CONTROL;
	}

	private final static DiameterAVPDefinition AVP_MULTIPLE_SERVICE_INDICATOR = new DiameterAVPDefinition("Multiple-Service-Indicator", 455, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Multiple-Service-Indicator AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getMultipleServiceIndicatorAVP() {
		return AVP_MULTIPLE_SERVICE_INDICATOR;
	}

	private final static DiameterAVPDefinition AVP_RATING_GROUP = new DiameterAVPDefinition("Rating-Group", 432, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Rating-Group AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRatingGroupAVP() {
		return AVP_RATING_GROUP;
	}

	private final static DiameterAVPDefinition AVP_REDIRECT_ADDRESS_TYPE = new DiameterAVPDefinition("Redirect-Address-Type", 433, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Redirect-Address-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRedirectAddressTypeAVP() {
		return AVP_REDIRECT_ADDRESS_TYPE;
	}

	private final static DiameterAVPDefinition AVP_REDIRECT_SERVER = new DiameterAVPDefinition("Redirect-Server", 434, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Redirect-Server AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRedirectServerAVP() {
		return AVP_REDIRECT_SERVER;
	}

	private final static DiameterAVPDefinition AVP_REDIRECT_SERVER_ADDRESS = new DiameterAVPDefinition("Redirect-Server-Address", 435, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Redirect-Server-Address AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRedirectServerAddressAVP() {
		return AVP_REDIRECT_SERVER_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_REQUESTED_ACTION = new DiameterAVPDefinition("Requested-Action", 436, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Requested-Action AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRequestedActionAVP() {
		return AVP_REQUESTED_ACTION;
	}

	private final static DiameterAVPDefinition AVP_REQUESTED_SERVICE_UNIT = new DiameterAVPDefinition("Requested-Service-Unit", 437, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Requested-Service-Unit AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRequestedServiceUnitAVP() {
		return AVP_REQUESTED_SERVICE_UNIT;
	}

	private final static DiameterAVPDefinition AVP_RESTRICTION_FILTER_RULE = new DiameterAVPDefinition("Restriction-Filter-Rule", 438, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IPFilterRuleFormat.INSTANCE, true);

	/**
	 * Gets the Restriction-Filter-Rule AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getRestrictionFilterRuleAVP() {
		return AVP_RESTRICTION_FILTER_RULE;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_CONTEXT_ID = new DiameterAVPDefinition("Service-Context-Id", 461, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Service-Context-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getServiceContextIdAVP() {
		return AVP_SERVICE_CONTEXT_ID;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_IDENTIFIER = new DiameterAVPDefinition("Service-Identifier", 439, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Service-Identifier AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getServiceIdentifierAVP() {
		return AVP_SERVICE_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_PARAMETER_INFO = new DiameterAVPDefinition("Service-Parameter-Info", 440, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Service-Parameter-Info AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getServiceParameterInfoAVP() {
		return AVP_SERVICE_PARAMETER_INFO;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_PARAMETER_TYPE = new DiameterAVPDefinition("Service-Parameter-Type", 441, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Service-Parameter-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getServiceParameterTypeAVP() {
		return AVP_SERVICE_PARAMETER_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_PARAMETER_VALUE = new DiameterAVPDefinition("Service-Parameter-Value", 442, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Service-Parameter-Value AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getServiceParameterValueAVP() {
		return AVP_SERVICE_PARAMETER_VALUE;
	}

	private final static DiameterAVPDefinition AVP_SUBSCRIPTION_ID = new DiameterAVPDefinition("Subscription-Id", 443, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Subscription-Id AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getSubscriptionIdAVP() {
		return AVP_SUBSCRIPTION_ID;
	}

	private final static DiameterAVPDefinition AVP_SUBSCRIPTION_ID_DATA = new DiameterAVPDefinition("Subscription-Id-Data", 444, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Subscription-Id-Data AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getSubscriptionIdDataAVP() {
		return AVP_SUBSCRIPTION_ID_DATA;
	}

	private final static DiameterAVPDefinition AVP_SUBSCRIPTION_ID_TYPE = new DiameterAVPDefinition("Subscription-Id-Type", 450, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Subscription-Id-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getSubscriptionIdTypeAVP() {
		return AVP_SUBSCRIPTION_ID_TYPE;
	}

	private final static DiameterAVPDefinition AVP_TARIFF_CHANGE_USAGE = new DiameterAVPDefinition("Tariff-Change-Usage", 452, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Tariff-Change-Usage AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getTariffChangeUsageAVP() {
		return AVP_TARIFF_CHANGE_USAGE;
	}

	private final static DiameterAVPDefinition AVP_TARIFF_TIME_CHANGE = new DiameterAVPDefinition("Tariff-Time-Change", 451, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Tariff-Time-Change AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getTariffTimeChangeAVP() {
		return AVP_TARIFF_TIME_CHANGE;
	}

	private final static DiameterAVPDefinition AVP_UNIT_VALUE = new DiameterAVPDefinition("Unit-Value", 445, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Unit-Value AVP definition according to the version.
	 * 
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUnitValueAVP() {
		return AVP_UNIT_VALUE;
	}

	private final static DiameterAVPDefinition AVP_USED_SERVICE_UNIT = new DiameterAVPDefinition("Used-Service-Unit", 446, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Used-Service-Unit AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getUsedServiceUnitAVP() {
		return AVP_USED_SERVICE_UNIT;
	}

	private final static DiameterAVPDefinition AVP_USER_EQUIPMENT_INFO = new DiameterAVPDefinition("User-Equipment-Info", 458, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the User-Equipment-Info AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getUserEquipmentInfoAVP() {
		return AVP_USER_EQUIPMENT_INFO;
	}

	private final static DiameterAVPDefinition AVP_USER_EQUIPMENT_INFO_TYPE = new DiameterAVPDefinition("User-Equipment-Info-Type", 459, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the User-Equipment-Info-Type AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getUserEquipmentInfoTypeAVP() {
		return AVP_USER_EQUIPMENT_INFO_TYPE;
	}

	private final static DiameterAVPDefinition AVP_USER_EQUIPMENT_INFO_VALUE = new DiameterAVPDefinition("User-Equipment-Info-Value", 460, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the User-Equipment-Info-Value AVP.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getUserEquipmentInfoValueAVP() {
		return AVP_USER_EQUIPMENT_INFO_VALUE;
	}

	private final static DiameterAVPDefinition AVP_VALUE_DIGITS = new DiameterAVPDefinition("Value-Digits", 447, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer64Format.INSTANCE, true);

	/**
	 * Gets the Value-Digits AVP definition.
	 * 
	 * @return The AVP definition.
	 */
	public final static DiameterAVPDefinition getValueDigitsAVP() {
		return AVP_VALUE_DIGITS;
	}

	private final static DiameterAVPDefinition AVP_VALIDITY_TIME = new DiameterAVPDefinition("Validity-Time", 448, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Validity-Time AVP definition according to the version.
	 * 
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getValidityTimeAVP() {
		return AVP_VALIDITY_TIME;
	}

	/*
	 * 3GPP AVPs
	 */
	private final static DiameterAVPDefinition AVP_3GPP_CHARGING_CHARACTERISTICS = new DiameterAVPDefinition("3GPP-Charging-Characteristics", 13, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-Charging-Characteristics AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppChargingCharacteristicsAVP(Version version) {
		return AVP_3GPP_CHARGING_CHARACTERISTICS;
	}

	private final static DiameterAVPDefinition AVP_3GPP_CHARGING_ID = new DiameterAVPDefinition("3GPP-Charging-Id", 2, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-Charging-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppChargingIdAVP(Version version) {
		return AVP_3GPP_CHARGING_ID;
	}

	private final static DiameterAVPDefinition AVP_3GPP_GGSN_MCC_MNC = new DiameterAVPDefinition("3GPP-GGSN-MCC-MNC", 9, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-GGSN-MCC-MNC AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppGgsnMccMncAVP(Version version) {
		return AVP_3GPP_GGSN_MCC_MNC;
	}

	private final static DiameterAVPDefinition AVP_3GPP_GPRS_NEGOCIATED_QOS_PROFILE = new DiameterAVPDefinition("3GPP-GPRS-Negociated-QoS-Profile", 5, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-GPRS-Negociated-QoS-Profile AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppGprsNegociatedQosProfileAVP(Version version) {
		return AVP_3GPP_GPRS_NEGOCIATED_QOS_PROFILE;
	}

	private final static DiameterAVPDefinition AVP_3GPP_IMSI_MCC_MNC = new DiameterAVPDefinition("3GPP-IMSI-MCC-MNC", 8, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-IMSI-MCC-MNC AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppImsiMccMncAVP(Version version) {
		return AVP_3GPP_IMSI_MCC_MNC;
	}

	private final static DiameterAVPDefinition AVP_3GPP_MS_TIMEZONE = new DiameterAVPDefinition("3GPP-MS-TimeZone", 23, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-MS-TimeZone AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppMsTimeZoneAVP(Version version) {
		return AVP_3GPP_MS_TIMEZONE;
	}

	private final static DiameterAVPDefinition AVP_3GPP_NSAPI = new DiameterAVPDefinition("3GPP-NSAPI", 10, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-NSAPI AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppNsapiAVP(Version version) {
		return AVP_3GPP_NSAPI;
	}

	private final static DiameterAVPDefinition AVP_3GPP_PDP_TYPE = new DiameterAVPDefinition("3GPP-PDP-Type", 3, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-PDP-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppPdpTypeAVP(Version version) {
		return AVP_3GPP_PDP_TYPE;
	}

	private final static DiameterAVPDefinition AVP_3GPP_RAT_TYPE = new DiameterAVPDefinition("3GPP-RAT-Type", 21, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-RAT-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppRatTypeAVP(Version version) {
		return AVP_3GPP_RAT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_3GPP_SELECTION_MODE = new DiameterAVPDefinition("3GPP-Selection-Mode", 12, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-Selection-Mode AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppSelectionModeAVP(Version version) {
		return AVP_3GPP_SELECTION_MODE;
	}

	private final static DiameterAVPDefinition AVP_3GPP_SESSION_STOP_INDICATOR = new DiameterAVPDefinition("3GPP-Session-Stop-Indicator", 11, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-Session-Stop-Indicator AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppSessionStopIndicatorAVP(Version version) {
		return AVP_3GPP_SESSION_STOP_INDICATOR;
	}

	private final static DiameterAVPDefinition AVP_3GPP_SGSN_MCC_MNC = new DiameterAVPDefinition("3GPP-SGSN-MCC-MNC", 18, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-SGSN-MCC-MNC AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppSgsnMccMncAVP(Version version) {
		return AVP_3GPP_SGSN_MCC_MNC;
	}

	private final static DiameterAVPDefinition AVP_3GPP_USER_LOCATION_INFO = new DiameterAVPDefinition("3GPP-User-Location-Info", 22, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP-User-Location-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gppUserLocationInfoAVP(Version version) {
		return AVP_3GPP_USER_LOCATION_INFO;
	}

	/**
	 * Found in document 3GPP2 X.S0057-0 v2.0
	 */
	private final static DiameterAVPDefinition AVP_3GPP2_BSID = new DiameterAVPDefinition("3GPP2-BSID", 9010, THREEGPP2_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP2-BSID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gpp2BsidAVP(Version version) {
		return AVP_3GPP2_BSID;
	}

	private final static DiameterAVPDefinition AVP_ACCESS_NETWORK_CHARGING_IDENTIFIER_VALUE = new DiameterAVPDefinition("Access-Network-Charging-Identifier-Value", 503, THREEGPP_VENDOR_ID, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Access-Network-Charging-Identifier-Value AVP definition according
	 * to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAccessNetworkChargingIdentifierValueAVP(Version version) {
		return AVP_ACCESS_NETWORK_CHARGING_IDENTIFIER_VALUE;
	}

	private final static DiameterAVPDefinition AVP_ACCESS_NETWORK_INFORMATION = new DiameterAVPDefinition("Access-Network-Information", 1263, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Access-Network-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAccessNetworkInformationAVP(Version version) {
		return AVP_ACCESS_NETWORK_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_ACCUMULATED_COST = new DiameterAVPDefinition("Accumulated-Cost", 2052, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Accumulated-Cost AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAccumulatedCostValueAVP(Version version) {
		return AVP_ACCUMULATED_COST;
	}

	private final static DiameterAVPDefinition AVP_ADAPTATIONS = new DiameterAVPDefinition("Adaptations", 1217, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Adaptations AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAdaptationsAVP(Version version) {
		return AVP_ADAPTATIONS;
	}

	private final static DiameterAVPDefinition AVP_ADDITIONAL_CONTENT_INFORMATION = new DiameterAVPDefinition("Additional-Content-Information", 1207, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Additional-Content-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAdditionalContentInformationAVP(Version version) {
		return AVP_ADDITIONAL_CONTENT_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_ADDITIONAL_TYPE_INFORMATION = new DiameterAVPDefinition("Additional-Type-Information", 1205, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Additional-Type-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAdditionalTypeInformationAVP(Version version) {
		return AVP_ADDITIONAL_TYPE_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_ADDRESS_DATA = new DiameterAVPDefinition("Address-Data", 897, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Address-Data AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAddressDataAVP(Version version) {
		return AVP_ADDRESS_DATA;
	}

	private final static DiameterAVPDefinition AVP_ADDRESS_DOMAIN = new DiameterAVPDefinition("Address-Domain", 898, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Address-Domain AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAddressDomainAVP(Version version) {
		return AVP_ADDRESS_DOMAIN;
	}

	private final static DiameterAVPDefinition AVP_ADDRESSEE_TYPE = new DiameterAVPDefinition("Addressee-Type", 1208, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Addressee-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAddresseeTypeAVP(Version version) {
		return AVP_ADDRESSEE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_ADDRESS_TYPE = new DiameterAVPDefinition("Address-Type", 899, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Address-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAddressTypeAVP(Version version) {
		return AVP_ADDRESS_TYPE;
	}

	private final static DiameterAVPDefinition AVP_AF_CHARGING_IDENTIFIER = new DiameterAVPDefinition("AF-Charging-Identifier", 505, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the AF-Charging-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAfChargingIdentifierAVP(Version version) {
		return AVP_AF_CHARGING_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_AF_CORRELATION_INFORMATION = new DiameterAVPDefinition("AF-Correlation-Information", 1276, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the AF-Correlation-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAfCorrelationInformationAVP(Version version) {
		return AVP_AF_CORRELATION_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_ALTERNATE_CHARGED_PARTY_ADDRESS = new DiameterAVPDefinition("Alternate-Charged-Party-Address", 1280, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Alternate-Charged-Party-Address AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAlternateChargedPartyAddressAVP(Version version) {
		return AVP_ALTERNATE_CHARGED_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_AOC_COST_INFORMATION = new DiameterAVPDefinition("AoC-Cost-Information", 2053, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the AoC-Cost-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAocCostInformationAVP(Version version) {
		return AVP_AOC_COST_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_AOC_INFORMATION = new DiameterAVPDefinition("AoC-Information", 2054, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the AoC-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAocInformationAVP(Version version) {
		return AVP_AOC_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_AOC_REQUEST_TYPE = new DiameterAVPDefinition("AoC-Request-Type", 2055, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the AoC-Request-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAocRequestTypeAVP(Version version) {
		return AVP_AOC_REQUEST_TYPE;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_PROVIDED_CALLED_PARTY_ADDRESS = new DiameterAVPDefinition("Application-Provided-Called-Party-Address", 837, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Application-Provided-Called-Party-Address AVP definition according
	 * to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationProvidedCalledPartyAddressAVP(Version version) {
		return AVP_APPLICATION_PROVIDED_CALLED_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_SERVER = new DiameterAVPDefinition("Application-Server", 836, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Application-Server AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationServerAVP(Version version) {
		return AVP_APPLICATION_SERVER;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_SERVER_ID = new DiameterAVPDefinition("Application-Server-ID", 2101, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Application-Server-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationServerIdAVP(Version version) {
		return AVP_APPLICATION_SERVER_ID;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_SERVER_INFORMATION = new DiameterAVPDefinition("Application-Server-Information", 850, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Application-Server-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationServerInformationAVP(Version version) {
		return AVP_APPLICATION_SERVER_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_SERVICE_TYPE = new DiameterAVPDefinition("Application-Service-Type", 2102, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Application-Service-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationServiceTypeAVP(Version version) {
		return AVP_APPLICATION_SERVICE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_APPLICATION_SESSION_ID = new DiameterAVPDefinition("Application-Session-ID", 2103, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Application-Session-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicationSessionIdAVP(Version version) {
		return AVP_APPLICATION_SESSION_ID;
	}

	private final static DiameterAVPDefinition AVP_APPLIC_ID = new DiameterAVPDefinition("Applic-ID", 1218, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Applic-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApplicIdAVP(Version version) {
		return AVP_APPLIC_ID;
	}

	private final static DiameterAVPDefinition AVP_ASSOCIATED_PARTY_ADDRESS = new DiameterAVPDefinition("Associated-Party-Address", 2035, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Associated-Party-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAssociatedPartyAddressAVP(Version version) {
		return AVP_ASSOCIATED_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_ASSOCIATED_URI = new DiameterAVPDefinition("Associated-URI", 856, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Associated-URI AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAssociatedUriAVP(Version version) {
		return AVP_ASSOCIATED_URI;
	}

	private final static DiameterAVPDefinition AVP_AUTHORIZED_QOS = new DiameterAVPDefinition("Authorized-QoS", 849, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Authorized-QoS AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAuthorizedQosAVP(Version version) {
		return AVP_AUTHORIZED_QOS;
	}

	private final static DiameterAVPDefinition AVP_AUX_APPLIC_INFO = new DiameterAVPDefinition("Aux-Applic-Info", 1219, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Aux-Applic-Info AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAuxApplicInfoAVP(Version version) {
		return AVP_AUX_APPLIC_INFO;
	}

	private final static DiameterAVPDefinition AVP_BASE_TIME_INTERVAL = new DiameterAVPDefinition("Base-Time-Interval", 1265, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Base-Time-Interval AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getBaseTimeIntervalAVP(Version version) {
		return AVP_BASE_TIME_INTERVAL;
	}

	private final static DiameterAVPDefinition AVP_BEARER_SERVICE = new DiameterAVPDefinition("Bearer-Service", 854, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Bearer-Service AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getBearerServiceAVP(Version version) {
		return AVP_BEARER_SERVICE;
	}

	private final static DiameterAVPDefinition AVP_CALLED_ASSERTED_IDENTITY = new DiameterAVPDefinition("Called-Asserted-Identity", 1250, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Called-Asserted-Identity AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCalledAssertedIdentityAVP(Version version) {
		return AVP_CALLED_ASSERTED_IDENTITY;
	}

	private final static DiameterAVPDefinition AVP_CALLED_PARTY_ADDRESS = new DiameterAVPDefinition("Called-Party-Address", 832, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Called-Party-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCalledPartyAddressAVP(Version version) {
		return AVP_CALLED_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_CALLING_PARTY_ADDRESS = new DiameterAVPDefinition("Calling-Party-Address", 831, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Calling-Party-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCallingPartyAddressAVP(Version version) {
		return AVP_CALLING_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_CARRIER_SELECT_ROUTING_INFORMATION = new DiameterAVPDefinition("Carrier-Select-Routing-Information", 2023, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Carrier-Select-Routing-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCarrierSelectRoutingInformationAVP(Version version) {
		return AVP_CARRIER_SELECT_ROUTING_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_CAUSE_CODE = new DiameterAVPDefinition("Cause-Code", 861, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer32Format.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_CAUSE_CODE_OLD = new DiameterAVPDefinition("Cause-Code", 861, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, false);

	/**
	 * Gets the Cause-Code AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCauseCodeAVP(Version version) {
		if (version.compareTo(VERSION_6_6) < 0) {
			return AVP_CAUSE_CODE_OLD;
		} else if (version.getMajor() == 7 && version.compareTo(VERSION_7_1) < 0) {
			return AVP_CAUSE_CODE_OLD;
		}
		return AVP_CAUSE_CODE;
	}

	private final static DiameterAVPDefinition AVP_CG_ADDRESS = new DiameterAVPDefinition("CG-Address", 846, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the CG-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCgAddressAVP(Version version) {
		return AVP_CG_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_CHANGE_CONDITION = new DiameterAVPDefinition("Change-Condition", 2037, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer32Format.INSTANCE, true);

	/**
	 * Gets the Change-Condition AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getChangeConditionAVP(Version version) {
		return AVP_CHANGE_CONDITION;
	}

	private final static DiameterAVPDefinition AVP_CHANGE_TIME = new DiameterAVPDefinition("Change-Time", 2038, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Change-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getChangeTimeAVP(Version version) {
		return AVP_CHANGE_TIME;
	}

	private final static DiameterAVPDefinition AVP_CHARGED_PARTY = new DiameterAVPDefinition("Charged-Party", 857, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Charged-Party AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getChargedPartyAVP(Version version) {
		return AVP_CHARGED_PARTY;
	}

	private final static DiameterAVPDefinition AVP_CHARGING_RULE_BASE_NAME = new DiameterAVPDefinition("Charging-Rule-Base-Name", 1004, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Charging-Rule-Base-Name AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getChargingRuleBaseNameAVP(Version version) {
		return AVP_CHARGING_RULE_BASE_NAME;
	}

	private final static DiameterAVPDefinition AVP_CLASS_IDENTIFIER = new DiameterAVPDefinition("Class-Identifier", 1214, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Class-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getClassIdentifierAVP(Version version) {
		return AVP_CLASS_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_CLIENT_ADDRESS = new DiameterAVPDefinition("Client-Address", 2018, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Client-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getClientAddressAVP(Version version) {
		return AVP_CLIENT_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_CONTENT_CLASS = new DiameterAVPDefinition("Content-Class", 1220, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Content-Class AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContentClassAVP(Version version) {
		return AVP_CONTENT_CLASS;
	}

	private final static DiameterAVPDefinition AVP_CONTENT_DISPOSITION = new DiameterAVPDefinition("Content-Disposition", 828, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Content-Disposition AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContentDispositionAVP(Version version) {
		return AVP_CONTENT_DISPOSITION;
	}

	private final static DiameterAVPDefinition AVP_CONTENT_LENGTH = new DiameterAVPDefinition("Content-Length", 827, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_CONTENT_LENGTH_OLD = new DiameterAVPDefinition("Content-Length", 827, ChargingUtils.THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);

	/**
	 * Gets the Content-Length AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContentLengthAVP(Version version) {
		if (version.compareTo(VERSION_6_6) < 0) {
			return AVP_CONTENT_LENGTH_OLD;
		} else if (version.getMajor() == 7 && version.compareTo(VERSION_7_1) < 0) {
			return AVP_CONTENT_LENGTH_OLD;
		}
		return AVP_CONTENT_LENGTH;
	}

	private final static DiameterAVPDefinition AVP_CONTENT_SIZE = new DiameterAVPDefinition("Content-Size", 1206, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Content-Size AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContentSizeAVP(Version version) {
		return AVP_CONTENT_SIZE;
	}

	private final static DiameterAVPDefinition AVP_CONTENT_TYPE = new DiameterAVPDefinition("Content-Type", 826, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Content-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getContentTypeAVP(Version version) {
		return AVP_CONTENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_CURRENT_TARIFF = new DiameterAVPDefinition("Current-Tariff", 2056, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Current-Tariff AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getCurrentTariffAVP(Version version) {
		return AVP_CURRENT_TARIFF;
	}

	private final static DiameterAVPDefinition AVP_DATA_CODING_SCHEME = new DiameterAVPDefinition("Data-Coding-Scheme", 2001, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer32Format.INSTANCE, true);

	/**
	 * Gets the Data-Coding-Scheme AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDataCodingSchemeAVP(Version version) {
		return AVP_DATA_CODING_SCHEME;
	}

	private final static DiameterAVPDefinition AVP_DEFERRED_LOCATION_EVENT_TYPE = new DiameterAVPDefinition("Deferred-Location-Event-Type", 1230, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Deferred-Location-Event-Type AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDeferredLocationEventTypeAVP(Version version) {
		return AVP_DEFERRED_LOCATION_EVENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_DELIVERY_REPORT_REQUESTED = new DiameterAVPDefinition("Delivery-Report-Requested", 1216, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Delivery-Report-Requested AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDeliveryReportRequestedAVP(Version version) {
		return AVP_DELIVERY_REPORT_REQUESTED;
	}

	private final static DiameterAVPDefinition AVP_DELIVERY_STATUS = new DiameterAVPDefinition("Delivery-Status", 2104, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Delivery-Status AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDeliveryStatusAVP(Version version) {
		return AVP_DELIVERY_STATUS;
	}

	private final static DiameterAVPDefinition AVP_DESTINATION_INTERFACE = new DiameterAVPDefinition("Destination-Interface", 2002, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Destination-Interface AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDestinationInterfaceAVP(Version version) {
		return AVP_DESTINATION_INTERFACE;
	}

	private final static DiameterAVPDefinition AVP_DIAGNOSTICS = new DiameterAVPDefinition("Diagnostics", 2039, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Integer32Format.INSTANCE, true);

	/**
	 * Gets the Diagnostics AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDiagnosticsAVP(Version version) {
		return AVP_DIAGNOSTICS;
	}

	private final static DiameterAVPDefinition AVP_DOMAIN_NAME = new DiameterAVPDefinition("Domain-Name", 1200, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Domain-Name AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDomainNameAVP(Version version) {
		return AVP_DOMAIN_NAME;
	}

	private final static DiameterAVPDefinition AVP_DRM_CONTENT = new DiameterAVPDefinition("DRM-Content", 1221, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the DRM-Content AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDrmContentAVP(Version version) {
		return AVP_DRM_CONTENT;
	}

	private final static DiameterAVPDefinition AVP_DYNAMIC_ADDRESS_FLAG = new DiameterAVPDefinition("Dynamic-Address-Flag", 2051, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Dynamic-Address-Flag AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getDynamicAddressFlagAVP(Version version) {
		return AVP_DYNAMIC_ADDRESS_FLAG;
	}

	private final static DiameterAVPDefinition AVP_EARLY_MEDIA_DESCRIPTION = new DiameterAVPDefinition("Early-Media-Description", 1272, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Early-Media-Description AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEarlyMediaDescriptionAVP(Version version) {
		return AVP_EARLY_MEDIA_DESCRIPTION;
	}

	private final static DiameterAVPDefinition AVP_ENVELOPE = new DiameterAVPDefinition("Envelope", 1266, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Envelope AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEnvelopeAVP(Version version) {
		return AVP_ENVELOPE;
	}

	private final static DiameterAVPDefinition AVP_ENVELOPE_END_TIME = new DiameterAVPDefinition("Envelope-End-Time", 1267, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Envelope-End-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEnvelopeEndTimeAVP(Version version) {
		return AVP_ENVELOPE_END_TIME;
	}

	private final static DiameterAVPDefinition AVP_ENVELOPE_REPORTING = new DiameterAVPDefinition("Envelope-Reporting", 1268, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Envelope-Reporting AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEnvelopeReportingAVP(Version version) {
		return AVP_ENVELOPE_REPORTING;
	}

	private final static DiameterAVPDefinition AVP_ENVELOPE_START_TIME = new DiameterAVPDefinition("Envelope-Start-Time", 1269, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Envelope-Start-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEnvelopeStartTimeAVP(Version version) {
		return AVP_ENVELOPE_START_TIME;
	}

	private final static DiameterAVPDefinition AVP_EVENT = new DiameterAVPDefinition("Event", 825, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Event AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEventAVP(Version version) {
		return AVP_EVENT;
	}

	private final static DiameterAVPDefinition AVP_EVENT_CHARGING_TIMESTAMP = new DiameterAVPDefinition("Event-Charging-TimeStamp", 1258, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Event-Charging-TimeStamp AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEventChargingTimeStampAVP(Version version) {
		return AVP_EVENT_CHARGING_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_EVENT_TYPE = new DiameterAVPDefinition("Event-Type", 823, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Event-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getEventTypeAVP(Version version) {
		return AVP_EVENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_EXPIRES = new DiameterAVPDefinition("Expires", 888, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Expires AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getExpiresAVP(Version version) {
		return AVP_EXPIRES;
	}

	private final static DiameterAVPDefinition AVP_FILE_REPAIR_SUPPORTED = new DiameterAVPDefinition("File-Repair-Supported", 1224, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the File-Repair-Supported AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFileRepairSupportedAVP(Version version) {
		return AVP_FILE_REPAIR_SUPPORTED;
	}

	private final static DiameterAVPDefinition AVP_FLOWS = new DiameterAVPDefinition("Flows", 510, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Flows AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFlowsAVP(Version version) {
		return AVP_FLOWS;
	}

	private final static DiameterAVPDefinition AVP_GGSN_ADDRESS = new DiameterAVPDefinition("GGSN-Address", 847, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the GGSN-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGgsnAddressAVP(Version version) {
		return AVP_GGSN_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_IM_INFORMATION = new DiameterAVPDefinition("IM-Information", 2110, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the IM-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getImInformationAVP(Version version) {
		return AVP_IM_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_IMS_CHARGING_IDENTIFIER = new DiameterAVPDefinition("IMS-Charging-Identifier", 841, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the IMS-Charging-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getImsChargingIdentifierAVP(Version version) {
		return AVP_IMS_CHARGING_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_IMS_COMMUNICATION_SERVICE_IDENTIFIER = new DiameterAVPDefinition("IMS-Communication-Service-Identifier", 1281, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the IMS-Communication-Service-Identifier AVP definition according to
	 * the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getImsCommunicationServiceIdentifierAVP(Version version) {
		return AVP_IMS_COMMUNICATION_SERVICE_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_IMS_INFORMATION = new DiameterAVPDefinition("IMS-Information", 876, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the IMS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getImsInformationAVP(Version version) {
		return AVP_IMS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_INCOMING_TRUNK_GROUP_ID = new DiameterAVPDefinition("Incoming-Trunk-Group-Id", 852, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Incoming-Trunk-Group-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getIncomingTrunkGroupIdAVP(Version version) {
		return AVP_INCOMING_TRUNK_GROUP_ID;
	}

	private final static DiameterAVPDefinition AVP_INCREMENTAL_COST = new DiameterAVPDefinition("Incremental-Cost", 2062, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Incremental-Cost AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getIncrementalCostAVP(Version version) {
		return AVP_INCREMENTAL_COST;
	}

	private final static DiameterAVPDefinition AVP_INTERFACE_ID = new DiameterAVPDefinition("Interface-Id", 2003, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Interface-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getInterfaceIdAVP(Version version) {
		return AVP_INTERFACE_ID;
	}

	private final static DiameterAVPDefinition AVP_INTERFACE_PORT = new DiameterAVPDefinition("Interface-Port", 2004, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Interface-Port AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getInterfacePortAVP(Version version) {
		return AVP_INTERFACE_PORT;
	}

	private final static DiameterAVPDefinition AVP_INTERFACE_TEXT = new DiameterAVPDefinition("Interface-Text", 2005, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Interface-Text AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getInterfaceTextAVP(Version version) {
		return AVP_INTERFACE_TEXT;
	}

	private final static DiameterAVPDefinition AVP_INTERFACE_TYPE = new DiameterAVPDefinition("Interface-Type", 2006, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Interface-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getInterfaceTypeAVP(Version version) {
		return AVP_INTERFACE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_INTER_OPERATOR_IDENTIFIER = new DiameterAVPDefinition("Inter-Operator-Identifier", 838, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Inter-Operator-Identifier AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getInterOperatorIdentifierAVP(Version version) {
		return AVP_INTER_OPERATOR_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_LCS_CLIENT_DIALED_BY_MS = new DiameterAVPDefinition("LCS-Client-Dialed-By-MS", 1233, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Client-Dialed-By-MS AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsClientDialedByMsAVP(Version version) {
		return AVP_LCS_CLIENT_DIALED_BY_MS;
	}

	private final static DiameterAVPDefinition AVP_LCS_CLIENT_EXTERNAL_ID = new DiameterAVPDefinition("LCS-Client-External-Id", 1234, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Client-External-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsClientExternalIdAVP(Version version) {
		return AVP_LCS_CLIENT_EXTERNAL_ID;
	}

	private final static DiameterAVPDefinition AVP_LCS_CLIENT_ID = new DiameterAVPDefinition("LCS-Client-Id", 1232, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Client-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsClientIdAVP(Version version) {
		return AVP_LCS_CLIENT_ID;
	}

	private final static DiameterAVPDefinition AVP_LCS_APN = new DiameterAVPDefinition("LCS-APN", 1231, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-APN AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsApnAVP(Version version) {
		return AVP_LCS_APN;
	}

	private final static DiameterAVPDefinition AVP_LCS_CLIENT_NAME = new DiameterAVPDefinition("LCS-Client-Name", 1235, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Client-Name AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsClientNameAVP(Version version) {
		return AVP_LCS_CLIENT_NAME;
	}

	private final static DiameterAVPDefinition AVP_LCS_CLIENT_TYPE = new DiameterAVPDefinition("LCS-Client-Type", 1241, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Client-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsClientTypeAVP(Version version) {
		return AVP_LCS_CLIENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_LCS_DATA_CODING_SCHEME = new DiameterAVPDefinition("LCS-Data-Coding-Scheme", 1236, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Data-Coding-Scheme AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsDataCodingSchemeAVP(Version version) {
		return AVP_LCS_DATA_CODING_SCHEME;
	}

	private final static DiameterAVPDefinition AVP_LCS_FORMAT_INDICATOR = new DiameterAVPDefinition("LCS-Format-Indicator", 1237, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Format-Indicator AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsFormatIndicatorAVP(Version version) {
		return AVP_LCS_FORMAT_INDICATOR;
	}

	private final static DiameterAVPDefinition AVP_LCS_INFORMATION = new DiameterAVPDefinition("LCS-Information", 878, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsInformationAVP(Version version) {
		return AVP_LCS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_LCS_NAME_STRING = new DiameterAVPDefinition("LCS-Name-String", 1238, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Name-String AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsNameStringAVP(Version version) {
		return AVP_LCS_NAME_STRING;
	}

	private final static DiameterAVPDefinition AVP_LCS_REQUESTOR_ID = new DiameterAVPDefinition("LCS-Requestor-ID", 1239, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Requestor-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsRequestorIdAVP(Version version) {
		return AVP_LCS_REQUESTOR_ID;
	}

	private final static DiameterAVPDefinition AVP_LCS_REQUESTOR_ID_STRING = new DiameterAVPDefinition("LCS-Requestor-ID-String", 1240, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the LCS-Requestor-ID-String AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLcsRequestorIdStringAVP(Version version) {
		return AVP_LCS_REQUESTOR_ID_STRING;
	}

	private final static DiameterAVPDefinition AVP_LOCAL_SEQUENCE_NUMBER = new DiameterAVPDefinition("Local-Sequence-Number", 2063, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Local-Sequence-Number AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLocalSequenceNumberAVP(Version version) {
		return AVP_LOCAL_SEQUENCE_NUMBER;
	}

	private final static DiameterAVPDefinition AVP_LOCATION_ESTIMATE = new DiameterAVPDefinition("Location-Estimate", 1242, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Location-Estimate AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLocationEstimateAVP(Version version) {
		return AVP_LOCATION_ESTIMATE;
	}

	private final static DiameterAVPDefinition AVP_LOCATION_ESTIMATE_TYPE = new DiameterAVPDefinition("Location-Estimate-Type", 1243, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Location-Estimate-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLocationEstimateTypeAVP(Version version) {
		return AVP_LOCATION_ESTIMATE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_LOCATION_TYPE = new DiameterAVPDefinition("Location-Type", 1244, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Location-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLocationTypeAVP(Version version) {
		return AVP_LOCATION_TYPE;
	}

	private final static DiameterAVPDefinition AVP_LOW_BALANCE_INDICATION = new DiameterAVPDefinition("Low-Balance-Indication", 2020, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Low-Balance-Indication AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getLowBalanceIndicationAVP(Version version) {
		return AVP_LOW_BALANCE_INDICATION;
	}

	private final static DiameterAVPDefinition AVP_MBMS_2G_3G_INDICATOR = new DiameterAVPDefinition("MBMS-2G-3G-Indicator", 907, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-2G-3G-Indicator AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbms2g3gIndicatorAVP(Version version) {
		return AVP_MBMS_2G_3G_INDICATOR;
	}

	private final static DiameterAVPDefinition AVP_MBMS_INFORMATION = new DiameterAVPDefinition("MBMS-Information", 880, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbmsInformationAVP(Version version) {
		return AVP_MBMS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_MBMS_SERVICE_AREA = new DiameterAVPDefinition("MBMS-Service-Area", 903, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-Service-Area AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbmsServiceAreaAVP(Version version) {
		return AVP_MBMS_SERVICE_AREA;
	}

	private final static DiameterAVPDefinition AVP_MBMS_SERVICE_TYPE = new DiameterAVPDefinition("MBMS-Service-Type", 906, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-Service-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbmsServiceTypeAVP(Version version) {
		return AVP_MBMS_SERVICE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_MBMS_SESSION_IDENTITY = new DiameterAVPDefinition("MBMS-Session-Identity", 908, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-Session-Identity AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbmsSessionIdentityAVP(Version version) {
		return AVP_MBMS_SESSION_IDENTITY;
	}

	private final static DiameterAVPDefinition AVP_MBMS_USER_SERVICE_TYPE = new DiameterAVPDefinition("MBMS-User-Service-Type", 1225, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the MBMS-User-Service-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMbmsUserServiceTypeAVP(Version version) {
		return AVP_MBMS_USER_SERVICE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_MEDIA_INITIATOR_FLAG = new DiameterAVPDefinition("Media-Initiator-Flag", 882, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Media-Initiator-Flag AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMediaInitiatorFlagAVP(Version version) {
		return AVP_MEDIA_INITIATOR_FLAG;
	}

	private final static DiameterAVPDefinition AVP_MEDIA_INITIATOR_PARTY = new DiameterAVPDefinition("Media-Initiator-Party", 1288, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Media-Initiator-Party AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMediaInitiatorPartyAVP(Version version) {
		return AVP_MEDIA_INITIATOR_PARTY;
	}

	private final static DiameterAVPDefinition AVP_MESSAGE_BODY = new DiameterAVPDefinition("Message-Body", 889, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Message-Body AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMessageBodyAVP(Version version) {
		return AVP_MESSAGE_BODY;
	}

	private final static DiameterAVPDefinition AVP_MESSAGE_CLASS = new DiameterAVPDefinition("Message-Class", 1213, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Message-Class AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMessageClassAVP(Version version) {
		return AVP_MESSAGE_CLASS;
	}

	private final static DiameterAVPDefinition AVP_MESSAGE_ID = new DiameterAVPDefinition("Message-ID", 1210, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Message-ID AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMessageIdAVP(Version version) {
		return AVP_MESSAGE_ID;
	}

	private final static DiameterAVPDefinition AVP_MESSAGE_SIZE = new DiameterAVPDefinition("Message-Size", 1212, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Message-Size AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMessageSizeAVP(Version version) {
		return AVP_MESSAGE_SIZE;
	}

	private final static DiameterAVPDefinition AVP_MESSAGE_TYPE = new DiameterAVPDefinition("Message-Type", 1211, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Message-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMessageTypeAVP(Version version) {
		return AVP_MESSAGE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_MMBOX_STORAGE_REQUESTED = new DiameterAVPDefinition("MMBox-Storage-Requested", 1248, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the MMBox-Storage-Requested AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMmboxStorageRequestedAVP(Version version) {
		return AVP_MMBOX_STORAGE_REQUESTED;
	}

	private final static DiameterAVPDefinition AVP_MM_CONTENT_TYPE = new DiameterAVPDefinition("MM-Content-Type", 1203, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the MM-Content-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMmContentTypeAVP(Version version) {
		return AVP_MM_CONTENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_MMS_INFORMATION = new DiameterAVPDefinition("MMS-Information", 877, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the MMS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMmsInformationAVP(Version version) {
		return AVP_MMS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_MMTEL_INFORMATION = new DiameterAVPDefinition("MMTel-Information", 2030, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the MMTel-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMmtelInformationAVP(Version version) {
		return AVP_MMTEL_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_NEXT_TARIFF = new DiameterAVPDefinition("Next-Tariff", 2057, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Next-Tariff AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNextTariffAVP(Version version) {
		return AVP_NEXT_TARIFF;
	}

	private final static DiameterAVPDefinition AVP_NODE_FUNCTIONALITY = new DiameterAVPDefinition("Node-Functionality", 862, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Node-Functionality AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNodeFunctionalityAVP(Version version) {
		return AVP_NODE_FUNCTIONALITY;
	}

	private final static DiameterAVPDefinition AVP_NON_3GPP_ACCESS_INFORMATION = new DiameterAVPDefinition("Non-3GPP-Access-Information", 2050, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Non-3GPP-Access-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNon3gppAccessInformationAVP(Version version) {
		return AVP_NON_3GPP_ACCESS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_DIVERSIONS = new DiameterAVPDefinition("Number-Of-Diversions", 2034, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Diversions AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfDiversionsAVP(Version version) {
		return AVP_NUMBER_OF_DIVERSIONS;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_MESSAGES_SENT = new DiameterAVPDefinition("Number-Of-Messages-Sent", 2019, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Messages-Sent AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfMessagesSentAVP(Version version) {
		return AVP_NUMBER_OF_MESSAGES_SENT;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_MESSAGES_SUCCESSFULLY_EXPLODED = new DiameterAVPDefinition("Number-Of-Messages-Successfully-Exploded", 2011, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Messages-Successfully-Exploded AVP definition according
	 * to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfMessagesSuccessfullyExplodedAVP(Version version) {
		return AVP_NUMBER_OF_MESSAGES_SUCCESSFULLY_EXPLODED;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_MESSAGES_SUCCESSFULLY_SENT = new DiameterAVPDefinition("Number-Of-Messages-Successfully-Sent", 2012, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Messages-Successfully-Sent AVP definition according to
	 * the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfMessagesSuccessfullySentAVP(Version version) {
		return AVP_NUMBER_OF_MESSAGES_SUCCESSFULLY_SENT;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_PARTICIPANTS = new DiameterAVPDefinition("Number-Of-Participants", 885, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Participants AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfParticipantsAVP(Version version) {
		return AVP_NUMBER_OF_PARTICIPANTS;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_RECEIVED_TALK_BURSTS = new DiameterAVPDefinition("Number-Of-Received-Talk-Bursts", 1282, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Received-Talk-Bursts AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfReceivedTalkBurstsAVP(Version version) {
		return AVP_NUMBER_OF_RECEIVED_TALK_BURSTS;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_OF_TALK_BURSTS = new DiameterAVPDefinition("Number-Of-Talk-Bursts", 1283, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Number-Of-Talk-Bursts AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberOfTalkBurstsAVP(Version version) {
		return AVP_NUMBER_OF_TALK_BURSTS;
	}

	private final static DiameterAVPDefinition AVP_NUMBER_PORTABILITY_ROUTING_INFORMATION = new DiameterAVPDefinition("Number-Portability-Routing-Information", 2074, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Number-Portability-Routing-Information AVP definition according to
	 * the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getNumberPortabilityRoutingInformationAVP(Version version) {
		return AVP_NUMBER_PORTABILITY_ROUTING_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_OFFLINE_CHARGING = new DiameterAVPDefinition("Offline-Charging", 1278, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Offline-Charging AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOfflineChargingAVP(Version version) {
		return AVP_OFFLINE_CHARGING;
	}

	/**
	 * Gets the Optional-Capability AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOptionalCapabilityAVP(Version version) {
		return CxUtils.getOptionalCapabilityAVP(getVersion29229(version));
	}

	private final static DiameterAVPDefinition AVP_ORIGINATING_IOI = new DiameterAVPDefinition("Originating-IOI", 839, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Originating-IOI AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatingIoiAVP(Version version) {
		return AVP_ORIGINATING_IOI;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATING_SCCP_ADDRESS = new DiameterAVPDefinition("Originating-SCCP-Address", 2008, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the Originating-SCCP-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatingSccpAddressAVP(Version version) {
		return AVP_ORIGINATING_SCCP_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATOR = new DiameterAVPDefinition("Originator", 864, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Originator AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatorAVP(Version version) {
		return AVP_ORIGINATOR;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATOR_ADDRESS = new DiameterAVPDefinition("Originator-Address", 886, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Originator-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatorAddressAVP(Version version) {
		return AVP_ORIGINATOR_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATOR_RECEIVED_ADDRESS = new DiameterAVPDefinition("Originator-Received-Address", 2027, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Originator-Received-Address AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatorReceivedAddressAVP(Version version) {
		return AVP_ORIGINATOR_RECEIVED_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_ORIGINATOR_INTERFACE = new DiameterAVPDefinition("Originator-Interface", 2009, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Originator-Interface AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOriginatorInterfaceAVP(Version version) {
		return AVP_ORIGINATOR_INTERFACE;
	}

	private final static DiameterAVPDefinition AVP_OUTGOING_TRUNK_GROUP_ID = new DiameterAVPDefinition("Outgoing-Trunk-Group-Id", 853, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Originator-Trunk-Group-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getOutgoingTrunkGroupIdAVP(Version version) {
		return AVP_OUTGOING_TRUNK_GROUP_ID;
	}

	private final static DiameterAVPDefinition AVP_PARTICIPANT_ACCESS_PRIORITY = new DiameterAVPDefinition("Participant-Access-Priority", 1259, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Participant-Access-Priority AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getParticipantAccessPriorityAVP(Version version) {
		return AVP_PARTICIPANT_ACCESS_PRIORITY;
	}

	private final static DiameterAVPDefinition AVP_PARTICIPANT_ACTION_TYPE = new DiameterAVPDefinition("Participant-Action-Type", 2049, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Participant-Action-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getParticipantActionTypeAVP(Version version) {
		return AVP_PARTICIPANT_ACTION_TYPE;
	}

	private final static DiameterAVPDefinition AVP_PARTICIPANT_GROUP = new DiameterAVPDefinition("Participant-Group", 1260, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Participant-Group AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getParticipantGroupAVP(Version version) {
		return AVP_PARTICIPANT_GROUP;
	}

	private final static DiameterAVPDefinition AVP_PARTICIPANTS_INVOLVED = new DiameterAVPDefinition("Participants-Involved", 887, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Participants-Involved AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getParticipantsInvolvedAVP(Version version) {
		return AVP_PARTICIPANTS_INVOLVED;
	}

	private final static DiameterAVPDefinition AVP_PDG_ADDRESS = new DiameterAVPDefinition("PDG-Address", 895, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the PDG-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPdgAddressAVP(Version version) {
		return AVP_PDG_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_PDG_CHARGING_ID = new DiameterAVPDefinition("PDG-Charging-Id", 896, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the PDG-Charging-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPdgChargingIdAVP(Version version) {
		return AVP_PDG_CHARGING_ID;
	}

	private final static DiameterAVPDefinition AVP_PDP_ADDRESS = new DiameterAVPDefinition("PDP-Address", 1227, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the PDP-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPdpAddressAVP(Version version) {
		return AVP_PDP_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_PDP_CONTEXT_TYPE = new DiameterAVPDefinition("PDP-Context-Type", 1247, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PDP-Context-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPdpContextTypeAVP(Version version) {
		return AVP_PDP_CONTEXT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_POC_CHANGE_CONDITION = new DiameterAVPDefinition("PoC-Change-Condition", 1261, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Change-Condition AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocChangeConditionAVP(Version version) {
		return AVP_POC_CHANGE_CONDITION;
	}

	private final static DiameterAVPDefinition AVP_POC_CHANGE_TIME = new DiameterAVPDefinition("PoC-Change-Time", 1262, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Change-Condition AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocChangeTimeAVP(Version version) {
		return AVP_POC_CHANGE_TIME;
	}

	private final static DiameterAVPDefinition AVP_POC_CONTROLLING_ADDRESS = new DiameterAVPDefinition("PoC-Controlling-Address", 858, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Controling-Address AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocControllingAddressAVP(Version version) {
		return AVP_POC_CONTROLLING_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_POC_EVENT_TYPE = new DiameterAVPDefinition("PoC-Event-Type", 2025, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Event-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocEventTypeAVP(Version version) {
		return AVP_POC_EVENT_TYPE;
	}

	private final static DiameterAVPDefinition AVP_POC_GROUP_NAME = new DiameterAVPDefinition("PoC-Group-Name", 859, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Group-Name AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocGroupNameAVP(Version version) {
		return AVP_POC_GROUP_NAME;
	}

	private final static DiameterAVPDefinition AVP_POC_INFORMATION = new DiameterAVPDefinition("PoC-Information", 879, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocInformationAVP(Version version) {
		return AVP_POC_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_POC_SERVER_ROLE = new DiameterAVPDefinition("PoC-Server-Role", 883, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Server-Role AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocServerRoleAVP(Version version) {
		return AVP_POC_SERVER_ROLE;
	}

	private final static DiameterAVPDefinition AVP_POC_SESSION_ID = new DiameterAVPDefinition("PoC-Session-Id", 1229, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Session-Id AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocSessionIdAVP(Version version) {
		return AVP_POC_SESSION_ID;
	}

	private final static DiameterAVPDefinition AVP_POC_SESSION_INITIATION_TYPE = new DiameterAVPDefinition("PoC-Session-Initiation-Type", 1277, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Session-Initiation-Type AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocSessionInitiationTypeAVP(Version version) {
		return AVP_POC_SESSION_INITIATION_TYPE;
	}

	private final static DiameterAVPDefinition AVP_POC_SESSION_TYPE = new DiameterAVPDefinition("PoC-Session-Type", 884, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-Session-Type AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocSessionTypeAVP(Version version) {
		return AVP_POC_SESSION_TYPE;
	}

	private final static DiameterAVPDefinition AVP_POC_USER_ROLE = new DiameterAVPDefinition("PoC-User-Role", 1252, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-User-Role AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocUserRoleAVP(Version version) {
		return AVP_POC_USER_ROLE;
	}

	private final static DiameterAVPDefinition AVP_POC_USER_ROLE_IDS = new DiameterAVPDefinition("PoC-User-Role-IDs", 1253, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the PoC-User-Role-IDs AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocUserRoleIdsAVP(Version version) {
		return AVP_POC_USER_ROLE_IDS;
	}

	private final static DiameterAVPDefinition AVP_POC_USER_ROLE_INFO_UNITS = new DiameterAVPDefinition("PoC-User-Role-Info-Units", 1254, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PoC-User-Role-Info-Units AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPocUserRoleInfoUnitsAVP(Version version) {
		return AVP_POC_USER_ROLE_INFO_UNITS;
	}

	private final static DiameterAVPDefinition AVP_POSITIONING_DATA = new DiameterAVPDefinition("Positioning-Data", 1245, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Positioning-Data AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPositioningDataAVP(Version version) {
		return AVP_POSITIONING_DATA;
	}

	private final static DiameterAVPDefinition AVP_PRIORITY = new DiameterAVPDefinition("Priority", 1209, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Priority AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPriorityAVP(Version version) {
		return AVP_PRIORITY;
	}

	private final static DiameterAVPDefinition AVP_PS_APPEND_FREE_FORMAT_DATA = new DiameterAVPDefinition("PS-Append-Free-Format-Data", 867, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the PS-Append-Free-Format-Data AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPsAppendFreeFormatDataAVP(Version version) {
		return AVP_PS_APPEND_FREE_FORMAT_DATA;
	}

	private final static DiameterAVPDefinition AVP_PS_FREE_FORMAT_DATA = new DiameterAVPDefinition("PS-Free-Format-Data", 866, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the PS-Free-Format-Data AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPsFreeFormatDataAVP(Version version) {
		return AVP_PS_FREE_FORMAT_DATA;
	}

	private final static DiameterAVPDefinition AVP_PS_FURNISH_CHARGING_INFORMATION = new DiameterAVPDefinition("PS-Furnish-Charging-Information", 865, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the PS-Furnish-Charging-Information AVP definition according to the
	 * version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPsFurnishChargingInformationAVP(Version version) {
		return AVP_PS_FURNISH_CHARGING_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_PS_INFORMATION = new DiameterAVPDefinition("PS-Information", 874, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the PS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPsInformationAVP(Version version) {
		return AVP_PS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_QOS_INFORMATION = new DiameterAVPDefinition("QoS-Information", 1016, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the QoS-Information AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getQosInformationAVP(Version version) {
		return AVP_QOS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_QUOTA_CONSUMPTION_TIME = new DiameterAVPDefinition("Quota-Consumption-Time", 881, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Quota-Consumption-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getQuotaConsumptionTimeAVP(Version version) {
		return AVP_QUOTA_CONSUMPTION_TIME;
	}

	private final static DiameterAVPDefinition AVP_QUOTA_HOLDING_TIME = new DiameterAVPDefinition("Quota-Holding-Time", 871, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Quota-Holding-Time AVP definition according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getQuotaHoldingTimeAVP(Version version) {
		return AVP_QUOTA_HOLDING_TIME;
	}

	private final static DiameterAVPDefinition AVP_RAI = new DiameterAVPDefinition("RAI", 909, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the RAI according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRaiAVP(Version version) {
		return AVP_RAI;
	}

	private final static DiameterAVPDefinition AVP_RATE_ELEMENT = new DiameterAVPDefinition("Rate-Element", 2058, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Rate-Element according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRateElementAVP(Version version) {
		return AVP_RATE_ELEMENT;
	}

	private final static DiameterAVPDefinition AVP_READ_REPLY_REPORT_REQUESTED = new DiameterAVPDefinition("Read-Reply-Report-Requested", 1222, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Read-Reply-Report-Requested according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReadReplyReportRequestedAVP(Version version) {
		return AVP_READ_REPLY_REPORT_REQUESTED;
	}

	private final static DiameterAVPDefinition AVP_RECEIVED_TALK_BURST_TIME = new DiameterAVPDefinition("Received-Talk-Burst-Time", 1284, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Received-Talk-Burst-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReceivedTalkBurstTimeAVP(Version version) {
		return AVP_RECEIVED_TALK_BURST_TIME;
	}

	private final static DiameterAVPDefinition AVP_RECEIVED_TALK_BURST_VOLUME = new DiameterAVPDefinition("Received-Talk-Burst-Volume", 1285, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Received-Talk-Burst-Volume according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReceivedTalkBurstVolumeAVP(Version version) {
		return AVP_RECEIVED_TALK_BURST_VOLUME;
	}

	private final static DiameterAVPDefinition AVP_RECIPIENT_ADDRESS = new DiameterAVPDefinition("Recipient-Address", 1201, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Recipient-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRecipientAddressAVP(Version version) {
		return AVP_RECIPIENT_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_RECIPIENT_INFO = new DiameterAVPDefinition("Recipient-Info", 2026, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Recipient-Info according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRecipientInfoAVP(Version version) {
		return AVP_RECIPIENT_INFO;
	}

	private final static DiameterAVPDefinition AVP_RECIPIENT_RECEIVED_ADDRESS = new DiameterAVPDefinition("Recipient-Received-Address", 2028, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Recipient-Received-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRecipientReceivedAddressAVP(Version version) {
		return AVP_RECIPIENT_RECEIVED_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_RECIPIENT_SCCP_ADDRESS = new DiameterAVPDefinition("Recipient-SCCP-Address", 2010, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the Recipient-SCCP-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRecipientSccpAddressAVP(Version version) {
		return AVP_RECIPIENT_SCCP_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_REFUND_INFORMATION = new DiameterAVPDefinition("Refund-Information", 2022, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Refund-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRefundInformationAVP(Version version) {
		return AVP_REFUND_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_REMAINING_BALANCE = new DiameterAVPDefinition("Remaining-Balance", 2021, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Remaining-Balance according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRemainingBalanceAVP(Version version) {
		return AVP_REMAINING_BALANCE;
	}

	private final static DiameterAVPDefinition AVP_REPLY_APPLIC_ID = new DiameterAVPDefinition("Reply-Applic-ID", 1223, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Reply-Applic-ID according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReplyApplicIdAVP(Version version) {
		return AVP_REPLY_APPLIC_ID;
	}

	private final static DiameterAVPDefinition AVP_REPLY_PATH_REQUESTED = new DiameterAVPDefinition("Reply-Path-Requested", 2011, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Reply-Path-Requested according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReplyPathRequestedAVP(Version version) {
		return AVP_REPLY_PATH_REQUESTED;
	}

	private final static DiameterAVPDefinition AVP_REPORTING_REASON = new DiameterAVPDefinition("Reporting-Reason", 872, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Reporting-Reason according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getReportingReasonAVP(Version version) {
		return AVP_REPORTING_REASON;
	}

	private final static DiameterAVPDefinition AVP_REQUESTED_PARTY_ADDRESS = new DiameterAVPDefinition("Requested-Party-Address", 1251, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Requested-Party-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRequestedPartyAddressAVP(Version version) {
		return AVP_REQUESTED_PARTY_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_REQUIRED_MBMS_BEARER_CAPABILITIES = new DiameterAVPDefinition("Required-MBMS-Bearer-Capabilities", 901, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Required-MBMS-Bearer-Capabilities according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRequiredMbmsBearerCapabilitiesAVP(Version version) {
		return AVP_REQUIRED_MBMS_BEARER_CAPABILITIES;
	}

	private final static DiameterAVPDefinition AVP_ROLE_OF_NODE = new DiameterAVPDefinition("Role-Of-Node", 829, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Role-Of-Node according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getRoleOfNodeAVP(Version version) {
		return AVP_ROLE_OF_NODE;
	}

	private final static DiameterAVPDefinition AVP_SCALE_FACTOR = new DiameterAVPDefinition("Scale-Factor", 2059, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Scale-Factor according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getScaleFactorAVP(Version version) {
		return AVP_SCALE_FACTOR;
	}

	private final static DiameterAVPDefinition AVP_SDP_ANSWER_TIMESTAMP = new DiameterAVPDefinition("SDP-Answer-Timestamp", 1275, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Answer-Timestamp according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpAnswerTimestampAVP(Version version) {
		return AVP_SDP_ANSWER_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_SDP_MEDIA_COMPONENT = new DiameterAVPDefinition("SDP-Media-Component", 843, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Media-Component according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpMediaComponentAVP(Version version) {
		return AVP_SDP_MEDIA_COMPONENT;
	}

	private final static DiameterAVPDefinition AVP_SDP_MEDIA_DESCRIPTION = new DiameterAVPDefinition("SDP-Media-Description", 845, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Media-Description according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpMediaDescriptionAVP(Version version) {
		return AVP_SDP_MEDIA_DESCRIPTION;
	}

	private final static DiameterAVPDefinition AVP_SDP_MEDIA_NAME = new DiameterAVPDefinition("SDP-Media-Name", 844, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Media-Name according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpMediaNameAVP(Version version) {
		return AVP_SDP_MEDIA_NAME;
	}

	private final static DiameterAVPDefinition AVP_SDP_OFFER_TIMESTAMP = new DiameterAVPDefinition("SDP-Offer-Timestamp", 1274, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Offer-Timestamp according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpOfferTimestampAVP(Version version) {
		return AVP_SDP_OFFER_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_SDP_SESSION_DESCRIPTION = new DiameterAVPDefinition("SDP-Session-Description", 842, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Session-Description according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpSessionDescriptionAVP(Version version) {
		return AVP_SDP_SESSION_DESCRIPTION;
	}

	private final static DiameterAVPDefinition AVP_SDP_TIMESTAMPS = new DiameterAVPDefinition("SDP-Timestamps", 1274, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Timestamps according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpTimestampsAVP(Version version) {
		return AVP_SDP_TIMESTAMPS;
	}

	private final static DiameterAVPDefinition AVP_SDP_TYPE = new DiameterAVPDefinition("SDP-Type", 2036, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the SDP-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSdpTypeAVP(Version version) {
		return AVP_SDP_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SERVED_PARTY_IP_ADRESS = new DiameterAVPDefinition("Served-Party-IP-Address", 848, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the Served-Party-IP-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServedPartyIpAddressAVP(Version version) {
		return AVP_SERVED_PARTY_IP_ADRESS;
	}

	/**
	 * Gets the Server-Capabilities according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServerCapabilitiesAVP(Version version) {
		return CxUtils.getServerCapabilitiesAVP(getVersion29229(version));
	}

	/**
	 * Gets the Server-Name according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServerNameAVP(Version version) {
		return CxUtils.getServerNameAVP(getVersion29229(version));
	}

	private final static DiameterAVPDefinition AVP_SERVICE_DATA_CONTAINER = new DiameterAVPDefinition("Service-Data-Container", 2040, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Service-Data-Container according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceDataContainerAVP(Version version) {
		return AVP_SERVICE_DATA_CONTAINER;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_GENERIC_INFORMATION = new DiameterAVPDefinition("Service-Generic-Information", 1256, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Service-Generic-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceGenericInformationAVP(Version version) {
		return AVP_SERVICE_GENERIC_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_ID = new DiameterAVPDefinition("Service-Id", 855, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Service-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceIdAVP(Version version) {
		return AVP_SERVICE_ID;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_INFORMATION = new DiameterAVPDefinition("Service-Information", 873, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Service-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceInformationAVP(Version version) {
		return AVP_SERVICE_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_MODE = new DiameterAVPDefinition("Service-Mode", 2032, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Service-Mode according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceModeAVP(Version version) {
		return AVP_SERVICE_MODE;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_SPECIFIC_DATA = new DiameterAVPDefinition("Service-Specific-Data", 863, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Service-Specific-Data according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceSpecificDataAVP(Version version) {
		return AVP_SERVICE_SPECIFIC_DATA;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_SPECIFIC_INFO = new DiameterAVPDefinition("Service-Specific-Info", 1249, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Service-Specific-Info according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceSpecificInfoAVP(Version version) {
		return AVP_SERVICE_SPECIFIC_INFO;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_SPECIFIC_TYPE = new DiameterAVPDefinition("Service-Specific-Type", 1257, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Service-Specific-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceSpecificTypeAVP(Version version) {
		return AVP_SERVICE_SPECIFIC_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SERVING_NODE_TYPE = new DiameterAVPDefinition("Serving-Node-Type", 2047, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Serving-Node-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServingNodeTypeAVP(Version version) {
		return AVP_SERVING_NODE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SERVICE_TYPE = new DiameterAVPDefinition("Service-Type", 2031, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Service-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getServiceTypeAVP(Version version) {
		return AVP_SERVICE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SGSN_ADDRESS = new DiameterAVPDefinition("SGSN-Address", 1228, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the SGSN-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSgsnAddressAVP(Version version) {
		return AVP_SGSN_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_SIP_METHOD = new DiameterAVPDefinition("SIP-Method", 824, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the SIP-Method according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipMethodAVP(Version version) {
		return AVP_SIP_METHOD;
	}

	private final static DiameterAVPDefinition AVP_SIP_REQUEST_TIMESTAMP = new DiameterAVPDefinition("SIP-Request-Timestamp", 834, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SIP_REQUEST_TIMESTAMP_OLD = new DiameterAVPDefinition("SIP-Request-Timestamp", 834, ChargingUtils.THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);

	/**
	 * Gets the SIP-Request-Timestamp according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipRequestTimestampAVP(Version version) {
		if (version.compareTo(VERSION_6_6) < 0) {
			return AVP_SIP_REQUEST_TIMESTAMP_OLD;
		} else if (version.getMajor() == 7 && version.compareTo(VERSION_7_1) < 0) {
			return AVP_SIP_REQUEST_TIMESTAMP_OLD;
		}
		return AVP_SIP_REQUEST_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_SIP_RESPONSE_TIMESTAMP = new DiameterAVPDefinition("SIP-Response-Timestamp", 835, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);
	private final static DiameterAVPDefinition AVP_SIP_RESPONSE_TIMESTAMP_OLD = new DiameterAVPDefinition("SIP-Response-Timestamp", 835, ChargingUtils.THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, false);

	/**
	 * Gets the SIP-Response-Timestamp according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSipResponseTimestampAVP(Version version) {
		if (version.compareTo(VERSION_6_6) < 0) {
			return AVP_SIP_RESPONSE_TIMESTAMP_OLD;
		} else if (version.getMajor() == 7 && version.compareTo(VERSION_7_1) < 0) {
			return AVP_SIP_RESPONSE_TIMESTAMP_OLD;
		}
		return AVP_SIP_RESPONSE_TIMESTAMP;
	}

	private final static DiameterAVPDefinition AVP_SM_DISCHARGE_TIME = new DiameterAVPDefinition("SM-Discharge-Time", 2012, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the SM-Discharge-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmDischargeTimeAVP(Version version) {
		return AVP_SM_DISCHARGE_TIME;
	}

	private final static DiameterAVPDefinition AVP_SM_MESSAGE_TYPE = new DiameterAVPDefinition("SM-Message-Type", 2007, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the SM-Message-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmMessageTypeAVP(Version version) {
		return AVP_SM_MESSAGE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SM_PROTOCOL_ID = new DiameterAVPDefinition("SM-Protocol-ID", 2013, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the SM-Protocol-ID according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmProtocolIdAVP(Version version) {
		return AVP_SM_PROTOCOL_ID;
	}

	private final static DiameterAVPDefinition AVP_SMSC_ADDRESS = new DiameterAVPDefinition("SMSC-Address", 2017, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the SMSC-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmscAddressAVP(Version version) {
		return AVP_SMSC_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_SMS_INFORMATION = new DiameterAVPDefinition("SMS-Information", 2000, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the SMS-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmsInformationAVP(Version version) {
		return AVP_SMS_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_SMS_NODE = new DiameterAVPDefinition("SMS-Node", 2016, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the SMS-Node according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmsNodeAVP(Version version) {
		return AVP_SMS_NODE;
	}

	private final static DiameterAVPDefinition AVP_SM_SERVICE_TYPE = new DiameterAVPDefinition("SM-Service-Type", 2029, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the SM-Service-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmServiceTypeAVP(Version version) {
		return AVP_SM_SERVICE_TYPE;
	}

	private final static DiameterAVPDefinition AVP_SM_STATUS = new DiameterAVPDefinition("SM-Status", 2014, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the SM-Status according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmStatusAVP(Version version) {
		return AVP_SM_STATUS;
	}

	private final static DiameterAVPDefinition AVP_SM_USER_DATA_HEADER = new DiameterAVPDefinition("SM-User-Data-Header", 2015, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the SM-User-Data-Header according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSmUserDataHeaderAVP(Version version) {
		return AVP_SM_USER_DATA_HEADER;
	}

	private final static DiameterAVPDefinition AVP_START_TIME = new DiameterAVPDefinition("Start-Time", 2041, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Start-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getStartTimeAVP(Version version) {
		return AVP_START_TIME;
	}

	private final static DiameterAVPDefinition AVP_STOP_TIME = new DiameterAVPDefinition("Stop-Time", 2042, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Stop-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getStopTimeAVP(Version version) {
		return AVP_STOP_TIME;
	}

	private final static DiameterAVPDefinition AVP_SUBMISSION_TIME = new DiameterAVPDefinition("Submission-Time", 1202, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Submission-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSubmissionTimeAVP(Version version) {
		return AVP_SUBMISSION_TIME;
	}

	private final static DiameterAVPDefinition AVP_SUBSCRIBER_ROLE = new DiameterAVPDefinition("Subscriber-Role", 2033, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Subscriber-Role according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSubscriberRoleAVP(Version version) {
		return AVP_SUBSCRIBER_ROLE;
	}

	private final static DiameterAVPDefinition AVP_SUPPLEMENTARY_SERVICE = new DiameterAVPDefinition("Supplementary-Service", 2048, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Supplementary-Service according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSupplementaryServiceAVP(Version version) {
		return AVP_SUPPLEMENTARY_SERVICE;
	}

	private final static DiameterAVPDefinition AVP_TALK_BURST_EXCHANGE = new DiameterAVPDefinition("Talk-Burst-Exchange", 1255, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Talk-Burst-Exchange according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTalkBurstExchangeAVP(Version version) {
		return AVP_TALK_BURST_EXCHANGE;
	}

	private final static DiameterAVPDefinition AVP_TALK_BURST_TIME = new DiameterAVPDefinition("Talk-Burst-Time", 1286, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Talk-Burst-Time according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTalkBurstTimeAVP(Version version) {
		return AVP_TALK_BURST_TIME;
	}

	private final static DiameterAVPDefinition AVP_TALK_BURST_VOLUME = new DiameterAVPDefinition("Talk-Burst-Volume", 1287, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Talk-Burst-Volume according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTalkBurstVolumeAVP(Version version) {
		return AVP_TALK_BURST_VOLUME;
	}

	private final static DiameterAVPDefinition AVP_TARIFF_INFORMATION = new DiameterAVPDefinition("Tariff-Information", 2060, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Tariff-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTariffInformationAVP(Version version) {
		return AVP_TARIFF_INFORMATION;
	}

	/**
	 * Found in TS 29.272
	 */
	private final static DiameterAVPDefinition AVP_TERMINAL_INFORMATION = new DiameterAVPDefinition("Terminal-Information", 1401, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Terminal-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTerminalInformationAVP(Version version) {
		return AVP_TERMINAL_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_SOFTWARE_VERSION = new DiameterAVPDefinition("Software-Version", 1403, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Software-Version according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getSoftwareVersionAVP(Version version) {
		return AVP_SOFTWARE_VERSION;
	}

	private final static DiameterAVPDefinition AVP_IMEI = new DiameterAVPDefinition("IMEI", 1402, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the IMEI according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getImeiAVP(Version version) {
		return AVP_IMEI;
	}
	private final static DiameterAVPDefinition AVP_3GPP2_MEID = new DiameterAVPDefinition("3GPP2-MEID", 1471, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the 3GPP2-MEID according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition get3gpp2MeidAVP(Version version) {
		return AVP_3GPP2_MEID;
	}

	private final static DiameterAVPDefinition AVP_TERMINATING_IOI = new DiameterAVPDefinition("Terminating-IOI", 840, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Terminating-IOI according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTerminatingIoiAVP(Version version) {
		return AVP_TERMINATING_IOI;
	}

	private final static DiameterAVPDefinition AVP_TIME_FIRST_USAGE = new DiameterAVPDefinition("Time-First-Usage", 2043, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Time-First-Usage according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeFirstUsageAVP(Version version) {
		return AVP_TIME_FIRST_USAGE;
	}

	private final static DiameterAVPDefinition AVP_TIME_LAST_USAGE = new DiameterAVPDefinition("Time-Last-Usage", 2044, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE, true);

	/**
	 * Gets the Time-Last-Usage according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeLastUsageAVP(Version version) {
		return AVP_TIME_LAST_USAGE;
	}

	private final static DiameterAVPDefinition AVP_TIME_QUOTA_MECHANISM = new DiameterAVPDefinition("Time-Quota-Mechanism", 1270, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Time-Quota-Mechanism according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeQuotaMechanismAVP(Version version) {
		return AVP_TIME_QUOTA_MECHANISM;
	}

	private final static DiameterAVPDefinition AVP_TIME_QUOTA_THRESHOLD = new DiameterAVPDefinition("Time-Quota-Threshold", 868, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Time-Quota-Threshold according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeQuotaThresholdAVP(Version version) {
		return AVP_TIME_QUOTA_THRESHOLD;
	}

	private final static DiameterAVPDefinition AVP_TIME_QUOTA_TYPE = new DiameterAVPDefinition("Time-Quota-Type", 1271, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Time-Quota-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeQuotaTypeAVP(Version version) {
		return AVP_TIME_QUOTA_TYPE;
	}

	private final static DiameterAVPDefinition AVP_TIME_STAMPS = new DiameterAVPDefinition("Time-Stamps", 833, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Time-Stamps according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeStampsAVP(Version version) {
		return AVP_TIME_STAMPS;
	}

	private final static DiameterAVPDefinition AVP_TIME_USAGE = new DiameterAVPDefinition("Time-Usage", 2045, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Time-Usage according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTimeUsageAVP(Version version) {
		return AVP_TIME_USAGE;
	}

	private final static DiameterAVPDefinition AVP_TMGI = new DiameterAVPDefinition("TMGI", 900, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the TMGI according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTmgiAVP(Version version) {
		return AVP_TMGI;
	}

	private final static DiameterAVPDefinition AVP_TOKEN_TEXT = new DiameterAVPDefinition("Token-Text", 1215, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the Token-Text according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTokenTextAVP(Version version) {
		return AVP_TOKEN_TEXT;
	}

	private final static DiameterAVPDefinition AVP_TOTAL_NUMBER_OF_MESSAGES_EXPLODED = new DiameterAVPDefinition("Total-Number-Of-Messages-Exploded", 2113, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Total-Number-Of-Messages-Exploded according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTotalNumberOfMessagesExplodedAVP(Version version) {
		return AVP_TOTAL_NUMBER_OF_MESSAGES_EXPLODED;
	}

	private final static DiameterAVPDefinition AVP_TOTAL_NUMBER_OF_MESSAGES_SENT = new DiameterAVPDefinition("Total-Number-Of-Messages-Sent", 2114, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Total-Number-Of-Messages-Sent according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTotalNumberOfMessagesSentAVP(Version version) {
		return AVP_TOTAL_NUMBER_OF_MESSAGES_SENT;
	}

	private final static DiameterAVPDefinition AVP_TRAFFIC_DATA_VOLUME = new DiameterAVPDefinition("Traffic-Data-Volume", 2046, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Traffic-Data-Volume according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTrafficDataVolumesAVP(Version version) {
		return AVP_TRAFFIC_DATA_VOLUME;
	}

	private final static DiameterAVPDefinition AVP_TRIGGER = new DiameterAVPDefinition("Trigger", 1264, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Trigger according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTriggerAVP(Version version) {
		return AVP_TRIGGER;
	}

	private final static DiameterAVPDefinition AVP_TRIGGER_TYPE = new DiameterAVPDefinition("Trigger-Type", 870, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Trigger-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTriggerTypeAVP(Version version) {
		return AVP_TRIGGER_TYPE;
	}

	private final static DiameterAVPDefinition AVP_TRUNK_GROUP_ID = new DiameterAVPDefinition("Trunk-Group-Id", 851, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Trunk-Group-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTrunkGroupIdAVP(Version version) {
		return AVP_TRUNK_GROUP_ID;
	}

	private final static DiameterAVPDefinition AVP_TYPE_NUMBER = new DiameterAVPDefinition("Type-Number", 1204, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Type-Number according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getTypeNumberAVP(Version version) {
		return AVP_TYPE_NUMBER;
	}

	private final static DiameterAVPDefinition AVP_UNIT_COST = new DiameterAVPDefinition("Unit-Cost", 2061, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Unit-Cost according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUnitCostAVP(Version version) {
		return AVP_UNIT_COST;
	}

	private final static DiameterAVPDefinition AVP_UNIT_QUOTA_THRESHOLD = new DiameterAVPDefinition("Unit-Quota-Threshold", 1226, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Unit-Quota-Threshold according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUnitQuotaThresholdAVP(Version version) {
		return AVP_UNIT_QUOTA_THRESHOLD;
	}

	/**
	 * Gets the User-Data according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserDataAVP(Version version) {
		return CxUtils.getUserDataAVP(getVersion29229(version));
	}

	private final static DiameterAVPDefinition AVP_USER_PARTICIPATING_TYPE = new DiameterAVPDefinition("User-Participating-Type", 1279, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the User-Participating-Type according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserParticipatingTypeAVP(Version version) {
		return AVP_USER_PARTICIPATING_TYPE;
	}

	private final static DiameterAVPDefinition AVP_USER_SESSION_ID = new DiameterAVPDefinition("User-Session-Id", 830, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the User-Session-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getUserSessionIdAVP(Version version) {
		return AVP_USER_SESSION_ID;
	}

	private final static DiameterAVPDefinition AVP_VAS_ID = new DiameterAVPDefinition("VAS-Id", 1102, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the VAS-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getVasIdAVP(Version version) {
		return AVP_VAS_ID;
	}

	private final static DiameterAVPDefinition AVP_VASP_ID = new DiameterAVPDefinition("VASP-Id", 1103, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the VASP-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getVaspIdAVP(Version version) {
		return AVP_VASP_ID;
	}

	private final static DiameterAVPDefinition AVP_VOLUME_QUOTA_THRESHOLD = new DiameterAVPDefinition("Volume-Quota-Threshold", 869, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Volume-Quota-Threshold according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getVolumeQuotaThresholdAVP(Version version) {
		return AVP_VOLUME_QUOTA_THRESHOLD;
	}

	private final static DiameterAVPDefinition AVP_WAG_ADDRESS = new DiameterAVPDefinition("WAG-Address", 890, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the WAG-Address according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWagAddressAVP(Version version) {
		return AVP_WAG_ADDRESS;
	}

	private final static DiameterAVPDefinition AVP_WAG_PLMN_ID = new DiameterAVPDefinition("WAG-PLMN-Id", 891, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the WAG-PLMN-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWagPlmnIdAVP(Version version) {
		return AVP_WAG_PLMN_ID;
	}

	private final static DiameterAVPDefinition AVP_WLAN_INFORMATION = new DiameterAVPDefinition("WLAN-Information", 875, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the WLAN-Information according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWlanInformationAVP(Version version) {
		return AVP_WLAN_INFORMATION;
	}

	private final static DiameterAVPDefinition AVP_WLAN_RADIO_CONTAINER = new DiameterAVPDefinition("WLAN-Radio-Container", 892, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the WLAN-Radio-Container according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWlanRadioContainerAVP(Version version) {
		return AVP_WLAN_RADIO_CONTAINER;
	}

	private final static DiameterAVPDefinition AVP_WLAN_SESSION_ID = new DiameterAVPDefinition("WLAN-Session-Id", 1246, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE, true);

	/**
	 * Gets the WLAN-Session-Id according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWlanSessionIdAVP(Version version) {
		return AVP_WLAN_SESSION_ID;
	}

	private final static DiameterAVPDefinition AVP_WLAN_TECHNOLOGY = new DiameterAVPDefinition("WLAN-Technology", 893, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the WLAN-Technology according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWlanTechnologyAVP(Version version) {
		return AVP_WLAN_TECHNOLOGY;
	}

	private final static DiameterAVPDefinition AVP_WLAN_UE_LOCAL_IPADDRESS = new DiameterAVPDefinition("WLAN-UE-Local-IPAddress", 894, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE, true);

	/**
	 * Gets the WLAN-UE-Local-IPAddress according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getWlanUeLocalIpaddressAVP(Version version) {
		return AVP_WLAN_UE_LOCAL_IPADDRESS;
	}

	// 29.214 needed AVPs

	private final static DiameterAVPDefinition AVP_MEDIA_COMPONENT_NUMBER = new DiameterAVPDefinition("Media-Component-Number", 518, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Media-Component-Number according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMediaComponentNumberAVP(Version version) {
		return AVP_MEDIA_COMPONENT_NUMBER;
	}

	private final static DiameterAVPDefinition AVP_FLOW_NUMBER = new DiameterAVPDefinition("Flow-Number", 509, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Flow-Number according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getFlowNumberAVP(Version version) {
		return AVP_FLOW_NUMBER;
	}

	private final static DiameterAVPDefinition AVP_MAX_REQUESTED_BANDWIDTH_UL = new DiameterAVPDefinition("Max-Requested-Bandwidth-UL", 516, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Max-Requested-Bandwidth-UL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMaxRequestedBandwidthULAVP(Version version) {
		return AVP_MAX_REQUESTED_BANDWIDTH_UL;
	}

	private final static DiameterAVPDefinition AVP_MAX_REQUESTED_BANDWIDTH_DL = new DiameterAVPDefinition("Max-Requested-Bandwidth-DL", 515, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Max-Requested-Bandwidth-DL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getMaxRequestedBandwidthDLAVP(Version version) {
		return AVP_MAX_REQUESTED_BANDWIDTH_DL;
	}

	// 29.212 needed AVPs

	private final static DiameterAVPDefinition AVP_QOS_CLASS_IDENTIFIER = new DiameterAVPDefinition("QoS-Class-Identifier", 1028, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the QoS-Class-Identifier according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getQoSClassIdentifierAVP(Version version) {
		return AVP_QOS_CLASS_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_ALLOCATION_RETENTION_PRIORITY = new DiameterAVPDefinition("Allocation-Retention-Priority", 1034, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE, true);

	/**
	 * Gets the Allocation-Retention-Priority according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getAllocationRetentionPriorityAVP(Version version) {
		return AVP_ALLOCATION_RETENTION_PRIORITY;
	}

	private final static DiameterAVPDefinition AVP_APN_AGGREGATE_MAX_BITRATE_DL = new DiameterAVPDefinition("APN-Aggregate-Max-Bitrate-DL", 1040, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the APN-Aggregate-Max-Bitrate-DL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApnAggregateMaxBitrateDLAVP(Version version) {
		return AVP_APN_AGGREGATE_MAX_BITRATE_DL;
	}

	private final static DiameterAVPDefinition AVP_APN_AGGREGATE_MAX_BITRATE_UL = new DiameterAVPDefinition("APN-Aggregate-Max-Bitrate-UL", 1041, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the APN-Aggregate-Max-Bitrate-UL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getApnAggregateMaxBitrateULAVP(Version version) {
		return AVP_APN_AGGREGATE_MAX_BITRATE_UL;
	}

	private final static DiameterAVPDefinition AVP_BEARER_IDENTIFIER = new DiameterAVPDefinition("Bearer-Identifier", 1020, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, OctetStringFormat.INSTANCE, true);

	/**
	 * Gets the Bearer-Identifier according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getBearerIdentifierAVP(Version version) {
		return AVP_BEARER_IDENTIFIER;
	}

	private final static DiameterAVPDefinition AVP_GARANTEED_BITRATE_DL = new DiameterAVPDefinition("Garanteed-Bitrate-DL", 1025, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Garanteed-Bitrate-DL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGaranteedBitrateDLAVP(Version version) {
		return AVP_GARANTEED_BITRATE_DL;
	}

	private final static DiameterAVPDefinition AVP_GARANTEED_BITRATE_UL = new DiameterAVPDefinition("Garanteed-Bitrate-UL", 1026, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Garanteed-Bitrate-UL according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getGaranteedBitrateULAVP(Version version) {
		return AVP_GARANTEED_BITRATE_UL;
	}

	private final static DiameterAVPDefinition AVP_PRE_EMPTION_CAPABILITY = new DiameterAVPDefinition("Pre-emption-Capability", 1047, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Pre-Emption-Capability according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPreemptionCapabilityAVP(Version version) {
		return AVP_PRE_EMPTION_CAPABILITY;
	}

	private final static DiameterAVPDefinition AVP_PRE_EMPTION_VULNERABILITY = new DiameterAVPDefinition("Pre-emption-Vulnerability", 1048, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE, true);

	/**
	 * Gets the Pre-Emption-Vulnerability according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPreemptionVulnerabilityAVP(Version version) {
		return AVP_PRE_EMPTION_VULNERABILITY;
	}

	private final static DiameterAVPDefinition AVP_PRIORITY_LEVEL = new DiameterAVPDefinition("Priority-Level", 1046, THREEGPP_VENDOR_ID, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE, true);

	/**
	 * Gets the Priority-Level according to the version.
	 * 
	 * @param version The 3GPP 32.299 document version.
	 * @return The AVP or null if unknown for this version.
	 */
	public final static DiameterAVPDefinition getPriorityLevelAVP(Version version) {
		return AVP_PRIORITY_LEVEL;
	}

	/**
	 * Formats the given time (in milliseconds) in UTC format.
	 * <p>
	 * UTC time format is a complete date plus hours, minutes, seconds, a decimal
	 * fraction of seconds and a time zone designator : YYYY-MM-DDThh:mm:ss.sTZD<br/>
	 * where: <br/>
	 * <ul>
	 * <li>YYYY = four-digit year</li>
	 * <li>MM = two-digit month (01=January, etc.)</li>
	 * <li>DD = two-digit day of month (01 through 31)</li>
	 * <li>hh = two digits of hour (00 through 23) (am/pm NOT allowed)</li>
	 * <li>mm = two digits of minute (00 through 59)</li>
	 * <li>ss = two digits of second (00 through 59)</li>
	 * <li>s = one or more digits representing a decimal fraction of a second</li>
	 * <li>TZD = time zone designator (Z or +hh:mm or -hh:mm)</li>
	 * </p>
	 * 
	 * @param tsInMilliSeconds The timestamp to format
	 * @return The timestamp formated in UTC.
	 */
	public static String toUtcTimestamp(long tsInMilliSeconds) {
		StringBuffer buffer = new StringBuffer();
		buffer = FORMAT.format(tsInMilliSeconds, buffer, new FieldPosition(0));
		return buffer.toString();
	}

	/**
	 * Parses a date formatted as YYYY-MM-DDThh:mm:ss.sTZD.
	 * 
	 * @param timestamp The timestamp string.
	 * @return The date in milliseconds or -1L if the timestamp cannot be parsed.
	 */
	public static long fromUtcTimestamp(String timestamp) {
		Date date;
		long res;
		try {
			date = FORMAT.parse(timestamp);
			res = date.getTime();
		}
		catch (ParseException e) {
			res = -1L;
		}
		return res;
	}

	/**
	 * Copy the array to another new one.
	 * 
	 * @param src The source array.
	 * @return The copied array.
	 */
	public static byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

}