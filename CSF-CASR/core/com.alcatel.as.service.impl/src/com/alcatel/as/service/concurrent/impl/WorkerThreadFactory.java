package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Factory used to create worker threads
 */
class WorkerThreadFactory implements ThreadFactory {
  /** Counter used to create worker thread names */
  protected final AtomicInteger _threadNumber = new AtomicInteger(1);
  
  /** Symbolic name for worker thread names */
  protected final String _name;
  
  /** The ThreadPoolBase using this factory */
  private ThreadPoolBase _tpool;
  
  WorkerThreadFactory(String name, ThreadPoolBase tpool) {
    _name = name;
    _tpool = tpool;
  }
  
  @Override
  public Thread newThread(Runnable r) {
    Thread t = new WorkerThread(r, _name + "-" + _threadNumber.getAndIncrement(), _tpool);
    t.setDaemon(true);
    return t;
  }
}
