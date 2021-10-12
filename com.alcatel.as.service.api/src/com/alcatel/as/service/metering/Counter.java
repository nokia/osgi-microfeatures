// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * Counter used to expose cumulative events. The value of the counter will grow during the life
 * cycle of the application. For example, use Counters for metering method execution elapsed
 * time, number of message received, etc ... Notice that, if you need to count event occurrence
 * per seconds, then you can use {@link Rate} instead of this class.
 */
public interface Counter extends Meter {
  /**
   * Accumulates a value into that counter.
   * 
   * @param value a Value to be added into this counter.
   */
  void add(long value);
  
  /**
   * Starts a stopwatch used to measure the time elapsed by a method (in millis.)
   * 
   * @return A StopWatch for measuring the time elapsed by a method (in millis.).
   */
  StopWatch start();
}
