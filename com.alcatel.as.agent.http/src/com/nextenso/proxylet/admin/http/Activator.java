// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.http;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import com.alcatel.as.management.platform.ConfigManager;

import com.alcatel.as.management.platform.CreateInstancePlugin;

/**
 * Only activate this service in the webadmin, and not in the http agent bundle.
 * (don't know why ...)
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bc, DependencyManager dm) throws Exception {
        Properties props = new Properties();
        props.setProperty("protocol", "http");
        Component agg = createComponent()
            .setInterface(CreateInstancePlugin.class.getName(), props)
            .setImplementation(HttpBearerAggregator.class)
            .add(createServiceDependency().setService(ConfigManager.class).setRequired(true).setCallbacks("bindConfigManager",  null));
        dm.add(agg);
    }

}
