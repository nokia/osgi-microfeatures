// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.Monitorable;

/**
 * A Hashed Wheel Timer optimized for approximated I/O timeout scheduling.
 */
public class WheelTimerServiceImpl extends AbstractTimerServiceImpl implements Runnable, TimerService {
  private final static int TICK_DELAY = 50;
  private final static int WHEEL_SIZE = 4096; // Must be a power of two.
  private final static int ROTATION_DELAY = WHEEL_SIZE * TICK_DELAY;
  private volatile int _currentSlot = 0;
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.timer.WheelTimerServiceImpl");
  private Thread _thread;
  private long _nextTickTime;
  @SuppressWarnings("unchecked")
  private ConcurrentHashMap<Task, Task> _wheel[] = new ConcurrentHashMap[WHEEL_SIZE];
  private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock _rlock = _lock.readLock();
  private final ReentrantReadWriteLock.WriteLock _wlock = _lock.writeLock();
  private volatile Meters _meters;
  
  public WheelTimerServiceImpl() {
    for (int i = 0; i < _wheel.length; i++) {
      _wheel[i] = new ConcurrentHashMap<Task, Task>(16, 0.75f, 2);
    }
  }

  void bindMeters(Monitorable meters) {
    _meters = (Meters) meters;
  }

  public void start() {
    _logger.info("Initializing wheel timer service.");
    _thread = new Thread(this, "WheelTimerService");
    _thread.setDaemon(true);
    _thread.start();
  }
  
  public void stop() {
    _thread.interrupt();
    try {
      _thread.join();
    } catch (InterruptedException e) {
      _logger.warn("can't stop timer service", e);
    }
  }
  
  @Override
  public ScheduledFuture<?> schedule(Executor executor, Runnable task, long delay, TimeUnit unit) {
    _meters.wheelTimerScheduled();
    delay = unit.toMillis(delay);
    long now = System.currentTimeMillis();
    DelayedTask dt = new DelayedTask(executor, task, now + delay);
    dt.schedule(now, delay, true);
    return dt;
  }
  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Executor executor, Runnable timer, long initDelay,
                                                   long delay, TimeUnit unit) {
    _meters.wheelTimerScheduled();
    initDelay = unit.toMillis(initDelay);
    long now = System.currentTimeMillis();
    FixedDelayTask fdt = new FixedDelayTask(executor, timer, now + initDelay, unit.toMillis(delay));
    fdt.schedule(now, initDelay, true);
    return fdt;
  }
  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Executor executor, Runnable timer, long initDelay,
                                                long delay, TimeUnit unit) {
    _meters.wheelTimerScheduled();
    initDelay = unit.toMillis(initDelay);
    long now = System.currentTimeMillis();
    FixedRateTask task = new FixedRateTask(executor, timer, now + initDelay, unit.toMillis(delay));
    task.schedule(now, initDelay, true);
    return task;
  }
  
  @Override
  public void shutdown() {
    _thread.interrupt();
    try {
      _thread.join(3000);
    } catch (InterruptedException e) {
      _logger.warn("exception while shutting down wheel timer service", e);
    }
  }
  
  public void run() {
    _logger.info("Starting wheel timer service");
    
    final long startTime = System.currentTimeMillis();
    long counter = 0;
    HashMap<Executor, LinkedList<Runnable>> expiredTasks = new HashMap<Executor, LinkedList<Runnable>>();
    
    while (!_thread.isInterrupted()) {
      try {
        // Wait for next tick to occur.
        _nextTickTime = startTime + ((++counter) * TICK_DELAY);
        Thread.sleep(Math.max(0, _nextTickTime - System.currentTimeMillis()));
        
        _wlock.lock();
        try {
          // Advance our slot by one
          _currentSlot = (_currentSlot + 1) & (WHEEL_SIZE - 1);
          
          // Fetch expired timers.
          for (Task t : _wheel[_currentSlot].keySet()) {
            t.getExpiredTimers(_nextTickTime, expiredTasks);
          }
        } finally {
          _wlock.unlock();
        }
        
        // Execute expired timers outside our write lock.
        if (expiredTasks.size() > 0) {
          for (Map.Entry<Executor, LinkedList<Runnable>> entry : expiredTasks.entrySet()) {
            final Executor exec = entry.getKey();
            final LinkedList<Runnable> runnables = entry.getValue();
            if (runnables.size() > 1) {
              exec.execute(() -> {
                for (Runnable runnable : runnables) {
                  try {
                    runnable.run();
                  } catch (Throwable t) {
                    _logger.error("timer service exception", t);
                  }
                }
              });
            } else {
              exec.execute(runnables.getFirst());
            }
          }
          expiredTasks.clear();
        }
      } catch (InterruptedException e) {
        return;
      } catch (Throwable t) {
        _logger.error("timer service exception", t);
      }
    }
  }
  
  /**
   * Base class for all scheduled tasks. We extends FutureTask which contains base methods for
   * implementing most of task methods.
   */
  abstract class Task extends FutureTask<Object> implements ScheduledFuture<Object> {
    Task(Runnable r) {
      super(Executors.callable(r, null));
    }
    
    abstract void schedule(long now, long delay, boolean lock);
    
    abstract Runnable getExpirationTask();
    
    abstract void getExpiredTimers(long tickTime, HashMap<Executor, LinkedList<Runnable>> expiredTasks);
    
    abstract long getExpirationTime();
    
    @Override
    protected void done() {
      // Called when the timer has been executed or has been cancelled.
      if (isCancelled()) {
        _meters.wheelTimerCancelled();
      } else {
        _meters.wheelTimerRun();
      }
    }
  }
  
  /**
   * A Delayed task, which is executed at some point in the future.
   */
  class DelayedTask extends Task {
    protected final Executor _taskExecutor;
    protected volatile long _expirationTime;
    protected volatile int _rounds;
    protected volatile int _slot;
    
    DelayedTask(Executor taskExecutor, Runnable task, long expirationTime) {
      super(task);
      _taskExecutor = taskExecutor;
      _expirationTime = expirationTime;
    }
    
    public void schedule(long now, long delay, boolean lock) {
      if (delay < TICK_DELAY) {
        delay = TICK_DELAY;
      }
      long lastRoundDelay = delay % ROTATION_DELAY;
      long lastTickDelay = delay % TICK_DELAY;
      long ticks = lastRoundDelay / TICK_DELAY + (lastTickDelay != 0 ? 1 : 0);
      _rounds = (int) delay / ROTATION_DELAY - ((delay % ROTATION_DELAY) == 0 ? 1 : 0);
      
      if (lock) {
        _rlock.lock();
      }
      try {
        _slot = (int) ((_currentSlot + ticks) & (_wheel.length - 1));
        _wheel[_slot].put(this, this);
      } finally {
        if (lock) {
          _rlock.unlock();
        }
      }
      
      if (isCancelled()) {
        _wheel[_slot].remove(this);
      }
    }
    
    @Override
    public boolean cancel(boolean mayInterrupt) {
      if (super.cancel(mayInterrupt)) {
        _wheel[_slot].remove(this);
        return true;
      }
      return false;
    }
    
    public void getExpiredTimers(long tickTime, HashMap<Executor, LinkedList<Runnable>> expiredTasks) {
      if (_rounds <= 0) {
        try {
          _wheel[_slot].remove(this);
          if (tickTime < getExpirationTime()) {
            // Reschedule if our timer is put in the wrong slot, typically
            // one slot earlier. We'll do this only if we are not cancelled.
            if (!isCancelled()) {
              schedule(tickTime, getExpirationTime() - tickTime, false);
            }
            return;
          }
          if (!isCancelled()) {
            LinkedList<Runnable> runnables = expiredTasks.get(_taskExecutor);
            if (runnables == null) {
              runnables = new LinkedList<Runnable>();
              expiredTasks.put(_taskExecutor, runnables);
            }
            runnables.add(getExpirationTask());
          }
        } catch (Throwable t) {
          _logger.warn("got timeout exception", t);
        }
      } else {
        _rounds--;
      }
    }
    
    @Override
    public long getExpirationTime() {
      return _expirationTime;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
      return getExpirationTime() - System.currentTimeMillis();
    }
    
    @Override
    public int compareTo(Delayed other) {
      long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
      return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
    
    public Runnable getExpirationTask() {
      return this;
    }
  }
  
  /**
   * A Fixed delay periodic task.
   */
  class FixedDelayTask extends DelayedTask {
    protected final long _period;
    
    FixedDelayTask(Executor executor, Runnable task, long initDelay, long delay) {
      super(executor, task, initDelay);
      _period = delay;
    }
    
    @Override
    public Runnable getExpirationTask() {
      return () -> {
        try {
          if (runAndReset()) {
            _expirationTime = getNextExpirationTime();
            schedule(System.currentTimeMillis(), _period, true);
          }
        } catch (Throwable t) {
          _logger.warn("got exception while running periodic task", t);
        }
      };
    }
    
    protected long getNextExpirationTime() {
      return System.currentTimeMillis() + _period;
    }     
  }
  
  /**
   * A Fixed rate periodic task.
   */
  class FixedRateTask extends DelayedTask {
    private volatile long _firstExecutionTime;
    private volatile long _executionsNumber = 0;
    protected final long _period;

    FixedRateTask(Executor executor, Runnable task, long initDelay, long delay) {
      super(executor, task, initDelay);
      _period = delay;
    }
    
    @Override
    public Runnable getExpirationTask() {
      return () -> {
        try {
          if (_firstExecutionTime == 0) {
            _firstExecutionTime = System.currentTimeMillis();
          }
          _executionsNumber++;
          if (runAndReset()) {
            _expirationTime = getNextExpirationTime();
            schedule(System.currentTimeMillis(), _period, true);
          }
        } catch (Throwable t) {
          _logger.warn("got exception while running periodic task", t);
        }
      };
    }
    
    protected long getNextExpirationTime() {
      long period = _firstExecutionTime + (_period * _executionsNumber);
      return Math.max(0, period);
    }
  }
}
