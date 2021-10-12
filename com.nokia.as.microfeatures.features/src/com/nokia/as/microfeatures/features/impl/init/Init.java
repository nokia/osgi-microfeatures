// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.microfeatures.features.impl.init;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.PropertyConfigurator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.nokia.as.microfeatures.features.impl.common.Helper;

/**
 * This component is assumed to be started as the first bundle, in order to
 * perform initialization (essentially log4j, and autoconfiguration of system properties into OSGi ConfigAdmin). 
 */
@Component
public class Init {

	private final Set<String> _scriptCommands = new HashSet<>(Arrays.asList("list", "create", "create-legacy", "create-all", "update.features", "update.obr"));

	@Inject
	BundleContext _bc;
	
	@ServiceDependency
	ConfigurationAdmin _cm;

	@Start
	void start() {
		try {
			// If running in java8, install jre18 bundle
			installJre18();
			
			// Initialize log4j
			if (_bc.getProperty("help") != null) {
				System.out.println("Usage: the command accepts the following system properties:");
				System.out.println("\t-Dm2 -> ~/.m2/repository/obr.xml will be used as the OBR.");
				System.out.println(
						"\t-Dobr -> specifies the remote OBR url (default=" + System.getProperty("obr.remote") + ")");
				System.out.println("\t-Dlist -> used to display the available features");
				System.out.println(
						"\t-Dcreate=appname,appversion,feature1,feature2,... -> this command creates a runtime.");
				System.out.println(
						"\t-Dupdate.features=<path to your runtime>,<list of features separated by comma> -> this command adds some new features to an existing runtime.");
				System.out.println(
						"\t-Dupdate.obr=<path to your runtime>,OBR url,[<list of features separated by comma>] -> this command updates an existing runtime with a new OBR, and opitonally with some additiona features..");
				System.out.println(
						"\t-Dfailok=true -> don't fail if some dependencies can't be downloaded. By default, the command fails if dependencies can't be downloaded.");
				System.out.println();
				System.exit(0);
			}

			System.out.println("Initializing Microfeatures; version " + getVersion());
			
			// init log4j
			PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("META-INF/log4j.properties"));

			// init jul
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream("META-INF/jul.properties");
			try {
				LogManager.getLogManager().readConfiguration(stream);
			} catch (SecurityException | IOException e1) {
				e1.printStackTrace();
			}

			// the following make sure felix http service uses jul
			System.getProperties().setProperty("org.apache.felix.http.log.jul", "");
			
			// determine obr storage directory (default=/tmp/store in script mode, or ./store in gui mode)
			String storeDir = System.getProperty("obr.local", scriptMode() ? Files.createTempDirectory("microfeatures.obr-").toString() : "microfeatures.obr");
			
			// In script mode, clear store dir at startup, and when jvm exits
			if (scriptMode()) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> Helper.removeDirectory(new File(storeDir))));
			} else {
				// start interactive gogo jline
				startBundle("org.apache.felix.gogo.jline");
			}

			// autoconfigure system properties
			autoConfigureSystemProps(storeDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getVersion() {
		return Helper.getMicrofeaturesVersion(_bc);
	}

	/**
	 * lookup any system properties having syntax "pid:propkey=propvalue"
	 */
	private void autoConfigureSystemProps(String storeDir) {
		// configure Ace OBR storage dir in system properties
		System.setProperty("org.apache.ace.obr.storage.file:fileLocation", storeDir);

		// Now populate all system properties into config admin 
		Map<String, Map<String, Object>> config = new HashMap<>();
		
		Map<String, Object> systemProps =
				System.getProperties()
					  .entrySet()
					  .stream()
					  .collect(Collectors.toMap(e -> (String) e.getKey(), 
							  					Entry::getValue));
		Map<String, Object> envProps = new HashMap<>();
		envProps.putAll(System.getenv());
		
		systemProps.forEach((k, v) -> {
			String[] splitKey = k.split(":");
			if(splitKey.length != 2) return;
			
			String pid = splitKey[0];
			Map<String, Object> m = config.get(pid);
			if(m == null) config.put(pid, new HashMap<>());
			m = config.get(pid);
			m.put(splitKey[1], v);
		});
		
		config.forEach((pid, map) -> {
			try {
				Configuration conf = _cm.getConfiguration(pid, "?");
				conf.update(new Hashtable(map));
			} catch (Exception e) {
				System.out.println("Could not provision configuration");
				e.printStackTrace();
			}
		});
	}

	private boolean scriptMode() {
		Optional<String> cmdPresent = _scriptCommands.stream().filter(cmd -> _bc.getProperty(cmd) != null).findFirst();
		return cmdPresent.isPresent();
	}

	/**
	 * Install jre 18 system packages. We only do this if jre is 1.8 and if the jre18 system bundle is not already installed.
	 */
	private void installJre18() throws Exception {
		if (System.getProperty("java.specification.version").equalsIgnoreCase("1.8")) {
			Optional<Bundle> jre18 = Stream.of(_bc.getBundles())
					.filter(b -> b.getSymbolicName().equals(Helper.JRE18_BSN)).findFirst();
			if (!jre18.isPresent()) {
				Enumeration<URL> entries = _bc.getBundle().findEntries("META-INF/", Helper.JRE18_BSN + "*.jar", false);
				if (!entries.hasMoreElements()) {
					throw new IllegalStateException("JRE 1.8 system bundle not found from local obr");
				}
				URL systemBundle = entries.nextElement();
				try (BufferedInputStream in = new BufferedInputStream(systemBundle.openStream())) {
					_bc.installBundle(systemBundle.toString(), in);
				}
			}
		}
	}
		
	private void startBundle(String bsn) throws Exception {
		Enumeration<URL> entries = _bc.getBundle().findEntries("META-INF/", bsn + "*.jar", false);
		if (!entries.hasMoreElements()) {
			throw new IllegalStateException("Bundle " + bsn + " not found from " + _bc.getBundle().getSymbolicName() + " bundle.");
		}
		URL systemBundle = entries.nextElement();
		try (BufferedInputStream in = new BufferedInputStream(systemBundle.openStream())) {
			Bundle bundle = _bc.installBundle(systemBundle.toString(), in);
			bundle.start();
		}
	}
		
//	private void installJre18() throws Exception {
//		if (System.getProperty("java.specification.version").equalsIgnoreCase("1.8")) {
//			// for jre18, some of ASR obrs do not provide a jre 1.8 system bundles (which reexport all jdk packages).
//			// In this case, we need to dynamically install in our runtime the jre18 system bundle which is embedded in our META-INF directory
//			List<Resource> resource = findResources("osgi.identity", "(osgi.identity=" +  ASR_JRE18_SYSTEM_BUNDLE + ")");
//			if (resource.size() == 0) {
//				Enumeration<URL> entries = _bctx.getBundle().findEntries("META-INF/", ASR_JRE18_SYSTEM_BUNDLE + "*.jar", false);
//				if (! entries.hasMoreElements()) {
//					throw new IllegalStateException("JRE 1.8 system bundle not found from local obr");
//				}
//				URL systemBundle = entries.nextElement();
//				try(BufferedInputStream in = new BufferedInputStream(systemBundle.openStream())) {
//					System.out.println("XX: installing " + systemBundle);
//					_bctx.installBundle(systemBundle.toString(), in);
//				}
//			} else {
//				System.out.println("XX: NOT INSTALLING JRE18");
//			}
//		}
//	}

}
