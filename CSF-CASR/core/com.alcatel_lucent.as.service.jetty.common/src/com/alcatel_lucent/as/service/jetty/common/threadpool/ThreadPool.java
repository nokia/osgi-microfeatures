package com.alcatel_lucent.as.service.jetty.common.threadpool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;

import com.alcatel.as.service.concurrent.TimerService;

public class ThreadPool implements org.eclipse.jetty.util.thread.ThreadPool, ScheduledExecutorServiceIf, LifeCycle {

  private ScheduledExecutorService executor;
  private TimerService timerService;

  
  public ThreadPool(ScheduledExecutorService executor, TimerService timerService)
  {
    super();
    this.executor = executor;
    this.timerService = timerService;
  }

  /**
   * Execute request or continuation
   * @param job
   * @return
   */
  public boolean schedule(Runnable job) {
    try {
      executor.execute(job);
      return true;
    } catch (RejectedExecutionException e) {
      Log.getRootLogger().warn(e);
      return false;
    }
  }

  /*-- interface ThreadPool --*/

  @Override
  public void execute(Runnable job) {
    schedule(job);
  }

  @Override
  public int getIdleThreads() {
    return -1;
  }

  @Override
  public int getThreads() {
    return -1;
  }

  @Override
  public boolean isLowOnThreads() {
    return false;
  }

  @Override
  public void join() throws InterruptedException {
  }

  @Override
  public void addLifeCycleListener(Listener listener) {
    // not implemented     
  }

  @Override
  public void removeLifeCycleListener(Listener listener) {
    // not implemented     
  }

  /*-- Interface LifeCycle --*/

  public boolean isFailed() {
    return false;
  }

  public boolean isRunning() {
    return !executor.isTerminated() && !executor.isShutdown();
  }

  public boolean isStarted() {
    return isRunning();
  }

  public boolean isStarting() {
    return false;
  }

  public boolean isStopped() {
    return executor.isTerminated() || executor.isShutdown();
  }

  public boolean isStopping() {
    return isStopped();
  }

  public void start() throws Exception {
    if (executor.isTerminated() || executor.isShutdown())
      throw new IllegalStateException("Cannot restart");
  }

  public void stop() throws Exception {
  }

  /* interface ScheduledExecutorServiceIf */

  @SuppressWarnings("rawtypes")
  public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return timerService.scheduleAtFixedRate(executor, command, initialDelay, period, unit);
  }

  @SuppressWarnings("rawtypes")
  public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return timerService.scheduleWithFixedDelay(executor, command, initialDelay, delay, unit);
  }

}
