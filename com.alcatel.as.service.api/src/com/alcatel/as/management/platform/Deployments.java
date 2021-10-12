// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.management.platform;

import java.util.Set;
import java.util.Map;

import com.alcatel.as.management.blueprint.Blueprint;

/** 
 * manages a structure of created|deployed instances by platform.
 * and a list of known hosts.
 */
public interface Deployments {
  /** maps platform.name to Deployments.Platform (all hosts) */
  Asr asr();
  /** maps host.name to Deployments.Host */
  Hosts hosts();
  /** maps platform.name to Deployments.Platform */
  public interface Asr extends Map<String, Platform> { Platform getset(String k); }
  /** maps host.name to Deployments.Host */
  public interface Hosts extends Map<String, Host> { Host getset(String k); }
  /** maps group.name to Deployments.Group */
  public interface Platform extends Map<String, Group> { Group getset(String k); }
  /** maps component.name to Deployments.Component */
  public interface Group extends Map<String, Component> { Component getset(String k); }
  /** maps instance.name to host.name */
  public interface Component extends Map<String, String> { 
    /**
     * reverse view of the Component (maps host.name to list of instance.name).
     * May be empty if no instance is deloyed.
     */
    Map<String, Set<String>> hostView() ;
    /**
     * loads the component's blueprint
     */
    Blueprint blueprint() throws Exception;
    /**
     * store/update the component's blueprint
     */
    void updateBlueprint(Blueprint bp) throws Exception;
  }
  /** map of host properties */
  public interface Host extends Map<String, String> { 
  }
}
