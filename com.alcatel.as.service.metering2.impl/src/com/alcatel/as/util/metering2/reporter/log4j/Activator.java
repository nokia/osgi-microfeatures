// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.log4j;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;

public class Activator extends DependencyActivatorBase {
  
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Declare the Log4j metrics reporter Component
      Component log4jReporter = createComponent()
        .setImplementation(Log4jMetricsReporter.class)
        .add(createConfigurationDependency().setPid(Log4jMetricsReporter.PID))
        .add(createServiceDependency().setService(MeteringCodahaleRegistry.class).setRequired(true));
      dm.add(log4jReporter);
  }
  
}
