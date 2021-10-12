// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering.Stat;

public class GaugeImpl extends MeterImpl implements Gauge {
  public final static String GAUGE_NAME = "gauge.name";
  
  /**
   * Constructor invoked when not running within OSGi
   * @param name the gauge name
   */
  public GaugeImpl(String name) {
    super(name);
  }
  
  @Override
  public String toString() {
    return new StringBuilder("Gauge(").append(getName()).append(")").toString();
  }
  
  public void add(long delta) {
    doSet(delta, true);
  }
  
  public void set(long value) {
    doSet(value, false);
  }
  
  public Sampler createSampler() {
    GaugeSampler sampler = new GaugeSampler();
    addMeterListener(sampler);
    return sampler;
  }
  
  @Override
  public String getDisplayName() {
    return "Gauge";
  }
  
  class GaugeSampler implements Sampler {
    protected GaugeMath _gaugeStat = new GaugeMath();
    protected GaugeMath _gaugeStatAcc = new GaugeMath();
    
    public synchronized void meterChanged(Meter meter, long count, boolean add) {
      if (add) {
        _gaugeStat.add(count);
        _gaugeStatAcc.add(count);
      } else {
        _gaugeStat.set(count);
        _gaugeStatAcc.set(count);
      }
    }
    
    public synchronized Stat computeStat() {
      _gaugeStat.accumulate();
      _gaugeStatAcc.accumulate();
      Stat event = new StatImpl(GaugeImpl.this, _gaugeStat.get(), _gaugeStatAcc.get(), _gaugeStat.getMean(),
          _gaugeStatAcc.getMean(), _gaugeStat.getStandardDeviation(), _gaugeStatAcc.getStandardDeviation(),
          _gaugeStat.getMin(), _gaugeStatAcc.getMin(), _gaugeStat.getMax(), _gaugeStatAcc.getMax());
      _gaugeStat.reset();
      return event;
    }
    
    public Meter getMeter() {
      return GaugeImpl.this;
    }
    
    public void remove() {
      removeMeterListener(this);
    }
  }
}
