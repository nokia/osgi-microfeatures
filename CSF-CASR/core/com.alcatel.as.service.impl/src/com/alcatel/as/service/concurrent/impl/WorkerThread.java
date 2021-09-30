package com.alcatel.as.service.concurrent.impl;

/**
 * A worker thread used to execute workers
 */
public class WorkerThread extends Thread {
  private final ThreadPoolBase _tpool;
  
  public WorkerThread(Runnable r, String name, ThreadPoolBase tpool) {
    super(r, name);
    _tpool = tpool;
  }
  
  ThreadPoolBase getThreadPool() {
    return _tpool;
  }
}
