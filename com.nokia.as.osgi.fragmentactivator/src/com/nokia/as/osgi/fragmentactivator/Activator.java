package com.nokia.as.osgi.fragmentactivator;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
	
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        dm.add(createBundleComponent()
          .setImplementation(FragmentActivator.class)
          .setBundleFilter(Bundle.RESOLVED, "(" + FragmentActivator.FRAGMENT_ACTIVATOR + "=*)"));
    }

}
