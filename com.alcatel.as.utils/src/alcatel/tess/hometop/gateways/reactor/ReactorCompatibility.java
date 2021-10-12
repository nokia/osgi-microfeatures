// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import alcatel.tess.hometop.gateways.reactor.util.SynchronousTimerTask;

import com.alcatel.as.service.concurrent.TimerService;

/**
 * This interface regroups deprecated methods concerning the @link {@link Reactor} interface.
 * @deprecated only use methods from @link {@link Reactor} interface.
 */
@Deprecated
public interface ReactorCompatibility {
  /**
   * Close the reactor, and its corresponding nio selector.
   * @deprecated use {@link Reactor#stop} method
   */
  @Deprecated
  void close();
  
  /**
   * Schedule a future task inside the reactor thread.
   * @param task the task to be scheduled
   * @param delay the delay before scheduling the task
   * @deprecated use {@link #schedule(Runnable, long, TimeUnit)}
   */
  @Deprecated
  void schedule(SynchronousTimerTask task, long delay);
  
  /**
   * Schedule a future periodic task inside the reactor thread.
   * @param task the task to be scheduled
   * @param delay the delay before scheduling the task
   * @param period the period used to reschedule
   * @deprecated use {@link #scheduleAtFixedRate(Runnable, long, long, TimeUnit)} or
   *                 {@link #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
   */
  @Deprecated
  void schedule(SynchronousTimerTask task, long delay, long period);
  
  /**
   * Cancel a future task.
   * @param task the task to cancel
   * @return true if the task has not yet run, false if not.
   * @deprecated use Future returned by other schedule methods.
   */
  @Deprecated
  boolean cancel(SynchronousTimerTask task);
  
  /**
   * Creates and executes a one-shot action that becomes enabled
   * after the given delay.
   * @param task the task to be scheduled in this reactor
   * @param command the task to execute
   * @param delay the time from now to delay execution
   * @param unit the time unit of the delay parameter
   * @return a ScheduledFuture object that can be used to cancel the scheduled timer.
   * 
   * @deprecated use @link {@link TimerService} service in order to schedule timers in a given
   * reactor.
   */
  @Deprecated
  ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit);
  
  /**
   * Creates and executes a periodic action that becomes enabled first
   * after the given initial delay, and subsequently with the given
   * period; that is executions will commence after
   * <tt>initialDelay</tt> then <tt>initialDelay+period</tt>, then
   * <tt>initialDelay + 2 * period</tt>, and so on.
   * If any execution of the task
   * encounters an exception, subsequent executions are suppressed.
   * Otherwise, the task will only terminate via cancellation or
   * termination of the executor.  If any execution of this task
   * takes longer than its period, then subsequent executions
   * may start late, but will not concurrently execute.
   * @param task the task to be scheduled in this reactor
   * @param initDelay the first time the task will be scheduled
   * @param command the task to execute
   * @param initialDelay the time to delay first execution
   * @param period the period between successive executions
   * @param unit the time unit of the initialDelay and period parameters
   * @return a Future object that can be used to cancel the scheduled timer.
   * 
   * @deprecated use @link {@link TimerService} service in order to schedule timers in a given
   * reactor.
   */
  @Deprecated
  ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initDelay, long period, TimeUnit unit);
  
  /**
   * Creates and executes a periodic action that becomes enabled first
   * after the given initial delay, and subsequently with the
   * given delay between the termination of one execution and the
   * commencement of the next.  If any execution of the task
   * encounters an exception, subsequent executions are suppressed.
   * Otherwise, the task will only terminate via cancellation or
   * termination of the executor.
   *
   * @param command the task to execute
   * @param initDelay the time to delay first execution
   * @param delay the delay between the termination of one
   * execution and the commencement of the next
   * @param unit the time unit of the initialDelay and delay parameters
   * @return a Future object that can be used to cancel the scheduled timer.
   * 
   * @deprecated use @link {@link TimerService} service in order to schedule timers in a given
   * reactor.
   */
  @Deprecated
  ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, long initDelay, long delay, TimeUnit unit);
  
  /**
   * Creates and executes a Future that becomes enabled after the
   * given delay.
   *
   * @param callable the function to execute
   * @param delay the time from now to delay execution
   * @param unit the time unit of the delay parameter
   * @return a Future object that can be used to cancel the scheduled timer.   
   * 
   * @deprecated use @link {@link TimerService} service in order to schedule timers in a given
   * reactor.
   */
  @Deprecated
  <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
}
