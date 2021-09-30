package com.alcatel.as.service.metering.impl;

import java.util.concurrent.atomic.AtomicLong;

public class StatefulGaugeImpl extends GaugeImpl {
  private final AtomicLong _accumValue = new AtomicLong();

  public StatefulGaugeImpl(String name) {
    super(name);
  }
  
  @Override
  public void add(long delta) {
    _accumValue.addAndGet(delta);
    super.add(delta);
  }
  
  @Override
  public void set(long value) {
    _accumValue.set(value);
    super.set(value);
  }
  
  @Override
  public long getValue() {
    return _accumValue.get();
  }
}
