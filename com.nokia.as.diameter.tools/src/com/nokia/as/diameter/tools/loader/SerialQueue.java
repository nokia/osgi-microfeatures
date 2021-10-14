package com.nokia.as.diameter.tools.loader;

// Utils
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

/**
 * An actor style task dispatcher. This executor executes tasks
 * serially in FIFO order, like a scala "Actor". A queue will only invoke one runnable at a time,
 * and independent queues may each be running concurrently in the given thread pool.
 */
public class SerialQueue implements Executor, Runnable {
  /**
   * Our Logger
   */
  private final static Logger _logger = Logger.getLogger(SerialQueue.class);
  
  /**
   * The executor used to execute actor tasks.
   */
  private final Executor _executor;
  
  /**
   * Flag telling if a thread pool worker is currently executing our queue.
   */
  private final AtomicBoolean _running = new AtomicBoolean();
  
  /**
   * List of tasks scheduled in our queue.
   */
  protected final ConcurrentLinkedQueue<Runnable> _tasks = new ConcurrentLinkedQueue<Runnable>();
  
  /**
   * Creates a new Actor dispatcher. 
   * @param executor the executor used to schedule the execution of this queue.
   */
  public SerialQueue(Executor executor) {
    _executor = executor;
  }
  
  /**
   * Runs a task on a specific queue.
   * 
   * @param queue an actor queue identifier. If the queue is a String, then 
   * the String.hashCode method can be used as the identifier.
   * @param task the actor task.
   */
  public void execute(Runnable task) {
    _tasks.add(task);
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
      // We synchronized in order to ensure consistent per-thread memory visibility 
      synchronized (this) {
        Runnable task;
        while ((task = _tasks.poll()) != null) {
          try {
            task.run();
          } catch (Throwable t) {
            _logger.warn("reactor task execution exception", t);
          }
        }
      }
    } finally {
      _running.set(false);
      if (!_tasks.isEmpty()) {
        scheduleExecution();
      }
    }
  }
  
  private void scheduleExecution() {
    if (_running.compareAndSet(false, true)) {
      _executor.execute(this);
    }
  }
}
