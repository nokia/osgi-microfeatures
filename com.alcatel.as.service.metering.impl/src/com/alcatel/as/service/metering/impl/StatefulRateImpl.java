// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import java.util.concurrent.atomic.AtomicLong;

public class StatefulRateImpl extends RateImpl {
  private final AtomicLong _accumValue = new AtomicLong();
  
  StatefulRateImpl(String name) {
    super(name);
  }
  
  @Override
  public void hit() {
    _accumValue.addAndGet(1);
    doSet(1, true);
  }
  
  @Override
  public void hit(long value) {
    _accumValue.addAndGet(value);
    doSet(value, true);
  }

  @Override
  public long getValue() {
    return _accumValue.get();
  }  
}
