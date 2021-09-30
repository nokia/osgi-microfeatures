package com.alcatel.as.service.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Timer Service used to schedule timer tasks in platform executors.
 * This interface extends ScheduledExecutorService and provides additional 
 * methods in order to allow to schedule a task within an explicit Executor. 
 * By default, ScheculedExecutorService will execute expired tasks using the executor
 * of the current thread which scheduled the task.
 * 
 * <p> Two kinds of TimerService may be used: an exact timer service (precision=millis), 
 * and inaccurate timer service (precision=50/100 millis, for support of high performance
 * timers). To use exact timers, you can use the OSGi service property filter "(strict=true)",
 * when defining an OSGi dependency over the timer service. Likewise, you can use the
 * OSGi filter "(strict=false") if you need to use the inaccurate (but faster) timer service.
 * 
 * <p>Important note: When the ScheduledExecutorService methods are invoked, 
 * the timers will be executed using the current thread executor (at the time the timer
 * was scheduled). However, it may be possible that the PlatformExecutors service is not 
 * currently available when scheduling timers; in this case the expired tasks will be 
 * invoked from the internal TimerService scheduler thread.
 * 
 * <p>
 * Usage example:
 * <p>
 * 
 * <pre>
 * class Test implements Runnable {
 *   private PlatformExecutor sipExecutor;
 *   private TimerService timerService;
 *   
 *   &#64;Reference(target="(id=sip)")
 *   void bindSipExecutor(PlatformExecutor sip) {
 *       this.sipExecutor = sip;
 *   }
 *   
 *   &#64;Reference(target="(strict=false)")
 *   void bindTimerService(TimerService timerService) {
 *       this.timerService = timerService;
 *   }
 *   
 *   &#64;Activate
 *   void start() {
 *       timerService.schedule(this.sipExecutor, this, 1000, TimeUnit.MILLISECONDS);
 *   }
 * 
 *   public void run() {
 *      // timeout executed within the sip executor.
 *   }
 * } 
 * </pre>
 * 
 **/
public interface TimerService extends ScheduledExecutorService {
  /**
   * OSGi service property for a timer service.
   * A TimerService whose STRICT OSGi service property is set to true ensures that
   * timers are scheduled in time. Other imprecise TimerService implementation should
   * be registered in the OSGi registry with STRICT=false. Imprecise TimerService 
   * generally rounds timer in 50/100 millis, but provides better performances,
   * especially when scheduling thousands/millions of timers per seconds.
   */
  public final static String STRICT = "strict";
  
  /**
   * Schedules a task in the future
   * @param taskExecutor the executor used to trigger the task execution when the timer expires.
   * @param task the task to be executed.
   * @param delay the delay
   * @param unit the delay unit
   * @return a Future object that can be used to cancel the scheduled timer.
   */
  ScheduledFuture<?> schedule(Executor taskExecutor, Runnable task, long delay, TimeUnit unit);
  
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
   *
   * @param taskExecutor the executor used to trigger the task execution when the timer expires.
   * @param task the task to execute
   * @param initDelay the time to delay first execution
   * @param delay the period between successive executions
   * @param unit the time unit of the initialDelay and period parameters
   * @return a Future that can be used to cancel the scheduled task.
   */
  ScheduledFuture<?> scheduleWithFixedDelay(Executor taskExecutor, Runnable task, long initDelay, long delay,
                                            TimeUnit unit);
  
  /**
   * Creates and executes a periodic action that becomes enabled first
   * after the given initial delay, and subsequently with the
   * given delay between the termination of one execution and the
   * commencement of the next.  If any execution of the task
   * encounters an exception, subsequent executions are suppressed.
   * Otherwise, the task will only terminate via cancellation or
   * termination of the executor.
   *
   * @param taskExecutor the executor used to trigger the task execution when the timer expires.
   * @param task the task to execute
   * @param initDelay the time to delay first execution
   * @param delay the delay between the termination of one
   * execution and the commencement of the next
   * @param unit the time unit of the initialDelay and delay parameters
   * @return a Future that can be used to cancel the scheduled task.
   */
  ScheduledFuture<?> scheduleAtFixedRate(Executor taskExecutor, Runnable task, long initDelay, long delay,
                                         TimeUnit unit);
}
