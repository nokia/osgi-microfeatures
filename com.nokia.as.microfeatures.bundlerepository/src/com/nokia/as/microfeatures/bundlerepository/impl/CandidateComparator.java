// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.bundlerepository.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CandidateComparator implements Comparator<Capability> {

    private final Set<Resource> mandatory;

    public CandidateComparator(Set<Resource> mandatory) {
        this.mandatory = mandatory;
    }

    public int compare(Capability cap1, Capability cap2) {
        int c = 0;
        // Always prefer system bundle
        if (cap1 instanceof BundleCapability && !(cap2 instanceof BundleCapability)) {
            c = -1;
        } else if (!(cap1 instanceof BundleCapability) && cap2 instanceof BundleCapability) {
            c = 1;
        }
        // Always prefer mandatory resources
        if (c == 0) {
            if (mandatory.contains(cap1.getResource()) && !mandatory.contains(cap2.getResource())) {
                c = -1;
            } else if (!mandatory.contains(cap1.getResource()) && mandatory.contains(cap2.getResource())) {
                c = 1;
            }
        }
        // Compare revision capabilities.
        if ((c == 0) && cap1.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)) {
            c = compareNames(cap1, cap2, BundleNamespace.BUNDLE_NAMESPACE);
            if (c == 0) {
                c = compareVersions(cap1, cap2, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
            }
        // Compare package capabilities.
        } else if ((c == 0) && cap1.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)) {
            c = compareNames(cap1, cap2, PackageNamespace.PACKAGE_NAMESPACE);
            if (c == 0) {
                c = compareVersions(cap1, cap2, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                // if same version, rather compare on the bundle version
                if (c == 0) {
                    c = compareVersions(cap1, cap2, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
                }
            }
        // Compare feature capabilities
        } else if ((c == 0) && cap1.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE)) {
            c = compareNames(cap1, cap2, IdentityNamespace.IDENTITY_NAMESPACE);
            if (c == 0) {
                c = compareVersions(cap1, cap2, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            }
        } else if ((c == 0) && cap1.getNamespace().equals("com.nokia.as.feature")) {
            c = compareNames(cap1, cap2, "com.nokia.as.feature");
            if (c == 0) {
                c = compareVersions(cap1.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0), 
                				 	cap2.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0),
                				 	IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            }
        }
        if (c == 0) {
            // We just want to have a deterministic heuristic
            String n1 = ResolverUtil.getSymbolicName(cap1.getResource());
            String n2 = ResolverUtil.getSymbolicName(cap2.getResource());
            c = n1.compareTo(n2);
        }
        return c;
    }

    private int compareNames(Capability cap1, Capability cap2, String attribute) {
        Object o1 = cap1.getAttributes().get(attribute);
        Object o2 = cap2.getAttributes().get(attribute);
        if (o1 instanceof List || o2 instanceof List) {
            List<String> l1 = o1 instanceof List ? (List) o1 : Collections.singletonList((String) o1);
            List<String> l2 = o2 instanceof List ? (List) o2 : Collections.singletonList((String) o2);
            for (String s : l1) {
                if (l2.contains(s)) {
                    return 0;
                }
            }
            return l1.get(0).compareTo(l2.get(0));
        } else {
            return((String) o1).compareTo((String) o2);
        }
    }
    
	private int compareVersions(Capability cap1, Capability cap2, String attribute) {
		Version v1 = (!cap1.getAttributes().containsKey(attribute)) ? Version.emptyVersion
				: getVersion(cap1.getAttributes().get(attribute));
		Version v2 = (!cap2.getAttributes().containsKey(attribute)) ? Version.emptyVersion
				:  getVersion(cap2.getAttributes().get(attribute));
		// Compare these in reverse order, since we want highest version to have priority.
		return v2.compareTo(v1);
	}
	
	private Version getVersion(Object version) {
		if (version instanceof Version) {
			return (Version) version;
		} else {
			return new Version(version.toString());
		}
	}
	

//    private int compareVersions(Capability cap1, Capability cap2, String attribute) {
//        Version v1 = (!cap1.getAttributes().containsKey(attribute))
//                ? Version.emptyVersion
//                : (Version) cap1.getAttributes().get(attribute);
//        Version v2 = (!cap2.getAttributes().containsKey(attribute))
//                ? Version.emptyVersion
//                : (Version) cap2.getAttributes().get(attribute);
//        // Compare these in reverse order, since we want highest version to have priority.
//        return v2.compareTo(v1);
//    }

}
