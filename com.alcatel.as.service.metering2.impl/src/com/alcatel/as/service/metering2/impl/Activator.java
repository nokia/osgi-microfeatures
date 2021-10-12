// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.util.MeteringRegistry;


public class Activator extends DependencyActivatorBase {
    private final static Logger _logger = Logger.getLogger(Activator.class);
    
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        // Declare the Metering Service
        dm.add(createComponent()
          .setImplementation(MeteringServiceImpl.class)
          .setInterface(MeteringService.class.getName(), null)
          .add(createServiceDependency().setService(Monitorable.class).setRequired(false).setCallbacks("add", "remove")));
    
        // Declare the Gogo Metering Command
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, "casr.service.metering");
	props.put("casr."+CommandProcessor.COMMAND_SCOPE+".alias", "asr.metering");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] { 
                "list", "update", "createRateMeter" , "createMaxValueMeter", 
                       "createMovingMaxValueMeter", "removeRateMeter", "removeMaxValueMeter",
                       "createMergeMeters"
        });

        
        dm.add(createComponent()
          .setInterface(MeteringRegistry.class.getName(), null)
          .setImplementation(MeteringRegistryImpl.class)
          .setCallbacks(null, "init", "destroy", null)
          .add(createServiceDependency().setService(Monitorable.class).setRequired(false).setCallbacks("bindMonitorable", "unbindMonitorable")));;

        dm.add(createComponent()
          .setImplementation(GogoMetering.class)
          .setInterface(GogoMetering.class.getName(), props)
	      .add(createServiceDependency().setService(MeteringRegistry.class).setRequired(true))
	      .add(createServiceDependency().setService(MeteringService.class).setRequired(true))
	      .add(createServiceDependency().setService(DerivedMeters2.class).setRequired(true)));
                    
//         Declare the DerivedMeters Service
//        dm.add(createComponent()
//          .setImplementation(DerivedMeters.class)
//          .setAutoConfig (Component.class, false)
//          .add(createConfigurationDependency().setPid(MeteringServiceImpl.class.getName()))
//          .add(createServiceDependency().setService(MeteringService.class).setRequired(true)));

        dm.add(createComponent()
      	      .setImplementation(DerivedMeters1.class)
      	      .setAutoConfig (Component.class, false)
      	      .add(createConfigurationDependency().setPid(MeteringServiceImpl.class.getName()))
      	      .add(createServiceDependency().setService(MeteringRegistry.class).setRequired(true))
      	      .add(createServiceDependency().setService(MeteringService.class).setRequired(true)));
        
        dm.add(createComponent()
	      .setImplementation(DerivedMeters2.class)
	      .setAutoConfig (Component.class, false)
	      .setInterface(DerivedMeters2.class.getName(), null)
	      .add(createConfigurationDependency().setPid(MeteringServiceImpl.class.getName()))
	      .add(createServiceDependency().setService(MeteringRegistry.class).setRequired(true))
	      .add(createServiceDependency().setService(MeteringService.class).setRequired(true)));
	
        // Declare the ShutdownMeters Service
        dm.add(createComponent()
          .setImplementation(ShutdownMeters.class)
          .setInterface(com.alcatel.as.service.shutdown.Shutdownable.class.getName(), null)
          .setAutoConfig (Component.class, false)
          .add(createConfigurationDependency().setPid(MeteringServiceImpl.class.getName()))
          .add(createServiceDependency().setService(MeteringService.class).setRequired (true))
          .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true)));
    
        // Declare the Monitorable System Service (which has an optional dependency on a configuration, using a ManagedService).
        props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, MeteringServiceImpl.class.getName());
        
        dm.add(createComponent()
          .setImplementation(MonitorableSystem.class)
          .add(createServiceDependency().setService(MeteringService.class).setRequired(true))
          .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true)));
        
        try {
            // Declare the MeteringServlet. Notice that the setHttpService callback is optional and will be called *AFTER*
            // all required dependencies have been injected.
            // (in DM, optional callbacks are always invoked after required callbacks and after start).
            dm.add(createComponent()
              .setImplementation(MeteringServlet.class)
              .add(createConfigurationDependency().setPid(MeteringServiceImpl.class.getName()))
	          .add(createServiceDependency().setService(MeteringService.class).setRequired(true))
	          .add(createServiceDependency().setService(MeteringRegistry.class).setRequired(true))      
	          .add(createServiceDependency().setService(HttpService.class).setCallbacks("setHttpService", null)));
        } catch (Throwable err) {
            _logger.warn("Metering servlet not registered: " + err.toString());
        }
  }
}
