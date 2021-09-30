package com.alcatel.as.service.concurrent;

/**
 * This class defines some policy used to control how a task must be executed by an executor.
 * <p>
 * Here is a typical sample code:
 * <p>
 * <hr>
 * <blockquote>
 * 
 * <pre>
 * 
 *   PlatformExecutors executors = PlatformExecutors.getInstance();
 *   Executor tpool = executors.getExecutor(PlatformExecutors.THREAD_POOL_EXECUTOR);
 *   // The task will be run in OUR thread, if the current thread is already part of the thread pool.
 *   tpool.execute(new Runnable() { ... }, ExecutorPolicy.INLINE);
 * </pre>
 * 
 * </blockquote>
 * <hr>
 */
public enum ExecutorPolicy {
  /**
   * The task must be scheduled on one executor, using the DEFAULT priority.
   * DEFAULT priority tasks are executed after all HIGH priority tasks
   */
  SCHEDULE,
  
  /**
   * The task must be scheduled in one executor, using the HIGH priority.
   * HIGH priority tasks are executed before any DEFAULT priority tasks.
   */
  SCHEDULE_HIGH,
  
  /**
   * The task must be executed in the calling thread, only if that calling thread is managed by
   * the invoked executor. Else the task is scheduled in the executor, using the DEFAULT priority.
   * DEFAULT priority tasks are executed after all HIGH priority tasks.
   */
  INLINE,
  
  /**
   * The task must be executed in the calling thread, only if that calling thread is managed by
   * the invoked executor. Else the task is scheduled in the executor, using the HIGH priority.
   * HIGH priority tasks are executed before any DEFAULT priority tasks.
   */
  INLINE_HIGH,
}
