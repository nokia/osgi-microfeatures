// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * Defines standard names for the Metering environment service properties, 
 * and Manifest header attribute keys. 
 */
public interface MeteringConstants {
  /**
   * MeterListener OSGi service property used by MeterListeners registered in the OSGi registry.
   * <p>
   * 
   * When you register a MeterListener in the OSGi registry, you must supply the following service proprty
   * in order to map your listener to a given meter.
   */
  public String METER_NAME = "metering.meterName";
  
  /**
   * OSGI manifest header identifying some stateful meters, so their <p>getValue</p> 
   * method can return the actual current meter value.
   * 
   * By default, a Meter is stateless and the meter value is only passed to any registered meter listeners.
   * This allows to ensure high performance if no listeners are registered (this is normally the default case
   * in a production environment because listeners are usually only used to make diagnostics when problems occur).
   * <p>
   * However, if you somehow want to be able to get the current value of a given meter using the {@link Meter#getValue()},
   * but you don't want to register a listener in the meter for this; then you can just declare the following 
   * OSGI manifest header in your bundle, and this header must contain the list of stateful meters, so you can 
   * invoke the meters' getValue method. The value of this header is a list of meter names, comma separated.
   */
  final String STATEFUL_METER = "Metering-StatefulMeters";
  
  /**
   * OSGI manifest header identifying the number of meters listener a bundle will register.
   * 
   * Sometimes, you want your listeners to be registered in a given meter before the metering service
   * is actually started, because you don't want to miss some meter values if your listener is registered
   * after the meter is first modified by applications. <p>
   * 
   * When your define this header in your bundle manifest with an Integer value, you ensure that the 
   * metering service won't be started until you have registered in the OSGi registry all the listeners 
   * which count is specified in the header.
   */
  final String METER_LISTENERS = "Metering-Listeners";
}
