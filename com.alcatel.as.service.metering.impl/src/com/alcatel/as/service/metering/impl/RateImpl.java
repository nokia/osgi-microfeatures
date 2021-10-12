// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering.Stat;

public class RateImpl extends MeterImpl implements Rate {
  public static final String RATE_NAME = "rate.name";
  
  public RateImpl(String name) {
    super(name);
  }
  
  @Override
  public String toString() {
    return new StringBuilder("Rate(").append(getName()).append(")").toString();
  }
  
  public void hit() {
    doSet(1, true);
  }
  
  public void hit(long value) {
    doSet(value, true);
  }
  
  @Override
  public String getDisplayName() {
    return "Rate";
  }
  
  public Sampler createSampler() {
    Sampler sampler = new HitCounterSampler();
    addMeterListener(sampler);
    return sampler;
  }
  
  class HitCounterSampler implements Sampler {
    protected RateMath _hitCounter = new RateMath();
    protected RateMath _hitCounterAcc = new RateMath();
    
    public synchronized void meterChanged(Meter meter, long count, boolean add) {
      _hitCounter.hit(count);
      _hitCounterAcc.hit(count);
    }
    
    public synchronized Stat computeStat() {
      _hitCounter.accumulate();
      _hitCounterAcc.accumulate();
      Stat stat = new StatImpl(RateImpl.this, _hitCounter.getHits(), _hitCounterAcc.getHits(),
          _hitCounter.getMean(), _hitCounterAcc.getMean(), _hitCounter.getStandardDeviation(),
          _hitCounterAcc.getStandardDeviation(), _hitCounter.getMin(), _hitCounterAcc.getMin(),
          _hitCounter.getMax(), _hitCounterAcc.getMax());
      _hitCounter.reset();
      return stat;
    }
    
    public Meter getMeter() {
      return RateImpl.this;
    }
    
    public void remove() {
      removeMeterListener(this);
    }
  }
}
