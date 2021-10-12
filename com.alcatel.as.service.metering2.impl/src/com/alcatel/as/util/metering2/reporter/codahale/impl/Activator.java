// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.metering2.reporter.codahale.impl;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;

public class Activator extends DependencyActivatorBase {
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Declare the Metering Cohadale Registry Tracker
      Component comp = createComponent()
          .setImplementation(MeteringCodahaleRegistryImpl.class)
          .setInterface(MeteringCodahaleRegistry.class.getName(), null)
          .add(createConfigurationDependency().setPid("system"))
          .add(createServiceDependency().setService(Monitorable.class, Monitorable.NAME + "=*").setCallbacks("added", "changed", "removed"));
      dm.add(comp);
  }
}
