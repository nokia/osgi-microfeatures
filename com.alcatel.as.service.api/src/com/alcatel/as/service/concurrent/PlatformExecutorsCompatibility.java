// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent;

import com.alcatel.as.util.serviceloader.ServiceLoader;

/**
 * This class contains deprecated methods for the {@link PlatformExecutors} service.
 * @deprecated use only methods specified in {@link PlatformExecutors} 
 */
@Deprecated
public abstract class PlatformExecutorsCompatibility {
  /**
   * Identifies the thread pool executor.
   * @deprecated use {@link PlatformExecutors#getThreadPoolExecutor()}
   */
  @Deprecated
  public static final String THREAD_POOL_EXECUTOR = "ThreadPool";
  
  /**
   * Identifies the executor for scheduling a task within the current thread.
   * @deprecated use the method getCurrentExecutor from {@link PlatformExecutors#getCurrentThreadContext()}
   * 
   * @see com.alcatel.as.service.concurrent.PlatformExecutors#getExecutor
   * @see com.alcatel.as.service.concurrent.ThreadContext#getCurrentExecutor
   */
  @Deprecated
  public static final String CURRENT_EXECUTOR = "Current";
  
  /**
   * Identifies the executor for scheduling sequential callback tasks.
   * @deprecated use the method getCallbackExecutor from {@link PlatformExecutors#getCurrentThreadContext()}
   * 
   * @see com.alcatel.as.service.concurrent.PlatformExecutors#getExecutor
   * @see com.alcatel.as.service.concurrent.ThreadContext#getCallbackExecutor
   */
  @Deprecated
  public static final String CURRENT_CALLBACK_EXECUTOR = "CurrentCallback";
  
  /**
   * Identifies the executor that initially scheduled the current task.
   * @deprecated use the method getRootExecutor from {@link PlatformExecutors#getCurrentThreadContext()}
   * 
   * @see com.alcatel.as.service.concurrent.PlatformExecutors#getExecutor
   * @see com.alcatel.as.service.concurrent.ThreadContext#getRootExecutor
   */
  @Deprecated
  public static final String CURRENT_ROOT_EXECUTOR = "CurrentRoot";
  
  /**
   * Identifies the preferred callback executor.
   * @deprecated use the method getPreferredCallbackExecutor from {@link PlatformExecutors#getCurrentThreadContext()}
   * 
   * @see com.alcatel.as.service.concurrent.PlatformExecutors#getExecutor
   * @see com.alcatel.as.service.concurrent.ThreadContext#getPreferredCallbackExecutor
   */
  @Deprecated
  public static final String CURRENT_PREFERRED_CALLBACK_EXECUTOR = "CurrentPreferredCallback";
  
  /**
   * Returns a sole PlatformExecutor instance.
   * 
   * @return a sole PlatformExecutor instance.
   * @deprecated You should use an OSGi service dependency in order to be injected with this service.
   */
  @Deprecated
  public static PlatformExecutors getInstance() {
    PlatformExecutors executors = (PlatformExecutors) ServiceLoader.getService(PlatformExecutors.class
        .getName());
    if (executors == null) {
      throw new RuntimeException("Platform Executor Service not currently available");
    }
    return executors;
  }
  
  /**
   * Returns the IO Thread Pool Executor. This thread pool can be typically used when doing IO-bound
   * operations.
   * 
   * @return the IO-bound Thread Pool Executor.
   * @deprecated use {@link PlatformExecutors#getIOThreadPoolExecutor()}
   */
  @Deprecated
  public abstract PlatformExecutor getThreadPoolExecutor();
  
  /**
   * Gets a serial queue executor using the IO thread pool. Using the same <code>queue</code>
   * parameter for several tasks ensures these tasks are always run one after another in the IO thread pool.<p>
   * The returned queue executor is picked up from an internal pool of preallocated queues.
   *
   * @param queue a queue object whose hashCode() method is used in order to pick up a specific queue from the pool 
   * @throws IllegalArgumentException if the executor passed as argument is not a thread pool executor
   * @return a serial thread pool executor
   * @deprecated use {@link PlatformExecutors#getIOThreadPoolExecutor(Object)}
   */
  public abstract PlatformExecutor getThreadPoolExecutor(Object queue);
  
  /**
   * Returns a well known platform executor.
   * 
   * @param id one of the constants provided in this class, or a well known executor name. By
   *          convention:
   *          <ul>
   *          <li>main: Refers to the callout agent main thread.
   *          <li>protocol (<b>http</b>, <b>sip</b>, ...): Refers to one callout agent protocol
   *          thread.
   *          <li>session: refers to the distributed session engine thread.
   *          </ul>
   * @return a well known executor
   * @deprecated this method actually does a lookup on the OSGi service registry (it looks for a PlatformExecutor Service
   * which has an id matching the id). Instead using this method, use standard OSGi service dependencies.
   */
  public abstract PlatformExecutor getExecutor(String id);
}
