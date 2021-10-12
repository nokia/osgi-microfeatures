// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.radius.agg;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.management.platform.ConfigManager;
import com.alcatel.as.management.platform.CreateInstancePlugin;

public class Activator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext ctx, DependencyManager mgr) throws Exception {
		// Proxylet Aggregator
		Component aggregrator = createComponent();
		Hashtable<String, Object> props = new Hashtable<>();
		props.put("protocol", "radius");		
		aggregrator.setImplementation(RadiusBearerAggregator.class)
				   .setInterface(CreateInstancePlugin.class.getName(), props)
				   .add(createServiceDependency().setService(ConfigManager.class).setRequired(true).setCallbacks("bindConfigManager", null));
		mgr.add(aggregrator);
	}
}
