// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.diagnostics.impl;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.scr.ScrService;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_SCOPE;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_FUNCTION;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import com.alcatel.as.service.diagnostics.ServiceDiagnostics;

public class Activator extends DependencyActivatorBase {
	@Override
	public void init(BundleContext context, DependencyManager manager) throws Exception {
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(COMMAND_SCOPE, "casr.system.services");
		props.put("casr." + COMMAND_SCOPE + ".alias", "asr.service");
		props.put(COMMAND_FUNCTION, new String[] { "list", "diag", "dsdiag" });
		Component c = manager.createComponent()
				.setImplementation(ServiceDiagnosticsImpl.class)
		                .setInterface(ServiceDiagnostics.class.getName(), props)
				.add(createServiceDependency().setService(ScrService.class).setRequired(true).setCallbacks("setScrService", null))
				.add(createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true).setCallbacks("setConfigAdmin", null));
		manager.add(c);				
	}
}
