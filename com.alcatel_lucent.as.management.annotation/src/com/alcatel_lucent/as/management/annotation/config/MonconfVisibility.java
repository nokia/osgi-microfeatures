// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.config;

/**
 * Legacy monconf property visibility (PUBLIC/PRIVATE)
 */
public enum MonconfVisibility {
  /**
   * Legacy property defined in the legacy group level. 
   */
  PUBLIC,
  
  /**
   * Legacy property defined in the private instance level.
   */
  PRIVATE,
  
  /**
   * Legacy property defined in both public (group) and private (instance) level.
   */  
  BOTH
}
