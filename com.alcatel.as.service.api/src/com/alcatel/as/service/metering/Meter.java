// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * Base interface for meters.
 */
public interface Meter {
  /**
   * Returns this meter name.
   * @return this meter name
   */
  String getName();
  
  /**
   * Attach a context to this meter.
   * @param attachment the context to attach to this Meter
   */
  void attach(Object attachment);
  
  /**
   * Returns the context attached to this meter.
   * @return the context attached to this meter
   */
  Object attachment();
  
  /**
   * Adds a Listener for being notified when a meter value has changed.
   * 
   * @param listener the listener.
   */
  void addMeterListener(MeterListener listener);
  
  /**
   * Unregister a meter listener.
   * 
   * @param listener the listener
   */
  void removeMeterListener(MeterListener listener);
  
  /**
   * Creates a sampler for this Meter instance, and register it into the current meter
   * listeners. The sample will then be notified each time this meter has changed.
   * <p>
   * <b> When your service is stopping, you must unregister your sampler from the associated
   * meter in order to avoid memory leaks.</b> You can do this, by calling the 
   * {@link com.alcatel.as.service.metering.Sampler#remove} method.
   * 
   * @return a sampler for this Meter instance.
   */
  Sampler createSampler();
  
  /**
   * Returns the current meter value.
   * <b>WARNING</b>. By default, a meter is stateless and a meter' value is only passed to meter's 
   * listeners. So, by default, this method is not allowed to be called, unless you explicitely configure
   * the meter as a stateful object.
   * @see MeteringConstants#STATEFUL_METER
   * @throws IllegalStateException if the meter is not configured as a stateful meter.
   */
  long getValue();
  
  /**
   * Checks if some listeners are currently registered in this meter.
   * @return true if some listeners are registered, false if not.
   */
  boolean hasListeners();
}
