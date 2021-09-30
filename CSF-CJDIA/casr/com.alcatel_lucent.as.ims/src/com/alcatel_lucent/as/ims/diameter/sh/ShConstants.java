package com.alcatel_lucent.as.ims.diameter.sh;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * The Sh Constants.
 */
public class ShConstants {

	/**
	 * The Data Reference values.
	 */
	public enum DataReference {
		/**
		 * The Data Reference AVP value when the requested element contains
		 * transparent data.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.1
		 */
		REPOSITORY_DATA("RepositoryData", 0),
		/**
		 * The Data Reference AVP value when the requested element contains an IMS
		 * Public User Identity or a Public Service Identity.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.2
		 */
		IMS_PUBLIC_IDENTITY("IMSPublicIdentity", 10),
		/**
		 * The Data Reference AVP value when the requested element contains the IMS
		 * User State of the public identifier referenced.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.3
		 */
		IMS_USER_STATE("IMSUserState", 11),
		/**
		 * The Data Reference AVP value when the requested element contains the name
		 * of the S-CSCF assigned to the IMS Subscription.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.4
		 */
		SCSCF_NAME("S-CSCFName", 12),
		/**
		 * The Data Reference AVP value when the requested element contains the
		 * triggering information for a service.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.5
		 */
		INITIAL_FILTER_CRITERIA("InitialFilterCriteria", 13),
		/**
		 * The Data Reference AVP value when the requested element contains the
		 * location of the served subscriber in the MSC/VLR if the requested domain
		 * is CS, or the location of the served subscriber in the SGSN if the
		 * requested domain is PS.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.6
		 */
		LOCATION_INFORMATION("LocationInformation", 14),
		/**
		 * The Data Reference AVP value when the requested element indicates the
		 * state of the User Identity in the domain indicated by the
		 * Requested-Domain
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.7
		 */
		USER_STATE("UserState", 15),
		/**
		 * The Data Reference AVP value when the requested element contains the
		 * addresses of the charging functions
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.8
		 */
		CHARGING_INFORMATION("ChargingInformation", 16),
		/**
		 * The Data Reference AVP value when the requested element contains a Basic
		 * MSISDN that is associated with the User Identity present in the request.
		 * All valid instances of this information element shall be included in the
		 * message.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.9
		 */
		MSISDN("MSISDN", 17),

		/**
		 * The Data Reference AVP value when the requested element contains the
		 * activation state of the Public Service Identity present in the request.
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.10
		 * 
		 * @since 3GPP TS 29.329 v7.3
		 */
		PSI_ACTIVATION("PSIActivation", 18),
		/**
		 * The Data Reference AVP value for Dynamic Service Activation Info.
		 * 
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.11
		 * 
		 * @since 3GPP TS 29.329 v7.3
		 */
		DSAI("DSAI", 19),
		/**
		 * Used when the information element contains transparent data associated to
		 * a set of Alias IMS Public User Identities (see 3GPP TS 23.228 for the
		 * definition of Alias Public User Identities). A data repository may be
		 * shared by more than one AS implementing the same service.
		 * 
		 * This is a Data Reference AVP value
		 * 
		 * <BR>
		 * see 3GPP TS 29.328 section 7.6.12
		 * 
		 * @since 3GPP TS 29.329 v7.3
		 */
		ALIASES_REPOSITORY_DATA("AliasesRepositoryData", 20),
		/**
		 * This information element contains the Service Level Tracing Information
		 * (see IETF draft-dawes-sipping-debug-event) that is related to a specific
		 * Public Identifier. If the ServiceLevelTraceInfo is present, service level
		 * tracing shall be enabled in the Application Server for the related Public
		 * Identifier according to the configuration data received. If the
		 * ServiceLevelTraceInfo is not present, service level tracing is disabled
		 * in the Application Server for the related Public Identifier.
		 */
		SERVICE_LEVEL_TRACE_INFO("ServiceLevelTraceInfo", 21),
		/**
		 * This information element contains the IP address (or the prefix in the
		 * case of IPv6 stateless autoconfiguration) at any given time.See 3GPP TS
		 * 33.203.
		 */
		IP_ADDRESS_SECURE_BINDING_INFORMATION("IPAddressSecureBindingInformation", 22),
		EARLY_IMS_RESERVED("EARLY_IMS_RESERVED", 10000);

		private String _name = null;
		private int _value = -1;
		private static final Map<Integer, DataReference> DATA = new HashMap<Integer, DataReference>();
		private static final Map<String, DataReference> NAME = new HashMap<String, DataReference>();

		static {
			for (DataReference ref : DataReference.values()) {
				DATA.put(ref.getValue(), ref);
				NAME.put(ref.toString(), ref);
			}
		}

		private DataReference(String name, int value) {
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

		/**
		 * Gets the DataReference object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The DataReference object or null if the value does not correspond
		 *         to a known DataReference.
		 */
		public static DataReference getData(int value) {
			return DATA.get(value);
		}

		/**
		 * Gets the DataReference object according to its name
		 * 
		 * @param name The name.
		 * @return The DataReference object or null if the value does not correspond
		 *         to a known DataReference
		 */
		public static DataReference getData(String name) {
			return NAME.get(name);
		}
	}

	/**
	 * The SubsReqType enumeration values.
	 */
	public enum SubsReqType {
		/**
		 * This value is used by an AS to subscribe to notifications of changes of
		 * data.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.6
		 */
		SUBSCRIBE("Subscribe", 0),
		/**
		 * This value is used by an AS to unsubscribe to notifications of changes of
		 * data.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.6
		 */
		UNSUBSCRIBE("Unsubscribe", 1);

		private String _name = null;
		private int _value = -1;

		private final static Map<Integer, SubsReqType> DATA = new HashMap<Integer, SubsReqType>();
		private final static Map<String, SubsReqType> NAME = new HashMap<String, SubsReqType>();
		static {
			for (SubsReqType obj : SubsReqType.values()) {
				DATA.put(obj.getValue(), obj);
				NAME.put(obj.toString(), obj);
			}
		}

		/**
		 * Gets the SubsReqType object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The SubsReqType object or null if the value does not correspond
		 *         to a known SubsReqType.
		 */
		public static SubsReqType getData(int value) {
			return DATA.get(value);
		}

		/**
		 * Gets the SubsReqType object according to its name.
		 * 
		 * @param name The name.
		 * @return The SubsReqType object or null if the value does not correspond
		 *         to a known SubsReqType.
		 */
		public static SubsReqType getData(String name) {
			return NAME.get(name);
		}

		private SubsReqType(String name, int value) {
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
	 * The RequestedDomain enumeration values.
	 */
	public enum RequestedDomain {
		/**
		 * The requested data apply on the CS domain.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.7
		 */
		CS_DOMAIN("CS-Domain", 0),
		/**
		 * The requested data apply on the PS domain.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.7
		 */
		PS_DOMAIN("PS-Domain", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, RequestedDomain> DATA = new HashMap<Integer, RequestedDomain>();
		static {
			for (RequestedDomain obj : RequestedDomain.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the RequestedDomain object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The RequestedDomain object or null if the value does not
		 *         conrespond to a known RequestedDomain.
		 */
		public static RequestedDomain getData(int value) {
			return DATA.get(value);
		}

		private RequestedDomain(String name, int value) {
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
	 * The Current Location enumeration values.
	 */
	public enum CurrentLocation {
		/**
		 * The request indicates that the indication of an active loaction retrieval
		 * is not required.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.8
		 */
		DO_NOT_NEED_INITIATE_ACTIVE_LOCATION_RETRIEVAL("DoNotNeedInitiateActiveLocationRertieval", 0),
		/**
		 * It is requested that an active location retrieval is initiated.
		 * 
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.8
		 */
		INITIATE_ACTIVE_LOCATION_RETRIEVAL("InitiateActiveLocationRertieval", 1);

		private String _name = null;
		private int _value = -1;

		private final static Map<Integer, CurrentLocation> DATA = new Hashtable<Integer, CurrentLocation>();
		static {
			for (CurrentLocation obj : CurrentLocation.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the CurrentLocation object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The CurrentLocation object or null if the value does not
		 *         conrespond to a known CurrentLocation.
		 */
		public static CurrentLocation getData(int value) {
			return DATA.get(value);
		}

		private CurrentLocation(String name, int value) {
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
	 * The IdentitySet enumeration values.
	 */
	public enum IdentitySet {
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.10
		 */
		ALL_IDENTITIES("ALL_IDENTITIES", 0),
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.10
		 */
		REQUESTED_IDENTITIES("REQUESTED_IDENTITIES", 1),
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.10
		 */
		IMPLICIT_IDENTITIES("IMPLICIT_IDENTITIES", 2),
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.10
		 */
		ALIAS_IDENTITIES("ALIAS_IDENTITIES", 3);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, IdentitySet> DATA = new HashMap<Integer, IdentitySet>();
		static {
			for (IdentitySet set : IdentitySet.values()) {
				DATA.put(set.getValue(), set);
			}
		}

		private IdentitySet(String name, int value) {
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

		/**
		 * Gets the IdentitySet object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The IdentitySet object or null if the value does not conrespond
		 *         to a known IdentitySet.
		 */
		public static IdentitySet getData(int value) {
			return DATA.get(value);
		}

	}

	/**
	 * The Send-Data-Indication enumeration values.
	 */
	public enum SendDataIndication {
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.17
		 */
		USER_DATA_NOT_REQUESTED("USER_DATA_NOT_REQUESTED", 0),
		/**
		 * <BR>
		 * see 3GPP TS 29.329 section 6.3.17
		 */
		USER_DATA_REQUESTED("USER_DATA_REQUESTED", 1), ;

		private String _name = null;
		private int _value = -1;

		private final static Map<Integer, SendDataIndication> DATA = new HashMap<Integer, SendDataIndication>();
		static {
			for (SendDataIndication obj : SendDataIndication.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the SendDataIndication object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The SendDataIndication object or null if the value does not
		 *         conrespond to a known SendDataIndication.
		 */
		public static SendDataIndication getData(int value) {
			return DATA.get(value);
		}

		private SendDataIndication(String name, int value) {
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

	// Command Codes 
	public final static int USER_DATA_COMMAND = 306;
	public final static int PROFILE_UPDATE_COMMAND = 307;
	public final static int SUBSCRIBE_NOTIFICATION_COMMAND = 308;
	public final static int PUSH_NOTIFICATION_COMMAND = 309;

	/**
	 * The size of the data pushed to the receiving entity exceeds its capacity.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.6
	 */
	public final static long ERROR_TOO_MUCH_DATA_RESULT_CODE = 5008;
	/**
	 * The data received by the AS is not supported or recognized.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.1
	 */
	public final static long ERROR_USER_DATA_NOT_RECOGNIZED_RESULT_CODE = 5100;
	/**
	 * The requested operation is not allowed for the user.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.2
	 */
	public final static long ERROR_OPERATION_NOT_ALLOWED_RESULT_CODE = 5101;
	/**
	 * The requested user data is not allowed to be read.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.3
	 */
	public final static long ERROR_USER_DATA_CANNOT_BE_READ_RESULT_CODE = 5102;
	/**
	 * The requested user data is not allowed to be modified.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.4
	 */
	public final static long ERROR_USER_DATA_CANNOT_BE_MODIFIED_RESULT_CODE = 5103;
	/**
	 * The requested user data is not allowed to be notified on changes.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.5
	 */
	public final static long ERROR_USER_DATA_CANNOT_BE_NOTIFIED_RESULT_CODE = 5104;
	/**
	 * The request to update the repository data at the HSS could not be completed
	 * because the requested update is based on an out-of-date version of the
	 * repository data. That is, the sequence number in the Sh-Update Request
	 * message, does not match with the immediate successor of the associated
	 * sequence number stored for that repository data at the HSS. It is also used
	 * where an AS tries to create a new set of repository data when the
	 * identified repository data already exists in the HSS.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.7
	 */
	public final static long ERROR_TRANSPARENT_DATA_OUT_OF_SYNC_RESULT_CODE = 5105;

	/**
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.8
	 */
	public final static long ERROR_FEATURE_UNSUPPORTED_RESULT_CODE = 5011;
	/**
	 * The Application Server requested to subscribe to changes to Repository Data
	 * that is not present in the HSS.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.9
	 */
	public final static long ERROR_SUBS_DATA_RESULT_CODE = 5106;
	/**
	 * The AS received a notification of changes of some information to which it
	 * is not subscribed.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.10
	 */
	public final static long ERROR_NO_SUBSCRIPTION_TO_DATA_RESULT_CODE = 5107;
	/**
	 * The Application Server addressed a DSAI not configured in the HSS.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.2.11
	 */
	public final static long ERROR_DSAI_NOT_AVAILABLE_RESULT_CODE = 5108;

	/**
	 * The requested user data is not available at this time to satisfy the
	 * requested operation.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.3.1
	 */
	public final static long ERROR_USER_DATA_NOT_AVAILABLE_RESULT_CODE = 4100;
	/**
	 * The request to update the repository data at the HSS could not be completed
	 * because the related repository data is currently being updated by another
	 * entity.
	 * 
	 * <BR>
	 * see 3GPP TS 29.328 section 6.2.3.2
	 */
	public final static long ERROR_PRIOR_UPDATE_IN_PROGRESS_RESULT_CODE = 4101;

}
