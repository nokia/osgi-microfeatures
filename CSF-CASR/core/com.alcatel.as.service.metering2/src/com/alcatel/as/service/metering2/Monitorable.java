package com.alcatel.as.service.metering2;

import java.util.concurrent.ConcurrentMap;

/**
 * A Monitorable OSGi service can provide informations about itself in the form of Meters.
 * A Monitorable is an OSGi service, which can register itself in the OSGi service registry when ready. 
 * Can also update its service properties if some meters are dynamically added/removed.
 */
public interface Monitorable {
  /**
   * Each monitorable services must be provided in the registry with this service property.
   */
  final static String NAME = MeteringConstants.MONITORABLE_NAME;
    
  /**
   * Returns an informative description about this Monitorable service.
   * @return An informative description of this Monitorable service.
   */
  String getDescription();
  
  /**
   * Returns the meters managed by this Monitorable service.
   * @return The map of Meters supported by this Monitorable service (Key=meter name, Value=Meter).
   */
  ConcurrentMap<String, Meter> getMeters();
  
  /**
   * Returns the Monitorable service name.
   */
  String getName();
  
  /**
   * Triggers a notification update to interested listeners. You must call thid method if you add meters 
   * after a Minitorable service has been registered (after the start of the Monitorable service).
   */
  Monitorable updated();
}
