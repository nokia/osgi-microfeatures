// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.legacy;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;

import alcatel.tess.hometop.gateways.utils.Log;

@SuppressWarnings("rawtypes")
public class LegacyConfiguration {
	
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private final String pid;
	private volatile Optional<ServiceRegistration<Dictionary>> reg = Optional.empty();
	private BundleContext bc;
	
	public LegacyConfiguration(String pid, BundleContext bc) {
		logger.debug("Creating legacy configuration for pid %s", pid);
		this.pid = pid;
		this.bc = bc;
	}
	
	public void update(Dictionary<?,?> conf) {
		logger.debug("Updating legacy configuration for pid %s", pid);
		// create a new conf service
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("service.pid", pid);
		ServiceRegistration<Dictionary> reg = bc.registerService(Dictionary.class, conf, props);
		// unregister previous one
		this.reg.ifPresent(previous -> previous.unregister());			
		this.reg = Optional.of(reg);
	}
	
	public void delete() {
		logger.debug("Deleting legacy configuration for pid %s", pid);
		this.reg.ifPresent(previous -> previous.unregister());
	}
}
