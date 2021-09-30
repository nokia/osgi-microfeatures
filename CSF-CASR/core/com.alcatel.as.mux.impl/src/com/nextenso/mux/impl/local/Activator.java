package com.nextenso.mux.impl.local;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventHandler;

import com.nextenso.mux.MuxFactory;

public class Activator extends DependencyActivatorBase {
  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Provides the local MuxFactory (used by the callout agent in standalone mode)
      Hashtable<String, Object> prop = new Hashtable<>();
      prop.put("type", "local");
      prop.put("event.topics", new String[] { 
              "com/alcatel_lucent/as/service/callout/launcher/START_LISTENING", 
              "com/alcatel_lucent/as/service/callout/launcher/STOP_LISTENING" 
      });
      
      dm.add(createComponent()
          .setInterface(new String[] { MuxFactory.class.getName(), EventHandler.class.getName()}, prop)
          .setImplementation(LocalMuxFactoryImpl.class)               
          .add(createConfigurationDependency().setPid("localstacks"))
          .add(createServiceDependency().setService(Dictionary.class, "(service.pid=system)").setAutoConfig("_system").setRequired(true)));
  }
}
