// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.impl.asrcommands;

import static org.osgi.framework.Constants.PROVIDE_CAPABILITY;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Component(provides = Object.class)
@Property(name = "osgi.command.scope", value = "casr.system.features")
@Property(name = "osgi.command.function", value = { "info" })
public class AsrVersion {

	@Inject
	private volatile BundleContext _bctx;

	private final static String FEATURE_NAMESPACE = "com.nokia.as.feature";
	private final static String FEATURE_SNAPSHOT = "SNAPSHOT";
	private final static String OBR = "X-CSF-OBR";
	private final static String REQUIREMENTS = "X-CSF-Requirements";

	public void info() throws Exception {
		try {
			// Lookup the bundle which has an "Application Snapshot" Provide-Capability
			// header: this is this bundle which contains version and deployed features

			Optional<Bundle> ob = lookupSnapshotBundle();
			if (!ob.isPresent()) {
				System.err.println("can't find version: CASR microfeatures snapshot bundle not found");
				return;
			}
			// Lookup OBR used to create this runtime
			Bundle b = ob.get();
			String obr = b.getHeaders().get(OBR);
			if (obr == null) {
				System.err.println("can't find OBR from bundle " + b.getSymbolicName());
				return;
			}

			System.out.println("OBR: " + obr);
			displayFeatures(b);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void displayFeatures(Bundle b) {
		String features = b.getHeaders().get(REQUIREMENTS);
		if (features != null) {
			StringBuilder sb = new StringBuilder();
			Pattern p = Pattern.compile("\\\"([^\\\"]*)\\\"");
			Matcher m = p.matcher(features);
			while (m.find()) {
				String filter = m.group(1);
				Pattern p2 = Pattern.compile(".*\\(com.nokia.as.feature=(.*)\\)\\(version=(.*)\\)\\)\\)");
				Matcher m2 = p2.matcher(filter);
				if (m2.find()) {
					sb.append(m2.group(1)).append(":").append(m2.group(2)).append(",");
				}
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length()-1);
				System.out.println("Features: " + sb);
			}
		}
	}

	private Optional<Bundle> lookupSnapshotBundle() throws Exception {
		return Stream.of(_bctx.getBundles()).filter(b -> isAppSnapshot(b)).findFirst();
	}

	private boolean isAppSnapshot(Bundle b) {
		String pc = b.getHeaders().get(PROVIDE_CAPABILITY);
		if (pc == null) {
			return false;
		}
		List<GenericMetadata> capabilities = ManifestHeaderProcessor.parseCapabilityString(pc);
		for (GenericMetadata cap : capabilities) {
			if (FEATURE_NAMESPACE.equals(cap.getNamespace())) {
				if (FEATURE_SNAPSHOT.equals(cap.getAttributes().get("type"))) {
					// something like Provide-Capability:
					// com.nokia.as.feature;com.nokia.as.feature="Application
					// Snapshot";type=SNAPSHOT;version=1.0.0;assembly.name=test;assembly.version=1.0.0
					return true;
				}
			}
		}
		return false;
	}

}
