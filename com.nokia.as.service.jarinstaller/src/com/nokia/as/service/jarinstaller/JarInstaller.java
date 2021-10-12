// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.jarinstaller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@Component
public class JarInstaller {
	private static final Logger log = Logger.getLogger(JarInstaller.class);
	@Inject
	private BundleContext bc;
	private Path[] searchDirPaths;
	private JarInstallerConfig conf;

	private Set<Bundle> installedBundles = new HashSet<>();

	@ConfigurationDependency
	public void updated(JarInstallerConfig conf) {
		this.conf = conf;
		searchDirPaths = setupSearchDirPaths();
		log.info("Configured Jar Installer paths: " + Arrays.toString(searchDirPaths));
	}

	@Start
	void start() {
		installJars(searchDirPaths);
		if (!installedBundles.isEmpty()) {
			log.info("Starting wrapped bundles...");
			startBundles(installedBundles);
		}
	}

	private void startBundles(Set<Bundle> bundles) {
		for (Bundle b : bundles) {
			log.warn("Starting bundle " + b.getSymbolicName());
			try {
				b.start();
			} catch (BundleException e) {
				log.error("Couldn't start bundle " + b.getSymbolicName(), e);
			}
		}
	}

	private void installJar(String jarPath, String bndPath) {
		try {
			if (Files.exists(Paths.get(bndPath))) {
				log.warn("Installing JAR " + jarPath + " using instruction file " + bndPath);
				Bundle b = bc.installBundle("wrap:file:" + jarPath + ",file:" + bndPath);
				installedBundles.add(b);
			} else {
				log.warn("Installing JAR " + jarPath);
				Bundle b = bc.installBundle("wrap:file:" + jarPath);
				installedBundles.add(b);
			}
		} catch (BundleException e) {
			log.error("Failed to wrap or install wrapped jar " + jarPath, e);
		}
	}

	private void installWar(String stringPath, String bndStrPath) {
		try {
			Path bndPath = Paths.get(bndStrPath);
			if (Files.exists(bndPath)) {
				log.warn("Installing war " + stringPath + " using instruction file " + bndStrPath);
				Path p = Files.createTempFile("casr-wrapped-war", ".bnd");

				try (BufferedWriter writer = Files.newBufferedWriter(p)) {
					Files.lines(bndPath).forEach(line -> {
						try {
							writer.append(line);
							writer.newLine();
						} catch (IOException e) {
							log.warn("error writing line!", e);
						}
					});

					writer.append("WAR-URL: file:" + stringPath);
					writer.newLine();
				}
				Bundle b = bc.installBundle("warref:file:" + p.toAbsolutePath().toString());
				installedBundles.add(b);
			} else {
				log.warn("Installing war " + stringPath);
				Bundle b = bc.installBundle("war:file:" + stringPath);
				installedBundles.add(b);
			}
		} catch (BundleException e) {
			log.error("Failed to wrap or install wrapped war " + stringPath, e);
		} catch (IOException e) {
			log.error("I/O Error, cannot install war", e);
		}
	}
	
	private boolean isBundle(String path) {
		try(JarFile theJar = new JarFile(path)) {
			Manifest mft = theJar.getManifest();
			if(mft == null) {
				return false;
			}
			
			Attributes attrs = mft.getMainAttributes();
			if(attrs == null) {
				return false;
			}
						
			if(attrs.getValue("Bundle-SymbolicName")!= null) {
				return true;
			}
			return false;
		} catch(IOException e) {
			log.warn("Error when opening the JAR to check the manifest", e);
			return false;
		}
	}

	private void installJars(Path[] paths) {
		for (Path d : paths) {
			if (!Files.exists(d)) {
				log.warn("Directory " + d + " does not exist");
				continue;
			}
			log.info("Attempting to wrap and install jar/war in " + d.toAbsolutePath());
			try (DirectoryStream<Path> candidates = Files.newDirectoryStream(d, "*.{jar,war}")) {
				candidates.forEach(jarPath -> {
					Path resolved = jarPath.toAbsolutePath();
					String stringPath = resolved.toString();
					if(isBundle(stringPath)) {
						log.warn(jarPath.getFileName() + " is already an OSGi bundle, "
								+ "please move it to your bundle dir.");
						return;
					}
					String bndPath = stringPath.substring(0, stringPath.lastIndexOf(".")) + ".bnd";
					if (stringPath.toLowerCase().endsWith("jar")) {
						installJar(stringPath, bndPath);
					} else {
						installWar(stringPath, bndPath);
					}
				});
			} catch (IOException e) {
				log.error("Error when walking path " + d, e);
			}
		}
	}

	private Path[] setupSearchDirPaths() {
		String dirPaths = conf.getJarDirs();
		Path[] paths;

		if (dirPaths != null) {
			StringTokenizer st = new StringTokenizer(dirPaths, ",: ");
			paths = new Path[st.countTokens()];
			int i = 0;
			while (st.hasMoreTokens()) {
				try {
					String token = st.nextToken().trim();
					Path dir = Paths.get(token);
					paths[i++] = dir;

				} catch (InvalidPathException e) {
					log.warn(e.getMessage());
				}
			}
		} else {
			paths = new Path[0];
		}

		return paths;
	}
}
