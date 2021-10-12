// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.urlinstaller;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@Component
public class URLInstaller {

	@Inject
	private BundleContext bc;

	private static final Logger log = Logger.getLogger(URLInstaller.class);
	private String urls;
	private Set<Bundle> installedBundles = new HashSet<>();

	@ConfigurationDependency
	public void updated(URLInstallerConfig conf) {
		urls = conf.getBundleUrls();
		log.debug("Config loaded: " + urls);
	}

	@Start
	void start() {
		if (urls == null || urls.trim().isEmpty()) {
			log.info("null or empty url file");
			return;
		}
		Stream.of(urls.split("\n")).forEach((i) -> {
			try {
				i = i.trim();
				if (i.startsWith("#") || i.isEmpty()) {
					return;
				}
				Bundle b = bc.installBundle(i);
				if (b.getState() == Bundle.ACTIVE) {
					log.warn("Bundle already active");
					return;
				}

				installedBundles.add(b);
				log.warn("Installed " + i);
			} catch (BundleException e) {
				log.warn("Failed to install " + i, e);
			}
		});

		for (Bundle b : installedBundles) {
			try {
				b.start();
				log.warn("Started " + b.getSymbolicName());
			} catch (BundleException e) {
				log.warn("Failed to install " + b.getSymbolicName(), e);
			}

		}
	}

}
