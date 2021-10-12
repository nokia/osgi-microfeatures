// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.config;

/**
 * Property Scope definition. Each property may optionally provide a scope,
 * which specifies which area the property must be applied to.
 * It should only be used when there is a real constraint and the system cannot 
 * work unless configured equally within the scope. Otherwise, leave the decision 
 * to the administrator.
 * 
 * By default, a property scope is {@link #ANY}
 */
public enum Scope {
  /**
   * "any" scope. When a property scope is set to this value, it means
   * that the property may be applied at any level, down to the individual instances.
   */
  ANY,
  
  /**
   * "component" scope. When a property scope is set to this value, it means
   * that the property is always applied to every instance of a given component.
   */
  COMPONENT,
  
  /**
   * "group" scope. When a property scope is set to this value, it means
   * that the property is always applied to every instance deployed in a given group.
   */
  GROUP,
  
  /**
   * "application" scope. When a property scope is set to this value, it means
   * that the property is always applied to every instance of a given application.
   */
  APPLICATION,
  
  /**
   * "all" scope. When a property scope is set to this value, it means
   * that the property is always applied to every instance deployed in the cluster.
   */
  ALL
}
