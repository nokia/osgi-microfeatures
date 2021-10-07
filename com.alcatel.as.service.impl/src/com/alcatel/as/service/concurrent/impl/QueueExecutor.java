package com.alcatel.as.service.concurrent.impl;

// Jdk
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.ThreadContext;

/**
 * Implementation for Executor returned by PlatformExecutors.createQueueExecutor(Executor)
 */
public class QueueExecutor extends PlatformExecutorBase implements PlatformExecutor {
  private final SerialQueue _serialQueue;
  private final String _id;
  
  /**
   * Executor used to ensure that timers are scheduled using HIGH priority.
   */
  final Executor _timerExecutor;
  
  /**
   * Allows to wrap a callable into a runnable
   */
  class CallableRunnable<T> extends FutureTask<T> {
    CallableRunnable(Callable<T> callable) {
      super(callable);
    }
  }
    
  /**
   * Creates a named queue executor. 
   * @param exec the executor where the queue will be executed
   * @param tpoolPri this queue will be scheduled in the tpool using this priority.
   * @param id the symbolic queue identifier
   */
  public QueueExecutor(Executor exec, String id, Meters meters) {
    _serialQueue = new SerialQueue(exec, id, meters);
    _timerExecutor = task -> _serialQueue.execute(task, TaskPriority.HIGH);
    _id = (id == null) ? "" : id;
  }
  
  // -------------------- AbstractExecutorService
  
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new RuntimeException("not implemented");
  }
  
  public boolean isTerminated() {
    return false;
  }
  
  public boolean isShutdown() {
    return false;
  }
  
  public List<Runnable> shutdownNow() {
    throw new RuntimeException("not implemented");
  }
  
  public void shutdown() {
    throw new RuntimeException("not implemented");
  }
  
  // -------------------- PlatformExecutor ------------------------------------------------------------------
  
  @Override
  public PlatformExecutors getPlatformExecutors() {
    return Helper.getPlatformExecutors();
  }
  
  @Override
  public String getId() {
    return _id;
  }
  
  @Override
  public String toString() {
    return new StringBuilder("QueueExecutor(").append(getId()).append(")").toString();
  }
  
  @Override
  public boolean isThreadPoolExecutor() {
    return false;
  }
  
  public void execute(Runnable r, ExecutorPolicy policy) {
    switch (policy) {
    case SCHEDULE:
      execute(r, TaskPriority.DEFAULT);
      break;
    
    case SCHEDULE_HIGH:
      execute(r, TaskPriority.HIGH);
      break;
    
    case INLINE:
      tryInline(r, TaskPriority.DEFAULT);
      break;
    
    case INLINE_HIGH:
      tryInline(r, TaskPriority.HIGH);
      break;
    
    default:
      throw new IllegalArgumentException("Invalid ExecutorPolicy parameter: " + policy);
    }
  }
  
  public <T> Future<T> submit(Callable<T> task, ExecutorPolicy policy) {
    RunnableFuture<T> ftask = newTaskFor(task);    
    switch (policy) {
    case SCHEDULE:
      execute(ftask, TaskPriority.DEFAULT);
      break;
    case SCHEDULE_HIGH:
      execute(ftask, TaskPriority.HIGH);
      break;
    case INLINE:
      if (mayInline()) {
        ftask.run();
      } else {
        execute(ftask, TaskPriority.DEFAULT);
      }
      break;
    case INLINE_HIGH:
      if (mayInline()) {
        ftask.run();
      } else {
        execute(ftask, TaskPriority.HIGH);
      }
      break;
    default:
      throw new IllegalArgumentException("Invalid ExecutorPolicy parameter: " + policy);
    }
    
    return ftask;
  }
  
  // -------------------- Executor -------------------------------------
  
  @Override
  public void execute(final Runnable task) {
    execute(task, TaskPriority.DEFAULT);
  }
  
  @Override
  public ScheduledFuture<?> schedule(final Runnable task, long delay, TimeUnit unit) {
    PlatformExecutorsImpl pfExecs = Helper.getPlatformExecutors();
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = ((ThreadContextImpl) pfExecs.getCurrentThreadContext()).getRootExecutor();
    return pfExecs.getTimerService().schedule(_timerExecutor, () -> Helper.runTask(task, cl, QueueExecutor.this, root), delay, unit);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> ScheduledFuture<T> schedule(final Callable<T> callable, long delay, TimeUnit unit) {
    PlatformExecutorsImpl pfExecs = Helper.getPlatformExecutors();
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = ((ThreadContextImpl) pfExecs.getCurrentThreadContext()).getRootExecutor();
    // The TimerCallable class allows wrap the callable into a runnable
    CallableRunnable<T> tc = new CallableRunnable<T>(() -> Helper.runCallable(callable, cl, QueueExecutor.this, root));
    return (ScheduledFuture<T>) pfExecs.getTimerService().schedule(_timerExecutor, tc, delay, unit);
  }
  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, long initDelay, long delay, TimeUnit unit) {
    PlatformExecutorsImpl pfExecs = Helper.getPlatformExecutors();
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = ((ThreadContextImpl) pfExecs.getCurrentThreadContext()).getRootExecutor();
    return pfExecs.getTimerService().scheduleAtFixedRate(_timerExecutor, 
    		() -> Helper.runTask(task, cl, QueueExecutor.this, root), initDelay, delay, unit);
  }
  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable task, long initDelay, long delay,
                                                   TimeUnit unit) {
    PlatformExecutorsImpl pfExecs = Helper.getPlatformExecutors();
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = ((ThreadContextImpl) pfExecs.getCurrentThreadContext()).getRootExecutor();
    return pfExecs.getTimerService().scheduleWithFixedDelay(_timerExecutor, 
    		() -> Helper.runTask(task, cl, QueueExecutor.this, root), initDelay, delay, unit);
  }
  
  private void tryInline(Runnable r, TaskPriority p) {
    if (mayInline()) {
      r.run();
    } else {
      execute(r, p);
    }
  }
  
  private boolean mayInline() {
    PlatformExecutors pfExecs = Helper.getPlatformExecutors();
    ThreadContext ctx = pfExecs.getCurrentThreadContext();
    PlatformExecutor pe = ctx.getCurrentExecutor();
    return pe.equals(this);
  }
    
  private void execute(final Runnable task, final TaskPriority pri) {
    final ClassLoader cl = Helper.getTCCL();
    final PlatformExecutor root = Helper.getRootExecutor(this);
    _serialQueue.execute(() -> Helper.runTask(task, cl, QueueExecutor.this, root), pri);
  }
}
