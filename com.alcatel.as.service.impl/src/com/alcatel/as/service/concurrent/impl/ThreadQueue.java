package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.log4j.Logger;

/**
 * A Single Thread Queue.
 */
public class ThreadQueue implements Runnable {
  // Our logger
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.ThreadQueue");
  
  // The queue holding scheduled priority tasks.
  private final PriorityQueue _queue = new PriorityQueue();
  
  // Counter used to reduce the number of park/unpark system calls
  private final AtomicInteger _scheduleCounter = new AtomicInteger();
  
  // Counter used to reduce the number of park/unpark system calls
  private volatile int _awaitCounter;
  
  // Flag used to reduce the number of park/unpark system calls
  private volatile boolean _waiting;

  // ThreadQueue name
  private final String _name;

  // Optional factory used to create our actual queue thread
  private final ThreadFactory _factory;
  
  // Our queue thread
  private volatile Thread _thread;
  
  private class Shutdown implements Runnable {
    private final CountDownLatch _latch;

    Shutdown(CountDownLatch latch) {
      _latch = latch;
    }
    
    @Override
    public void run() {
      _latch.countDown();
    }    
  }
  
  /**
   * Constructor
   * @param name the thread executor name.
   */
  public ThreadQueue(String name, ThreadFactory factory) {
    _name = name;
    _factory = factory;
  }
  
  public void start() {
    if (_factory != null) {
      _thread = _factory.newThread(this);
    } else {
      _thread = new Thread(this, _name);
      _thread.setDaemon(true);
    }
    _thread.start();
  }
  
  public Thread getThreadQueue() {
    return _thread;
  }
  
  public boolean shutdown(long millis) {
    CountDownLatch latch = new CountDownLatch(1);
    Shutdown shutdown = new Shutdown(latch);
    execute(shutdown, TaskPriority.DEFAULT);
    try {
      if (Thread.currentThread() != _thread) {
        return latch.await(millis, TimeUnit.MILLISECONDS);
      } else {
        return true;
      }
    } catch (InterruptedException e) {
      return false;
    } finally {
      _logger.info("threadqueue stopped: " + _name);
    }
  }
  
  /**
   * Schedules a runnable in our worker queue.
   */
  public void execute(Runnable task, TaskPriority pri) {
    _queue.addLast(task, pri);
    scheduleExecution();
  }
  
  @Override
  public void run() {
    Runnable task;
    PriorityQueue queue = _queue;
    
    try {
      while (true) {
        if ((task = queue.pollFirst()) == null) {
          await();
          continue;
        }
        
        if (! runTask(task)) {
          break;
        }
      }
    } catch (Throwable t) {
      if (!_thread.isInterrupted()) {
        _logger.warn("Exception caught from single thread pool " + _name, t);
      }
    } finally {
      _logger.info("single thread queue stopped");
    }
  }
  
  /**
   * Executes the task.
   * @param task the task to execute
   * @return false if the thread is shutdown, else true
   */
  private boolean runTask(Runnable task) {
    try {
      task.run();
    } catch (Throwable t) {
      _logger.warn("task execution exception", t);
    } finally {
      Thread.interrupted(); // clear possible interrupt flag, if task got interrupt
    }
    return task instanceof Shutdown ? false : true;
  }
  
  private void scheduleExecution() {
    // The following trick allows to dramatically reduce park/unpark calls.
    if (_scheduleCounter.getAndIncrement() == _awaitCounter && _waiting) {
      // Give one permit to next park() call, or wakeup our "await" method.
      LockSupport.unpark(_thread);
    }
  }
  
  /**
   * Wait for some new submitted messages.
   */
  private void await() throws InterruptedException {
    _waiting = true;
    try {
      // The following trick allows to dramatically reduce Lock pack/unpack calls.
      if (_awaitCounter == _scheduleCounter.get()) {
        // The following will block until our submit method calls "unpark".
        // But it won't block if the submit method has already called "unpark".          
        LockSupport.park();
      }
    } finally {
      _waiting = false;
      _awaitCounter = _scheduleCounter.get();
    }
  }
}
