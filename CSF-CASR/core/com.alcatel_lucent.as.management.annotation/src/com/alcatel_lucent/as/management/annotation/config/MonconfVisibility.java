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
