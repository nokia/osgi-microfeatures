// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Simple thread pool, used by the Blocking IOThreadPool Executor.
 * This thread pool has support for task priorities.
 */
public class SimpleThreadPool implements ThreadPoolBase {
  /** We just use the simple executor from the jdk. **/
  private final ThreadPoolExecutor _executor;
  
  /** Our logger */
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.IOThreadPool");
  
  /** The thread pool name */
  private final String _name;

  /** Metrics */
  private final Meters _meters;

  /** Are we a blocking or a processing threadpool ? */
  private final boolean _blocking;
  
  /** Actual Jdk ThreadPool. We override ThreadPoolExecutor in case we need to use before/afterExecutor ... for now, we don't*/
  private class JdkThreadPool extends ThreadPoolExecutor {
    public JdkThreadPool(int corePoolSize,
                         int maximumPoolSize,
                         long keepAliveTime,
                         TimeUnit unit,
                         BlockingQueue<Runnable> workQueue,
                         ThreadFactory threadFactory) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      if (_blocking) {
        _meters.tpoolBlockingRun();
      } else {
        _meters.tpoolProcessingRun();
      }
    }
  }
      
  public SimpleThreadPool(String name, int size, Meters meters, boolean blocking) {
    _name = name;
    _executor = new JdkThreadPool(size, size, 10L, TimeUnit.SECONDS, new LinkedTransferQueue<Runnable>(),
        new WorkerThreadFactory(name, this));
    _executor.allowCoreThreadTimeOut(true);
    _meters = meters;
    _blocking = blocking;
  }
  
  public void setSize(int size) {
    _executor.setMaximumPoolSize(size);
    _executor.setCorePoolSize(size);
  }
  
  public void setKeepAlive(long idleTimeoutSc) {
      _executor.setKeepAliveTime(idleTimeoutSc == -1 ? Long.MAX_VALUE : idleTimeoutSc, TimeUnit.SECONDS);
  }
  
  public int getSize() {
    return _executor.getCorePoolSize();
  }
  
  public String getName() {
    return _name;
  }
  
  @Override
  public void shutdown() {
    _logger.info("Shutting down ThreadPool " + _name);
    _executor.shutdown();
  }
  
  @Override
  public List<Runnable> shutdownNow() {
    return _executor.shutdownNow();
  }
  
  @Override
  public boolean isShutdown() {
    return _executor.isShutdown();
  }
  
  @Override
  public boolean isTerminated() {
    return _executor.isTerminated();
  }
  
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return _executor.awaitTermination(timeout, unit);
  }
  
  @Override
  public void execute(final Runnable task) {
    execute(task, TaskPriority.DEFAULT);
  }
  
  public void execute(final Runnable task, TaskPriority pri /* not supported */) {
      if (_blocking) {
	  _meters.tpoolBlockingScheduled();
      } else {
        _meters.tpoolProcessingScheduled();
      }
    _executor.execute(task);
  }
  
  /**
   * Display our executor state.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(_name).append("[").append(_executor.toString()).append("]");
    return sb.toString();
  }
  
  /**
   * Tells if a given thread is managed by this thread pool.
   * @param t the thead to be checked
   * @return true if the thread t is managed by this thread pool.
   */
  public boolean isPooled(Thread t) {
    return t instanceof WorkerThread && ((WorkerThread) t).getThreadPool() == this;
  }
}
