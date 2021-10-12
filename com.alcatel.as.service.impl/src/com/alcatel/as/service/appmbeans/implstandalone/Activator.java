// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.appmbeans.implstandalone;

import java.util.Dictionary;

import org.apache.felix.dm.DependencyManager;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.util.osgi.DependencyActivatorHelper;

/**
 * Creates a NullObject for ApplicationMBeanFactory service.
 */
public class Activator extends DependencyActivatorHelper {
  private final static Logger _logger = Logger.getLogger("as.service.appmbeans.Activator");

  public Activator() {
    super(_logger);
  }

  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {    
    super.init(ctx, dm);
    // 
    // Application MBeans service
    //
    addService(createService()
               .setInterface(ApplicationMBeanFactory.class.getName(), null)
               .setImplementation(ApplicationMBeanFactoryImpl.class)
               .add(createServiceDependency()
                    .setService(Dictionary.class, "(service.pid=system)")
                    .setCallbacks("bind", null)
                    .setRequired(true)));
  }
}
