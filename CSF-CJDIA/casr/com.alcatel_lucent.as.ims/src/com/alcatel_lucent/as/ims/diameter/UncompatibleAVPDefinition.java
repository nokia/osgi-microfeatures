package com.alcatel_lucent.as.ims.diameter;

import com.nextenso.proxylet.diameter.util.DiameterAVPFormat;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * This exception is thrown when the AVP definition is not compatible with the
 * AVP type.
 */
public class UncompatibleAVPDefinition
		extends RuntimeException {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private String _requiredType;
	private String _foundType;

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param requiredType The required type.
	 * @param foundType The found type.
	 */
	public UncompatibleAVPDefinition(String requiredType, String foundType) {
		_requiredType = requiredType;
		_foundType = foundType;
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param requiredFormat The required format.
	 * @param foundFormat The found format.
	 */
	public UncompatibleAVPDefinition(DiameterAVPFormat requiredFormat, DiameterAVPFormat foundFormat) {
		if (requiredFormat != null) {
			_requiredType = requiredFormat.getClass().getName();
		}
		if (foundFormat != null) {
			_foundType = foundFormat.getClass().getName();
		}
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param requiredDefinition The required definition.
	 * @param foundDefinition The found definition.
	 */
	public UncompatibleAVPDefinition(DiameterAVPDefinition requiredDefinition, DiameterAVPDefinition foundDefinition) {
		if (requiredDefinition != null) {
			_requiredType = requiredDefinition.getClass().getName();
		}
		if (foundDefinition != null) {
			_foundType = foundDefinition.getClass().getName();
		}
	}

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * <P>
	 * This constructor is used when no definition is required.
	 * 
	 * @param foundDefinition The found definition.
	 */
	public UncompatibleAVPDefinition(DiameterAVPDefinition foundDefinition) {
		if (foundDefinition != null) {
			_foundType = foundDefinition.getClass().getName();
		}
	}

	/**
	 * Gets the required type.
	 * 
	 * @return The required type.
	 */
	public String getRequiredType() {
		return _requiredType;
	}

	/**
	 * Gets the found type.
	 * 
	 * @return The found type.
	 */
	public String getFoundType() {
		return _foundType;
	}
}
