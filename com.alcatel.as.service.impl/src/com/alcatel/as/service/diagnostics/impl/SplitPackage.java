// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.diagnostics.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Helper class used to display the list of missing import packages which
 * prevent a bundle to be started
 *
 */
class SplitPackage {
	private final BundleContext _bc;
	// map of all known exported packages. Key = package, value = export-package
	// info
	private final Map<String, List<Exporter>> _allExportedPkgs = new HashMap<>();

	private class Exporter {
		final Bundle _bundle;
		final Map<String, String> _attributes;

		Exporter(Bundle b, Map<String, String> exportedPkgAttributes) {
			_bundle = b;
			_attributes = exportedPkgAttributes;
		}
	}

	SplitPackage(BundleContext bc) {
		_bc = bc;
		// calculate all exported packages.
		Stream.of(_bc.getBundles()).forEach(this::parseExportedPackages);
	}

	void detectSplitPackages() {
		boolean headerDisplayed = false;
		for (Map.Entry<String, List<Exporter>> e : _allExportedPkgs.entrySet()) {
			String pkg = e.getKey();
			List<Exporter> exporters = e.getValue();
			StringBuilder sb = new StringBuilder();
			if (isSplitSpackage(exporters)) {
				if (!headerDisplayed) {
					headerDisplayed = true;
					System.out.println("Detected split packages: ");
				}
				sb.append(" * " + pkg + " exported by ");
				for (Exporter exporter : e.getValue()) {
					sb.append(exporter._bundle.getSymbolicName() + "/" + exporter._bundle.getVersion() + " [" + exporter._bundle.getBundleId() + "]");
					sb.append("; ");
				}
				sb.setLength(sb.length() - 2);
				System.out.println(sb.toString());
			}
		}
	}

	private boolean isSplitSpackage(List<Exporter> exporters) {
		if (exporters.size() > 1) {			
			for (Exporter exporter : exporters) {
				String version = exporter._attributes.get("version");
				if (version == null || version.startsWith("0.0.0")) {
					if (exporter._bundle.getBundleId() != 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean isProvided(String importedPkg, Map<String, String> importedAttrs) {
		// see if imported package is exported by someone
		List<Exporter> potentialExporters = _allExportedPkgs.get(importedPkg);
		if (potentialExporters == null) {
			return false;
		}
		return true;
	}

	private void parseExportedPackages(Bundle b) {
		List<NameValuePair> exportedPkgs = ManifestHeaderProcessor
				.parseExportString(b.getHeaders().get("Export-Package"));
		exportedPkgs.stream().forEach(pair -> {
			List<Exporter> exporters = _allExportedPkgs.computeIfAbsent(pair.getName(), (name) -> new ArrayList<>());
			exporters.add(new Exporter(b, pair.getAttributes()));
		});
	}
}
