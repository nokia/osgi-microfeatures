// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter.dictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * A dictionary for Diameter command definitions. Let you access Diameter command 
 * definitions by their name, their code/applicationId pair or their abbreviation/applicationId pair.
 * <br/>
 * Both the request and answers are indexed.
 */
public class DiameterCommandDictionary {

	private Map<String, DiameterCommandDefinition> nameToCommand;
	private Map<String, DiameterCommandDefinition> codeToCommand;
	private Map<String, DiameterCommandDefinition> abbreviationToCommand;

	
	/**
	 * Construct a new DiameterCommandDictionary using the list of DiameterCommandDefinition
	 * as its content.
	 * 
	 * @param defs the content for the new dictionary
	 */
	public DiameterCommandDictionary(List<DiameterCommandDefinition> defs) {
		Objects.requireNonNull(defs);
		nameToCommand = new HashMap<>();
		codeToCommand = new HashMap<>();
		abbreviationToCommand = new HashMap<>();

		for (DiameterCommandDefinition def : defs) {
			nameToCommand.put(def.getRequestName(), def);
			nameToCommand.put(def.getAnswerName(), def);
			codeToCommand.put(hash(def.getCode(), def.getApplicationId()), def);
			abbreviationToCommand.put(hash(def.getRequestAbbreviation(), def.getApplicationId()), def);
			abbreviationToCommand.put(hash(def.getAnswerAbbreviation(), def.getApplicationId()), def);

		}
	}

	/**
	 * Get a Diameter command definition by its request or answer name or null
	 * if it could not be found.
	 * 
	 * @param name the name of Diameter request or answer
	 * @return a DiameterCommandDefinition or null if not found
	 */
	public DiameterCommandDefinition getCommandDefinitionByName(String name) {
		return nameToCommand.get(name);
	}

	/**
	 * Get a Diameter command definition by its command code and application ID
	 * or null if it could not be found.
	 * 
	 * @param code the Diameter command code
	 * @param applicationId the Diameter application ID
	 * @return a DiameterCommandDefinition or null if not found
	 */
	public DiameterCommandDefinition getCommandDefinitionByCode(long code, long applicationId) {
		return codeToCommand.get(hash(code, applicationId));
	}

	
	/**
	 * Get a Diameter command definition by its abbreviation and application ID
	 * or null if it could not be found.
	 * @param abbrev the abbreviation of a Diameter request or answer
	 * @param applicationId the Diameter application ID
	 * @return a DiameterCommandDefinition or null if not found
	 */
	public DiameterCommandDefinition getCommandDefinitionByAbbreviation(String abbrev, long applicationId) {
		return abbreviationToCommand.get(hash(abbrev, applicationId));
	}

	/**
	 * Get the set of Diameter command definitions contained in this dictionary
	 * 
	 * @return a set of {@link DiameterCommandDefinition DiameterCommandDefinition} 
	 */
	public Set<DiameterCommandDefinition> getDefinitionSet() {
		return new HashSet<>(codeToCommand.values());
	}

	private String hash(long code, long applicationId) {
		return code + "/" + applicationId;
	}

	private String hash(String abbrev, long applicationId) {
		return abbrev.toUpperCase() + "/" + applicationId;
	}

}
