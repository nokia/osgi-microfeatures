// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering;

/**
 * Counter used to calculate event occurrence per seconds. For example, you can use this counter
 * when metering the number of message received per seconds.
 */
public interface Rate extends Meter {
  /**
   * Registers into this counter that a new event occurred. This counter will compute the number
   * of hits invoked per seconds.
   */
  void hit();
  
  /**
   * Registers into this counter that some new events occurred. This counter will compute the number
   * of hits invoked per seconds.
   * 
   * @param hits the number of event that occurred.
   */
  void hit(long hits);
}
