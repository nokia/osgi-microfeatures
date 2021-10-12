// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.atomic.LongAdder;

import org.osgi.framework.BundleContext;

import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.service.metering2.ValueSupplier;

/**
 * PlatformExecutors Meters.
 */
public class Meters extends SimpleMonitorable {
  public final static String MONITORABLE = "as.service.concurrent";
  private MeteringService _metering;
  private LongAdderSupplier _jdkTimerScheduled;
  private LongAdderSupplier _jdkTimerCancelled;
  private LongAdderSupplier _jdkTimerRun;
  private LongAdderSupplier _wheelTimerScheduled;
  private LongAdderSupplier _wheelTimerCancelled;
  private LongAdderSupplier _wheelTimerRun;
  private LongAdderSupplier _tpoolBlockingRun;
  private LongAdderSupplier _tpoolBlockingScheduled;
  private LongAdderSupplier _tpoolProcessingRun;
  private LongAdderSupplier _tpoolProcessingScheduled;
  private LongAdderSupplier _queueRun;
  private LongAdderSupplier _queueScheduled;
  private LongAdderSupplier _taskRun;
  private LongAdderSupplier _taskScheduled;
  
  private static class LongAdderSupplier implements ValueSupplier {
    final LongAdder _meter;
    final LongAdderSupplier _parent;
    
    LongAdderSupplier(LongAdderSupplier parent) {
      _meter = new LongAdder();
      _parent = parent;
    }
    
    void inc(long delta) {
      _meter.add(delta);
      if (_parent != null) {
        _parent.inc(delta);
      }
    }
    
    void dec(long delta) {
      _meter.add(-delta);
      if (_parent != null) {
        _parent.dec(delta);
      }
    }
    
    @Override
    public long getValue() {
      return _meter.longValue();
    }
  }
  
  public Meters() {
    super(MONITORABLE, "Platform Executors Metrics");
  }

  public void bindMeteringService(MeteringService metering) {
    _metering = metering;
  }
  
  public void start(BundleContext bctx) {  
    _taskScheduled = newLongAdderSupplier("task.scheduled");
    _taskRun = newLongAdderSupplier( "task.run");
    _jdkTimerScheduled = newLongAdderSupplier( "timer.jdk.scheduled", _taskScheduled);
    _jdkTimerCancelled = newLongAdderSupplier("timer.jdk.cancelled");
    _jdkTimerRun = newLongAdderSupplier("timer.jdk.run");
    _wheelTimerScheduled = newLongAdderSupplier("timer.wheel.scheduled", _taskScheduled);
    _wheelTimerCancelled = newLongAdderSupplier("timer.wheel.cancelled");
    _wheelTimerRun = newLongAdderSupplier("timer.wheel.run");
    _tpoolBlockingScheduled = newLongAdderSupplier("tpool.blocking.scheduled", _taskScheduled);
    _tpoolBlockingRun = newLongAdderSupplier("tpool.blocking.run", _taskRun);
    _tpoolProcessingScheduled = newLongAdderSupplier("tpool.processing.scheduled", _taskScheduled);
    _tpoolProcessingRun = newLongAdderSupplier("tpool.processing.run", _taskRun);
    _queueScheduled = newLongAdderSupplier("queue.scheduled", _taskScheduled);
    _queueRun = newLongAdderSupplier("queue.run", _taskRun);

    if (bctx != null) {
      super.start(bctx);
    }
  }
  
  private LongAdderSupplier newLongAdderSupplier(String name) {
    return newLongAdderSupplier(name, null);
  }

  private LongAdderSupplier newLongAdderSupplier(String name, LongAdderSupplier parent) {
    LongAdderSupplier meter = new LongAdderSupplier(parent);
    createValueSuppliedMeter(_metering, name, meter);
    return meter;    
  }
  
  public void jdkTimerScheduled() {
    _jdkTimerScheduled.inc(1);
  }
  
  public void jdkTimerRun() {
    _jdkTimerRun.inc(1);
    _jdkTimerScheduled.dec(1);
  }
  
  public void jdkTimerCancelled() {
    _jdkTimerCancelled.inc(1);
    _jdkTimerScheduled.dec(1);
  }
  
  public void wheelTimerScheduled() {
    _wheelTimerScheduled.inc(1);
  }
  
  public void wheelTimerRun() {
    _wheelTimerRun.inc(1);
    _wheelTimerScheduled.dec(1);
  }
  
  public void wheelTimerCancelled() {
    _wheelTimerCancelled.inc(1);
    _wheelTimerScheduled.dec(1);
  }

  public void tpoolBlockingScheduled() {
   _tpoolBlockingScheduled.inc(1);
  }

  public void tpoolBlockingRun() {
    _tpoolBlockingRun.inc(1);
    _tpoolBlockingScheduled.dec(1);
  }  
  
  public void tpoolProcessingScheduled() {
    _tpoolProcessingScheduled.inc(1);
  }

  public void tpoolProcessingRun() {
    _tpoolProcessingRun.inc(1);
    _tpoolProcessingScheduled.dec(1);
  }
  
  public void queueScheduled() {
    _queueScheduled.inc(1);
  }
  
  public void queueRun() {
    _queueRun.inc(1);
    _queueScheduled.dec(1);
  }
}
