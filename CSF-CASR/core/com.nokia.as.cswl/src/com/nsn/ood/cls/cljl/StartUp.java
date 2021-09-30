/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LoggingConfig;
import com.nokia.licensing.plugins.PluginRegistry;
import com.nsn.ood.cls.cljl.plugin.CLSPreferences;
import com.nsn.ood.cls.util.osgi.transaction.TransactionService;


/**
 * @author marynows
 *
 */
@Component
//@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StartUp {
	private static final Logger LOG = LoggerFactory.getLogger(StartUp.class);
	
	@Start
	public void init() {
		CLSPreferences.importPreferences();

		try {
			PluginRegistry.setCLJLPreferences(new CLSPreferences());
		} catch (final LicenseException e) {
			LOG.error("Error during CLJL initialization.", e);
		}

		LoggingConfig.initialize();
	}

	@Stop
	public void closeLoggers() {
		LoggingConfig.shutdown();
	}
}
