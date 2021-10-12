// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.jaxrs.adapter;

import java.util.ArrayList;
import java.util.List;

public class WebResourceCollection {

	public WebResourceCollection() {
		super();
	}

	/**
	 * The name of your security constraint
	 */
	private String name;
	/**
	 * The description of your security collection
	 */
	private String description;
	/**
	 * A list of URL patterns that should match to apply the security collection
	 */
	private List<String> patterns = new ArrayList<String>();
	/**
	 * A list of HTTP methods that applies for this security collection
	 */
	private List<String> methods = new ArrayList<String>();
	/**
	 * A list of HTTP methods that will be omitted for this security collection
	 */
	private List<String> omittedMethods = new ArrayList<String>();

	public List<String> getPatterns() {
		return patterns;
	}

	public List<String> getMethods() {
		return methods;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public List<String> getOmittedMethods() {
		return omittedMethods;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPatterns(List<String> patterns) {
		this.patterns = patterns;
	}

	public void setMethods(List<String> methods) {
		this.methods = methods;
	}

	public void setOmittedMethods(List<String> omittedMethods) {
		this.omittedMethods = omittedMethods;
	}

	@Override
	public String toString() {
		return "WebResourceCollection [name=" + name + ", description=" + description + ", patterns=" + patterns
				+ ", methods=" + methods + ", omittedMethods=" + omittedMethods + "]";
	}
}