// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent;

import java.util.concurrent.Executor;

/**
 * Helper used to orchestrate execution of a task chain, using different executors.
 * <p>
 * 
 * Usage example: <p>
 * Here, the first function is executed using the "exec1" executor, then the
 * scheduler will execute the second function, using the "exec2" executor. Finally, the third
 * function is executed using "exec1" executor.
 * 
 * <code>
 * <pre>
 * 
 * import static java.lang.System.out;
 * 
 * import java.util.concurrent.Executor;
 * 
 * import org.osgi.service.component.annotations.Reference;
 * 
 * import com.alcatel.as.service.concurrent.PlatformExecutors;
 * import com.alcatel.as.service.concurrent.Scheduler;
 * import com.alcatel.as.service.concurrent.Scheduler.F;
 * 
 * public class Test {
 *   PlatformExecutors _pfexecs;
 *   
 *   &#64;Reference
 *   void bind(PlatformExecutors execs) {
 *     _pfexecs = execs;
 *   }
 *   
 *   void testAsyncThings() {
 *     final Scheduler scheduler = _pfexecs.createScheduler();
 *     final Executor exec1 = _pfexecs.createQueueExecutor(_pfexecs.getProcessingThreadPoolExecutor());
 *     final Executor exec2 = _pfexecs.createQueueExecutor(_pfexecs.getProcessingThreadPoolExecutor());
 *     
 *     scheduler.atFirst(exec1, new F() {
 *       public void f(Scheduler s) {
 *         out.println("in the first callback, attachment is " + s.attachment());
 *         s.next("param passed to the second function");
 *       }
 *     }).andThen(exec2, new F() {
 *       public void f(Scheduler s) {
 *         out.println("in the second callback, attachment is " + s.attachment());
 *         s.next("another param for the last function");
 *       }
 *     }).andThen(exec1, new F() {
 *       public void f(Scheduler s) {
 *         out.println("finally this is the result: " + s.attachment());
 *       }
 *     });
 *     
 *     scheduler.next("initial attachment");
 *   }
 * } 
 * </pre>
 * </blockquote>
 */
public interface Scheduler {
  /**
   * Base interface for tasks this scheduler is managing.
   */
  public interface F {
    public void f(Scheduler s);
  }
  
  /**
   * Schedule execution of the next tasks scheduled using the atFirst or andThen method.
   */
  void next();
  
  /**
   * Schedules execution of the next tasks scheduled using the atFirst or andThen method.
   * @param attachement an attachement passed to the next scheduled task.
   */
  void next(Object attachement);
  
  /**
   * Returns the attachment bound to this scheduler.
   * @return the attachment bound to this scheduler.
   */
  <T> T attachment();
  
  /**
   * Initialize the first task this scheduler must execute.
   * @param exec the executor used to execute the task
   * @param f the function executed using the executor
   * @return A Scheduler object that can be used to add a next task, using {@link #andThen(Executor, F)}} method.
   */
  Scheduler atFirst(Executor exec, final F f);
  
  /**
   * Chain a task to this scheduler.
   * @param exec the executor used to execute the task
   * @param f the task to be scheduler in the given executor
   * @return A Scheduler object that can be used to add a next task, using {@link #andThen(Executor, F)}} method.
   */
  public Scheduler andThen(Executor exec, final F f);
}
