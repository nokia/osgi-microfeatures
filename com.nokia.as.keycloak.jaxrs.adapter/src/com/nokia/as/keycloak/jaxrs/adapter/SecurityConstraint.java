// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.jaxrs.adapter;

import java.util.List;
import java.util.Set;

public class SecurityConstraint {
	public SecurityConstraint() {
		super();
	}

	private Set<String> authRoles;
	private List<WebResourceCollection> webResourceCollection;

	public Set<String> getAuthRoles() {
		return authRoles;
	}

	public void setAuthRoles(Set<String> authRoles) {
		this.authRoles = authRoles;
	}

	public List<WebResourceCollection> getWebResourceCollection() {
		return webResourceCollection;
	}

	public void setWebResourceCollection(List<WebResourceCollection> webResourceCollection) {
		this.webResourceCollection = webResourceCollection;
	}

	public void addWebResourceCollection(WebResourceCollection collection) {
		webResourceCollection.add(collection);
	}
}