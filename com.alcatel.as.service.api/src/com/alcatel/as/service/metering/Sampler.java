package com.alcatel.as.service.metering;

/**
 * A Sampler is a {@link MeterListener} used to compute statistics about a given meter. You have
 * to invoke the {@link Meter#createSampler()} in order to create a sampler for a given Meter.
 */
public interface Sampler extends MeterListener {
  /**
   * Calculates some statistics on the meter associated to this sampler.
   * @return The meter statistics.
   */
  Stat computeStat();
  
  /**
   * Returns the meter associated to this sampler.
   * @return the meter associated to this sampler
   */
  Meter getMeter();
  
  /**
   * Stop this sampler and unregister it from its associated meter.
   */
  void remove();
}
