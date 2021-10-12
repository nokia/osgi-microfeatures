// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * Factory used to get/create Meters. Meters are used to monitor application performance
 * hotspots. The Metering Service is an OSGi service and you must define a dependency over it.
 *
 * <b>Important note:</b> The MeteringService is stateless: when you increment counters, the values
 * you pass to the meter's add methods are ignored and only meter listeners attached to the meters 
 * are statefull.
 */
public interface MeteringService {
  /**
   * Returns a Gauge for calculating application state variation. Gauges are used to expose
   * state variations (current number of connections, thread pool size variation, JVM free
   * memory variations, etc ...).
   * 
   * @param name the Gauge name
   * @return A Gauge
   */
  Gauge getGauge(String name);
  
  /**
   * Returns a Counter for exposing cumulative events. The value of the counter will grow during
   * the life cycle of the application. For example, use Counters for metering method
   * execution elapsed time, number of request received, etc ...
   * 
   * @param name the Counter name
   * @return A Counter
   */
  Counter getCounter(String name);
  
  /**
   * Returns a Counter for exposing number of event occurrences per seconds. For example, use
   * Rates for metering the number of messages handled per seconds.
   * 
   * @param name the Rate name
   * @return A Rate instance
   */
  Rate getRate(String name);
}
