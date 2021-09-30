package com.alcatel.as.service.concurrent.impl;

// Jdk
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;

/**
 * Queue Executor using dedicated single thread queue.
 */
public class ThreadQueueExecutor extends PlatformExecutorBase implements PlatformExecutor {
  private final ThreadQueue _threadQueue;
  private final String _name;
  private volatile boolean _isShutdown;
  
  /**
   * Executor used to schedule timers using high priority.
   */
  private final Executor _timerExecutor;
  
  /**
   * Constructor
   * @param name the thread queue name
   */
  public ThreadQueueExecutor(String name, ThreadFactory factory) {
    _name = name;
    _threadQueue = new ThreadQueue(name, factory);
    _threadQueue.start();    
    _timerExecutor = command -> _threadQueue.execute(command, TaskPriority.HIGH);
    
    // We must set our threadlocal info in the reactor executor.
    _threadQueue.execute(() -> {
      ThreadContextImpl ctx = (ThreadContextImpl) Helper.getPlatformExecutors().getCurrentThreadContext();
      ctx.setCurrentThreadExecutor(ThreadQueueExecutor.this);
    }, TaskPriority.HIGH);
  }
  
  @Override
  public boolean equals(Object that) {
    return (that instanceof ThreadQueueExecutor && ((ThreadQueueExecutor) that)._name.equalsIgnoreCase(_name));
  }
  
  @Override
  public int hashCode() {
    return _name.toLowerCase().hashCode();
  }
  
  public String toString() {
    return new StringBuilder("ThreadQueueExecutor(").append(getId()).append(")").toString();
  }
  
  // --------------------- AbstractExecutorService------------------------------------------
  
  public boolean isShutdown() {
    return _isShutdown;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return true; // out shutdown method waits for termination.
  }
  
  public boolean isTerminated() {
    return _isShutdown;
  }
  
  public List<Runnable> shutdownNow() {
    shutdown();
    return Collections.emptyList();
  }
  
  public void shutdown() {
    synchronized (this) {
      if (_isShutdown) {
        return;
      }
      _isShutdown = true;
    }
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    execs.removeThreadQueueExecutor(_name);
    _threadQueue.shutdown(10000);
  }
  
  // -------------------- Executor -------------------------------------------------------
  
  public void execute(final Runnable task) {
    execute(task, TaskPriority.DEFAULT);
  }
  
  // ---------------------- ScheduledExecutorService--------------------------------------
  
  public ScheduledFuture<?> schedule(final Runnable task, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    final ClassLoader cl = Helper.getTCCL();
    return execs.getTimerService().schedule(_timerExecutor, () -> Helper.runTask(task, cl, ThreadQueueExecutor.this, root), delay, unit);
  }
  
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    final ClassLoader cl = Helper.getTCCL();
    Callable<V> wrap = () -> Helper.runCallable(callable, cl, ThreadQueueExecutor.this, root);
    return (ScheduledFuture<V>) execs.getTimerService().schedule(_timerExecutor, newTaskFor(wrap), delay,
                                                                 unit);
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, long initDelay, long delay, TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    final ClassLoader cl = Helper.getTCCL();
    return execs.getTimerService().scheduleAtFixedRate(_timerExecutor, 
      () -> Helper.runTask(task, cl, ThreadQueueExecutor.this, root), initDelay, delay, unit);
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable task, long initDelay, long delay,
                                                   TimeUnit unit) {
    PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    final ClassLoader cl = Helper.getTCCL();
    return execs.getTimerService().scheduleWithFixedDelay(_timerExecutor, 
      () -> Helper.runTask(task, cl, ThreadQueueExecutor.this, root), initDelay, delay, unit);
  }
  
  // -------------------- PlatformExecutor
  
  public String getId() {
    return _name;
  }
  
  public boolean isThreadPoolExecutor() {
    return false;
  }
  
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
      if (_threadQueue.getThreadQueue() == Thread.currentThread()) {
        ftask.run();
      } else {
        execute(ftask, TaskPriority.DEFAULT);
      }
      break;
    case INLINE_HIGH:
      if (_threadQueue.getThreadQueue() == Thread.currentThread()) {
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
    
  private void tryInline(Runnable r, TaskPriority pri) {
    if (_threadQueue.getThreadQueue() == Thread.currentThread()) {
      r.run();
    } else {
      execute(r, pri);
    }
  }
  
  private void execute(final Runnable task, TaskPriority pri) {
    int pindex = pri.ordinal();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    final ClassLoader cl = Helper.getTCCL();
    _threadQueue.execute(() -> Helper.runTask(task, cl, ThreadQueueExecutor.this, root), pri);
  }
}
