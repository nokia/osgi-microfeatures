// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * PlatformExecutor main entry point. This interface defines the contract of scheduling some tasks in 
 * a given platform executor. Tasks may have two level of priorities: DEFAULT, and HIGH.<p>
 * 
 * <ul><li> DEFAULT priority: tasks are executed after all HIGH priority tasks.
 * <li> HIGH priority tasks are executed before any pending DEFAULT tasks.
 * </ul>
 * 
 * By default, tasks are scheduled using the DEFAULT priority level.
 * You can choose the task priority using the @link {@link ExecutorPolicy} interface.
 *
 * <p>
 * Usage example:
 * <p>
 * <hr>
 * <blockquote>
 * 
 * <pre> {@code
 * class Test {
 *   private PlatformExecutor threadPool;
 *   private PlatformExecutors executors;
 *   
 *   void bindPlatformExecutors(PlatformExecutors executors) {
 *     this.threadPool = executors.getIOThreadPoolExecutor();
 *     this.executors = executors;
 *   }
 *   
 *   void doBlockingIO() {
 *     // Use a thread pool in order to perform a blocking IO ...
 *     
 *     final Executor callback = executors.getCurrentThreadContext().getCallbackExecutor();
 * 
 *     threadPool.execute(new Runnable() {
 *       public void run() {
 *          // Do a blocking io and callback the caller thread ...
 *          
 *          // And callback the caller thread
 *          callback.execute(new Runnable() {
 * 	           public void run() {
 * 	             // We'll be run within the thread where doBlockingIO was invoked.
 * 	             proceed();
 * 	           }
 * 	        });
 *       }
 *     });
 *   }
 * 
 *   void proceed() {
 *      // The IO operation has completed, proceed in our original thread ...
 *   }
 * } }</pre>
 * 
 * </blockquote>
 * <hr>
 */
@SuppressWarnings("deprecation")
public abstract class PlatformExecutors extends PlatformExecutorsCompatibility {
  /**
   * Returns the IO Blocking Thread Pool Executor. This thread pool can be typically used when doing IO-bound
   * Blocking operations.
   * 
   * @return the Blocking IO-bound Thread Pool Executor.
   */
  public abstract PlatformExecutor getIOThreadPoolExecutor();
 
  /**
   * Returns the Processing Thread Pool Executor. This thread pool can be typically used when doing 
   * CPU-bound operations. Tasks scheduled in this thread pool *MUST NOT* do any blocking operations
   * (io blocking read/write, wait/sleep). Tasks should also avoid to use synchronized method. 
   * 
   * @return the Non-Blocking CPU-bound Thread Pool Executor.
   */
  public abstract PlatformExecutor getProcessingThreadPoolExecutor();
    
  /**
   * Creates a new IO Blocking Thread Pool Executor. This thread pool can be typically used when doing IO-bound
   * Blocking operations.
   * 
   * @param label a symbolic name for the new thread pool
   * @param stat the prefix used to generate the thread pool Meter's name.
   * @param size the thread pool size
   * @return a new Blocking IO-bound Thread Pool Executor.
   */
  public abstract PlatformExecutor createIOThreadPoolExecutor(String label, String stat, int size);
 
  /**
   * Creates a new Processing Thread Pool Executor. This thread pool can be typically used when doing 
   * CPU-bound operations. 
   * 
   * @param label a symbolic name for the new thread pool
   * @param stat the prefix used to generate the threadpool Meter's name.
   * @param size the thread pool size
   * @return a new Non-Blocking CPU-bound Thread Pool Executor.
   */
  public abstract PlatformExecutor createProcessingThreadPoolExecutor(String label, String stat, int size);
    
  /**
   * Creates a serial queue executing on a given platform thread pool executor. A queue executes tasks
   * serially in FIFO order. A queue will only invoke one runnable at a time,
   * and independent queues may each be running concurrently in the given thread pool.
   * 
   * @param threadPool the thread pool used by the queue when executing submitted tasks
   * @return a new serial queue.
   */
  public abstract PlatformExecutor createQueueExecutor(PlatformExecutor threadPool);

  /**
   * Creates a serial queue executing on a given thread pool and a given queue label. 
   * This executor executes tasks serially in FIFO order. A queue will 
   * only invoke one runnable at a time, and independent queues may each be running concurrently 
   * in the given thread pool.
   * 
   * @param threadPool the thread pool used by the queue when executing submitted tasks.
   * @param label the label used as the symbolic name for the created queue executor.
   *        Using a label allows to ease debugging because the queue label is displayed
   *        using Log4j NDC (Nested Diagnostic Context).
   *
   * @return a new queue executor
   */
  public abstract PlatformExecutor createQueueExecutor(PlatformExecutor threadPool, String id);

  /**
   * Gets a serial queue executing on the IO blocking thread pool. Using the same <code>queue</code>
   * parameter for several tasks ensures these tasks are always run one after another in the IO thread pool.<p>
   * The returned queue executor is picked up from an internal pool of preallocated queues.
   *
   * @param queue a queue object whose hashCode() method is used in order to pick up a specific queue from the pool 
   * @throws IllegalArgumentException if the executor passed as argument is not a thread pool executor
   * @return a serial thread pool executor
   */
  public abstract PlatformExecutor getIOThreadPoolExecutor(Object queue);
  
  /**
   * Gets a serial queue executor using the processing thread pool. Using the same <code>queue</code>
   * parameter for several tasks ensures these tasks are always run one after another in the processing thread pool.<p>
   * The returned queue executor is picked up from an internal pool of preallocated queues.
   * 
   * @param queue a queue object whose hashCode() method is used in order to pick up a specific queue from the pool 
   * @throws IllegalArgumentException if the executor passed as argument is not a thread pool executor
   * @return a serial thread pool executor
   */
  public abstract PlatformExecutor getProcessingThreadPoolExecutor(Object queue);
  
  /**
   * Creates a queue on a single thread. A Thread is created in order to consume tasks scheduled
   * in the returned Queue.
   *
   * @param name the name of the queue
   * @param factory an optional factory used to instantiate the actual queue thread. If null, then a daemon thread
   *        is created by default.
   * @return a new thread queue executor.
   */
  public abstract PlatformExecutor createThreadQueueExecutor(String name, ThreadFactory factory);
    
  /**
   * Returns the current thread context.
   * @return the current thread context
   */
  public abstract ThreadContext getCurrentThreadContext();
  
  /**
   * Creates a scheduler useful when needing to orchestrate execution of a task chain using various
   * executors.
   * @return a new Scheduler for executing a task chain using various executors.
   */
  public abstract Scheduler createScheduler();
}
