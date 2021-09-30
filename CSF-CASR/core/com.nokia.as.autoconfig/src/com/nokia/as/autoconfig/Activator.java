package com.nokia.as.autoconfig;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.alcatel.as.service.metatype.MetatypeParser;

public class Activator extends DependencyActivatorBase {
	
	public static final String CONFIG_DIR = "as.config.file.confdir";

	private static volatile List<String> confDirs;
	
	public static List<String> getConfDirs() {
		return confDirs;
	}
	
	public void init(BundleContext bc, DependencyManager dm) throws Exception {
		String dirs = Optional.ofNullable(bc.getProperty(CONFIG_DIR)).orElse("conf");
		confDirs = new LinkedList<>(Arrays.asList(dirs.split(",")));

		// Provide a backend Felix CM PersistenceManager in the registry for avoiding CM to store
        // our config in the file system. We must do that before CM actually starts.
        Properties props = new Properties();
        props.put("name", "casr");
        dm.add(createComponent().
				setInterface(PersistenceManager.class.getName(), props).
				setImplementation(FelixPersistenceManager.class));
        
        Properties eventProps = new Properties();
        String[] topics = new String[] {
				"com/nokia/casr/ConfigEvent/UPDATED"
	    };
		eventProps.put(EventConstants.EVENT_TOPIC, topics);
		
		dm.add(createComponent().
				setInterface(EventHandler.class, eventProps).
				setImplementation(AutoConfigurator.class).
				add(createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true)).
				add(createServiceDependency().setService(MetatypeParser.class).setRequired(true)).
				add(createBundleDependency().setFilter("(X-AS-MBeansDescriptors=*)").setStateMask(Bundle.ACTIVE).setCallbacks("bundleAdded", null)).
				add(createBundleDependency().setFilter("(Provide-Capability=com.nokia.as.conf*)").setStateMask(Bundle.ACTIVE).setCallbacks("configAdded", "configRemoved"))
			);
	}
}
