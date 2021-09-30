package com.alcatel.as.service.mbeanparser.impl;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
	
  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {    
    Component properyFactory = createComponent()
    		.setInterface(com.alcatel.as.service.metatype.PropertyFactory.class.getName(), null)
    		.setImplementation(com.alcatel.as.service.mbeanparser.impl.PropertyFactoryImpl.class);
    dm.add(properyFactory);

    // automatic injection of BundleContext here
    Component parser = createComponent()    
    		.setInterface(com.alcatel.as.service.metatype.MetatypeParser.class.getName(),null)
    		.setImplementation(com.alcatel.as.service.mbeanparser.impl.MBeanParserImpl.class);
    dm.add(parser);
  }
}
