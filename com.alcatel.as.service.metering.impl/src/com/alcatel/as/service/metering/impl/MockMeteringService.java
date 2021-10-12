// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeterListener;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering.Sampler;
import com.alcatel.as.service.metering.Stat;
import com.alcatel.as.service.metering.StopWatch;

public class MockMeteringService implements MeteringService {
  public Counter getCounter(String name) {
    return new MockCounter(name);
  }
  
  public Gauge getGauge(String name) {
    return new MockGauge(name);
  }
  
  public Rate getRate(String name) {
    return new MockRate(name);
  }
  
  private class MockMeter implements Meter {
    private Object _attachment;
    private String _name;
    
    public MockMeter(String name) {
      _name = name;
    }
    
    public void addMeterListener(MeterListener listener) {
    }
    
    public void attach(Object attachment) {
      _attachment = attachment;
    }
    
    public Object attachment() {
      return _attachment;
    }
    
    public Sampler createSampler() {
      return new SamplerImpl(this);
    }
    
    public String getName() {
      return _name;
    }
    
    public void removeMeterListener(MeterListener listener) {
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public boolean hasListeners() {
      return false;
    }
  }
  
  private class MockCounter extends MockMeter implements Counter, StopWatch {
    MockCounter(String name) {
      super(name);
    }
    
    public void add(long value) {
    }
    
    public StopWatch start() {
      return this;
    }
    
    public Counter getCounter() {
      return this;
    }
    
    public long getElapsedTime() {
      return 0;
    }
    
    public long pause() {
      return 0;
    }
    
    public void resume() {
    }
    
    public long stop() {
      return 0;
    }

	public void close() {
		stop();
	}
  }
  
  private class MockGauge extends MockMeter implements Gauge {
    MockGauge(String name) {
      super(name);
    }
    
    public void add(long delta) {
    }
    
    public void set(long value) {
    }
  }
  
  private class MockRate extends MockMeter implements Rate {
    MockRate(String name) {
      super(name);
    }
    
    public void hit() {
    }
    
    public void hit(long hits) {
    }
  }
  
  private static class SamplerImpl implements Sampler, Stat {
    private Meter _meter;
    
    SamplerImpl(Meter meter) {
      _meter = meter;
    }
    
    public Stat computeStat() {
      return this;
    }
    
    public Meter getMeter() {
      return _meter;
    }
    
    public void remove() {
    }
    
    public void meterChanged(Meter meter, long newValue, boolean add) {
    }
    
    public double getDeviation() {
      return 0;
    }
    
    public double getDeviationAcc() {
      return 0;
    }
    
    public double getMax() {
      return 0;
    }
    
    public double getMaxAcc() {
      return 0;
    }
    
    public double getMean() {
      return 0;
    }
    
    public double getMeanAcc() {
      return 0;
    }
    
    public double getMin() {
      return 0;
    }
    
    public double getMinAcc() {
      return 0;
    }
    
    public double getValue() {
      return 0;
    }
    
    public double getValueAcc() {
      return 0;
    }
  }
}
