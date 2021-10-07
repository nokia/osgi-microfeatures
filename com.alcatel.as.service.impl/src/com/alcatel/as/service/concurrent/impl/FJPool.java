package com.alcatel.as.service.concurrent.impl;

import java.util.List;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;

public class FJPool implements ThreadPoolBase {  
  private volatile ForkJoinPool _pool;
  private final static Logger _logger = Logger.getLogger(FJPool.class);
  private final static boolean ASYNC_MODE = true;
  private final AtomicInteger _sequencer = new AtomicInteger(1);
  private final String _name;
  private final Meters _meters;

  public class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      return new WorkerThread(pool);
    }
  }
  
  public class WorkerThread extends ForkJoinWorkerThread {
    public WorkerThread(ForkJoinPool pool) {
      super(pool);
      super.setName(generateWorkerThreadName());
      super.setDaemon(true);
    }
        
    /**
     * Generates a thread pool name
     */
    private String generateWorkerThreadName() {
      int id = _sequencer.getAndIncrement();
      StringBuilder buf = new StringBuilder();
      buf.append(_name);
      buf.append("-");
      buf.append(id);
      return (buf.toString());
    }
  }
  
  public FJPool(String name, int size, Meters meters) {
    _name = name;
    _logger.info("Initializing processing thread pool with size " + size);
    _pool = new ForkJoinPool(size, new WorkerThreadFactory(), null, ASYNC_MODE);
    _meters = meters;
  }
  
  @Override
  public void execute(Runnable command) {
    execute(command, TaskPriority.DEFAULT);    
  }

  @Override
  public boolean isPooled(Thread t) {
    return t instanceof WorkerThread && ((WorkerThread) t).getPool() == _pool;
  }

  @Override
  public void setSize(int size) {
    if (_pool.getParallelism() != size) {
      _logger.info("Resizing processing thread pool with size " + size);
      ForkJoinPool oldPool = _pool;
      _pool = new ForkJoinPool(size, new WorkerThreadFactory(), null, ASYNC_MODE);
      oldPool.shutdown();
    }
  }

  @Override
  public void setKeepAlive(long idleTimeoutSec) {
	  // not supported		
  }

  @Override
  public int getSize() {
    return _pool.getParallelism();
  }

  @Override
  public void execute(Runnable task, TaskPriority pri) {
      ForkJoinPool currentPool = _pool;
      Runnable wrappedTask = () -> {
          try {
	      task.run();
	  } finally {
	      _meters.tpoolProcessingRun();
	  }
      };
    
      try {
	  _meters.tpoolProcessingScheduled();
	  currentPool.execute(wrappedTask);
      } catch (RejectedExecutionException rejected) {
	  ForkJoinPool newPool = _pool;
	  if (currentPool != newPool) {
	      // The threadpool has been resized: reschedule in the new thread pool
	      newPool.execute(wrappedTask);
	  } else {        
	      throw rejected;
	  }
      }
  }

  @Override
  public void shutdown() {
    _pool.shutdown();    
  }

  @Override
  public List<Runnable> shutdownNow() {
    return _pool.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return _pool.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return _pool.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return _pool.awaitTermination(timeout, unit);
  }
}
