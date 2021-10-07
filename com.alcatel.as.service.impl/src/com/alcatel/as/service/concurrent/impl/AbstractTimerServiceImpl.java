package com.alcatel.as.service.concurrent.impl;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.concurrent.PlatformExecutors;

public abstract class AbstractTimerServiceImpl extends AbstractExecutorService implements ScheduledExecutorService {
    
  private volatile PlatformExecutors _pfExecs;
  
  void setPlatformExecutors(PlatformExecutors pfExecs) {
	  _pfExecs = pfExecs;
  }
  
  public abstract ScheduledFuture<?> schedule(Executor taskExecutor, final Runnable task, long delay,
                                              TimeUnit unit);
  
  public abstract ScheduledFuture<?> scheduleWithFixedDelay(final Executor taskExecutor, Runnable task,
                                                            long initDelay, long delay, TimeUnit unit);
  
  public abstract ScheduledFuture<?> scheduleAtFixedRate(Executor taskExecutor, Runnable task,
                                                         long initDelay, long delay, TimeUnit unit);
  
  public abstract void shutdown();
  
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return schedule(getCurrentThreadExecutor(), command, delay, unit);
  }
  
  @SuppressWarnings("unchecked")
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    RunnableFuture<V> ftask = new FutureTask<V>(callable);
    return (ScheduledFuture<V>) schedule(getCurrentThreadExecutor(), ftask, delay, unit);
  }
  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                TimeUnit unit) {
    return scheduleAtFixedRate(getCurrentThreadExecutor(), command, initialDelay, period, unit);
  }
  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                                                   TimeUnit unit) {
    return scheduleWithFixedDelay(getCurrentThreadExecutor(), command, initialDelay, delay, unit);
  }
  
  @Override
  public void execute(Runnable command) {
    getCurrentThreadExecutor().execute(command);
  }
  
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }
  
  public boolean isShutdown() {
    throw new UnsupportedOperationException();
  }
  
  public boolean isTerminated() {
    throw new UnsupportedOperationException();
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException();
  }
  
  private Executor getCurrentThreadExecutor() {
    return _pfExecs.getCurrentThreadContext().getCallbackExecutor();
  }
}
