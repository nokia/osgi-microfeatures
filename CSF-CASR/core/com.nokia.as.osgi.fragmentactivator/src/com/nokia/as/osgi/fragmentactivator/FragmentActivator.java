package com.nokia.as.osgi.fragmentactivator;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

/**
 * This component is instantiated for each fragment bundle having a CSF-Fragment-Activator header.
 */
public class FragmentActivator {
	public final static String FRAGMENT_ACTIVATOR = "CASR-Fragment-Activator";

	volatile Bundle _fragmentBundle;
	volatile DependencyManager _dm;
	volatile Component _comp;
	private final Map<String, BundleActivator> _activators = new HashMap<>();

    void start() {
    	for (String host: _fragmentBundle.getHeaders().get("Fragment-Host").split(",")) {
    		BundleDependency hostDep = _dm.createBundleDependency()
    				.setFilter("(Bundle-SymbolicName=" + host + ")")
    				.setStateMask(Bundle.ACTIVE)
    				.setCallbacks("hostStarted", "hostStopped");
    		_comp.add(hostDep);
    	}
    }
            
    void hostStarted(Bundle host) {
    	Object activatorClassObj = _fragmentBundle.getHeaders().get(FRAGMENT_ACTIVATOR);
    	if (activatorClassObj != null) {
    		String activatorClassName = activatorClassObj.toString();
    		try {
				@SuppressWarnings("unchecked")
				Class<BundleActivator> clazz = (Class<BundleActivator>) host.loadClass(activatorClassName);
				BundleActivator hostActivator = clazz.newInstance();
				hostActivator.start(host.getBundleContext());
				_activators.put(host.getSymbolicName() + ":" + host.getVersion(), hostActivator);
    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
    
    void hostStopped(Bundle host) {
    	BundleActivator hostActivator = _activators.remove(host.getSymbolicName() + ":" + host.getVersion());
    	if (hostActivator != null) {
    		try {
				hostActivator.stop(host.getBundleContext());
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
}
