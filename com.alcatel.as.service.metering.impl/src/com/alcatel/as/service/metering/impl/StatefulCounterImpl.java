// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import java.util.concurrent.atomic.AtomicLong;

public class StatefulCounterImpl extends CounterImpl {
  private final AtomicLong _accumValue = new AtomicLong();

  public StatefulCounterImpl(String name) {
    super(name);
  }
  
  @Override
  public void add(long value) {
    _accumValue.addAndGet(value);
    doSet(value, true);
  }
  
  @Override
  public long getValue() {
    return _accumValue.get();
  }  
}
