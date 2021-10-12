// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.features.impl.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.nokia.as.microfeatures.features.Feature;

public class FeatureImpl implements Feature {	

	private final Type _type; 
	private final String _name;
	private final String _bsn;
	private final String _version;
	private final String _doc;
	private final String _desc;
	private final Set<String> _categories = new HashSet<>();
	private final String _url;
	private final Map<String, Object> _attributes;
	private final Set<String> _alias;

	public FeatureImpl(Type type, String name, String bsn, String version, String desc, String docUrl, String url, Map<String, Object> attributes, Set<String> alias) {
		_type = type;
		_name = name;
		_bsn = bsn;
		_version = version;
		_doc = docUrl;
		_desc = desc;
		_url = url;
		_attributes = attributes;
		_alias = alias;
		
		// derive the category from the feature name. we assume that the name is in the forms "category.xx.yy..." and in this case
		// we extract the category from the feature prefix.
		
		int dot = _name.indexOf(".");
		if (dot != -1) {
			addCategory(_name.substring(0, dot));			
		}
	}
	
	@Override
	public Optional<Object> getAttributes(String name) {
		return Optional.ofNullable(_attributes.get(name));
	}

	@Override
	public String getName() {
		return _name;
	}
	
	@Override
	public String getVersion() {
		return _version;
	}
	
	@Override
	public Set<String> getCategories() {
		return Collections.unmodifiableSet(_categories);
	}
	
	@Override
	public String toString() {
		return getSymbolicName() + "/" + getVersion() + ": " + getName() + (_categories.size() == 0 ? "" : (" " + _categories));
	}
	
	@Override
	public String getDoc() {
		return _doc;
	}	
	
	@Override
	public String getDesc() {
		return _desc;
	}

	void addCategory(String category) {
		_categories.add(category);
	}
	
	@Override
	public String getSymbolicName() {
		return _bsn;
	}

	@Override
	public Type getType() {
		return _type;
	}

	@Override
	public String getURL() {
		return _url;
	}

	@Override
	public Set<String> getAliases() {
		return _alias;
	}

}
