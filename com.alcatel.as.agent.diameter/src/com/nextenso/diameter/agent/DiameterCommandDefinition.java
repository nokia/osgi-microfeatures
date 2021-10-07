package com.nextenso.diameter.agent;

import java.util.HashMap;
import java.util.Map;

import com.nextenso.proxylet.diameter.nasreq.NASApplicationConstants;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

/**
 * The Diameter command definitions.<BR/>
 * This class is used when a Diameter message is represented as a String in
 * order to be readable by a human user. Example of a call:<BR/>
 * <code>registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_DPR, "DPR", "DPA"));</code>
 */
@Deprecated
public class DiameterCommandDefinition {

	private String _requestU, _responseU;
	private String _requestL, _responseL;
	private int _commandCode;

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param command The command code.
	 * @param request The display string for a request.
	 * @param response The display string for a response.
	 */
	public DiameterCommandDefinition(int command, String request, String response) {
		_commandCode = command;
		_requestU = request.toUpperCase(java.util.Locale.getDefault());
		_responseU = response.toUpperCase(java.util.Locale.getDefault());
		_requestL = request.toLowerCase(java.util.Locale.getDefault());
		_responseL = response.toLowerCase(java.util.Locale.getDefault());
	}

	/**
	 * The command code.
	 * 
	 * @return The command code.
	 */
	public int getCommand() {
		return _commandCode;
	}

	/**
	 * Gets the display string for a request.
	 * 
	 * @return The display string for a request.
	 */
	public String getRequestString(boolean upper) {
		return upper ? _requestU : _requestL;

	}

	/**
	 * Gets the display string for a response.
	 * 
	 * @return The display string for a response.
	 */
	public String getResponseString(boolean upper) {
		return upper ? _responseU : _responseL;
	}

	public static class Dictionary {

		private static final Map<Integer, DiameterCommandDefinition> DEFINITIONS = new HashMap<Integer, DiameterCommandDefinition>();
		static {
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_DPR, "DPR", "DPA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_DWR, "DWR", "DWA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_CER, "CER", "CEA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_ACR, "ACR", "ACA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_ASR, "ASR", "ASA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_STR, "STR", "STA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(DiameterBaseConstants.COMMAND_RAR, "RAR", "RAA"));
			registerDiameterCommandDefinition(new DiameterCommandDefinition(NASApplicationConstants.COMMAND_AAR, "AAR", "AAA"));
		}

		/**
		 * Gets a command definition according to the command code.
		 * 
		 * @param command The command code.
		 * @return The commane definition or null if not known.
		 */
		public static DiameterCommandDefinition getDiameterCommandDefinition(int command) {
			return DEFINITIONS.get(command);
		}

		/**
		 * Registers a command definition.
		 * 
		 * @param definition The definition to be registered.
		 */
		public static void registerDiameterCommandDefinition(DiameterCommandDefinition definition) {
			if (definition != null) {
				DEFINITIONS.put(definition.getCommand(), definition);
			}
		}
	}
}