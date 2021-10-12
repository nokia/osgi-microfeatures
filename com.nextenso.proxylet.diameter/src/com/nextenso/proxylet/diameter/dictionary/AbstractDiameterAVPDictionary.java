// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DerivedFormat;


/**
 * Base abstract class for an AVP dictionary that let you access
 * AVP definitions by name or code/vendorId pair.
 * 
 * This class is abstract because it need to be specialized for
 * the system dictionary and dictionaries generated from JSON
 *
 */
public abstract class AbstractDiameterAVPDictionary {
	private Map<String, DiameterAVPDefinition> nameToAVP;
	private Map<Long, DiameterAVPDefinition> codeToAVP;
	
	/**
	 * Construct an new empty AVP dictionary
	 */
	public AbstractDiameterAVPDictionary() {
		nameToAVP = new HashMap<>();
		codeToAVP = new HashMap<>();
	}

	/**
	 * Contruct a new AVP dictionary using the given list of AVP definition
	 * as its content.
	 * @param defs the list of AVP definition to index
	 */
	public AbstractDiameterAVPDictionary(List<DiameterAVPDefinition> defs) {
		this();

		Objects.requireNonNull(defs);
				
		for(DiameterAVPDefinition def : defs) {
			registerDiameterAVPDefinition(def);
		}
	}
	
	/**
	 * Get an AVP definition by its name. Return null if the definition
	 * could not be found.
	 * @param name the name of AVP to look for
	 * @return the AVP definition or null if not found
	 */
	public DiameterAVPDefinition getAVPDefinitionByName(String name) {
		return nameToAVP.get(name);
	}
	
	/**
	 * Get an AVP definition by its code and vendorId. Return null if the definition
	 * could not be found.
	 * @param code the AVP code
	 * @param vendorId the AVP vendor Id
	 * @return the AVP definition or null if not found
	 */
	public DiameterAVPDefinition getAVPDefinitionByCode(long code, long vendorId) {
		return codeToAVP.get(hash(code, vendorId));
	}
	
	
	/**
	 * Index a new definition in the dictionary
	 * 
	 * @param def the AVP definition to index
	 */
	public void registerDiameterAVPDefinition(DiameterAVPDefinition def) {
		registerDiameterAVPDefinition(def, true);
	}
	
	/**
	 * Index a new definition in the dictionary - note that it is not thread safe - should be ok normally
	 * 
	 * @param def the AVP definition to index
	 * @param throwIfAlreadyPresent if true, a RuntimeException if the definition is already present in the dictionary
	 */
	public void registerDiameterAVPDefinition(DiameterAVPDefinition def, boolean throwIfAlreadyPresent) {
		if(throwIfAlreadyPresent && nameToAVP.containsKey(def.getAVPName())) {
			throw new RuntimeException("Duplicate AVP name " + def.getAVPName());
		}
		nameToAVP.put(def.getAVPName(), def);
		
		if(throwIfAlreadyPresent && codeToAVP.containsKey(hash(def.getAVPCode(), def.getVendorId()))) {
			nameToAVP.remove(def.getAVPName());
			throw new RuntimeException("Duplicate AVP code " 
					+ hash(def.getAVPCode(), def.getVendorId()));
		}
		codeToAVP.put(hash(def.getAVPCode(), def.getVendorId()), def);
	}
	
	/**
	 * Get the list of AVP definitions indexed in the dictionary
	 * @return the list of AVP definitions indexed in the dictionary
	 */
	public List<DiameterAVPDefinition> getAVPDefList() {
		return new ArrayList<>(codeToAVP.values());
	}
	
	
	/**
	 * get all derived formats found in the AVP definitions. Used
	 * for JSON serialization
	 * @return the set of DerivedFormat
	 */
	public Set<DerivedFormat> getCustomFormats() {
		Set<DerivedFormat> formats = new HashSet<>();
		
		for(DiameterAVPDefinition avpDef : getAVPDefList()) {
			if(avpDef.getDiameterAVPFormat() instanceof DerivedFormat) {
				formats.add((DerivedFormat) avpDef.getDiameterAVPFormat());
			}
		}
		
		return formats;
	}
	
	private Long hash(long code, long vendorId) {
		return (code << 32) | vendorId;
	}
}
