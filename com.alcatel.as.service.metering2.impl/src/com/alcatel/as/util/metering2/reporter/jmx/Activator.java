// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.jmx;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;

public class Activator extends DependencyActivatorBase {
  
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Declare the JmxMetricsReporter Component.
      Component jmxReporter = createComponent()
        .setImplementation(JmxMetricsReporter.class)
        .add(createServiceDependency().setService(MeteringCodahaleRegistry.class).setRequired(false).setCallbacks("added", "changed", "removed"));
      dm.add(jmxReporter);
  }
}
