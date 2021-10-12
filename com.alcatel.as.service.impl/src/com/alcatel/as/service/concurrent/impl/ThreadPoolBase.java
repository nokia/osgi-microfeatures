// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;

/**
 * Base class for our thread pool implementations.
 */
public interface ThreadPoolBase extends Executor {
  boolean isPooled(Thread t);
  
  void setSize(int size);
  
  int getSize();
  
  void setKeepAlive(long idleTimeoutSec);
  
  void execute(Runnable task, TaskPriority pri);
  
  void shutdown();
  
  List<Runnable> shutdownNow();
  
  boolean isShutdown();
  
  boolean isTerminated();
  
  boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
  
  String toString();
}
