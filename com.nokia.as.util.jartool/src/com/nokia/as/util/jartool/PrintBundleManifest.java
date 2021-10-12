// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.jartool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;

import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;

/**
 * Prints a bundle(s manifest headers, except the ones provided as arguments. The Import-Package header is also checked for well known jdk packages.
 */
public class PrintBundleManifest {
	
	private final static Set<String> JAVAX_ANNOTATION_PKG = new HashSet<>(Arrays.asList("javax.annotation", "javax.annotation.security", "javax.annotation.sql"));
	private final static Set<String> JAVAX_ACTIVATION_PKG = new HashSet<>(Arrays.asList("com.sun.activation.registries", "com.sun.activation.viewers", "javax.activation"));

	public static void main(String... args) throws Exception {
		PrintBundleManifest print = new PrintBundleManifest();
		String[] headersToIgnore = Arrays.copyOfRange(args, 1, args.length);
		print.printManifest(args[0], new HashSet<String>(Arrays.asList(headersToIgnore)));
	}

	private void printManifest(String jar, HashSet<String> headersToIgnore)throws IOException {
		JarFile jarFile = new JarFile(new File(jar));
		Set<Map.Entry<Object, Object>> headers = jarFile.getManifest().getMainAttributes().entrySet();
		List<Map.Entry<Object, Object>> headersSorted = headers.stream().collect(Collectors.toList());
		Collections.sort(headersSorted, (o1, o2) -> o1.getKey().toString().compareTo(o2.getKey().toString()));
		headersSorted.stream()
			.filter(entry -> ! headersToIgnore.contains(entry.getKey().toString()))
			.forEach(this::printManifest);
	}	
	
	void printManifest(Map.Entry<Object,Object> header) {
		String headerName = header.getKey().toString();
		if (headerName.equalsIgnoreCase("Import-Package")) {
			printImportPackage(header.getValue().toString());
		} else if (headerName.equalsIgnoreCase("Export-Package")) {
			printExportPackage(header.getValue().toString());
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(header.getKey());
			sb.append(": ");
			sb.append(header.getValue());				
			System.out.println(sb.toString());
		}
	}

	private void printImportPackage(String importPackage) {
		System.out.println("Import-Package: \\");
		Map<String, Map<String, String>> header = ManifestHeaderProcessor.parseImportString(importPackage);
		Map<String, Map<String, String>> sortedHeader = 
				header.entrySet()
				.stream()
				.sorted(comparingByKey())
				.collect(
				toMap(e -> e.getKey(), e -> e.getValue(),
				(e1, e2) -> e2, LinkedHashMap::new));

		Iterator<String> pkgIterator = sortedHeader.keySet().iterator();
		while (pkgIterator.hasNext()) {
			String pkg = pkgIterator.next();
			StringBuilder sb = new StringBuilder();
			sb.append("  ").append(pkg);
			Map<String, String> attributes = header.get(pkg);
			if (attributes != null) {
				sb.append(";");
				for (String attrName : attributes.keySet()) {
					// handle a specific case: some 3rd party libraries uses some javax.annotation packages with sometimes some versions we don't support.
					// So, just ignore any versions when we find a package being part of javax.annotation API.
					if (attrName.equals("version") && JAVAX_ANNOTATION_PKG.contains(pkg)) {
						continue;
					}
					// handle a specific case: some 3rd party libraries uses some javax.activation packages with sometimes some versions we don't support.
					// So, just ignore any versions when we find a package being part of javax.activation API.
					if (attrName.equals("version") && JAVAX_ACTIVATION_PKG.contains(pkg)) {
						continue;
					}
					sb.append(attrName).append("=\"").append(attributes.get(attrName)).append("\"");
					sb.append(";");
				}
				sb.setLength(sb.length()-1);
			}
			if (pkgIterator.hasNext()) {
				sb.append(",\\");
			}
			System.out.println(sb.toString());
		}
	}

	private void printExportPackage(String exportPackage) {
		List<NameValuePair> parsedExportPackage = ManifestHeaderProcessor.parseExportString(exportPackage);
		StringBuilder sb = new StringBuilder("Export-Package: \\\n");
		for (int i = 0; i < parsedExportPackage.size(); i ++) {
			NameValuePair pair = parsedExportPackage.get(i);
			sb.append("  ");
			sb.append(pair.getName());
			Map<String, String> attributes = pair.getAttributes();
			attributes.forEach((attrName, attrValue) -> {
				if (! attrName.equals("uses:")) {
					sb.append(";");
					sb.append(attrName);
					sb.append("=\"");
					sb.append(attrValue);
					sb.append("\"");
					sb.append(",\\\n");
				}
			});
		}
		if (parsedExportPackage.size() > 0) {
			sb.setLength(sb.length()-3);
		}
		System.out.println(sb.toString());
	}

}
