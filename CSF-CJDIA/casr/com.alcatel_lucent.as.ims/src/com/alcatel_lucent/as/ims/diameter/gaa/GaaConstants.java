package com.alcatel_lucent.as.ims.diameter.gaa;

import java.util.Hashtable;
import java.util.Map;

/**
 * The Generic Authentication Architecture (GAA) constants.
 */
public class GaaConstants {

	/**
	 * The GBA_U-Awareness-Indicator enumeration values.
	 */
	public enum GbaUAwarenessIndicator {
		/**
		 * The sending note is not GBA_U aware.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.8
		 */
		NO("NO", 0),
		/**
		 * The sending note is GBA_U aware.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.8
		 */
		YES("YES", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, GbaUAwarenessIndicator> DATA = new Hashtable<Integer, GbaUAwarenessIndicator>();
		static {
			for (GbaUAwarenessIndicator obj : GbaUAwarenessIndicator.values()) {
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
		public static GbaUAwarenessIndicator getData(int value) {
			return DATA.get(value);
		}

		private GbaUAwarenessIndicator(String name, int value) {
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
	 * The GBA-Type enumeration values.
	 */
	public enum GbaType {
		/**
		 * The 3G GBA has been performed as defined in TS 33.220.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.11
		 */
		GBA_3G("3G_GBA", 0),
		/**
		 * The 2G GBA has been performed as defined in TS 33.220.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.11
		 */
		GBA_2G("2G_GBA", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, GbaType> DATA = new Hashtable<Integer, GbaType>();
		static {
			for (GbaType obj : GbaType.values()) {
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
		public static GbaType getData(int value) {
			return DATA.get(value);
		}

		private GbaType(String name, int value) {
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
	 * The UE-Id-Type enumeration values.
	 */
	public enum UEIdType {
		/**
		 * Private user identity.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.13
		 */
		PRIVATE("Private", 0),
		/**
		 * Public user identity.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.13
		 */
		PUBLIC("Public", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, UEIdType> DATA = new Hashtable<Integer, UEIdType>();
		static {
			for (UEIdType obj : UEIdType.values()) {
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
		public static UEIdType getData(int value) {
			return DATA.get(value);
		}

		private UEIdType(String name, int value) {
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
	 * The UICC-ME enumeration values.
	 */
	public enum UiccMe {
		/**
		 * GBA_ME shall be run.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.15
		 */
		GBA_ME("GBA_ME", 0),
		/**
		 * GBA_U shall be run.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.15
		 */
		GBA_U("GBA_U", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, UiccMe> DATA = new Hashtable<Integer, UiccMe>();
		static {
			for (UiccMe obj : UiccMe.values()) {
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
		public static UiccMe getData(int value) {
			return DATA.get(value);
		}

		private UiccMe(String name, int value) {
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
	 * The Private-Identity-Request enumeration values.
	 */
	public enum PrivateIdentityRequest {
		/**
		 * The private identity is requested.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.17
		 */
		REQUESTED("Requested", 0),
		/**
		 * The private identity is not requested.
		 * 
		 * <BR>see 3GPP TS 29.109 section 6.3.1.17
		 */
		NOT_REQUESTED("NotRequested", 1);

		private String _name = null;
		private int _value = -1;
		private final static Map<Integer, PrivateIdentityRequest> DATA = new Hashtable<Integer, PrivateIdentityRequest>();
		static {
			for (PrivateIdentityRequest obj : PrivateIdentityRequest.values()) {
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
		public static PrivateIdentityRequest getData(int value) {
			return DATA.get(value);
		}

		private PrivateIdentityRequest(String name, int value) {
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

	public final static int ZH_MULTIMEDIA_AUTH_COMMAND_CODE = 303;

	public static final int ZN_BOOTSTRAPPING_INFO_COMMAND_CODE = 310;

}
