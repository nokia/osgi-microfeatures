// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.log4j.Logger;

/**
 * This class implements a queue per worker ThreadPool with Work Stealing.
 * 
 * Scheduling policy: Tasks scheduled by external threads are randomly dispatched on 
 * all currently active queues. Tasks forked from inside the pool are always pushed on 
 * the current worker thread queue and another idle worker will potentially "steal"
 * jobs from the forking worker queue. When one worker becomes idle, it steals tasks from
 * other queues before going to idle state.
 * 
 * The number of active queues is dynamic: the "resizeQueues" method is periodically
 * called and adapts the number of active queues, depending on the work load. Indeed,
 * when the load is high and when tasks are short-lived, it is crucial that not too much 
 * queues are active because if so, then we would spend too much time in waking up idle 
 * workers. When one worker is added, it immediately stills other tasks from previous active
 * workers, in order to participate in task processing.
 */
@SuppressWarnings("unused")
public class StealingThreadPool implements ThreadPoolBase {
  /** 
   * Our Logger 
   **/
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.StealingThreadPool.class");
  
  /** 
   * Our actual workers 
   **/
  private volatile Worker[] _workers = new Worker[0];
  
  /**
   * Sequencer used to generate worker thread names. 
   **/
  private final AtomicInteger _sequencer = new AtomicInteger(1);
  
  /** 
   * Current number of active queues, where scheduled tasks can be dispatched.
   * We start with only one active queue. If the load increase, then more workers will be activated.
   **/
  private final AtomicInteger _activeWorkers = new AtomicInteger(1);
  
  /** 
   * Our thread pool name 
   **/
  private final String _name;
  
  /** 
   * Current number of awaiting idle workers 
   **/
  private final AtomicInteger _idleWorkers = new AtomicInteger();
  
  /** 
   * Lock used by worker when resizing number of active workers 
   **/
  private final AtomicBoolean _resizeLock = new AtomicBoolean();
  
  /**
   * Metrics.
   */
  private final Meters _meters;
      
  /**
   * Scheduled used to schedule next worker for a given task to execute.
   */
  private final ThreadLocal<Scheduler> _scheduler = new ThreadLocal<Scheduler>() {
    protected Scheduler initialValue() {
      return new Scheduler();
    }
  };
  
  /**
   * Makes a new thread pool
   * @param name the symbolic name for this new threadpool
   * @param size the number of worker threads
   * @param _strictTimerService 
   * @param ms the metering service used for monitoring
   */
  StealingThreadPool(String name, int size, Meters meters) {
    _name = name;
    setSize(size);
    _meters = meters;
  }
  
  /**
   * Randomly schedules a task in one of the worker queues.
   * @param x the task to be scheduled
   */
  @Override
  public void execute(Runnable x) {
    execute(x, TaskPriority.DEFAULT);
  }
  
  /**
   * Randomly schedules a priority task in one of the worker queues.
   * @param task the task to be scheduled
   * @param pri the priority to be used
   */
  @Override
  public void execute(Runnable task, TaskPriority pri) {
    Thread thread;
        
    _meters.tpoolProcessingScheduled();
    if ((thread = Thread.currentThread()) instanceof Worker && ((Worker) thread).getPool() == this) {
      ((Worker) thread).executeLocal(task, pri);
    } else {
      _scheduler.get().nextWorker().execute(task, pri);
    }
  }
  
  /**
   * Check if a given thread is one of the worker threads managed by this pool.
   * @return true if the given thread is  one of the worker threads managed by this pool, false if not.
   */
  @Override
  public boolean isPooled(Thread t) {
    return t instanceof Worker && ((Worker) t).getPool() == this;
  }
  
  /**
   * Set size of the thread pool. For now, we can only expand the tpool size (we can't shrink the size).
   */
  @Override
  public synchronized void setSize(int size) {
    if (_workers.length == size) {
      _logger.info("threadpool " + _name + " size left unchanged: " + size);
      return;
    }
    
    if (size < _workers.length) {
      // We don't support downsizing. Reducing the size of a thread pool would require synchronizing
      // the execute() method ... let's keep this class simple and effective.
      _logger.warn("cannot shrink thread pool " + _name + " to " + size + " (unsupported operation)");
      return;
    }
        
    Worker[] workers = new Worker[size];
    System.arraycopy(_workers, 0, workers, 0, _workers.length);
    for (int i = _workers.length; i < size; i++) {
      workers[i] = new Worker(i);
    }
    int previousSize = _workers.length;
    _workers = workers;
    for (int i = previousSize; i < size; i++) {
      _workers[i].start();
    }
    
    // By default, activeWorkers =
    _logger.info("Configured thread pool " + _name + " with size: " + size);
  }
  
  /**
   * Returns the number of workers managed by this pool.
   */
  @Override
  public int getSize() {
    return _workers.length;
  }
  
  @Override
  public void setKeepAlive(long idleTimeoutSec) {
	  // not supported		
  }
  
  @Override
  public void shutdown() {
    // TODO
  }
  
  @Override
  public List<Runnable> shutdownNow() {
    // TODO
    return Collections.emptyList();
  }
  
  @Override
  public boolean isShutdown() {
    return false; // TODO
  }
  
  @Override
  public boolean isTerminated() {
    return false; // TODO
  }
  
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    shutdown();
    return true;
  }
  
  /**
   * Display our executor state.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(_name);
    return sb.toString();
  }
  
  /**
   * A Worker class, in charge of consuming tasks using a private queue.
   */
  private class Worker extends Thread {
    /**
     * The queue holding scheduled priority tasks.
     */
    final PriorityQueue _queue;
    // Heuristic padding to ameliorate unfortunate memory placements.
    long _queueP1,_queueP2,_queueP3,_queueP4,_queueP5,_queueP6; int _queueP7; 

    /**
     * Our index in the array of workers
     */
    final int _workerIndex;
    // Heuristic padding to ameliorate unfortunate memory placements.
    long _workerIndexP1,_workerIndexP2,_workerIndexP3,_workerIndexP4,_workerIndexP5,_workerIndexP6;int _workerIndexP7; 

    /** 
     * Counter used to reduce the number of park/unpark calls.
     */
    final AtomicInteger _unparkCounter = new AtomicInteger();
    // Heuristic padding to ameliorate unfortunate memory placements.
    long _unparkCounterP1,_unparkCounterP2,_unparkCounterP3,_unparkCounterP4,_unparkCounterP5,_unparkCounterP6;int _unparkCounterP7; 

    /** 
     * Counter used to reduce the number of park/unpark calls.
     */
    volatile int _parkCounter;
    long _parkCounterP1,_parkCounterP2,_parkCounterP3,_parkCounterP4,_parkCounterP5,_parkCounterP6;int _parkCounterP7; 

    /** 
     * Flag to indicate that a worker id parked (waiting for tasks).
     */
    volatile boolean _waiting;
    long _waitingP1,_waitingP2,_waitingP3,_waitingP4,_waitingP5,_waitingP6;int _waitingP7; boolean _waitingP8;

    /** 
     * Period used to check if number of active workers must be resized. Must be a power of two.
     **/
    final int _resizePeriod = Integer.getInteger("system.processing-tpool.asr.resizePeriod", 32); // must be a power of two
    long _resizePeriodP1,_resizePeriodP2,_resizePeriodP3,_resizePeriodP4,_resizePeriodP5,_resizePeriodP6;int _resizePeriodP7; 

    /** 
     * Count incremented each time a tasks is executed. When resizeCount % resizePeriod == 0, the worker try to resize
     * the number of active workers.
     */
    int _resizeCount = 0;
    long _resizeCountP1,_resizeCountP2,_resizeCountP3,_resizeCountP4,_resizeCountP5,_resizeCountP6;int _resizeCountP7; 

    /**
     * Constructor.
     * @param workerIndex the index of this worker in the global worker index array.
     */
    public Worker(int workerIndex) {
      super.setName(generateWorkerThreadName());
      super.setDaemon(true);
      _workerIndex = workerIndex;
      _queue = new PriorityQueue();
    }
    
    /**
     * Returns the index of this worker in the global worker index array.
     * @return the index of this worker in the global worker index array.
     */
    int getIndex() {
      return _workerIndex;
    }
    
    /**
     * Schedules a runnable in our worker queue.
     */
    public void execute(Runnable task, TaskPriority pri) {
      _queue.addLast(task, pri);
      wakeup();
    }
    
    /**
     * Handles a task scheduled from within our worker thread (typical "fork/join" use case).
     * We use the following policy: we push the task in our own queue (lifo), and we expect
     * that resizeQueues() method will expand active queues, which will then
     * potentially take jobs from our own queue. If tasks are short-lived, it is likely that
     * our own thread will consume most of local tasks. Else more workers will help us by
     * stealing our jobs.
     */
    public void executeLocal(Runnable task, TaskPriority pri) {
      _queue.addFirst(task, pri);
      resizeQueues();
    }
    
    /**
     * Gets the pool managing this worker thread
     * @return the pool managing this worker thread
     */
    public StealingThreadPool getPool() {
      return StealingThreadPool.this;
    }
    
    /**
     * Executes tasks scheduled in our private worker queue. When no jobs are available, try to steal other
     * workers, and wait if no jobs are available from any other queues.
     */
    @Override
    public void run() {
      Runnable task;
      PriorityQueue queue = _queue;
      int hint = -1;
      
      try {
        while (true) {
          // We poll the head of our queue (other stealing thread will poll from last side).
          if ((task = queue.pollFirst()) != null) {
            runTask(task);
          } else if ((hint = scan(hint)) == -1) {
            // No job to steal from other queues: we can now pause.
            await();
          }
        }
      } catch (Throwable t) {
        _logger.warn("Exception caught from a task scheduled in thread pool " + _name, t);
      }
      
      _logger.info("worker thread stopped for thread pool " + getName());
    }
    
    /**
     * Executes a task.
     * @param task
     */
    private void runTask(Runnable task) {
      try {
        task.run();
      } catch (Throwable t) {
        _logger.warn("task execution exception", t);
      } finally {
        Thread.interrupted(); // clear possible interrupt flag, if task got interrupt
        resizeQueues();
        _meters.tpoolProcessingRun();
      }
    }
    
    /**
     * Wakeup this worker, if it is dormant.
     */
    private void wakeup() {
      // The following trick allows to dramatically reduce park/unpark calls.
      if (_unparkCounter.getAndIncrement() == _parkCounter && _waiting) {
        // Give one permit to next park() call, or wakeup our "await" method.
        LockSupport.unpark(this);
      }
    }
    
    /**
     * Wait for some new submitted messages.
     */
    private void await() throws InterruptedException {
      _waiting = true;
      try {
        // The following trick allows to dramatically reduce Lock pack/unpack calls.
        if (_parkCounter == _unparkCounter.get()) {
          // The following will block until our submit method calls "unpark".
          // But it won't block if the submit method has already called "unpark". 
          _idleWorkers.incrementAndGet();
          LockSupport.park();
          _idleWorkers.decrementAndGet();
        }
      } finally {
        _waiting = false;
        _parkCounter = _unparkCounter.get();
      }
    }
    
    /**
     * Look for tasks to take from other workers.
     * @param hint a worker index hint where to start scanning. -1 if scan must start from
     * a random position. 
     */
    private int scan(int hint) {
      Worker[] workers = _workers;
      int myIndex = _workerIndex;
      Runnable work;
      Worker worker;
      
      if (hint == -1) {
        hint = ThreadLocalRandom.current().nextInt(_activeWorkers.get());
      }
      
      for (int i = 0; i < workers.length; i++) {
        worker = workers[hint];
        
        if (++hint >= workers.length) {
          hint = 0;
        }
        
        if (worker.getIndex() != myIndex && (work = worker.steal()) != null) {
          runTask(work);
          return worker.getIndex();
        }
      }
      
      return -1;
    }
    
    private Runnable steal() {
      return _queue.pollLast(); // Don't content with the worker thread, and poll from last side.
    }
    
    /**
     * Shrinks or expands number of active workers. Method called regularly to adapt the number of active 
     * queues depending on the work load.
     * If load is high with many very short-lived tasks, and if we have too much workers, then we reduce the number
     * of active workers in order to avoid potential excessive wakeups (time consuming).
     * Otherwise, if all current active workers are all busy (not idle), then we expand the number of active workers by 1.
     * <WARNING> this method must be called from worker threads.
     */
    private void resizeQueues() {
      if ((_resizeCount++ & (_resizePeriod - 1)) == 0 && _resizeLock.compareAndSet(false, true)) {
        try {
          Worker[] workers = _workers;
          int activeWorkers = _activeWorkers.get();
          int idleWorkers = _idleWorkers.get();
          int inactiveWorkers = workers.length - activeWorkers;
          
          if (idleWorkers > inactiveWorkers && activeWorkers > 1) {
            // There is more idle workers than current number of inactive workers: it means that some active workers 
            // seem to be idle, and we can then shrink the number of active workers.
            _activeWorkers.decrementAndGet();
          } else if (activeWorkers < workers.length) {
            // All active workers seems busy, expand by one active worker count.
            workers[_activeWorkers.incrementAndGet() - 1].wakeup(); // will potentially steal other active queues.
          }
        } finally {
          _resizeLock.set(false);
        }
      }
    }
    
    /**
     * Generates a thread pool name
     */
    private String generateWorkerThreadName() {
      int id = _sequencer.getAndIncrement();
      StringBuilder buf = new StringBuilder();
      buf.append(_name);
      buf.append("-");
      buf.append(id);
      return (buf.toString());
    }
  }
  
  /**
   * Class used to generate next worker to schedule.
   */
  private class Scheduler {
    int _nextWorker = 0;
    
    Worker nextWorker() {
      Worker[] workers = _workers;
      int nextWorker = _nextWorker ++;
      if (nextWorker >= _activeWorkers.get()) {
        return workers[_nextWorker = 0];
      }      
      return workers[nextWorker];
    }
  }
}
