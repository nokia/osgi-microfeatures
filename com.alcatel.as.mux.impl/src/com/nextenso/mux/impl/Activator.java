package com.nextenso.mux.impl;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.management.RuntimeStatistics;
import com.alcatel.as.service.metering2.MeteringConstants;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.alcatel.as.service.recorder.RecorderService;
import com.nextenso.mux.MuxFactory;
import com.nextenso.mux.SimpleMuxFactory;
import com.nextenso.mux.impl.ioh.IOHMuxFactory;

import alcatel.tess.hometop.gateways.concurrent.ThreadPool;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

public class Activator extends DependencyActivatorBase {
  @Override
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Provides the statistic service (provided by the flow control manager)
      Properties props = new Properties();
      props.put("service.pid", "flowControl");
      Component flowctrl = createComponent()
          .setInterface(RuntimeStatistics.class.getName(), props)
          .setImplementation(FlowControl.class)
          .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true))
          .add(createServiceDependency().setService(Monitorable.class, "(" + Monitorable.NAME + "=" + MeteringConstants.SYSTEM + ")").setRequired(true));
      dm.add(flowctrl);
                  
      // Provides the legacy default MuxFactory service (used by the callout agent when connecting to IOH written in "C").
      Properties prop = new Properties();
      prop.put("type", "default");
      Component defaultMuxImpl = createComponent()          
          .setInterface(MuxFactory.class.getName(), prop)
          .setImplementation(MuxFactoryImpl.class)
          .add(createServiceDependency().setService(EventAdmin.class).setRequired(false).setAutoConfig(false).setCallbacks(this, "bindEventAdmin", "unbindEventAdmin"))
          .add(createServiceDependency().setService(ThreadPool.class).setRequired(true).setCallbacks(null, null))                           
          .add(createServiceDependency().setService(ReactorProvider.class).setRequired(true).setCallbacks("bindReactorProvider", null))                                
          .add(createServiceDependency().setService(RuntimeStatistics.class).setAutoConfig("_mgmtRtService").setRequired(true))                                                  
          .add(createServiceDependency().setService(TimerService.class, "(strict=true)").setCallbacks("bindTimerService", null).setRequired(true))                             
          .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true).setCallbacks("bindPlatformExecutors", null));
      dm.add(defaultMuxImpl);
                  
      // Provides the SimpleMuxFactory (used by FastCache and DistributedSession)
      Component simpleMux = createComponent()       
          .setInterface(SimpleMuxFactory.class.getName(), null)
          .setImplementation(SimpleMuxFactoryImpl.class)
          .add(createServiceDependency().setService(ThreadPool.class).setRequired(true).setCallbacks(null, null))                               
          .add(createServiceDependency().setService(MuxFactory.class, "(type=default)").setRequired(true).setAutoConfig("_muxFactory"));
      dm.add(simpleMux);                
                
      
      // Provide new MuxFactory (IOH)
      prop = new Properties();
      prop.put("type", "ioh");
      Component iohMux = createComponent()
          .setInterface(MuxFactory.class.getName(), prop)
          .setImplementation(new IOHMuxFactory())
          .add(createServiceDependency().setService(PlatformExecutors.class).setRequired(true))                                                   
          .add(createServiceDependency().setService(MeteringService.class).setRequired(true))
          .add(createServiceDependency().setService(MeteringRegistry.class).setRequired(true))
          .add(createServiceDependency().setService(EventAdmin.class).setRequired(true))
          .add(createServiceDependency().setService(ReactorProvider.class).setRequired(true))
	  .add(createServiceDependency().setService(RecorderService.class).setRequired(true));
      dm.add(iohMux);                                    
  }
  
  @SuppressWarnings("unused")
  private void bindEventAdmin(EventAdmin eventAdmin) {
    MuxConnectionImpl.setEventAdmin(eventAdmin);
  }
  
  @SuppressWarnings("unused")
  private void unbindEventAdmin(EventAdmin eventAdmin) {
    MuxConnectionImpl.setEventAdmin(null);
  }
}
