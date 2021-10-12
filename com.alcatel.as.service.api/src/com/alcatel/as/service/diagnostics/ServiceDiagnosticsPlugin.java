// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.diagnostics;

import java.util.List;
import java.util.Map;

/**
 * this interface is for plugin developers who 
 * want to implement the feature for a specific dependency injection engine.
 * (Currently known implementations are available for SCR and DM)
 */
public interface ServiceDiagnosticsPlugin {
  /** 
   * returns a map of unresolved service name -&gt; list of missing deps 
   * leafs only: should not include intermediates
   * @return the unresolved service names
   */
  Map<String, List<ServiceDiagnosticsPlugin.Dependency>> getUnresolvedDependencies();

  interface Dependency {
    String getName();
    String getFilter();
  }
}
