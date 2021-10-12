// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

// Jdk
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

public class ThreadPoolExecutor extends PlatformExecutorBase implements PlatformExecutor {
  /** The actual thread pool implementation */
  protected final ThreadPoolBase _tpool;
    
  /**
   * Executor used to ensure that timers are scheduled using HIGH priority.
   */
  final Executor _timerExecutor;
  
  /**
   * Constructor.
   * @param tpool
   * @param statPrefix
   */
  ThreadPoolExecutor(ThreadPoolBase tpool) {
    _tpool = tpool;
    _timerExecutor = task -> _tpool.execute(task, TaskPriority.HIGH);
  }
  
  // -------------------- AbstractExecutorService
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return _tpool.awaitTermination(timeout, unit);
  }
  
  public boolean isTerminated() {
    return _tpool.isTerminated();
  }
  
  public boolean isShutdown() {
    return _tpool.isShutdown();
  }
  
  public List<Runnable> shutdownNow() {
    return _tpool.shutdownNow();
  }
  
  public void shutdown() {
    _tpool.shutdown();
  }
  
  // -------------------- Executor -------------------------------------
  
  public void execute(final Runnable task) {
    execute(task, TaskPriority.DEFAULT);
  }
  
  public String toString() {
    return new StringBuilder("ThreadPoolExecutor: ").append(_tpool.toString()).toString();
  }
  
  // --------------------- ScheduledExecutorService ---------------------
  
  public ScheduledFuture<?> schedule(final Runnable task, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    return execs.getTimerService().schedule(_timerExecutor, 
    		() -> Helper.runTask(task, cl, ThreadPoolExecutor.this, root), delay, unit);
  }
  
  public <T> ScheduledFuture<T> schedule(final Callable<T> callable, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    Callable<T> wrap = () -> Helper.runCallable(callable, cl, ThreadPoolExecutor.this, root);
    return (ScheduledFuture<T>) execs.getTimerService().schedule(_timerExecutor, newTaskFor(wrap), delay,
                                                                 unit);
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, long initDelay, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    return execs.getTimerService().scheduleAtFixedRate(_timerExecutor, 
    		() -> Helper.runTask(task, cl, ThreadPoolExecutor.this, root), initDelay, delay, unit);
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable task, long initDelay, long delay,
                                                   TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    return execs.getTimerService().scheduleWithFixedDelay(_timerExecutor, 
    		() -> Helper.runTask(task, cl, ThreadPoolExecutor.this, root), initDelay, delay, unit);
  }
  
  // -------------------- PlatformExecutor ------------------------
  
  @SuppressWarnings("deprecation")
  public String getId() {
    return PlatformExecutors.THREAD_POOL_EXECUTOR;
  }
  
  public boolean isThreadPoolExecutor() {
    return true;
  }
  
  @Override
  public void execute(Runnable r, ExecutorPolicy policy) {
    switch (policy) {
    case SCHEDULE:
      execute(r, TaskPriority.DEFAULT);
      break;
    
    case SCHEDULE_HIGH:
      execute(r, TaskPriority.HIGH);
      break;
    
    case INLINE:
      tryInline(r, TaskPriority.DEFAULT);
      break;
    
    case INLINE_HIGH:
      tryInline(r, TaskPriority.HIGH);
      break;
    
    default:
      throw new IllegalArgumentException("Invalid ExecutorPolicy parameter: " + policy);
    }
  }
  
  public <T> Future<T> submit(Callable<T> task, ExecutorPolicy policy) {
    RunnableFuture<T> ftask = newTaskFor(task);
    switch (policy) {
    case SCHEDULE:
      execute(ftask, TaskPriority.DEFAULT);
      break;
    case SCHEDULE_HIGH:
      execute(ftask, TaskPriority.HIGH);
      break;
    case INLINE:
      if (mayInline()) {
        ftask.run();
      } else {
        execute(ftask, TaskPriority.DEFAULT);
      }
      break;
    case INLINE_HIGH:
      if (mayInline()) {
        ftask.run();
      } else {
        execute(ftask, TaskPriority.HIGH);
      }
      break;
    default:
      throw new IllegalArgumentException("Invalid ExecutorPolicy parameter: " + policy);
    }
    
    return ftask;
  }
  
  public PlatformExecutors getPlatformExecutors() {
    return Helper.getPlatformExecutors();
  }
  
  // -------------------- Protected/private methods -----------------------------
  
  public int getSize() {
    return _tpool.getSize();
  }
  
  protected boolean mayInline() {
    return _tpool.isPooled(Thread.currentThread());
  }
  
  protected ThreadPoolBase getThreadPool() {
    return _tpool;
  }
  
  private void tryInline(Runnable r, TaskPriority default1) {
    if (mayInline()) {
      r.run();
    } else {
      execute(r, TaskPriority.DEFAULT);
    }
  }
    
  private void execute(final Runnable task, TaskPriority pri) {
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    _tpool.execute(() -> Helper.runTask(task, cl, ThreadPoolExecutor.this, root), pri);
  }
}
