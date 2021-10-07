package com.alcatel.as.service.coordinator.impl;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.service.bundleinstaller.BundleInstaller;
import com.alcatel.as.service.coordinator.Participant;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext bc, final DependencyManager dm) throws Exception {
        // We wait for the bundle installer, to make sure all bundles have been started before us.
        // (the bundle installer registers once it has started all bundles).
    	String filter = "(deployed=true)";
        dm.add(createComponent()
            .setImplementation(CoordinatorImpl.class)
            .add(createServiceDependency().setService(BundleInstaller.class, filter).setRequired(true).setCallbacks(null, null))
            .add(createServiceDependency().setService(Participant.class).setRequired(false).setCallbacks("bind", null)));
    }
}
