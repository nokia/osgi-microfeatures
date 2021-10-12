// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import com.nokia.as.microfeatures.bundlerepository.RequirementBuilder;

public class RequirementBuilderImpl implements RequirementBuilder, Requirement {

	/**
	 * MIME type to be stored in the extra field of a {@code ZipEntry} object
	 * for an installable bundle file.
	 * 
	 * @see org.osgi.service.provisioning.ProvisioningService#MIME_BUNDLE
	 */
	public final static String MIME_BUNDLE = "application/vnd.osgi.bundle";

	/**
	 * Alternative MIME type to be stored in the extra field of a
	 * {@code ZipEntry} object for an installable bundle file.
	 * 
	 * @see org.osgi.service.provisioning.ProvisioningService#MIME_BUNDLE_ALT
	 */
	public final static String MIME_BUNDLE_ALT = "application/x-osgi-bundle";

	final private String namespace;
	final private Map<String, Object> attributes = new HashMap<String, Object>();
	final private Map<String, String> directives = new HashMap<String, String>();

	public RequirementBuilderImpl(final String ns) {
		namespace = ns;
	}

	public RequirementBuilderImpl(final String ns, final String nsFilter) {
		namespace = ns;
		addDirective("filter", "(" + ns + "=" + nsFilter + ")");
	}

	public void addAttribute(final String key, final Object val) {
		attributes.put(key, val);
	}

	public void addDirective(final String key, final String val) {
		directives.put(key, val);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public Resource getResource() {
		return null;
	}

	public void addBundleIdentityFilter() {
		String bf = eq(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE);
		String ff = eq(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_FRAGMENT);
		multiOpFilter('&', multiOp('|', bf, ff));
	}

	public void addBundleContentFilter() {
		String bf = eq(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MIME_BUNDLE);
		String ff = eq(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MIME_BUNDLE_ALT);
		multiOpFilter('&', multiOp('|', bf, ff));
	}

	public void addVersionRangeFilter(VersionRange versionRange) {
		multiOpFilter('&', versionRange.toFilterString("version"));
	}

	public void multiOpFilter(char op, String... andFilter) {
		if (andFilter.length == 0) {
			throw new IllegalArgumentException("Expected at least one argument");
		}
		String[] f;
		String filter = directives.get("filter");
		if (filter != null) {
			f = new String[andFilter.length + 1];
			f[0] = filter;
			System.arraycopy(andFilter, 0, f, 1, andFilter.length);
		} else {
			f = andFilter;
		}
		addDirective("filter", multiOp(op, f));
	}

	public String multiOp(char op, String... args) {
		if (args.length == 1) {
			return args[0];
		} else if (args.length > 1) {
			StringBuffer f = new StringBuffer("(");
			f.append(op);
			for (String a : args) {
				f.append(a);
			}
			return f.append(')').toString();
		} else {
			throw new IllegalArgumentException("Expected at least one argument");
		}
	}

	public String op(char op, String l, String r) {
		return "(" + l + op + r + ")";
	}

	public String eq(String l, String r) {
		return op('=', l, r);
	}

	@Override
	public String toString() {
		return "BasicRequirement [namespace=" + namespace + ", attributes=" + attributes + ", directives=" + directives
				+ "]";
	}

	@Override
	public Requirement build() {
		return this;
	}

}
