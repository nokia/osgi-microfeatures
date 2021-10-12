// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2;

/** 
 * Supplies a meter value.
 */
public interface ValueSupplier {
  /**
   * Returns a meter value. This method can be called from any thread.
   * @return A meter value
   */
  long getValue();
  
}
