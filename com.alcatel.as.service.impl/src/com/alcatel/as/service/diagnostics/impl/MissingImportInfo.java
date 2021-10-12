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
class MissingImportInfo {
	private final BundleContext _bc;
	// map of all known exported packages. Key = package, value = export-package info
	private final Map<String, List<Exporter>> _allExportedPkgs = new HashMap<>();
	
	private class Exporter {
		final Bundle _bundle;
		final Map<String, String> _attributes;
		
		Exporter(Bundle b, Map<String, String> exportedPkgAttributes) {
			_bundle = b;
			_attributes = exportedPkgAttributes;
		}
	}

	MissingImportInfo(BundleContext bc) {
		_bc = bc;
		// calculate all exported packages.
		Stream.of(_bc.getBundles()).forEach(this::parseExportedPackages);
	}

	void displayMissingImports(Bundle unresolvedBundle) {
		boolean headerDisplayed = false;
		try {
			// Parse Import-Package using aries tool
			Map<String, Map<String, String>> importPkgs = ManifestHeaderProcessor.parseImportString(unresolvedBundle.getHeaders().get("Import-Package"));
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, Map<String, String>> e : importPkgs.entrySet()) {
				if (!isProvided(e.getKey(), e.getValue())) {
					if (!headerDisplayed) {
						headerDisplayed = true;
						sb.append(" -> missing packages [");
					}
					sb.append(e.getKey());
					sb.append("; ");
				}
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length()-2);
				sb.append("]");
			}
			System.out.print(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		List<NameValuePair> exportedPkgs = ManifestHeaderProcessor.parseExportString(b.getHeaders().get("Export-Package"));
		exportedPkgs.stream().forEach(pair -> {
			
			List<Exporter> exporters = _allExportedPkgs.computeIfAbsent(pair.getName(), (name) -> new ArrayList<>());
			exporters.add(new Exporter(b, pair.getAttributes()));
		});
	}
}
