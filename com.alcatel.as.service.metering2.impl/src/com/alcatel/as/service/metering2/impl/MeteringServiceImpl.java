// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.ValueSupplier;

public class MeteringServiceImpl implements MeteringService {
  /**
   * Map of all existing Monitorable services. Key = Monitorable name, Value = Monitorable.
   */
  protected final ConcurrentMap<String, Monitorable> _monitorables = new ConcurrentHashMap<>();
  
  /**
   * The timer used to schedule periodic monitoring jobs.
   */
  protected final ScheduledExecutorService _timer =
      Executors.newScheduledThreadPool(1, 
                                       new ThreadFactory() {
                                        public Thread newThread(Runnable r) {
                                          Thread t = new Thread(r, "MeteringService");
                                          t.setDaemon(true);
                                          return t;
                                        }
      });
  
  @Override
  public Meter createAbsoluteMeter(String name) {
      return new AbsoluteMeterImpl(name, _timer);
  }
  
  @Override
  public Meter createIncrementalMeter(String name, Meter parent) {
      return new IncrementalMeterImpl(name, parent, _timer);
  }

  @Override
  public Meter createValueSuppliedMeter(String name, ValueSupplier valueSupplier) {
    return new SuppliedMeterImpl(name, valueSupplier, _timer);
  }

  @Override
  public Monitorable getMonitorable(String name) {
    return _monitorables.get(name);
  }
  
  @SuppressWarnings("unused")
  private void add(Monitorable monitorable) {
    _monitorables.put(monitorable.getName(), monitorable);
  }
  
  @SuppressWarnings("unused")
  private void remove(Monitorable monitorable) {
    _monitorables.remove(monitorable.getName());
  }
}
