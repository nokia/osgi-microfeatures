// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class ResolverUtil {
	final static Pattern NOKIA_OBR_PATTERN = Pattern
			.compile(".*com\\.nokia\\.casr\\.obr-(\\d+)\\.(\\d+)\\.(\\d+)\\.xml");

	public static String getSymbolicName(Resource resource) {
		List<Capability> caps = resource.getCapabilities(null);
		for (Capability cap : caps) {
			if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE)) {
				return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
			}
		}
		return null;
	}
	
	/**
	 * Detect if the OBR url is an official Nokia OBR, and check if this obr 
	 * needs to resolve optional dependencies
	 * 
	 * We need to resolve optional dependencies for OBR <= 18.3.2
	 */
	public static boolean detectNokiaObrsWhichNeedsOptionalDependencies(String url) {
		// TODO for the moment, unfortunately we have many optional imports, so we must assume all 
		// obrs must be resolved with optional dependencies
		if (true) return true;
		
		Matcher matcher = NOKIA_OBR_PATTERN.matcher(url);
		if (matcher.find()) {
			int major = Integer.valueOf(matcher.group(1));
			int minor = Integer.valueOf(matcher.group(2));
			int micro = Integer.valueOf(matcher.group(3));

			if (major < 18) {
				return true;
			}
			
			if (major == 18) {
				if (minor < 3) {
					return true;
				}
				
				if (minor == 3 && micro <= 2) {
					return true;
				}
			}
		} 
				
		return false;		
	}
	
	/**
	 * Detects if the OBR url is an official Nokia OBR, and check if this obr 
	 * has the CSFAR-1822 bug.
	 */
	public static boolean detectNokiaObrsWithCSFAR1822Bug(String url) {		
		Matcher matcher = NOKIA_OBR_PATTERN.matcher(url);
		if (matcher.find()) {
			int major = Integer.valueOf(matcher.group(1));
			int minor = Integer.valueOf(matcher.group(2));
			int micro = Integer.valueOf(matcher.group(3));

			return major == 19 && minor == 3 && (micro == 1 || micro == 2);
		} 				
		return false;		
	}
	
	/**
	 * Detects if the OBR url is an official Nokia OBR, and check if this obr 
	 * must be resolved by forcing selection of highest bundle version.
	 * (all OBRs <= 19.4.2 must be resolved by selecting highest bundle version, 
	 * onward OBRS can be resolved normally). 
	 * See CSFAR-1904
	 */
	public static boolean resolveWithHigestBundleVersion(String url) {		
		Matcher matcher = NOKIA_OBR_PATTERN.matcher(url);
		if (matcher.find()) {
			int major = Integer.valueOf(matcher.group(1));
			int minor = Integer.valueOf(matcher.group(2));
			int micro = Integer.valueOf(matcher.group(3));

			return major <= 19 && minor <= 4 && micro <= 2;
		} 				
		return false;		
	}

}
