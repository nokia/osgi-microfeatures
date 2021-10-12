// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import com.alcatel.as.service.metering2.Meter;

public class AbsoluteMeterImpl extends MeterImpl {
  private final AtomicLong _value = new AtomicLong();
  
  public AbsoluteMeterImpl(String name, ScheduledExecutorService timer) {
    super(name, timer);
  }
  
  @Override
  public Meter inc(long delta) {
    _value.addAndGet(delta);
    updated();
    return this;
  }
  
  @Override
  public Meter dec(long delta) {
    _value.addAndGet(-delta);
    updated();
    return this;
  }
  
  @Override
  public long getValue() {
    return _value.get() & Long.MAX_VALUE; // if we reach Long.MAX_VALUE, wrap to 0 and start again to add from 0
  }
  
  @Override
  public Type getType() {
    return Meter.Type.ABSOLUTE;
  }
  
  @Override
  public Meter set(long value) {
    _value.lazySet(value);
    updated();
    return this;
  }
  
  @Override
  public long getAndSet(long value) {
    long result = _value.getAndSet(value);
    updated();
    return result;
  }
  
  @Override
  public long getAndReset() {
    long result = _value.getAndSet(0);
    updated();
    return result;
  }
}
