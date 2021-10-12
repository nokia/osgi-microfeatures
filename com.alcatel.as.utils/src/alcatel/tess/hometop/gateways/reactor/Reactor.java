// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

// Jdk
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.TimerService;

/**
 * This class is the central point where <b>ALL</b> sockets IO events take place.
 * The Reactor is a Runnable object that blocks on a nio select.
 * Once io events are detected, the reactor will dispatch events
 * to concerned handler accordingly.
 * More over, some tasks may be scheduled inside the reactor thread
 * (including timers).
 * 
 * Notice that Timers can be scheduled in a reactor using the {@link TimerService} 
 * API.
 */
public interface Reactor extends ReactorCompatibility, Runnable, Executor {
  
  /**
   * Reactor Task priority.
   */
  enum TaskPriority {
    /** Tasks scheduled with DEFAULT priority are executed after all HIGH priority tasks are run */
    DEFAULT,
    
    /** Tasks scheduled with HIGH priority are executed before any pending DEFAULT priority tasks */
    HIGH
  }
  
  /**
   * Starts a dedicated thread for that reactor. The reactor name will be used for the new
   * thread.
   */
  void start();
  
  /**
   * Closes the reactor, and its corresponding nio selector. This method returns only when all
   * sockets are closed.
   */
  void stop();
  
  /**
   * Disconnect all channels which have been opened using this reactor instance. 
   * @param abort true if the channels must be closed even if not all data have 
   * been sent out.
   */
  void disconnect(boolean abort);
  
  /**
   * Return the reactor's thread.
   * @return the Reactor which is executing the current thread, or null if the current thread 
   * is not managed by any reactors.
   */
  Thread getReactorThread();
  
  /**
   * Returns the PlatformExecutor associated to this reactor companion thread.
   */
  PlatformExecutor getPlatformExecutor();
  
  /**
   * Returns the Tracer
   * @return the Tracer
   */
  Logger getLogger();
  
  /**
   * Set the reactor name.
   * @param name the reactor name
   */
  public void setName(String name);
  
  /**
   * Get the reactor name.
   * @return the reactor name
   */
  public String getName();
  
  /**
   * Schedule a task inside the reactor thread.
   * The task will be handled by the reactor just before
   * this once enters into its select system call.
   * @param task the task to be scheduled in this reactor
   */
  void schedule(Runnable task);
  
  /**
   * Schedule a task inside the reactor thread.
   * The task will be handled by the reactor just before
   * this once enters into its select system call.
   * @param task the task to be scheduled in this reactor
   * @param pri the task priority.
   */
  void schedule(Runnable task, TaskPriority pri);

  /**
   * Schedule a task inside the reactor thread. If the caller is
   * running inside this reactor thread, then the task is called
   * right now. If not, the task is scheduled, as with the
   * {@link #schedule(Runnable)} method.
   * @param task the task to be scheduled in this reactor
   */
  void scheduleNow(Runnable task);
  
  /**
   * Schedule a task inside the reactor thread. If the caller is
   * running inside this reactor thread, then the task is called
   * right now. If not, the task is scheduled, as with the
   * {@link #schedule(Runnable)} method.
   * @param task the task to be scheduled in this reactor
   * @param pri the task priority.
   */
  void scheduleNow(Runnable task, TaskPriority pri);

  /**
   * Submits a value-returning task for execution and returns a
   * Future representing the pending results of the task. The
   * Future's <tt>get</tt> method will return the task's result upon
   * successful completion.
   * @param task the task to submit
   * @return a Future representing pending completion of the task
   */
  <T> Future<T> schedule(Callable<T> task);
  
  /**
   * Submits a value-returning task for execution and returns a
   * Future representing the pending results of the task. The
   * Future's <tt>get</tt> method will return the task's result upon
   * successful completion.
   * @param task the task to submit
   * @param pri the task priority.
   * @return a Future representing pending completion of the task
   */
  <T> Future<T> schedule(Callable<T> task, TaskPriority pri);
}
