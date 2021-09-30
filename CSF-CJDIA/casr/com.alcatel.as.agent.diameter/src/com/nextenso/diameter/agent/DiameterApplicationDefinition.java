package com.nextenso.diameter.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * The Diameter application definitions.<br/>
 * 
 * This class is used when a Diameter message is represented as a String in
 * order to be readable by a human user.<br/>
 * 
 * Example of a call:<br/>
 * <code>
 * 			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(0, "Diameter Common Messages"));
 * </code>
 */
public class DiameterApplicationDefinition {

	private String _name;
	private int _applicationId;

	/**
	 * Constructor for this class.
	 * 
	 * @param applicationId The application identifier.
	 * @param displayName The display name.
	 */
	public DiameterApplicationDefinition(int applicationId, String displayName) {
		_name = displayName;
		_applicationId = applicationId;
	}

	/**
	 * Gets the display name.
	 * 
	 * @return The display name.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Gets the application identifier.
	 * 
	 * @return The application identifier.
	 */
	public int getApplicationId() {
		return _applicationId;
	}

	/**
	 * The application dictionary.
	 */
	public static class Dictionary {

		private final static Map<Integer, DiameterApplicationDefinition> DEFINITIONS = new HashMap<Integer, DiameterApplicationDefinition>();
		static {
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(0, "Diameter Common Messages"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(1, "NASREQ"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(2, "Mobile-IP"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(3, "Diameter Base Accounting"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(4, "Ro"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(16777216, "Rf"));
			registerDiameterApplicationDefinition(new DiameterApplicationDefinition(0xffffffff, "Relay"));
		}

		/**
		 * Registers an application definition.
		 * 
		 * @param definition The definition to be registered.
		 */
		public static void registerDiameterApplicationDefinition(DiameterApplicationDefinition definition) {
			if (definition != null) {
				DEFINITIONS.put(definition.getApplicationId(), definition);
			}
		}

		/**
		 * Gets the application according to its identifier.
		 * 
		 * @param applicationId The application identifier.
		 * @return The definition of the application or null if it is unknown.
		 */
		public static DiameterApplicationDefinition getDiameterApplicationDefinition(int applicationId) {
			return DEFINITIONS.get(applicationId);
		}
	}
}
