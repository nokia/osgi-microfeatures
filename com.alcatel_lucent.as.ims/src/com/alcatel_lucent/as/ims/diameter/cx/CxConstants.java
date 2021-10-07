package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.Hashtable;
import java.util.Map;

/**
 * The Cx Constants.
 */
public class CxConstants {

	/**
	 * The ServerAssignmentType enumeration values.
	 */
	public enum ServerAssignmentType {
		/**
		 * This value is used to request from HSS the user profile assigned to one
		 * or more public identities, without affecting the registration state of
		 * those identities.
		 * 
		 * see 3GPP TS 29.229 section 6.3.15
		 */
		NO_ASSIGNMENT("NO_ASSIGNMENT", 0),
		/**
		 * The request is generated as a consequence of a first registration of an
		 * identity.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		REGISTRATION("REGISTRATION", 1),
		/**
		 * The request corresponds to the re-registration of an identity.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		RE_REGISTRATION("RE_REGISTRATION", 2),
		/**
		 * The request is generated because the S-CSCF received an INVITE for a
		 * public identity that is not registered.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		UNREGISTERED_USER("UNREGISTERED_USER", 3),
		/**
		 * The SIP registration timer of an identity has expired.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		TIMEOUT_DEREGISTRATION("TIMEOUT_DEREGISTRATION", 4),
		/**
		 * The S-CSCF has received a user initiated de-registration request.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		USER_DEREGISTRATION("USER_DEREGISTRATION", 5),
		/**
		 * The SIP registration timer of an identity has expired. The S-CSCF keeps
		 * the user data stored in the S-CSCF and requests HSS to store the S-CSCF
		 * name.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME("TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME", 6),
		/**
		 *The S-CSCF has received a user initiated de-registration request. The
		 * S-CSCF keeps the user data stored in the S-CSCF and requests HSS to store
		 * the S-CSCF name.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		USER_DEREGISTRATION_STORE_SERVER_NAME("USER_DEREGISTRATION_STORE_SERVER_NAME", 7),
		/**
		 *The S-CSCF, due to administrative reasons, has performed the
		 * de-registration of an identity.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		ADMINISTRATIVE_DEREGISTRATION("ADMINISTRATIVE_DEREGISTRATION", 8),
		/**
		 * The authentication of a user has failed.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		AUTHENTICATION_FAILURE("AUTHENTICATION_FAILURE", 9),
		/**
		 * The authentication timeout has expired.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		AUTHENTICATION_TIMEOUT("AUTHENTICATION_TIMEOUT", 10),
		/**
		 *The S-CSCF has requested user profile information from the HSS and has
		 * received a volume of data higher than it can accept.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 */
		DEREGISTRATION_TOO_MUCH_DATA("DEREGISTRATION_TOO_MUCH_DATA", 11),
		/**
		 * Used in the SWx protocol. This value is not used in the Cx protocol.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 * <BR>see 3GPP TS 29.273.
		 */
		AAA_USER_DATA_REQUEST("AAA_USER_DATA_REQUEST", 12),
		/**
		 * Used in the SWx protocol. This value is not used in the Cx protocol.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.15
		 * <BR>see 3GPP TS 29.273.
		 */
		PGW_UPDATE("PGW_UPDATE ", 13);

		private String _name = null;
		private int _value = -1;
		
		private final static Map<Integer, ServerAssignmentType> DATA = new Hashtable<Integer, ServerAssignmentType>();
		static {
			for (ServerAssignmentType obj : ServerAssignmentType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}
		
		/**
		 * Gets the ServerAssignmentType object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The ServerAssignmentType object or null if the value does not conrespond
		 *         to a known ServerAssignmentType.
		 */
		public static ServerAssignmentType getData(int value) {
			return DATA.get(value);
		}

		private ServerAssignmentType(String name, int value) {
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
	 * The ReasonCode enumeration values.
	 */
	public enum ReasonCode {
		/**
		 * <BR>see 3GPP TS 29.229 section 6.3.17
		 */
		PERMANENT_TERMINATION("PERMANENT_TERMINATION", 0),
		/**
		 * <BR>see 3GPP TS 29.229 section 6.3.17
		 */
		NEW_SERVER_ASSIGNED("NEW_SERVER_ASSIGNED", 1),
		/**
		 * <BR>see 3GPP TS 29.229 section 6.3.17
		 */
		SERVER_CHANGE("SERVER_CHANGE", 2),
		/**
		 * <BR>see 3GPP TS 29.229 section 6.3.17
		 */
		REMOVE_SCSCF("REMOVE_S-CSCF", 3);

		private String _name = null;
		private int _value = -1;

		private final static Map<Integer, ReasonCode> DATA = new Hashtable<Integer, ReasonCode>();
		static {
			for (ReasonCode obj : ReasonCode.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}
		
		/**
		 * Gets the ReasonCode object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The ReasonCode object or null if the value does not conrespond
		 *         to a known ReasonCode.
		 */
		public static ReasonCode getData(int value) {
			return DATA.get(value);
		}

		private ReasonCode(String name, int value) {
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
	 * The User-Authorization-Type enumeration values.
	 */
	public enum UserAuthorizationType {
		/**
		 * This value is used in case of the initial registration or
		 * re-registration. I-CSCF determines this from the Expires field or expires
		 * parameter in Contact field in the SIP REGISTER method if it is not equal
		 * to zero. This is the default value.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.24
		 */
		REGISTRATION("REGISTRATION", 0),
		/**
		 * This value is used in case of the de-registration. I-CSCF determines this
		 * from the Expires field or expires parameter in Contact field in the SIP
		 * REGISTER method if it is equal to zero.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.24
		 */
		DE_REGISTRATION("DE_REGISTRATION", 2),
		/**
		 * This value is used in case of initial registration or re-registration and
		 * when the I-CSCF explicitly requests S- CSCF capability information from
		 * the HSS. The I-CSCF shall use this value when the user's current S-CSCF,
		 * which is stored in the HSS, cannot be contacted and a new S-CSCF needs to
		 * be selected
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.24
		 */
		REGISTRATION_AND_CAPABILITIES("REGISTRATION_AND_CAPABILITIES ", 3);

		private String _name = null;
		private int _value = -1;

		private final static Map<Integer, UserAuthorizationType> DATA = new Hashtable<Integer, UserAuthorizationType>();
		static {
			for (UserAuthorizationType obj : UserAuthorizationType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}
		
		/**
		 * Gets the UserAuthorizationType object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The UserAuthorizationType object or null if the value does not conrespond
		 *         to a known UserAuthorizationType.
		 */
		public static UserAuthorizationType getData(int value) {
			return DATA.get(value);
		}

		private UserAuthorizationType(String name, int value) {
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
	 * The User-Data-Request-Type enumeration values.
	 */
	public enum UserDataRequestType {
		/**
		 * This value is used to request from the HSS the complete user profile
		 * corresponding to one or more public identities.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.25
		 */
		COMPLETE_PROFILE("COMPLETE_PROFILE", 0),
		/**
		 * This value is used to request from the HSS the registered part of the
		 * user profile corresponding to one or more public identities.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.25
		 */
		REGISTERED_PROFILE("REGISTERED_PROFILE", 1),
		/**
		 * This value is used to request from the HSS the unregistered part of the
		 * user profile corresponding to one or more public identities.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.25
		 */
		UNREGISTERED_PROFILE("UNREGISTERED_PROFILE", 2);

		private String _name = null;
		private int _value = -1;
		
		private final static Map<Integer, UserDataRequestType> DATA = new Hashtable<Integer, UserDataRequestType>();
		static {
			for (UserDataRequestType obj : UserDataRequestType.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}
		
		/**
		 * Gets the UserDataRequestType object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The UserDataRequestType object or null if the value does not conrespond
		 *         to a known UserDataRequestType.
		 */
		public static UserDataRequestType getData(int value) {
			return DATA.get(value);
		}

		private UserDataRequestType(String name, int value) {
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
	 * The User-Data-Already-Available enumeration values.
	 */
	public enum UserDataAlreadyAvailable {
		/**
		 * The S-CSCF does not have the data that it needs to serve the user.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.26
		 */
		USER_DATA_NOT_AVAILABLE("USER_DATA_NOT_AVAILABLE", 0),
		/**
		 * The S-CSCF already has the data that it needs to serve the user.
		 * 
		 * <BR>see 3GPP TS 29.229 section 6.3.26
		 */
		USER_DATA_ALREADY_AVAILABLE("USER_DATA_ALREADY_AVAILABLE", 1);

		private String _name = null;
		private int _value = -1;
		
		private final static Map<Integer, UserDataAlreadyAvailable> DATA = new Hashtable<Integer, UserDataAlreadyAvailable>();
		static {
			for (UserDataAlreadyAvailable obj : UserDataAlreadyAvailable.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}
		
		/**
		 * Gets the UserDataAlreadyAvailable object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The UserDataAlreadyAvailable object or null if the value does not conrespond
		 *         to a known UserDataAlreadyAvailable.
		 */
		public static UserDataAlreadyAvailable getData(int value) {
			return DATA.get(value);
		}

		private UserDataAlreadyAvailable(String name, int value) {
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
	 * The Loose-Route-Indication enumeration values.
	 */
	public enum LooseRouteIndication {
		/**
		 * see 3GPP TS 29.229 section 6.3.45.
		 */
		LOOSE_ROUTE_NOT_REQUIRED("LOOSE_ROUTE_NOT_REQUIRED", 0),
		/**
		 * see 3GPP TS 29.229 section 6.3.45.
		 */
		LOOSE_ROUTE_REQUIRED("LOOSE_ROUTE_REQUIRED", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, LooseRouteIndication> DATA = new Hashtable<Integer, LooseRouteIndication>();
		static {
			for (LooseRouteIndication obj : LooseRouteIndication.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object..
		 */
		public static LooseRouteIndication getData(int value) {
			return DATA.get(value);
		}


		private LooseRouteIndication(String name, int value) {
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
	 * The Multiple-Registration-Indication enumeration values.
	 */
	public enum MultipleRegistrationIndication {
		/**
		 * see 3GPP TS 29.229 section 6.3.51.
		 */
		NOT_MULTIPLE_REGISTRATION("NOT_MULTIPLE_REGISTRATION", 0),
		/**
		 * see 3GPP TS 29.229 section 6.3.51.
		 */
		MULTIPLE_REGISTRATION("MULTIPLE_REGISTRATION", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, MultipleRegistrationIndication> DATA = new Hashtable<Integer, MultipleRegistrationIndication>();
		static {
			for (MultipleRegistrationIndication obj : MultipleRegistrationIndication.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}

		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object..
		 */
		public static MultipleRegistrationIndication getData(int value) {
			return DATA.get(value);
		}

		private MultipleRegistrationIndication(String name, int value) {
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
	 * The Session-Priority enumeration values.
	 */
	public enum SessionPriority {
		/**
		 * see 3GPP TS 29.229 section 6.3.56 
		 */
		PRIORITY_0("PRIORITY_0", 0),
		/**
		 * see 3GPP TS 29.229 section 6.3.56 
		 */
		PRIORITY_1("PRIORITY_1", 1),
		/**
		 * see 3GPP TS 29.229 section 6.3.56 
		 */
		PRIORITY_2("PRIORITY_2", 2),
		/**
		 * see 3GPP TS 29.229 section 6.3.56 
		 */
		PRIORITY_3("PRIORITY_3", 3),
		/**
		 * see 3GPP TS 29.229 section 6.3.56 
		 */
		PRIORITY_4("PRIORITY_4", 4);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, SessionPriority> DATA = new Hashtable<Integer, SessionPriority>();
		static {
			for (SessionPriority obj : SessionPriority.values()) {
				DATA.put(obj.getValue(), obj);
			}
		}


		/**
		 * Gets the object according to its value.
		 * 
		 * @param value The numeric value (contained in the AVP).
		 * @return The object or null if the value does not conrespond to a known
		 *         object..
		 */
		public static SessionPriority getData(int value) {
			return DATA.get(value);
		}

		private SessionPriority(String name, int value) {
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
	
	public final static int USER_AUTHORIZATION_COMMAND_CODE = 300;
	public final static int SERVER_ASSIGNMENT_COMMAND_CODE = 301;
	public final static int LOCATION_INFO_COMMAND_CODE = 302;
	public final static int MULTIMEDIA_AUTH_COMMAND_CODE = 303;
	public final static int REGISTRATION_TERMINATION_COMMAND_CODE = 304;
	public final static int PUSH_PROFILE_COMMAND_CODE = 305;

	/**
	 * The HSS informs the I-CSCF that:
	 * <UL>
	 * <LI>The user is authorized to register this public identity;
	 * <LI>A S-CSCF shall be assigned to the user.
	 * </UL>
	 */
	public final static long FIRST_REGISTRATION_RESULT_CODE = 2001;
	/**
	 * The HSS informs the I-CSCF that:
	 * <UL>
	 * <LI>The user is authorized to register this public identity;
	 * <LI>A S-CSCF is already assigned and there is no need to select a new one.
	 * </UL>
	 */
	public final static long SUBSEQUENT_REGISTRATION_RESULT_CODE = 2002;
	/**
	 * The HSS informs the I-CSCF that:
	 * <UL>
	 * <LI>The public identity is not registered but has services related to
	 * unregistered state;
	 * <LI>A S-CSCF shall be assigned to the user.
	 * </UL>
	 */
	public final static long UNREGISTERED_SERVICE_RESULT_CODE = 2003;
	/**
	 * The HSS informs to the S-CSCF that:
	 * <UL>
	 * <LI>The de-registration is completed;
	 * <LI>The S-CSCF name is not stored in the HSS.
	 * </UL>
	 */
	public final static long SUCCESS_SERVER_NAME_NOT_STORED_RESULT_CODE = 2004;
	/**
	 * The HSS informs the I-CSCF that:
	 * <UL>
	 * <LI>The user is authorized to register this public identity;
	 * <LI>A S-CSCF is already assigned for services related to unregistered
	 * state;
	 * <LI>It may be necessary to assign a new S-CSCF to the user.
	 * </UL>
	 */
	public final static long SERVER_SELECTION_RESULT_CODE = 2005;

	/**
	 * A message was received for a user that is unknown.
	 */
	public final static long ERROR_USER_UNKNOWN_RESULT_CODE = 5001;
	/**
	 * A message was received with a public identity and a private identity for a
	 * user, and the server determines that the public identity does not
	 * correspond to the private identity.
	 */
	public final static long ERROR_IDENTITIES_DONT_MATCH_RESULT_CODE = 5002;
	/**
	 * A query for location information is received for a public identity that has
	 * not been registered before. The user to which this identity belongs cannot
	 * be given service in this situation.
	 */
	public final static long ERROR_IDENTITIES_NOT_REGISTERED_RESULT_CODE = 5003;
	/**
	 * The user is not allowed to roam in the visited network.
	 */
	public final static long ERROR_ROAMING_NOT_ALLOWED_RESULT_CODE = 5004;
	/**
	 * The identity being registered has already a server assigned and the
	 * registration status does not allow that it is overwritten.
	 */
	public final static long ERROR_IDENTITY_ALREADY_REGISTERED_RESULT_CODE = 5005;
	/**
	 * The authentication scheme indicated in an authentication request is not
	 * supported.
	 */
	public final static long ERROR_AUTH_SCHEME_NOT_SUPPORTED_RESULT_CODE = 5006;
	/**
	 * The identity being registered has already the same server assigned and the
	 * registration status does not allow the server assignment type.
	 */
	public final static long ERROR_IN_ASSIGNMENT_TYPE_RESULT_CODE = 5007;
	/**
	 * The volume of the data pushed to the receiving entity exceeds its capacity.
	 */
	public final static long ERROR_TOO_MUCH_DATA_RESULT_CODE = 5008;
	/**
	 * The S-CSCF informs HSS that the received subscription data contained
	 * information, which was not recognised or supported
	 */
	public final static long ERROR_NOT_SUPPORTED_USER_DATA_RESULT_CODE = 5009;
	/**
	 * The HSS informs the S-CSCF that the message did not contain a Private-Id
	 * and/or a Public-Id and so the message could not be processed.
	 */
	public final static long ERROR_MISSING_USER_ID_RESULT_CODE = 5010;
	/**
	 * A request application message was received indicating that the origin host
	 * requests that the command pair would be handled using a feature which is
	 * not supported by the destination host.
	 */
	public final static long ERROR_FEATURE_UNSUPPORTED_RESULT_CODE = 5011;

}
