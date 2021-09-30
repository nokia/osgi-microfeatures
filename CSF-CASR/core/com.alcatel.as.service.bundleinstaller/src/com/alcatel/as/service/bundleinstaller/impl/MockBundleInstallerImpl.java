package com.alcatel.as.service.bundleinstaller.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.alcatel.as.service.bundleinstaller.BundleInstaller;

public class MockBundleInstallerImpl implements BundleInstaller, BundleActivator {
  public  Map<String, Bundle> deployApplications(List<String> bundleNames) throws Exception {
    return null;
  }

  /**
   * Starts all bundles which has been deployed from the OBR.
   */
  public  void startDeployedApplications() {}

  /**
   * Returns application bundles deployed in the current group
   */
  public  Collection<Bundle> getDeployedApplicationBundles() {
    return Collections.EMPTY_LIST;
  }

  /**
   * properly stop bundles before exiting the JVM
   */
  public  void shutdown() {}

  @Override
  public void start(final BundleContext context) throws Exception {
      final Hashtable<String, Object> properties = new Hashtable<>();
      properties.put(BundleInstallerImpl.DEPLOYED, "true");

      if (context.getBundle(0).getState() == Bundle.ACTIVE) {
          context.registerService(BundleInstaller.class.getName(), this, properties);
      } else {
          context.addFrameworkListener(new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == Bundle.ACTIVE) {
                    context.registerService(BundleInstaller.class.getName(), this, properties);
                }                
            }
        });
      }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
  }
}
