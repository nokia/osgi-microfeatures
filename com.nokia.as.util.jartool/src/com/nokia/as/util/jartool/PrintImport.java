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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;

/**
 * Prints Import-Package header from a given bundle
 */
public class PrintImport {
	
	private final static Set<String> JAVAX_ANNOTATION_PKG = new HashSet(Arrays.asList("javax.annotation", "javax.annotation.security", "javax.annotation.sql"));
	private final static Set<String> JAVAX_ACTIVATION_PKG = new HashSet(Arrays.asList("com.sun.activation.registries", "com.sun.activation.viewers", "javax.activation"));

	public static void main(String... args) throws Exception {
		PrintImport print = new PrintImport();
		print.printImport(args[0]);
	}

	private void printImport(String jar) throws IOException {
		JarFile jarFile = new JarFile(new File(jar));
		
		Optional<String> importPackage = jarFile.getManifest().getMainAttributes().entrySet().stream()
				.filter(entry -> entry.getKey().toString().equals("Import-Package"))
				.map(entry -> entry.getValue().toString()).findFirst();

		if (importPackage.isPresent()) {
			System.out.println("Import-Package: \\");
			Map<String, Map<String, String>> header = ManifestHeaderProcessor.parseImportString(importPackage.get());
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
			
			System.exit(0);
		} else {
			System.exit(1);
		}
	}

}
