// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.management.gs.impl;

import org.apache.felix.dm.DependencyManager;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.management.ShutdownService;
import com.alcatel.as.util.osgi.DependencyActivatorHelper;
public class Activator extends DependencyActivatorHelper {
  final static Logger logger = Logger.getLogger("as.management.gs");
  public Activator() {
    super(logger);
  }
  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    super.init(ctx, dm);
    addService(createService().setInterface(ShutdownService.class.getName(),null).setImplementation(ShutdownServiceImpl.class));
  }
}

