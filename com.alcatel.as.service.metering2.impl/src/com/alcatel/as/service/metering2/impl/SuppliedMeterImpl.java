// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import java.util.concurrent.ScheduledExecutorService;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.ValueSupplier;

public class SuppliedMeterImpl extends MeterImpl {
  private final ValueSupplier _valueSupplier;
  
  public SuppliedMeterImpl(String name, ValueSupplier valueSupplier, ScheduledExecutorService timer) {
    super(name, timer);
    if (valueSupplier==null) throw new IllegalArgumentException("no valueSupplier for"+name);
    _valueSupplier = valueSupplier;
  }
  
  @Override
  public Type getType() {
    return Meter.Type.SUPPLIED;
  }
  
  @Override
  public Meter set(long value) {
    throw new IllegalStateException("Method not supported by Supplied Meter");
  }
  
  @Override
  public long getAndSet(long value) {
    throw new IllegalStateException("Method not supported by Supplied Meter");
  }
  
  @Override
  public long getAndReset() {
    throw new IllegalStateException("Method not supported by Supplied Meter");
  }
  
  @Override
  public Meter inc(long delta) {
    throw new IllegalStateException("Method not supported by Supplied Meter");
  }
  
  @Override
  public Meter dec(long delta) {
    throw new IllegalStateException("Method not supported by Supplied Meter");
  }
  
  @Override
  public long getValue() {
    return _valueSupplier.getValue() & Long.MAX_VALUE; // if we reach Long.MAX_VALUE, wrap to 0 and start again to add from 0
  }
}
