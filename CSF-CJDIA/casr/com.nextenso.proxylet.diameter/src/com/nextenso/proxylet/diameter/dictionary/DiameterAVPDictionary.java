package com.nextenso.proxylet.diameter.dictionary;

import java.util.List;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;


/**
 * Dictionary for Diameter AVP definitions. This particular subclass
 * will first check in its own content, and then in the CJDIA system dictionary
 * if a definition could not be found.
 *
 */
public class DiameterAVPDictionary extends AbstractDiameterAVPDictionary {

	public DiameterAVPDictionary(List<DiameterAVPDefinition> defs) {
		super(defs);
	}
	
	/**
	 * Get an AVP definition by its code and vendorId. Return null if the definition
	 * could not be found.
	 * @param code the AVP code
	 * @param vendorId the AVP vendor Id
	 * @return the AVP definition or null if not found
	 */
	@Override
	public DiameterAVPDefinition getAVPDefinitionByCode(long code, long vendorId) {
		DiameterAVPDefinition avpDef = super.getAVPDefinitionByCode(code, vendorId);
		if(avpDef == null) {
			return DiameterAVPDefinition.DICTIONARY
					.getBackingAVPDictionary()
					.getAVPDefinitionByCode(code, vendorId);
		} else {
			return avpDef;
		}
	}
	/**
	 * Get an AVP definition by its name. Return null if the definition
	 * could not be found.
	 * @param name the name of AVP to look for
	 * @return the AVP definition or null if not found
	 */
	@Override
	public DiameterAVPDefinition getAVPDefinitionByName(String name) {
		DiameterAVPDefinition avpDef = super.getAVPDefinitionByName(name);
		if(avpDef == null) {
			return DiameterAVPDefinition.DICTIONARY
					.getBackingAVPDictionary()
					.getAVPDefinitionByName(name);
		} else {
			return avpDef;
		}
	}
}
