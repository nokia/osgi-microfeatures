// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Dictionary;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentExecutorFactory;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.utils.Log;

/**
 * This class allows to concurrently activate dependency manager components.
 * Each component which provides a "dependencymanager.parallel" property set to "true" will be
 * activated in the processing threadpool, and a dedicated queue will be created for it.
 * (each lifecycle or dependency callbacks will be scheduled in the per-component queue).
 */
@org.osgi.service.component.annotations.Component
public class DMComponentExecutorFactory implements ComponentExecutorFactory {
    private final static Log _log = Log.getLogger("as.service.concurrent.DMComponentExecutorFactory");
    private final static String COMP_PARALLEL = "asr.component.parallel";
    private final static String COMP_QUEUE = "asr.component.queue";
    private final static String COMP_CPUBOUND = "asr.component.cpubound";

    /**
     * The Executors service used to create a queue for each created DM components.
     */
    @Reference
    private volatile PlatformExecutors _execs; // injected
        
    /**
     * a DM component is about to be activated: if it provides a service property having 
     * a "dependencymanager.parallel" service property will be activated concurrently.
     */
    @Override
    public Executor getExecutorFor(Component component) {
        ComponentDeclaration decl = component.getComponentDeclaration();
        Dictionary<String, Object> properties = decl.getServiceProperties();
        // Only handle concurrently components marked with the DM_PARALLEL property.
        if (properties != null && "true".equals(properties.get(COMP_PARALLEL))) {
            String queue = (String) properties.get(COMP_QUEUE);            
            String cpubound = (String) properties.get(COMP_CPUBOUND);
            boolean cpu = cpubound == null ? true : Boolean.valueOf(cpubound.toString());
            PlatformExecutor tpool = cpu ? _execs.getProcessingThreadPoolExecutor() : _execs.getIOThreadPoolExecutor();
            _log.debug("Creating queue executor for DM component %s", component);
            return _execs.createQueueExecutor(tpool, queue).toExecutor(ExecutorPolicy.INLINE);
        }
        return null;
    }
}
