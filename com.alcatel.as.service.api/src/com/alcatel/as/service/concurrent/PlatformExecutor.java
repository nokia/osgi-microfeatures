// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * A platform executor (a thread pool, or a threaded queue executor, ...). Platform Executors are
 * used to schedule tasks in one of the threads running within the callout agent Jvm.<p>
 * 
 * Tasks may have two level of priorities: DEFAULT and HIGH priorities.<p>
 * 
 * <ul><li> DEFAULT priority: tasks are executed after all HIGH priority tasks.
 * <li> HIGH priority tasks are executed before any DEFAULT priority tasks.
 * </ul>
 * 
 * <p> By default, tasks are scheduled using the DEFAULT priority level.
 * You can choose the task priority using the @link {@link ExecutorPolicy} interface.<p>
 * 
 * <b>Important note</b>: The PlatformExecutor interface extends jdk ScheduledExecutorService
 * in order to schedule timers in a given PlatformExecutor, but these methods are deprecated. 
 * If you need to schedule timers within a given <code>PlatformExecutor</code>, then you can
 * use the {@link TimerService} interface.
 */
public interface PlatformExecutor extends PlatformExecutorCompatibility {
  
  /**
   * Identifies a worker thread which is running inside a thread pool.
   */
  interface WorkerThread {
    /**
     * Get the worker thread id.
     * 
     * @return the worker thread id.
     */
    public String getId();
  }
  
  /**
   * Return this executor id. By convention:
   * <ul>
   * <li><b>main</b>: refers to the executor on the callout agent main thread
   * <li><b>protocol</b>: (with protocol="http", "diameter", "sip", ...) refers to the executor
   * on a protocol thread running within the callout agent Jvm.
   * <li><b>session</b>: refers to the distributed session engine thread
   */
  String getId();
  
  /**
   * Attach a context to this executor.
   */
  void attach(Object attachment);
  
  /**
   * Gets the context attached to this executor.
   */
  <T> T attachment();
  
  /**
   * Indicates if this executor is using a thread pool or not.
   */
  boolean isThreadPoolExecutor();
  
  /**
   * Returns the PlatformExecutors service which this executor belongs to.
   * @return the PlatformExecutors service which this executor belongs to.
   */
  PlatformExecutors getPlatformExecutors();
  
  /**
   * Schedules the execution for a given task in this <code>PlatformExecutor</code>, using DEFAULT priority. The
   * <code>Thread Context Class Loader</code> of the current thread will be set by the executor,
   * just before running the scheduled task.
   * 
   * @param task the runnable task to be scheduled
   */
  void execute(Runnable command);
  
  /**
   * Schedules the execution for a given task at some time in the future. The
   * <code>Thread Context Class Loader</code> of the current thread will be set by the executor,
   * just before running the scheduled task.
   * 
   * @param task the runnable task to be scheduled
   * @param policy a policy used to control how the task should be executed
   * @see com.alcatel.as.service.concurrent.ExecutorPolicy
   */
  void execute(Runnable task, ExecutorPolicy policy);
  
  /**
   * Submits a value-returning task for execution and returns a
   * Future representing the pending results of the task. The
   * Future's <tt>get</tt> method will return the task's result upon
   * successful completion. The task is scheduled using the DEFAULT priority.
   *
   * <p>
   * If you would like to immediately block waiting
   * for a task, you can use constructions of the form
   * <tt>result = exec.submit(aCallable).get();</tt>
   *
   * <p> Note: The {@link Executors} class includes a set of methods
   * that can convert some other common closure-like objects,
   * for example, {@link java.security.PrivilegedAction} to
   * {@link Callable} form so they can be submitted.
   *
   * @param task the task to submit
   * @return a Future representing pending completion of the task
   * @throws RejectedExecutionException if the task cannot be
   *         scheduled for execution
   * @throws NullPointerException if the task is null
   */
  <T> Future<T> submit(Callable<T> task);
  
  /**
   * Submits a value-returning task for execution and returns a
   * Future representing the pending results of the task. The
   * Future's <tt>get</tt> method will return the task's result upon
   * successful completion. Th
   *
   * <p>
   * If you would like to immediately block waiting
   * for a task, you can use constructions of the form
   * <tt>result = exec.submit(aCallable).get();</tt>
   *
   * <p> Note: The {@link Executors} class includes a set of methods
   * that can convert some other common closure-like objects,
   * for example, {@link java.security.PrivilegedAction} to
   * {@link Callable} form so they can be submitted.
   *
   * @param task the task to submit
   * @param policy a policy used to control how the task should be executed
   * @return a Future representing pending completion of the task
   * @throws RejectedExecutionException if the task cannot be
   *         scheduled for execution
   * @throws NullPointerException if the task is null
   */
  <T> Future<T> submit(Callable<T> task, ExecutorPolicy policy);
  
  
  /**
   * Wraps this PlatformExecutor to a java.util.concurrent.Executor using a given Execution Policy.
   * @param policy the policy used by the returned Executor, which will reschedule submitted tasks 
   * in this executor, unsing the given policy.
   * @return an Executor whose execute method will invoke this PlatformExecutor's execute method 
   *         with the given policy.
   */
  Executor toExecutor(ExecutorPolicy policy);
}
