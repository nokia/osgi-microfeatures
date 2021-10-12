// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.config;

/**
 * Visibility Level for a given Property.
 * Each property have a visibility level, allowing to expose/hide some
 * properties from the GUI, depending on the configured visibility mode.
 */
public enum Visibility {
  /**
   * Basic visibility where the property is always displayed
   */
  BASIC,
  
  /**
   * Advanced visibility where the property is only displayed in advanced mode, and hidden in basic mode.
   */
  ADVANCED,
  
  /**
   * Hidden visibility where the property is not exposed in the GUI.
   */
  HIDDEN
}
