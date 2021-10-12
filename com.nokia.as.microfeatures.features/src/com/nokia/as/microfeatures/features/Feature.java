// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.features;

import java.util.Optional;
import java.util.Set;

/**
 * Represents a feature found from the bundle repositories.
 * A Feature extends the standard OSGi org.osg.resource.Resoruce interface, in order
 * to allow to access to all possible informations available from the resource bundle.
 * (exported packages, imported packages, etc ...).
 */
public interface Feature {
	
	final String NAMESPACE = "com.nokia.as.feature";
	final String VERSION = "version";
	final String DOC = "doc";
	final String DESC = "desc";
	final String CATEGORY = "category";
	final String ALIAS = "alias";
	final String TYPE = "type";
	final String ASMB_BSN = "assembly.name";
	final String ASMB_VERSION = "assembly.version";
	final String INTERNAL = "internal";
	final String BLACKLIST_IDENTITY = "blacklist.identity";
	
	/**
	 * Feature types.
	 */
	public enum Type {
		/**
		 * default type: a user feature which can be either public or internal.
		 */
		FEATURE, 
		
		/**
		 * An assembly feature, which holds many features.
		 */
		ASMB,
		
		/**
		 * A resolved assembly with list of bundles being part of the assembly resolution.
		 */
		SNAPSHOT,		
	}	
	
	// Returns feature URL
	String getURL();
	
	// Returns feature type
	Type getType();
	
	// feature description name
	String getName();
	
	// feature bundle symbolic name
	String getSymbolicName();
	
	// feature version
	String getVersion();
		
	// feature categories (only available for type=FEATURE)
	Set<String> getCategories();
	
	// feature categories (only available for type=FEATURE)
	Set<String> getAliases();

	// feature doc url (only availale for type=FEATURE)
	String getDoc();
	
	// feature desc (only available for type=FEATURE)
	String getDesc();
	
	// return any feature attributes
	Optional<Object> getAttributes(String name);	
}
