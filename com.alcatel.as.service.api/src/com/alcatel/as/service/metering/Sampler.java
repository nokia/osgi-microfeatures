// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
