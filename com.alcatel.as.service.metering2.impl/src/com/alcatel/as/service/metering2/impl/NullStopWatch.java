// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import com.alcatel.as.service.metering2.StopWatch;

class NullStopWatch implements StopWatch {
  private final static NullStopWatch _instance = new NullStopWatch();
  
  public static StopWatch instance() {
    return _instance;
  }
  
  @Override
  public void close() {
  }
}
