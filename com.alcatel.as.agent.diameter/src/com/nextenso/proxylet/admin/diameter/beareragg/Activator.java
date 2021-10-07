package com.nextenso.proxylet.admin.diameter.beareragg;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.management.platform.ConfigManager;
import com.alcatel.as.management.platform.CreateInstancePlugin;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext bc, DependencyManager dm) throws Exception {
        Properties props = new Properties();
        props.put("protocol", "diameter");
        
        Component aggregator = createComponent()
            .setImplementation(DiameterBearerAggregator.class)
            .setInterface(CreateInstancePlugin.class.getName(), props)
            .add(createServiceDependency().setService(ConfigManager.class).setRequired(true).setCallbacks("bindConfigManager",null));
        
        dm.add(aggregator);
    }
    
}
