package com.alcatel.as.service.metering2;

/**
 * Service used to create meters, register monitoring jobs, and retrieve the currently available {@link Monitorable} services.
 * @see Monitorable
 */
public interface MeteringService {
  /**
   * Creates a {@link Meter} for measuring an absolute value. 
   * @param name The meter name
   * @return A new {@link Meter} instance
   */
  Meter createAbsoluteMeter(String name);
  
  /**
   * Creates a {@link Meter} for measuring an incremental value. 
   * @param name The meter name
   * @param parent an incremental parent meter, or null
   * @return A new {@link Meter} instance
   */
  Meter createIncrementalMeter(String name, Meter parent);

  /**
   * Creates a {@link Meter} for measuring a value supplied dynamically by a java callback 
   * @param name The meter name
   * @param valueSupplier a java callback used to supply the actual meter value
   * @return A new {@link Meter} instance
   */
  Meter createValueSuppliedMeter(String name, ValueSupplier valueSupplier);
  
  /**
   * Returns the existing Monitorable service with the given name, or null or it does not exist.
   * @param name the monitorable name
   * @return the existing monitorable service, matching the given name, or null if it does not exist
   */
  Monitorable getMonitorable(String name);
}
