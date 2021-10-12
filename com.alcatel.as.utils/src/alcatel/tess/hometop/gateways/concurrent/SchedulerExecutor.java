// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

// Utils
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import alcatel.tess.hometop.gateways.utils.SnapshotManager;

/**
 * Serialized Thread Pool Executor. This thread pool is similar to the ThreadPoolExecutor,
 * but serializes the execution of Runnable which toString method returns the same identifier.
 * IE: if two runnables R1, R2 returns the same String from their toString() method, then the
 * execution of R2 is scheduled AFTER the execution of R1.
 */
public class SchedulerExecutor implements Executor {
  public SchedulerExecutor() {
  }
  
  public void execute(final Runnable task) {
    final Object[] states = SnapshotManager.instance.takeSnapshots();
    final String taskId = task.toString();
    final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    
    Runnable wrappedTask = new Runnable() {
      public void run() {
        try {
          if (states != null) {
            SnapshotManager.instance.restoreSnapshots(states);
          }
          Thread.currentThread().setContextClassLoader(ccl);
          task.run();
        }
        
        finally {
          _dispatcher.removeUser(taskId);
        }
      }
    };
    
    _dispatcher.addUser(taskId);
    
    try {
      _dispatcher.dispatch(taskId, wrappedTask);
    } catch (InterruptedException e) {
      throw new RejectedExecutionException("Can not schedule the task " + task, e);
    }
  }
  
  private final static EventDispatcher _dispatcher = new EventDispatcher(); // will use default thread pool.
}
