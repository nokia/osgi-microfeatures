// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.Monitorable;

/**
 * A Jdk based timer, which schedules timers accurately (resolution=MILLIS).
 * This class allows to clear timer references when scheduled tasks are cancelled,
 * in order to avoid memory leaks.
 */
public class JdkTimerServiceImpl extends AbstractTimerServiceImpl implements TimerService {
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.JdkTimerServiceImpl");
  /** Counter used to give a name to the worker threads. */
  private final static AtomicInteger _schedulerCounterName = new AtomicInteger();
  private final static String POOLSIZE_CONF = System.getProperty("system.jdktimer.poolsize", "1");
  private final static int POOLSIZE_INT = Integer.valueOf(POOLSIZE_CONF);
  private final static int POOLSIZE = POOLSIZE_INT == 0 ? Runtime.getRuntime().availableProcessors() : POOLSIZE_INT;
  private final ScheduledThreadPoolExecutor[] _jdkTimers = new ScheduledThreadPoolExecutor[POOLSIZE];
  public final static AtomicInteger _jdkTimersRoundRobin = new AtomicInteger();
  private final static AtomicLong _sequencer = new AtomicLong();
  private volatile Meters _meters;
  
  void bindMeters(Monitorable meters) {
    _meters = (Meters) meters;
  }
  
  /**
   * Class extending FutureTask for allowing to have access to runAndReset method,
   * which is protected in FutureTask class.
   */
  class AsyncTask<T> extends FutureTask<T> {
    public AsyncTask(Runnable task) {
      super(Executors.callable(task, (T) null));
    }
    
    @Override
    public boolean runAndReset() {
      return super.runAndReset();
    }
    
    @Override
    protected void done() {
      // Called when the timer has been executed or has been cancelled.
      if (isCancelled()) {
        _meters.jdkTimerCancelled();
      } else {
        _meters.jdkTimerRun();
      }
    }
  }
  
  /**
   * Task returned by {@link JdkTimerServiceImpl#schedule(Executor, Runnable, long, TimeUnit)}
   */
  class Task<T> implements Runnable, ScheduledFuture<T> {
    protected final long _sequenceNumber;
    protected volatile long _eTime; // expiration time in millis
    protected volatile Executor _executor;
    protected volatile AsyncTask<T> _task;
    protected volatile ScheduledFuture<?> _future;
    
    Task(Executor executor, Runnable task, long expirationTimeMs) {
      _task = new AsyncTask<T>(task);
      _executor = executor;
      _eTime = expirationTimeMs;
      _sequenceNumber = _sequencer.incrementAndGet();
    }
    
    public void schedule(long delayMs) {
      ScheduledThreadPoolExecutor jdkTimer = pickupTimer();
      _future = jdkTimer.schedule(this, delayMs, TimeUnit.MILLISECONDS);
    }
    
    protected ScheduledThreadPoolExecutor pickupTimer() {
      int index = _jdkTimersRoundRobin.incrementAndGet() & Integer.MAX_VALUE;
      return _jdkTimers[index % _jdkTimers.length];      
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      mayInterruptIfRunning = false; // TODO remove 
      AsyncTask<?> task = _task;
      if (task != null && task.cancel(mayInterruptIfRunning)) {
        _future.cancel(mayInterruptIfRunning);
        _task = null;
        _executor = null;
        return true;
      }
      return false;
    }
    
    @Override
    public void run() {
      final AsyncTask<?> task = _task;
      Executor executor = _executor;
      if (task != null && executor != null) {
        executor.execute(() -> {
          try {
            task.run(); // won't run if cancelled
          }
          catch (Throwable t) {
            _logger.warn("unexpected exception while executing timer task", t);
          }
        });
      }
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(_eTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
      long d = (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
      if (d == 0) {
        d = _sequenceNumber - ((Task<?>) other)._sequenceNumber;
      }
      return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
    
    @Override
    public boolean isCancelled() {
      final AsyncTask<?> task = _task;
      return (task != null) ? task.isCancelled() : true;
    }
    
    @Override
    public boolean isDone() {
      final AsyncTask<?> task = _task;
      return (task != null) ? task.isDone() : true;
    }
    
    @Override
    public T get() throws InterruptedException, ExecutionException {
      FutureTask<T> task = _task;
      if (task == null) {
        throw new CancellationException();
      }
      return task.get();
    }
    
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
        TimeoutException {
      FutureTask<T> task = _task;
      if (task == null) {
        throw new CancellationException();
      }
      return task.get(timeout, unit);
    }
  }
  
  /**
   * Task returned by scheduleWithFixedDelay method.
   */
  class FixedDelayTask<T> extends Task<T> {
    protected final long _period;
    
    FixedDelayTask(Executor executor, Runnable task, long expirationTimeMs, long periodMs) {
      super(executor, task, expirationTimeMs);
      _period = periodMs;
    }
    
    @Override
    public void run() {
      final AsyncTask<?> task = _task;
      Executor executor = _executor;
      if (task != null && executor != null) {
        executor.execute(() -> {
          if (task.runAndReset()) {
            reschedule(_period);
          }
        });
      }
    }
    
    protected synchronized void reschedule(long period) {
      AsyncTask<?> task = _task;
      if (task != null) {
        _eTime = System.currentTimeMillis() + period;
        _future = pickupTimer().schedule(this, period, TimeUnit.MILLISECONDS);
      }
    }
    
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(mayInterruptIfRunning);
    }
  }
  
  /**
   * Task returned by scheduleAtFixedRate method.
   */
  class FixedRateTask<T> extends FixedDelayTask<T> {
    protected volatile long _firstExecutionTime;
    protected volatile int _executionsNumber;
    
    FixedRateTask(Executor executor, Runnable task, long expirationTimeMs, long periodMs) {
      super(executor, task, expirationTimeMs, periodMs);
    }
    
    @Override
    public void run() {
      final AsyncTask<?> task = _task;
      Executor executor = _executor;
      if (task != null && executor != null) {
        executor.execute(() -> {
          _executionsNumber++;
          if (_firstExecutionTime == 0) {
            _firstExecutionTime = System.currentTimeMillis();
          }
          if (task.runAndReset()) {
            long nextTime = Math.max(0, _firstExecutionTime + (_period * _executionsNumber));
            reschedule(Math.max(0, nextTime - System.currentTimeMillis()));
          }
        });
      }
    }
  }
  
  void start(Map<String, Object> config) {
    _logger.info("Initializing jdk timer (pool=" + POOLSIZE  + ")");

    for (int i = 0; i < _jdkTimers.length; i ++) {
      _jdkTimers[i] = new ScheduledThreadPoolExecutor(1);    
      // Initialize the name of our scheduler worker threads
      _jdkTimers[i].setThreadFactory(r -> {
        StringBuilder sb = new StringBuilder();
        sb.append("JdkTimerService-" + _schedulerCounterName.incrementAndGet());
        Thread t = new Thread(r, sb.toString());
        t.setDaemon(true);
        return t;
      });
    }
  }
  
  void stop() {
    for (int i = 0; i < _jdkTimers.length; i ++) {
      _jdkTimers[i].shutdown();
    }
  }
  
  @Override
  public ScheduledFuture<?> schedule(final Executor taskExecutor, final Runnable task, long delay,
                                     TimeUnit unit) {
    _meters.jdkTimerScheduled();
    long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
    Task<?> t = new Task<Object>(taskExecutor, task, System.currentTimeMillis() + delayMs);
    t.schedule(delayMs);
    return t;
  }
  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Executor taskExecutor, final Runnable task,
                                                   long initDelay, long delay, TimeUnit unit) {
    _meters.jdkTimerScheduled();
    long initDelayMs = TimeUnit.MILLISECONDS.convert(initDelay, unit);
    long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
    
    FixedDelayTask<?> t = new FixedDelayTask<Object>(taskExecutor, task, System.currentTimeMillis()
        + initDelayMs, delayMs);
    t.schedule(initDelayMs);
    return t;
  }
  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Executor taskExecutor, final Runnable task,
                                                long initDelay, long delay, TimeUnit unit) {
    _meters.jdkTimerScheduled();
    long initDelayMs = TimeUnit.MILLISECONDS.convert(initDelay, unit);
    long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
    
    FixedRateTask<?> t = new FixedRateTask<Object>(taskExecutor, task, System.currentTimeMillis()
        + initDelayMs, delayMs);
    t.schedule(initDelayMs);
    return t;
  }
  
  @Override
  public void shutdown() {
    for (int i = 0; i < _jdkTimers.length; i ++) {
      _jdkTimers[i].shutdown();
    }
  }
  
  @Override
  public List<Runnable> shutdownNow() {
    List<Runnable> runnables = new ArrayList<>();
    for (int i = 0; i < _jdkTimers.length; i ++) {
      runnables.addAll(_jdkTimers[i].shutdownNow());
    }
    return runnables;
  }
  
  @Override
  public boolean isShutdown() {
    boolean isShutdown = false;
    for (int i = 0; i < _jdkTimers.length; i ++) {
      isShutdown |= _jdkTimers[i].isShutdown();
    }
    return isShutdown;
  }
  
  @Override
  public boolean isTerminated() {
    boolean isTerminated = false;
    for (int i = 0; i < _jdkTimers.length; i ++) {
      isTerminated |= _jdkTimers[i].isTerminated();
    }
    return isTerminated;
  }
  
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    boolean status = false;
    for (int i = 0; i < _jdkTimers.length; i ++) {
      status |= _jdkTimers[i].awaitTermination(timeout, unit);
    }
    return status;
  }
}
