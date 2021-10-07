package com.alcatel.as.service.concurrent;

/**
 * This class provides access to executors which are bound to the current running thread.
 */
public interface ThreadContext {
  /**
   * Set the preferred callback executor for this current thread. This executor will be returned
   * by {@link #getCallbackExecutor()} and {@link #getPreferredCallbackExecutor} methods. This
   * method aim at letting you specify the thread where an asynchronous service should callback
   * you.
   * 
   * @param executor the preferred callback executor which will be bound to this current thread.
   */
  void setPreferredCallbackExecutor(PlatformExecutor executor);

  /**
   * Returns an the preferred callback executor bound to this current thread.
   * 
   * @return the preferred callback executor bound to this current thread.
   * @see com.alcatel.as.service.concurrent.ThreadContext#setPreferredCallbackExecutor
   */
  PlatformExecutor getPreferredCallbackExecutor();

  /**
   * Returns an executor for scheduling sequential callback tasks. The following strategy is
   * applied:
   * <ul>
   * <li>If the setPreferredCallbackExecutor() has been invoked by the current thread prior to
   * this method call, then the preferred executor will be returned, and the current preferrred
   * executor will be reset to <b>null</b> after this this method returns.
   * <li>If no preferred executor is found, then the executor for the current thread will be
   * returned. Notice that if the current thread is running within the thread pool, then all
   * tasks will be executed sequentially within the thread pool.
   * <li>If no current executor if found, then the thread pool executor will be used, and each
   * tasks scheduled through the thread pool will be sequentially executed.
   * </ul>
   * 
   * @return the executor for scheduling sequential callback tasks
   */
  PlatformExecutor getCallbackExecutor();

  /**
   * Returns an executor for scheduling tasks within the current thread.
   * 
   * @return The Thread Pool Executor if the current thread is runing within the thread pool, or
   *         the protocol Executor which is bound to this current thread. If the current thread
   *         is neither running from the thread pool, nor a protocol thread executor; then the
   *         thread pool executor is returned by default.
   */
  PlatformExecutor getCurrentExecutor();

  /**
   * Returns the executor which initially scheduled the current thread's task. For instance,
   * invoking this method from a task running within the thread-pool will return <b>http</b>, if
   * that task was originally scheduled by an http proxylet.
   * 
   * @return the initial executor that initially scheduled the task being run within the current
   *         thread.
   */
  PlatformExecutor getRootExecutor();

  /**
   * Return the current worker thread which is running the current thread.
   * 
   * @return the current thread pool worker thread, or null if the current thread is not part of
   *         the thread pool.
   */
  PlatformExecutor.WorkerThread getCurrentWorkerThread();
}
