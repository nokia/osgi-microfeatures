// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.Hashtable;
import java.util.Map;

/**
 * Constants for charging.
 */
public class ChargingConstants {
	
	/**
	 * The  Command code for Credit Control.
	 */
  public static final int COMMAND_CC = 272;

	/**
	 * The OCF denies the service request due to service restrictions (e.g.
	 * terminate rating group) or limitations related to the end-user, for example
	 * the end-user's account could not cover the requested service.
	 */
	public final static long ERROR_DIAMETER_END_USER_SERVICE_DENIED = 4010;

	/**
	 * The OCF determines that the service can be granted to the end user but no
	 * further credit control needed for the service (e.g. service is free of
	 * charge or the PDP context is treated for offline charging).
	 */
	public final static long ERROR_DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE = 4011;

	/**
	 * The OCF denies the service request since the end- user's account could not
	 * cover the requested service. If the CCR contained used-service-units they
	 * are deducted, if possible.
	 */
	public final static long ERROR_DIAMETER_CREDIT_LIMIT_REACHED = 4012;

	/**
	 * The OCF denies the service request in order to terminate the service for
	 * which credit is requested. For example this error code is used to inform
	 * PDP Context has to be terminated in the CCR message or to inform blacklist
	 * the rating group in the Multiple-Service-Credit-Control AVP.
	 */
	public final static long ERROR_DIAMETER_AUTHORIZATION_REJECTED = 5003;

	/**
	 * The specified end user could not be found in the OCF.
	 */
	public final static long ERROR_DIAMETER_USER_UNKNOWN = 5030;

	/**
	 * This error code is used to inform the CTF that the OCF cannot rate the
	 * service request due to insufficient rating input, incorrect AVP combination
	 * or due to an AVP or an AVP value that is not recognized or supported in the
	 * rating. For Flow Based Charging this error code is used if the Rating group
	 * is not recognized. The Failed-AVP AVP MUST be included and contain a copy
	 * of the entire AVP(s) that could not be processed successfully or an example
	 * of the missing AVP complete with the Vendor-Id if applicable. The value
	 * field of the missing AVP should be of correct minimum length and contain
	 * zeroes.
	 */
	public final static long ERROR_DIAMETER_RATING_FAILED = 5031;

	/**
	 * The Accounting-Realtime-Required enumeration values.
	 * 
	 * See RFC 3588 section 9.8.7.
	 */
	public enum AccountingRealtimeRequired {
		DELIVER_AND_GRANT("DELIVER_AND_GRANT", 1),
		GRANT_AND_STORE("GRANT_AND_STORE", 2),
		GRANT_AND_LOSE("GRANT_AND_LOSE", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, AccountingRealtimeRequired> DATA = new Hashtable<Integer, AccountingRealtimeRequired>();
		static {
			for (AccountingRealtimeRequired obj : AccountingRealtimeRequired.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static AccountingRealtimeRequired getData(int value) {
			return DATA.get(value);
		}

		private AccountingRealtimeRequired(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Accounting-Record-Type enumeration values.
	 * 
	 * See RFC 3588 section 9.8.1.
	 */
	public enum AccountingRecordType {
		EVENT_RECORD("EVENT_RECORD", 1),
		START_RECORD("START_RECORD", 2),
		INTERIM_RECORD("INTERIM_RECORD", 3),
		STOP_RECORD("STOP_RECORD", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, AccountingRecordType> DATA = new Hashtable<Integer, AccountingRecordType>();
		static {
			for (AccountingRecordType obj : AccountingRecordType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static AccountingRecordType getData(int value) {
			return DATA.get(value);
		}

		private AccountingRecordType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Redirect-Host-Usage enumeration values.
	 * 
	 * See RFC 3588 section 6.13.
	 */
	public enum RedirectHostUsage {
		DONT_CACHE("DONT_CACHE", 0),
		ALL_SESSION("ALL_SESSION", 1),
		ALL_REALM("ALL_REALM", 2),
		REALM_AND_APPLICATION("REALM_AND_APPLICATION", 3),
		ALL_APPLICATION("ALL_APPLICATION", 4),
		ALL_HOST("ALL_HOST", 5),
		ALL_USER("ALL_USER", 6);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, RedirectHostUsage> DATA = new Hashtable<Integer, RedirectHostUsage>();
		static {
			for (RedirectHostUsage obj : RedirectHostUsage.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static RedirectHostUsage getData(int value) {
			return DATA.get(value);
		}

		private RedirectHostUsage(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Termination-Cause enumeration values.
	 * 
	 * See RFC 3588 section 8.15.
	 */
	public enum TerminationCause {
		DIAMETER_LOGOUT("DIAMETER_LOGOUT", 1),
		DIAMETER_SERVICE_NOT_PROVIDED("DIAMETER_SERVICE_NOT_PROVIDED", 2),
		DIAMETER_BAD_ANSWER("DIAMETER_BAD_ANSWER", 3),
		DIAMETER_ADMINISTRATIVE("DIAMETER_ADMINISTRATIVE", 4),
		DIAMETER_LINK_BROKEN("DIAMETER_LINK_BROKEN", 5),
		DIAMETER_AUTH_EXPIRED("DIAMETER_AUTH_EXPIRED", 6),
		DIAMETER_USER_MOVED("DIAMETER_USER_MOVED", 7),
		DIAMETER_SESSION_TIMEOUT("DIAMETER_SESSION_TIMEOUT", 8);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, TerminationCause> DATA = new Hashtable<Integer, TerminationCause>();
		static {
			for (TerminationCause obj : TerminationCause.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static TerminationCause getData(int value) {
			return DATA.get(value);
		}

		private TerminationCause(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The CC-Record-Type enumeration values.
	 * 
	 * See RFC 4006 section 8.3.
	 */
	public enum CcRecordType {
		INITIAL_REQUEST("INITIAL_REQUEST", 1),
		UPDATE_REQUEST("UPDATE_REQUEST", 2),
		TERMINATION_REQUEST("TERMINATION_REQUEST", 3),
		EVENT_REQUEST("EVENT_REQUEST", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CcRecordType> DATA = new Hashtable<Integer, CcRecordType>();
		static {
			for (CcRecordType obj : CcRecordType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CcRecordType getData(int value) {
			return DATA.get(value);
		}

		private CcRecordType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The CC-Session-Failover enumeration values.
	 * 
	 * See RFC 4006 section 8.4.
	 */
	public enum CcSessionFailover {
		FAILOVER_NOT_SUPPORTED("FAILOVER_NOT_SUPPORTED", 0),
		FAILOVER_SUPPORTED("FAILOVER_SUPPORTED", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CcSessionFailover> DATA = new Hashtable<Integer, CcSessionFailover>();
		static {
			for (CcSessionFailover obj : CcSessionFailover.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CcSessionFailover getData(int value) {
			return DATA.get(value);
		}

		private CcSessionFailover(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The CC-Session-Failover enumeration values.
	 * 
	 * See RFC 4006 section 8.32.
	 */
	public enum CcUnitType {
		TIME("TIME", 0),
		MONEY("MONEY", 1),
		TOTAL_OCTETS("TOTAL-OCTETS", 2),
		INPUT_OCTETS("INPUT-OCTETS", 3),
		OUTPUT_OCTETS("OUTPUT-OCTETS", 4),
		SERVICE_SPECIFIC_UNITS("SERVICE-SPECIFIC-UNITS", 5);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CcUnitType> DATA = new Hashtable<Integer, CcUnitType>();
		static {
			for (CcUnitType obj : CcUnitType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CcUnitType getData(int value) {
			return DATA.get(value);
		}

		private CcUnitType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Check-Balance-Result enumeration values.
	 * 
	 * See RFC 4006 section 8.6.
	 */
	public enum CheckBalanceResult {
		ENOUGH_CREDIT("ENOUGH_CREDIT", 0),
		NO_CREDIT("NO_CREDIT", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CheckBalanceResult> DATA = new Hashtable<Integer, CheckBalanceResult>();
		static {
			for (CheckBalanceResult obj : CheckBalanceResult.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CheckBalanceResult getData(int value) {
			return DATA.get(value);
		}

		private CheckBalanceResult(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Credit-Control enumeration values.
	 * 
	 * See RFC 4006 section 8.13.
	 */
	public enum CreditControl {
		CREDIT_AUTHORIZATION("CREDIT_AUTHORIZATION", 0),
		RE_AUTHORIZATION("RE_AUTHORIZATION", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CreditControl> DATA = new Hashtable<Integer, CreditControl>();
		static {
			for (CreditControl obj : CreditControl.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CreditControl getData(int value) {
			return DATA.get(value);
		}

		private CreditControl(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Credit-Control-Failure-Handling enumeration values.
	 * 
	 * See RFC 4006 section 8.14.
	 */
	public enum CreditControlFailureHandling {
		TERMINATE("TERMINATE", 0),
		CONTINUE("CONTINUE", 1),
		RETRY_AND_TERMINATE("RETRY_AND_TERMINATE", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, CreditControlFailureHandling> DATA = new Hashtable<Integer, CreditControlFailureHandling>();
		static {
			for (CreditControlFailureHandling obj : CreditControlFailureHandling.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static CreditControlFailureHandling getData(int value) {
			return DATA.get(value);
		}

		private CreditControlFailureHandling(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Direct-Debiting-Failure-Handling enumeration values.
	 * 
	 * See RFC 4006 section 8.15.
	 */
	public enum DirectDebitingFailureHandling {
		TERMINATE_OR_BUFFER("TERMINATE_OR_BUFFER", 0),
		CONTINUE("CONTINUE", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, DirectDebitingFailureHandling> DATA = new Hashtable<Integer, DirectDebitingFailureHandling>();
		static {
			for (DirectDebitingFailureHandling obj : DirectDebitingFailureHandling.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static DirectDebitingFailureHandling getData(int value) {
			return DATA.get(value);
		}

		private DirectDebitingFailureHandling(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Final-Unit-Action enumeration values.
	 * 
	 * See RFC 4006 section 8.35.
	 */
	public enum FinalUnitAction {
		TERMINATE("TERMINATE", 0),
		REDIRECT("REDIRECT", 1),
		RESTRICT_ACCESS("RESTRICT_ACCESS", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, FinalUnitAction> DATA = new Hashtable<Integer, FinalUnitAction>();
		static {
			for (FinalUnitAction obj : FinalUnitAction.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static FinalUnitAction getData(int value) {
			return DATA.get(value);
		}

		private FinalUnitAction(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Multiple-Services-Indicator enumeration values.
	 * 
	 * See RFC 4006 section 8.40.
	 */
	public enum MultipleServicesIndicator {
		MULTIPLE_SERVICES_NOT_SUPPORTED("MULTIPLE_SERVICES_NOT_SUPPORTED", 0),
		MULTIPLE_SERVICES_SUPPORTED("MULTIPLE_SERVICES_SUPPORTED", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MultipleServicesIndicator> DATA = new Hashtable<Integer, MultipleServicesIndicator>();
		static {
			for (MultipleServicesIndicator obj : MultipleServicesIndicator.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MultipleServicesIndicator getData(int value) {
			return DATA.get(value);
		}

		private MultipleServicesIndicator(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Redirect-Address-Type enumeration values.
	 * 
	 * See RFC 4006 section 8.38.
	 */
	public enum RedirectAddressType {
		IPV4_ADDRESS("IPv4 Address", 0),
		IPV6_ADDRESS("IPv6 Address", 1),
		URL("URL", 2),
		SIP_URI("SIP URI", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, RedirectAddressType> DATA = new Hashtable<Integer, RedirectAddressType>();
		static {
			for (RedirectAddressType obj : RedirectAddressType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static RedirectAddressType getData(int value) {
			return DATA.get(value);
		}

		private RedirectAddressType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Subscription-Id-Type enumeration values.
	 * 
	 * See RFC 4006 section 8.47.
	 */
	public enum SubscriptionIdType {
		END_USER_E164("END_USER_E164", 0),
		END_USER_IMSI("END_USER_IMSI", 1),
		END_USER_SIP_URI("END_USER_SIP_URI", 2),
		END_USER_NAI("END_USER_NAI", 3),
		END_USER_PRIVATE("END_USER_PRIVATE", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SubscriptionIdType> DATA = new Hashtable<Integer, SubscriptionIdType>();
		static {
			for (SubscriptionIdType obj : SubscriptionIdType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SubscriptionIdType getData(int value) {
			return DATA.get(value);
		}

		private SubscriptionIdType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Requested-Action enumeration values.
	 * 
	 * See RFC 4006 section 8.41.
	 */
	public enum RequestedAction {
		DIRECT_DEBITING("DIRECT_DEBITING", 0),
		REFUND_ACCOUNT("REFUND_ACCOUNT", 1),
		CHECK_BALANCE("CHECK_BALANCE", 2),
		PRICE_ENQUIRY("PRICE_ENQUIRY", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, RequestedAction> DATA = new Hashtable<Integer, RequestedAction>();
		static {
			for (RequestedAction obj : RequestedAction.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static RequestedAction getData(int value) {
			return DATA.get(value);
		}

		private RequestedAction(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Tariff-Change-Usage enumeration values.
	 * 
	 * See RFC 4006 section 8.27.
	 */
	public enum TariffChangeUsage {
		UNIT_BEFORE_TARIFF_CHANGE("UNIT_BEFORE_TARIFF_CHANGE", 0),
		UNIT_AFTER_TARIFF_CHANGE("UNIT_AFTER_TARIFF_CHANGE", 1),
		UNIT_INDETERMINATE("UNIT_INDETERMINATE", 2),
		PRICE_ENQUIRY("PRICE_ENQUIRY", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, TariffChangeUsage> DATA = new Hashtable<Integer, TariffChangeUsage>();
		static {
			for (TariffChangeUsage obj : TariffChangeUsage.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static TariffChangeUsage getData(int value) {
			return DATA.get(value);
		}

		private TariffChangeUsage(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The User-Equipment-Info-Type enumeration values.
	 * 
	 * See RFC 4006 section 8.50.
	 */
	public enum UserEquipmentInfoType {
		IMEISV("IMEISV", 0),
		MAC("MAC", 1),
		EUI64("EUI64", 2),
		MODIFIED_EUI64("MODIFIED_EUI64", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, UserEquipmentInfoType> DATA = new Hashtable<Integer, UserEquipmentInfoType>();
		static {
			for (UserEquipmentInfoType obj : UserEquipmentInfoType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static UserEquipmentInfoType getData(int value) {
			return DATA.get(value);
		}

		private UserEquipmentInfoType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Adaptations enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.1.
	 */
	public enum Adaptations {
		YES("Yes", 0),
		NO("NO", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, Adaptations> DATA = new Hashtable<Integer, Adaptations>();
		static {
			for (Adaptations obj : Adaptations.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static Adaptations getData(int value) {
			return DATA.get(value);
		}

		private Adaptations(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Address-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.7.
	 */
	public enum AddressType {
		E_MAIL("e-mail address", 0),
		MSISDN("MSISDN", 1),
		IPV4_ADDRESS("IPv4 Address", 2),
		IPV6_ADDRESS("IPv6 Address", 3),
		NUMERIC_SHORTCODE("Numeric Shortcode", 4),
		ALPHANUMERIC_SHORTCODE("Alphanumeric Shortcode", 5),
		OTHER("Other", 6);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, AddressType> DATA = new Hashtable<Integer, AddressType>();
		static {
			for (AddressType obj : AddressType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static AddressType getData(int value) {
			return DATA.get(value);
		}

		private AddressType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Addressee-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.8.
	 */
	public enum AddresseeType {
		TO("TO", 0),
		CC("CC", 1),
		BCC("BCC", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, AddresseeType> DATA = new Hashtable<Integer, AddresseeType>();
		static {
			for (AddresseeType obj : AddresseeType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static AddresseeType getData(int value) {
			return DATA.get(value);
		}

		private AddresseeType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The AoC-Request-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.12C.
	 */
	public enum AocRequestType {
		AOC_NOT_REQUESTED("AoC_NOT_REQUESTED", 0),
		AOC_FULL("AoC_FULL", 1),
		AOC_COST_ONLY("AoC_COST_ONLY", 2),
		AOC_TARIFF_ONLY("AoC_TARIFF_ONLY", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, AocRequestType> DATA = new Hashtable<Integer, AocRequestType>();
		static {
			for (AocRequestType obj : AocRequestType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static AocRequestType getData(int value) {
			return DATA.get(value);
		}

		private AocRequestType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Class-Identifier enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.27.
	 */
	public enum ClassIdentifier {
		PERSONAL("Personal", 0),
		ADVERTISEMENT("Advertisement", 1),
		INFORMATIONAL("Informational", 2),
		AUTO("Auto", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ClassIdentifier> DATA = new Hashtable<Integer, ClassIdentifier>();
		static {
			for (ClassIdentifier obj : ClassIdentifier.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ClassIdentifier getData(int value) {
			return DATA.get(value);
		}

		private ClassIdentifier(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Content-Class enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.29.
	 */
	public enum ContentClass {
		TEXT("text", 0),
		IMAGE_BASIC("image-basic", 1),
		IMAGE_RICH("image-rich", 2),
		VIDEO_BASIC("video-basic", 3),
		VIDEO_RICH("video-rich", 4),
		MEGAPIXEL("megapixel", 5),
		CONTENT_BASIC("content-basic", 6),
		CONTENT_RICH("content-rich", 7);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ContentClass> DATA = new Hashtable<Integer, ContentClass>();
		static {
			for (ContentClass obj : ContentClass.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ContentClass getData(int value) {
			return DATA.get(value);
		}

		private ContentClass(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Delivery-Report-Requested enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.36.
	 */
	public enum DeliveryReportRequested {
		NO(" No", 0),
		YES("Yes", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, DeliveryReportRequested> DATA = new Hashtable<Integer, DeliveryReportRequested>();
		static {
			for (DeliveryReportRequested obj : DeliveryReportRequested.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static DeliveryReportRequested getData(int value) {
			return DATA.get(value);
		}

		private DeliveryReportRequested(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}
	/**
	 * The DRM-Content enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.39.
	 */
	public enum DrmContent {
		NO(" No", 0),
		YES("Yes", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, DrmContent> DATA = new Hashtable<Integer, DrmContent>();
		static {
			for (DrmContent obj : DrmContent.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static DrmContent getData(int value) {
			return DATA.get(value);
		}

		private DrmContent(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Dynamic-Address-Flag enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.39A.
	 */
	public enum DynamicAddressFlag {
		STATIC(" Static", 0),
		DYNAMIC("Dynamic", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, DynamicAddressFlag> DATA = new Hashtable<Integer, DynamicAddressFlag>();
		static {
			for (DynamicAddressFlag obj : DynamicAddressFlag.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static DynamicAddressFlag getData(int value) {
			return DATA.get(value);
		}

		private DynamicAddressFlag(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Envelope-Reporting enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.43.
	 */
	public enum EnvelopeReporting {
		DO_NOT_REPORT_ENVELOPES(" DO_NOT_REPORT_ENVELOPES", 0),
		REPORT_ENVELOPES("REPORT_ENVELOPES", 1),
		REPORT_ENVELOPES_WITH_VOLUME("REPORT_ENVELOPES_WITH_VOLUME", 2),
		REPORT_ENVELOPES_WITH_EVENTS("REPORT_ENVELOPES_WITH_EVENTS", 3),
		REPORT_ENVELOPES_WITH_VOLUME_AND_EVENTS("REPORT_ENVELOPES_WITH_VOLUME_AND_EVENTS", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, EnvelopeReporting> DATA = new Hashtable<Integer, EnvelopeReporting>();
		static {
			for (EnvelopeReporting obj : EnvelopeReporting.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static EnvelopeReporting getData(int value) {
			return DATA.get(value);
		}

		private EnvelopeReporting(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The File-Repair-Supported enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.49.
	 */
	public enum FileRepairSupported {
		SUPPORTED("SUPPORTED", 1),
		NOT_SUPPORTED("NOT_SUPPORTED", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, FileRepairSupported> DATA = new Hashtable<Integer, FileRepairSupported>();
		static {
			for (FileRepairSupported obj : FileRepairSupported.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static FileRepairSupported getData(int value) {
			return DATA.get(value);
		}

		private FileRepairSupported(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Interface-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.55.
	 */
	public enum InterfaceType {
		UNKNOWN("Unknown", 0),
		MOBILE_ORIGINATING("MOBILE_ORIGINATING", 1),
		MOBILE_TERMINATING("MOBILE_TERMINATING", 2),
		APPLICATION_ORIGINATING("APPLICATION_ORIGINATING", 3),
		APPLICATION_TERMINATION("APPLICATION_TERMINATION", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, InterfaceType> DATA = new Hashtable<Integer, InterfaceType>();
		static {
			for (InterfaceType obj : InterfaceType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static InterfaceType getData(int value) {
			return DATA.get(value);
		}

		private InterfaceType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The LCS-Client-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.66.
	 */
	public enum LcsClientType {
		EMERGENCY_SERVICES("EMERGENCY_SERVICES", 0),
		VALUE_ADDED_SERVICES("VALUE_ADDED_SERVICES", 1),
		PLMN_OPERATOR_SERVICES("PLMN_OPERATOR_SERVICES", 2),
		LAWFUL_INTERCEPT_SERVICES("LAWFUL_INTERCEPT_SERVICES", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, LcsClientType> DATA = new Hashtable<Integer, LcsClientType>();
		static {
			for (LcsClientType obj : LcsClientType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static LcsClientType getData(int value) {
			return DATA.get(value);
		}

		private LcsClientType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The LCS-Format-Indicator enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.68.
	 */
	public enum LcsFormatIndicator {
		LOGICAL_NAME("LOGICAL_NAME", 0),
		EMAIL_ADDRESS("EMAIL_ADDRESS", 1),
		MSISDN("MSISDN", 2),
		URL("URL", 3),
		SIP_URL("SIP_URL", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, LcsFormatIndicator> DATA = new Hashtable<Integer, LcsFormatIndicator>();
		static {
			for (LcsFormatIndicator obj : LcsFormatIndicator.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static LcsFormatIndicator getData(int value) {
			return DATA.get(value);
		}

		private LcsFormatIndicator(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Location-Estimate-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.74.
	 */
	public enum LocationEstimateType {
		CURRENT_LOCATION("CURRENT_LOCATION", 0),
		CURRENT_LAST_KNOWN_LOCATION("CURRENT_LAST_KNOWN_LOCATION", 1),
		INITIAL_LOCATION("INITIAL_LOCATION", 2),
		ACTIVATE_DEFERRED_LOCATION("ACTIVATE_DEFERRED_LOCATION", 3),
		CANCEL_DEFERRED_LOCATION("CANCEL_DEFERRED_LOCATION", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, LocationEstimateType> DATA = new Hashtable<Integer, LocationEstimateType>();
		static {
			for (LocationEstimateType obj : LocationEstimateType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static LocationEstimateType getData(int value) {
			return DATA.get(value);
		}

		private LocationEstimateType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Low-Balance-Indication enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.74.
	 */
	public enum LowBalanceIndication {
		NOT_APPLICABLE("NOT-APPLICABLE", 0),
		YES("YES", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, LowBalanceIndication> DATA = new Hashtable<Integer, LowBalanceIndication>();
		static {
			for (LowBalanceIndication obj : LowBalanceIndication.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static LowBalanceIndication getData(int value) {
			return DATA.get(value);
		}

		private LowBalanceIndication(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The MBMS-User-Service-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.78.
	 */
	public enum MbmsUserServiceType {
		DOWNLOAD("DOWNLOAD", 1),
		STREAMING("STREAMING", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MbmsUserServiceType> DATA = new Hashtable<Integer, MbmsUserServiceType>();
		static {
			for (MbmsUserServiceType obj : MbmsUserServiceType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MbmsUserServiceType getData(int value) {
			return DATA.get(value);
		}

		private MbmsUserServiceType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Media-Initiator-Flag enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.79.
	 */
	public enum MediaInitiatorFlag {
		CALLED_PARTY("called party", 0),
		CALLING_PARTY("calling party", 1),
		UNKWOWN("unknown", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MediaInitiatorFlag> DATA = new Hashtable<Integer, MediaInitiatorFlag>();
		static {
			for (MediaInitiatorFlag obj : MediaInitiatorFlag.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MediaInitiatorFlag getData(int value) {
			return DATA.get(value);
		}

		private MediaInitiatorFlag(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Message-Type enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.85.
	 */
	public enum MessageType {
		CALLING_PARTY("m-send-req", 1),
		M_SEND_CONF("m-send-conf", 2),
		M_NOTIFICATION_IND("m-notification-ind", 3),
		M_NOTIFYRESP_IND("m-notifyresp-ind", 4),
		M_RETRIEVE_CONF("m-retrieve-conf", 5),
		M_ACKNOWLEDGE_IND("m-acknowledge-ind", 6),
		M_DELIVERY_IND("m-delivery-ind", 7),
		M_READ_REC_IND("m-read-rec-ind", 8),
		M_READ_ORIG_IND("m-read-orig-ind", 9),
		M_FORWARD_REQ("m-forward-req", 10),
		M_FORWARD_CONF("m-forward-conf", 11),
		M_MMBOX_STORE_CONF("m-mbox-store-conf", 12),
		M_MMBOX_VIEW_CONF("m-mbox-view-conf", 13),
		M_MBOX_UPLOAD_CONF("m-mbox-upload-conf", 14),
		M_MBOX_DELETE_CONF("m-mbox-delete-conf", 15);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MessageType> DATA = new Hashtable<Integer, MessageType>();
		static {
			for (MessageType obj : MessageType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MessageType getData(int value) {
			return DATA.get(value);
		}

		private MessageType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The MMBox-Storage-Requested enumeration values.
	 * 
	 * See 3GPP TS 32.299 section 7.2.87.
	 */
	public enum MMBoxStorageRequested {
		NO("No", 0),
		YES("Yes", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MMBoxStorageRequested> DATA = new Hashtable<Integer, MMBoxStorageRequested>();
		static {
			for (MMBoxStorageRequested obj : MMBoxStorageRequested.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MMBoxStorageRequested getData(int value) {
			return DATA.get(value);
		}

		private MMBoxStorageRequested(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Node-Functionality enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.89.
	 */
	public enum NodeFunctionality {
		S_CSCF("S-CSCF", 0),
		P_CSCF("P-CSCF", 1),
		I_CSCF("I-CSCF", 2),
		MRFC("MRFC", 3),
		MGCF("MGCF", 4),
		BGCF("BGCF", 5),
		AS("AS", 6),
		IBCF("IBCF", 7),
		S_GW("S-GW", 8),
		P_GW("P-GW", 9),
		HSGW("HSGW", 10);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, NodeFunctionality> DATA = new Hashtable<Integer, NodeFunctionality>();
		static {
			for (NodeFunctionality obj : NodeFunctionality.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static NodeFunctionality getData(int value) {
			return DATA.get(value);
		}

		private NodeFunctionality(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Originator enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.95.
	 */
	public enum Originator {
		CALLING_PARTY("CALLING_PARTY", 0),
		CALLED_PARTY("CALLED_PARTY", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, Originator> DATA = new Hashtable<Integer, Originator>();
		static {
			for (Originator obj : Originator.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static Originator getData(int value) {
			return DATA.get(value);
		}

		private Originator(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Participant-Access-Priority enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.102.
	 */
	public enum ParticipantAccessPriority {
		PRE_EMPTIVE("Pre-emptive", 1),
		HIGH("High", 2),
		NORMAL("Normal", 3),
		LOW("Low", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ParticipantAccessPriority> DATA = new Hashtable<Integer, ParticipantAccessPriority>();
		static {
			for (ParticipantAccessPriority obj : ParticipantAccessPriority.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ParticipantAccessPriority getData(int value) {
			return DATA.get(value);
		}

		private ParticipantAccessPriority(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Participant-Action-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.102A.
	 */
	public enum ParticipantActionType {
		CREATE_CONF("CREATE_CONF", 0),
		JOIN_CONF("JOIN_CONF", 1),
		INVITE_INTO_CONF("INVITE_INTO_CONF", 2),
		QUIT_CONF("QUIT_CONF", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ParticipantActionType> DATA = new Hashtable<Integer, ParticipantActionType>();
		static {
			for (ParticipantActionType obj : ParticipantActionType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ParticipantActionType getData(int value) {
			return DATA.get(value);
		}

		private ParticipantActionType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PDP-Context-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.106.
	 */
	public enum PdpContextType {
		PRIMARY("PRIMARY", 0),
		SECONDARY("SECONDARY", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PdpContextType> DATA = new Hashtable<Integer, PdpContextType>();
		static {
			for (PdpContextType obj : PdpContextType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PdpContextType getData(int value) {
			return DATA.get(value);
		}

		private PdpContextType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-Change-Condition enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.107.
	 */
	public enum PoCChangeCondition {
		SERVICE_CHANGE("serviceChange", 0),
		VOLUME_LIMIT("volumeLimit", 1),
		TIME_LIMIT("timeLimit", 2),
		NUMBER_OF_TALK_BURT_LIMIT("numberofTalkBurstLimit", 3),
		NUMBER_OF_ACTIVE_PARTICIPANTS("numberofActiveParticipants", 4),
		TARIFF_TIME("tariffTime", 5);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PoCChangeCondition> DATA = new Hashtable<Integer, PoCChangeCondition>();
		static {
			for (PoCChangeCondition obj : PoCChangeCondition.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PoCChangeCondition getData(int value) {
			return DATA.get(value);
		}

		private PoCChangeCondition(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-Event-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.109A.
	 */
	public enum PocEventType {
		NORMAL("Normal", 0),
		INSTANT_PERSONAL_ALERT_EVENT("Instant Personal Alert event", 1),
		POC_GROUP_ADEVERTISEMENT_EVENT("PoC Group Advertisement event", 2),
		EARLY_SESSION_SETTING_UP_EVENT("Early Session Setting-up event", 3),
		POC_TALK_BURST("PoC Talk Burst", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PocEventType> DATA = new Hashtable<Integer, PocEventType>();
		static {
			for (PocEventType obj : PocEventType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PocEventType getData(int value) {
			return DATA.get(value);
		}

		private PocEventType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-Server-Role enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.112.
	 */
	public enum PocServerRole {
		PARTICIPATING("Participating PoC Server", 0),
		CONTROLLING("Controlling PoC Server", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PocServerRole> DATA = new Hashtable<Integer, PocServerRole>();
		static {
			for (PocServerRole obj : PocServerRole.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PocServerRole getData(int value) {
			return DATA.get(value);
		}

		private PocServerRole(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-Session-Initiation-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.114.
	 */
	public enum PocSessionInitiationType {
		PRE_ESTABLISHED("Pre-established", 0),
		ON_DEMAND("On-demand", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PocSessionInitiationType> DATA = new Hashtable<Integer, PocSessionInitiationType>();
		static {
			for (PocSessionInitiationType obj : PocSessionInitiationType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PocSessionInitiationType getData(int value) {
			return DATA.get(value);
		}

		private PocSessionInitiationType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-Session-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.115.
	 */
	public enum PocSessionType {
		ONE_TO_ONE("1 to 1 PoC session", 0),
		CHAT("chat PoC group session", 1),
		PRE_ARRANGED("pre-arranged PoC group session", 2),
		AD_HOC("ad-hoc PoC group session", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PocSessionType> DATA = new Hashtable<Integer, PocSessionType>();
		static {
			for (PocSessionType obj : PocSessionType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PocSessionType getData(int value) {
			return DATA.get(value);
		}

		private PocSessionType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PoC-User-Role-info-Units enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.118.
	 */
	public enum PocUserRoleInfoUnits {
		MODERATOR("Moderator", 1),
		DISPATCHER("Dispatcher", 2),
		SESSION_OWNER("Session-Owner", 3),
		SESSION_PARTICIPANT("Session-Participant", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PocUserRoleInfoUnits> DATA = new Hashtable<Integer, PocUserRoleInfoUnits>();
		static {
			for (PocUserRoleInfoUnits obj : PocUserRoleInfoUnits.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PocUserRoleInfoUnits getData(int value) {
			return DATA.get(value);
		}

		private PocUserRoleInfoUnits(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Priority enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.120.
	 */
	public enum Priority {
		LOW("Low", 0),
		NORMAL("Normal", 1),
		HIGH("High", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, Priority> DATA = new Hashtable<Integer, Priority>();
		static {
			for (Priority obj : Priority.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static Priority getData(int value) {
			return DATA.get(value);
		}

		private Priority(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The PS-Append-Free-Format-Data enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.121.
	 */
	public enum PsAppendFreeFormatData {
		APPEND("Append", 0),
		OVERWRITE("Overwrite", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PsAppendFreeFormatData> DATA = new Hashtable<Integer, PsAppendFreeFormatData>();
		static {
			for (PsAppendFreeFormatData obj : PsAppendFreeFormatData.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PsAppendFreeFormatData getData(int value) {
			return DATA.get(value);
		}

		private PsAppendFreeFormatData(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Read-Reply-Report-Requested enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.127.
	 */
	public enum ReadReplyReportRequested {
		YES("Yes", 0),
		NO("No", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ReadReplyReportRequested> DATA = new Hashtable<Integer, ReadReplyReportRequested>();
		static {
			for (ReadReplyReportRequested obj : ReadReplyReportRequested.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ReadReplyReportRequested getData(int value) {
			return DATA.get(value);
		}

		private ReadReplyReportRequested(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Reply-Path-Requested enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.135.
	 */
	public enum ReplyPathRequested {
		NO("No Reply Path Set", 0),
		YES("Reply Path Set", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ReplyPathRequested> DATA = new Hashtable<Integer, ReplyPathRequested>();
		static {
			for (ReplyPathRequested obj : ReplyPathRequested.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ReplyPathRequested getData(int value) {
			return DATA.get(value);
		}

		private ReplyPathRequested(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Reporting-Reason enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.136.
	 */
	public enum ReportingReason {
		THRESHOLD("THRESHOLD", 0),
		QHT("QHT", 1),
		FINAL("FINAL", 2),
		QUOTA_EXHAUSTED("QUOTA_EXHAUSTED", 3),
		VALIDITY_TIME("VALIDITY_TIME", 4),
		OTHER_QUOTA_TYPE("OTHER_QUOTA_TYPE", 5),
		RATING_CONDITION_CHANGE("RATING_CONDITION_CHANGE", 6),
		FORCED_REAUTHORISATION("FORCED_REAUTHORISATION", 7),
		POOL_EXHAUSTED("POOL_EXHAUSTED", 8);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ReportingReason> DATA = new Hashtable<Integer, ReportingReason>();
		static {
			for (ReportingReason obj : ReportingReason.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ReportingReason getData(int value) {
			return DATA.get(value);
		}

		private ReportingReason(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Role-Of-Node enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.138.
	 */
	public enum RoleOfNode {
		/**
		 * The AS/CSCF is applying an originating role, serving the calling
		 * subscriber.
		 */
		ORIGINATING_ROLE("ORIGINATING_ROLE", 0),
		/**
		 * The AS/CSCF is applying a terminating role, serving the called
		 * subscriber.
		 */
		TERMINATING_ROLE("TERMINATING_ROLE", 1),
		/**
		 * The AS is applying a proxy role.
		 */
		PROXY_ROLE("PROXY_ROLE", 2),
		/**
		 * The AS is applying a B2BUA role.
		 */
		B2BUA_ROLE("B2BUA_ROLE", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, RoleOfNode> DATA = new Hashtable<Integer, RoleOfNode>();
		static {
			for (RoleOfNode obj : RoleOfNode.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static RoleOfNode getData(int value) {
			return DATA.get(value);
		}

		private RoleOfNode(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The SDP-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.145A.
	 */
	public enum SdpType {
		SDP_OFFER("SDP_OFFER", 0),
		SDP_ANSWER("SDP_ANSWER", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SdpType> DATA = new Hashtable<Integer, SdpType>();
		static {
			for (SdpType obj : SdpType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SdpType getData(int value) {
			return DATA.get(value);
		}

		private SdpType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Serving-Node-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.152B.
	 */
	public enum ServingNodeType {
		SGSN("SGSN", 0),
		PMIPSGW("PMIPSGW", 1),
		GTPSGW("GTPSGW", 2),
		E_PDG("ePDG", 3),
		H_SGW("hSGW", 4),
		MME("MME", 5);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ServingNodeType> DATA = new Hashtable<Integer, ServingNodeType>();
		static {
			for (ServingNodeType obj : ServingNodeType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ServingNodeType getData(int value) {
			return DATA.get(value);
		}

		private ServingNodeType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}
	/**
	 * The SM-Message-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.158.
	 */
	public enum SmMessageType {
		SUBMISSION("SUBMISSION", 0),
		DELIVERY_REPORT("DELIVERY_REPORT", 1),
		SM_SERVICE_REQUEST("SM Service Request", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SmMessageType> DATA = new Hashtable<Integer, SmMessageType>();
		static {
			for (SmMessageType obj : SmMessageType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SmMessageType getData(int value) {
			return DATA.get(value);
		}

		private SmMessageType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The SM-Service-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.163A.
	 */
	public enum SmServiceType {
		CONTENT_PROCESSING("content processing", 0),
		FORWARDING("forwarding", 1),
		FORWARDING_MULTIPLE_SUBSCRIPTIONS("forwarding multiple subscriptions", 2),
		FILTERING("filtering", 3),
		RECEIPT("receipt", 4),
		NETWORK_STORAGE("network storage", 5),
		MULTIPLE_DESTINATION("multiple destination", 6),
		VPN("virtual private network", 7),
		AUTOREPLY("autoreply", 8),
		PERSONAL_SIGNATURE("personal signature", 9),
		DEFERRED_DELIVERY("defered delivery", 10);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SmServiceType> DATA = new Hashtable<Integer, SmServiceType>();
		static {
			for (SmServiceType obj : SmServiceType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SmServiceType getData(int value) {
			return DATA.get(value);
		}

		private SmServiceType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The SMS-Node enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.163.
	 */
	public enum SmsNode {
		SMS_ROUTER("SMS Router", 0),
		IP_SM_GW("IP-SM-GW", 1),
		SMS_ROUTER_AND_IP_SM_GW("SMS Router and IP-SM-GW", 2),
		SMS_SC("SMS-SC", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SmsNode> DATA = new Hashtable<Integer, SmsNode>();
		static {
			for (SmsNode obj : SmsNode.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SmsNode getData(int value) {
			return DATA.get(value);
		}

		private SmsNode(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Subscriber-Role enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.165A.
	 */
	public enum SubscriberRole {
		ORIGINATING("ORIGINATING", 0),
		TERMINATING("TERMINATING", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SubscriberRole> DATA = new Hashtable<Integer, SubscriberRole>();
		static {
			for (SubscriberRole obj : SubscriberRole.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static SubscriberRole getData(int value) {
			return DATA.get(value);
		}

		private SubscriberRole(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Time-Quota-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.172.
	 */
	public enum TimeQuotaType {
		DISCRETE_TIME_PERIOD("DISCRETE_TIME_PERIOD", 0),
		CONTINUOUS_TIME_PERIOD("CONTINUOUS_TIME_PERIOD", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, TimeQuotaType> DATA = new Hashtable<Integer, TimeQuotaType>();
		static {
			for (TimeQuotaType obj : TimeQuotaType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static TimeQuotaType getData(int value) {
			return DATA.get(value);
		}

		private TimeQuotaType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Trigger-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.176.
	 */
	public enum TriggerType {
		CHANGE_IN_SGSN_IP_ADDRESS("CHANGE_IN_SGSN_IP_ADDRESS", 1),
		CHANGE_IN_QOS("CHANGE_IN_QOS", 2),
		CHANGE_IN_LOCATION("CHANGE_IN_LOCATION", 3),
		CHANGE_IN_RAT("CHANGE_IN_RAT", 4),
		CHANGEINQOS_TRAFFIC_CLASS("CHANGEINQOS_TRAFFIC_CLASS", 10),
		CHANGEINQOS_RELIABILITY_CLASS("CHANGEINQOS_RELIABILITY_CLASS", 11),
		CHANGEINQOS_DELAY_CLASS("CHANGEINQOS_DELAY_CLASS", 12),
		CHANGEINQOS_PEAK_THROUGHPUT("CHANGEINQOS_PEAK_THROUGHPUT", 13),
		CHANGEINQOS_PRECEDENCE_CLASS("CHANGEINQOS_PRECEDENCE_CLASS", 14),
		CHANGEINQOS_MEAN_THROUGHPUT("CHANGEINQOS_MEAN_THROUGHPUT ", 15),
		CHANGEINQOS_MAXIMUM_BIT_RATE_FOR_UPLINK("CHANGEINQOS_MAXIMUM_BIT_RATE_FOR_UPLINK ", 16),
		CHANGEINQOS_MAXIMUM_BIT_RATE_FOR_DOWNLINK("CHANGEINQOS_MAXIMUM_BIT_RATE_FOR_DOWNLINK ", 17),
		CHANGEINQOS_RESIDUAL_BER("CHANGEINQOS_RESIDUAL_BER ", 18),
		CHANGEINQOS_SDU_ERROR_RATIO("CHANGEINQOS_SDU_ERROR_RATIO ", 19),
		CHANGEINQOS_TRANSFER_DELAY("CHANGEINQOS_TRANSFER_DELAY ", 20),
		CHANGEINQOS_TRAFFIC_HANDLING_PRIORITY("CHANGEINQOS_TRAFFIC_HANDLING_PRIORITY ", 21),
		CHANGEINQOS_GUARANTEED_BIT_RATE_FOR_UPLINK("CHANGEINQOS_GUARANTEED_BIT_RATE_FOR_UPLINK ", 22),
		CHANGEINQOS_GUARANTEED_BIT_RATE_FOR_DOWNLINK("CHANGEINQOS_GUARANTEED_BIT_RATE_FOR_DOWNLINK ", 23),
		CHANGEINLOCATION_MCC("CHANGEINLOCATION_MCC", 30),
		CHANGEINLOCATION_MNC("CHANGEINLOCATION_MNC", 31),
		CHANGEINLOCATION_RAC("CHANGEINLOCATION_RAC", 32),
		CHANGEINLOCATION_LAC("CHANGEINLOCATION_LAC", 33),
		CHANGEINLOCATION_CELLID("CHANGEINLOCATION_CellId", 34),
		CHANGE_IN_MEDIA_COMPOSITION("CHANGE_IN_MEDIA_COMPOSITION", 40),
		CHANGE_IN_PARTICIPANTS_NMB("CHANGE_IN_PARTICIPANTS_NMB", 50),
		CHANGE_IN_THRSHLD_OF_PARTICIPANTS_NMB("CHANGE_IN_ THRSHLD_OF_PARTICIPANTS_NMB", 51),
		CHANGE_IN_USER_PARTICIPATING_TYPE("CHANGE_IN_USER_PARTICIPATING_TYPE", 52),
		CHANGE_IN_SERVICE_CONDITION("CHANGE_IN_SERVICE_CONDITION", 60),
		// TODO change the value when it  is set in the TS
		CHANGE_IN_SERVING_NODE("CHANGE_IN_SERVING_NODE", 100);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, TriggerType> DATA = new Hashtable<Integer, TriggerType>();
		static {
			for (TriggerType obj : TriggerType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static TriggerType getData(int value) {
			return DATA.get(value);
		}

		private TriggerType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The User-Participating-Type enumeration values.
	 * 
	 * See 3GPP 32.299 section 7.2.180.
	 */
	public enum UserParticipatingType {
		NORMAL("Normal", 0),
		NW_POC_BOX("NW PoC Box", 1),
		UE_POC_BOX("UE PoC Box", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, UserParticipatingType> DATA = new Hashtable<Integer, UserParticipatingType>();
		static {
			for (UserParticipatingType obj : UserParticipatingType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static UserParticipatingType getData(int value) {
			return DATA.get(value);
		}

		private UserParticipatingType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The 3GPP-PDP-Type enumeration values.
	 * 
	 * See 3GPP 29.061.
	 */
	public enum ThreeGppPdpType {
		NOT_DEFINED("Not Defined values", 0);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, ThreeGppPdpType> DATA = new Hashtable<Integer, ThreeGppPdpType>();
		static {
			for (ThreeGppPdpType obj : ThreeGppPdpType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static ThreeGppPdpType getData(int value) {
			return DATA.get(value);
		}

		private ThreeGppPdpType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The MBMS-Service-Type enumeration values.
	 * 
	 * See 3GPP 29.061.
	 */
	public enum MbmsServiceType {
		MULTICAST("MULTICAST", 0),
		BROADCAST("BROADCAST", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MbmsServiceType> DATA = new Hashtable<Integer, MbmsServiceType>();
		static {
			for (MbmsServiceType obj : MbmsServiceType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static MbmsServiceType getData(int value) {
			return DATA.get(value);
		}

		private MbmsServiceType(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The MBMS-2G-3G-Indicator enumeration values.
	 * 
	 * See 3GPP 29.061.
	 */
	public enum Mbms2g3gIndicator {
		TWOG("2G", 0),
		THREEG("3G", 1),
		TWOG_AND_THREEG("2G-AND-3G", 2);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, Mbms2g3gIndicator> DATA = new Hashtable<Integer, Mbms2g3gIndicator>();
		static {
			for (Mbms2g3gIndicator obj : Mbms2g3gIndicator.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static Mbms2g3gIndicator getData(int value) {
			return DATA.get(value);
		}

		private Mbms2g3gIndicator(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The QoS-Class-Identifier enumeration values.
	 * 
	 * See 3GPP 29.212 section 5.3.17.
	 */
	public enum QosClassIdentifier {
		NOT_DEFINED("Not Defined values", 0);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, QosClassIdentifier> DATA = new Hashtable<Integer, QosClassIdentifier>();
		static {
			for (QosClassIdentifier obj : QosClassIdentifier.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static QosClassIdentifier getData(int value) {
			return DATA.get(value);
		}

		private QosClassIdentifier(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Pre-emption-Capabilities enumeration values.
	 * 
	 * See 3GPP 29.212 section 5.3.46.
	 */
	public enum PreemptionCapability {
		PRE_EMPTION_CAPABILITY_ENABLED("PRE-EMPTION_CAPABILITY_ENABLED", 0),
		PRE_EMPTION_CAPABILITY_DISABLED("PRE-EMPTION_CAPABILITY_DISABLED", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PreemptionCapability> DATA = new Hashtable<Integer, PreemptionCapability>();
		static {
			for (PreemptionCapability obj : PreemptionCapability.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PreemptionCapability getData(int value) {
			return DATA.get(value);
		}

		private PreemptionCapability(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Pre-emption-Vulnerability enumeration values.
	 * 
	 * See 3GPP 29.212 section 5.3.47.
	 */
	public enum PreemptionVulnerability {
		PRE_EMPTION_VULNERABILITY_ENABLED("PRE-EMPTION_VULNERABILITY_ENABLED", 0),
		PRE_EMPTION_VULNERABILITY_DISABLED("PRE-EMPTION_VULNERABILITY_DISABLED", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PreemptionVulnerability> DATA = new Hashtable<Integer, PreemptionVulnerability>();
		static {
			for (PreemptionVulnerability obj : PreemptionVulnerability.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static PreemptionVulnerability getData(int value) {
			return DATA.get(value);
		}

		private PreemptionVulnerability(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

	/**
	 * The Type-NUmber enumeration values.
	 * 
	 */
	public enum TypeNumber {
		NOT_DEFINED("Not Defined values", 0);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, TypeNumber> DATA = new Hashtable<Integer, TypeNumber>();
		static {
			for (TypeNumber obj : TypeNumber.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object.
		 */
		public static TypeNumber getData(int value) {
			return DATA.get(value);
		}

		private TypeNumber(String name, int value) {
			_name = name;
			_value = value;
		}

		/**
		 * Gets the value.
		 * 
		 * @return The value.
		 */
		public int getValue() {
			return _value;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}
	}

}
