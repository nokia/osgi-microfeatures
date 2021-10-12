// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import java.util.concurrent.atomic.AtomicLong;

import com.alcatel.as.service.metering2.StopWatch;
import com.alcatel.as.service.metering2.ValueSupplier;

public class SlidingAverage implements ValueSupplier {
  
  private final int _coef;
  private final AtomicLong _value = new AtomicLong(0);
  private AtomicLong _deviation;
  private ValueSupplier _deviationSupplier;
  private boolean _init = true, _trackDeviation = false;
  private final SerialExecutor _exec = new SerialExecutor();
  
  public SlidingAverage() {
    this(3);
  }
  
  public SlidingAverage(int coef) {
    _coef = coef;
  }

  public ValueSupplier getDeviation(){
    if (!_trackDeviation){
      _deviation = new AtomicLong (0);
      _deviationSupplier = new ValueSupplier (){ public long getValue() {return _deviation.get ();}};
      _trackDeviation = true;
    }
    return _deviationSupplier;
  }
  
  public long getValue() {
    return _value.get();
  }
  
  public void update(final long value) {
    Runnable r = new Runnable() {
      public void run() {
        updateInline(value);
      }
    };
    _exec.execute(r);
  }
  
  public void updateInline(long value) {
    if (_init) {
      _value.lazySet(value);
      _init = false;
    } else {
      long average = _value.get();
      long diff = value - average;
      if (_trackDeviation){
	long err = Math.abs (diff);
	long deviation = _deviation.get ();
	_deviation.lazySet (deviation + ((err - deviation) >> 2));
      }
      _value.lazySet(average + (diff >> _coef));      
    }
  }
  
  public StopWatch startWatch() {
    final long startTime = System.nanoTime();
    
    return new StopWatch() {
      public void close() {
        update(System.nanoTime() - startTime);
      }
    };
  }
}
