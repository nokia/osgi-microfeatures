package com.alcatel.as.service.bundleinstaller;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

/**
 * Interface registered when
 */
public interface BundleInstaller
{
  /**
   * Deploy a bundle (and all its dependencies) from the OBR.
   * @deprecated This method was used in old asr and is not implemented anymore
   */
  Map<String, Bundle> deployApplications(List<String> bundleNames) throws Exception;

  /**
   * Starts all bundles which has been deployed from the OBR.
   * @deprecated This method was used in old asr and is not implemented anymore
   */
  void startDeployedApplications();

  /**
   * Returns application bundles deployed in the current group
   * @deprecated This method was used in old asr and is not implemented anymore
   */
  Collection<Bundle> getDeployedApplicationBundles();

  /**
   * properly stop bundles before exiting the JVM
   */
  void shutdown();
}
