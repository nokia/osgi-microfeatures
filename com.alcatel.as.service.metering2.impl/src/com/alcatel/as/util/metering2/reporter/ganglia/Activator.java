package com.alcatel.as.util.metering2.reporter.ganglia;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import com.alcatel.as.util.metering2.reporter.codahale.MeteringCodahaleRegistry;

public class Activator extends DependencyActivatorBase {
  public void init(BundleContext ctx, DependencyManager dm) throws Exception {
      // Declare the Ganglia Metrics Reporter Component (will be activated only if enabled from configuration).
      Component gangiaReporter = createComponent()
          .setImplementation(GangliaMetricsReporter.class)
          .add(createConfigurationDependency().setPid(GangliaMetricsReporter.PID))
          .add(createServiceDependency().setService(MeteringCodahaleRegistry.class).setRequired(true));
      dm.add(gangiaReporter);
  }
}
