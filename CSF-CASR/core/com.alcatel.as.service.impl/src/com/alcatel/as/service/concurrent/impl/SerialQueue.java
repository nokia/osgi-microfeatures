package com.alcatel.as.service.concurrent.impl;

// Utils
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.StopWatch;

/**
 * An actor style task dispatcher. This executor executes tasks
 * serially in FIFO order, like a scala "Actor". A queue will only invoke one runnable at a time,
 * and independent queues may each be running concurrently in the given thread pool.
 */
public class SerialQueue implements Executor, Runnable {
  /** Our Logger */
  private final static Logger _logger = Logger.getLogger("as.service.concurrent.SerialQueue");
  
  /** The executor used to execute actor tasks. */
  private final Executor _tpool;
  
  /** Flag telling if a thread pool worker is currently executing our queue. */
  private final AtomicBoolean _running = new AtomicBoolean();
  
  /** List of tasks scheduled in our queue. */
  protected final ConcurrentLinkedQueue<Runnable>[] _tasks = new ConcurrentLinkedQueue[TaskPriority.values().length];
  
  /** Label of this serial queue. */
  private final String _id;
  
  /** Metrics */
  private final Meters _meters;
  
  /**
   * Creates a new Actor dispatcher. 
   * @param tpool the executor used to schedule the execution of this queue.
   * @param id the queue label (null for anonymous queue).
   */
  public SerialQueue(Executor tpool, String id, Meters meters) {
    _tpool = tpool;
    _id = id;
    _meters = meters;
    for (int i = 0; i < TaskPriority.values().length; i++) {
      _tasks[i] = new ConcurrentLinkedQueue<Runnable>();
    }
  }
  
  /**
   * Runs a task on a specific queue, using the default priority.
   * 
   * @param queue an actor queue identifier. If the queue is a String, then 
   * the String.hashCode method can be used as the identifier.
   * @param task the actor task.
   */
  public void execute(Runnable task) {
    execute(task, TaskPriority.DEFAULT);
  }
  
  /**
   * Runs a task on a specific queue.
   * 
   * @param queue an actor queue identifier. If the queue is a String, then 
   *        the String.hashCode method can be used as the identifier.
   * @param task the actor task.
   */
  public void execute(Runnable task, TaskPriority p) {
    _meters.queueScheduled();
    _tasks[p.ordinal()].add(task);
    scheduleExecution();
  }
  
  /**
   * Consume all tasks scheduled on this queue.
   * IMPORTANT NOTE: this method is synchronized in order to ensure a 
   * per-thread "consistent working memory visibility": Indeed, when a worker thread terminates 
   * to consume the runnables scheduled in this queue, then the next worker thread which will execute this 
   * queue must enter into the same synchronization monitor in order to have a consistent visibility
   * over all java object fields possibly modified by the previous worker thread. 
   */
  @Override
  public void run() {
    try {
      if (_id != null && _logger.isInfoEnabled()) {
        NDC.push(_id); // Display the queue name in log4j "Nested Diagnostic Context".
      }
      
      // We do a memory barrier in order to ensure consistent per-thread memory visibility
      Runnable task;
      while ((task = dequeue()) != null) {
        try {
          task.run();
        } catch (Throwable t) {
          _logger.warn("reactor task execution exception", t);
        } finally {
          _meters.queueRun();
        }
      }
    } finally {
      _running.set(false);
      if (!isEmpty()) {
        scheduleExecution();
      }
      if (_id != null && _logger.isInfoEnabled()) {
        NDC.remove();
      }
    }
  }
  
  private Runnable dequeue() {
    Runnable x = _tasks[TaskPriority.HIGH.ordinal()].poll();
    if (x == null) {
      x = _tasks[TaskPriority.DEFAULT.ordinal()].poll();
    }
    return x;
  }
  
  private boolean isEmpty() {
    Runnable x = _tasks[TaskPriority.HIGH.ordinal()].peek();
    if (x == null) {
      x = _tasks[TaskPriority.DEFAULT.ordinal()].peek();
    }
    return x == null;
  }
  
  private void scheduleExecution() {
    if (_running.compareAndSet(false, true)) {
      _tpool.execute(this);
    }
  }
}
