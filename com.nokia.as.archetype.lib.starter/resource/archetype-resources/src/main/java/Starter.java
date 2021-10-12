// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package $package;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.nokia.as.osgi.launcher.OsgiLauncher;

public class Starter {
	private String[] getBundles(String path) throws Exception {
		Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory
		URL dirURL = getClass().getClassLoader().getResource(path);
		Class<?> clazz = getClass();

		if (dirURL == null) {
			throw new IllegalStateException("Could not find " + path + " from classpath");
		}

		if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
			String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR
																							// file
			JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.startsWith(path) && name.endsWith(".jar")) {
					String entry = name.substring(path.length());
					int checkSubdir = entry.indexOf("/");
					if (checkSubdir >= 0) {
						// if it is a subdirectory, we just return the directory name
						entry = entry.substring(0, checkSubdir);
					}
					result.add(dirURL + entry);
				}
			}
		} else {
			throw new IllegalStateException(path + " found from  classpath");
		}

		return result.stream().toArray(String[]::new);
	}

	public OsgiLauncher getOsgiLauncher() throws Exception {
		ServiceLoader<OsgiLauncher> servLoad = ServiceLoader.load(OsgiLauncher.class);
		OsgiLauncher framework = servLoad.iterator().next();
		return framework.useExceptionHandler(Throwable::printStackTrace)
			     .withBundles(getBundles("CASR-INF/bundles/"))
			     .start();
	}
}
