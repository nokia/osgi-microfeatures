// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This executor ensures that at most one task is running at any time, but it achieves this without creating 
 * a thread or locking anything during the execution of a task.
 * For performance reasons, this class does not contain recursive methods, and try to minimize
 * synchronization.
 * 
 * This class can be typically used by components which must protect its internal state, while
 * invoking listeners. It can also be used in OSGi, from activators where services must be registered.
 * (in OSGi, a service must never be registered while holding a lock, or from a synchronized method ...).
 * 
 * @author ASR Team.
 */
public class SerialExecutor implements Executor {
  /** All tasks scheduled are stored there and only one thread may run them. */
  protected final ConcurrentLinkedQueue<Runnable> _tasks = new ConcurrentLinkedQueue<Runnable>();
  
  /** Flag telling if a thread is currently executing the tasks queue. */
  protected final AtomicBoolean _running = new AtomicBoolean();
    
  public void execute(Runnable task) {
    _tasks.add(task);
    if (_running.compareAndSet(false, true)) {
      runTasks();
    }
  }
  
  protected void runTasks() {
    do {
      try {
        // We do a memory barrier in order to ensure consistent per-thread memory visibility.
        // Only one thread at a time is running this method, so there is no possible contention.
        Runnable task;
        ConcurrentLinkedQueue<Runnable> tasks = _tasks;
        
        synchronized (this) {
          while ((task = tasks.poll()) != null) {
            try {
              task.run();
            } catch (Throwable t) {
              t.printStackTrace(); // will be logged to log4j
            }
          }
        }
      } finally {
        _running.set(false);
      }
    } while (!_tasks.isEmpty() && _running.compareAndSet(false, true));
  }
}
