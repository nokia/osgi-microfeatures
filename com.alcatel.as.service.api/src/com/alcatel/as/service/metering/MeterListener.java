// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * This is the interface for being notified when a meter value has changed. Typically, listeners
 * will calculate statistics on meter's values change.<p>
 * 
 * In order to register a Listener on a given meter, you can use the whiteboard pattern and register
 * your meter listener in the OSGi service registry, using the {@link MeteringConstants#METER_NAME}
 * service property.
 * 
 * This service property will be used to map your meter listener to the meter whose name is matching your
 * "metering.meterName" service property.
 * <b>Note:</b>: if you want your listener to be registered in the meter before the meter is incremented
 * by any application, then you can also specify the {@link MeteringConstants#METER_LISTENERS} in your bundle.
 * This header indicates the number of listeners to wait for before activating the metering service.
 */
public interface MeterListener {
  /**
   * Notify a listener about a meter value change.
   * 
   * @param meter The meter whose value is being changed
   * @param newValue the new value
   * @param add true if the value must be accumulated into the meter, false if the meter is
   *          being set to a new absolute value.
   */
  void meterChanged(Meter meter, long newValue, boolean add);
}
