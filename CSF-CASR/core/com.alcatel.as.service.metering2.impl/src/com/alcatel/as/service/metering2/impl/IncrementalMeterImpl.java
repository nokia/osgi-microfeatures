package com.alcatel.as.service.metering2.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

import com.alcatel.as.service.metering2.Meter;

public class IncrementalMeterImpl extends MeterImpl {
  private final LongAdder _value = new LongAdder();
  private final Meter _parent;
  
  public IncrementalMeterImpl(String name, Meter parent, ScheduledExecutorService timer) {
    super(name, timer);
    _parent = parent;
  }
  
  @Override
  public Type getType() {
    return Meter.Type.INCREMENTAL;
  }
  
  @Override
  public Meter set(long value) {
    throw new IllegalStateException("Method not supported by Incremental Meter");
  }
  
  @Override
  public long getAndSet(long value) {
    throw new IllegalStateException("Method not supported by Incremental Meter");
  }
  
  @Override
  public long getAndReset() {
    long result = _value.sumThenReset();
    updated();
    return result;
  }
  
  @Override
  public Meter inc(long delta) {
    _value.add(delta);
    updated();
    if (_parent != null) {
      _parent.inc(delta);
    }
    return this;
  }
  
  @Override
  public Meter dec(long delta) {
    _value.add(-delta);
    updated();
    if (_parent != null) {
      _parent.dec(delta);
    }
    return this;
  }
  
  @Override
  public long getValue() {
    return _value.sum() & Long.MAX_VALUE; // if we reach Long.MAX_VALUE, wrap to 0 and start again to add from 0
  }
}
