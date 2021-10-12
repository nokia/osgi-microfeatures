// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

import com.alcatel.as.service.appmbeans.ApplicationMBeanFactory;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.coordinator.Coordinator;
import com.alcatel.as.service.coordinator.Participant;
import com.alcatel.as.service.discovery.Advertisement;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.service.metering2.MeteringConstants;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.recorder.RecorderService;
import com.alcatel.as.service.reporter.api.ReporterSession;
import com.alcatel.as.service.shutdown.ShutdownService;
import com.alcatel.as.util.serviceloader.ServiceLoader;
import com.nextenso.mux.MuxFactory;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.utils.Config;

/**
 * Dependency Manager Activator for the CalloutServer component.
 * The Callout provides itself as a Participant to the "ACTIVATION" coordination.
 * It also provides a "dm.parallel" service property in order to be activated in the 
 * ASR PlatformExecutor (concurrently).
 * All Component liefecycle and dependency callbacks will be scheduled in a PlatformExecutor queue
 * running in the processing threadpool.
 */
@SuppressWarnings("deprecation")
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        // Define the CalloutServer components service properties.
        // We first participate to the ACTIVATION coordination, which we'll join once all mux handlers have been 
        // initialized.
        // We also configure our DM component as a concurrent one, because we want to get a queue for our component,
        // and we'll use that queue to serialize external events like muxConnected events with all lifecycle callbacks.
        // In this way, we don't need to synchronize or use tricky serial executors.
        
    	String monitorableFilter = "(" + Monitorable.NAME + "=" + MeteringConstants.SYSTEM + ")";
    	
        Properties properties = new Properties();
        properties.setProperty(Participant.COORDINATION, "ACTIVATION");
        properties.setProperty("asr.component.parallel", "true");
                
        Component calloutServer = createComponent()
            .setImplementation(CalloutServer.class)
            .setInterface(Participant.class.getName(), properties)
            // Configuration ... injected before any other required dependencies, before init
            .add(createConfigurationDependency().setPid("system"))
            .add(createConfigurationDependency().setPid("agent"))
            // Required dependencies are injected before init()
            .add(createServiceDependency().setService(ServiceLoader.class).setRequired(true))
            .add(createServiceDependency().setService(Monitorable.class, monitorableFilter).setRequired(true))
            .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true))
            .add(createServiceDependency().setService(ApplicationMBeanFactory.class).setRequired(true))
            .add(createServiceDependency().setService(RuntimeStatistics.class).setRequired(true))
            .add(createServiceDependency().setService(ReactorProvider.class).setRequired(true))
            .add(createServiceDependency().setService(ShutdownService.class).setRequired(true))
            .add(createServiceDependency().setService(ThreadPool.class).setRequired(true))
            .add(createServiceDependency().setService(Config.class).setRequired(true))
            .add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
            .add(createServiceDependency().setService(ReporterSession.class).setRequired(true))
            .add(createServiceDependency().setService(RecorderService.class).setRequired(true))
            // Optional dependencies with callbacks are injected after start()
            .add(createServiceDependency().setService(Coordinator.class).setRequired(false).setCallbacks("bind", null))
            .add(createServiceDependency().setService(ClassLoader.class, "(feature=legacy3.0)").setRequired(false).setCallbacks("bind", null))
            .add(createServiceDependency().setService(MuxFactory.class, "(type=*)").setRequired(false).setCallbacks("bind", null))
            .add(createServiceDependency().setService(Advertisement.class, "(provider=*)").setRequired(false).setCallbacks("bind", "unbind"));
        dm.add(calloutServer);
        
        // Other dynamic dependencies will be defined in the CalloutServer.init() method.
    }
}
