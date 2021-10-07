package com.alcatel.as.service.concurrent.impl;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.Scheduler;
import com.alcatel.as.service.concurrent.ThreadContext;

/**
 * Implementation of PlatformExecutors used outside OSGi.
 */
public class PlatformExecutorsImplStandalone extends PlatformExecutors {
  private final PlatformExecutors _impl;
  
  public PlatformExecutorsImplStandalone() {
    this(new Hashtable<String, Object>());
  }
  
  public PlatformExecutorsImplStandalone(Map<String, Object> conf) {
	  Standalone.init(conf);
	  _impl = Standalone.getPlatformExecutors();	
  }
  
  @Override
  public PlatformExecutor getThreadPoolExecutor() {
    return _impl.getThreadPoolExecutor();
  }
  
  @Override
  public PlatformExecutor getIOThreadPoolExecutor() {
    return _impl.getIOThreadPoolExecutor();
  }
  
  @Override
  public PlatformExecutor getThreadPoolExecutor(Object queue) {
    return _impl.getThreadPoolExecutor(queue);
  }
  
  @Override
  public PlatformExecutor getIOThreadPoolExecutor(Object queue) {
    return _impl.getIOThreadPoolExecutor(queue);
  }
  
  @Override
  public PlatformExecutor getProcessingThreadPoolExecutor() {
    return _impl.getProcessingThreadPoolExecutor();
  }
  
  @Override
  public PlatformExecutor getProcessingThreadPoolExecutor(Object queue) {
    return _impl.getProcessingThreadPoolExecutor(queue);
  }
  
  @Override
  public PlatformExecutor getExecutor(String id) {
    return _impl.getExecutor(id);
  }
  
  @Override
  public ThreadContext getCurrentThreadContext() {
    return _impl.getCurrentThreadContext();
  }
  
  @Override
  public PlatformExecutor createQueueExecutor(PlatformExecutor threadPool) {
    return _impl.createQueueExecutor(threadPool);
  }
  
  @Override
  public PlatformExecutor createQueueExecutor(PlatformExecutor threadPool, String id) {
    return _impl.createQueueExecutor(threadPool, id);
  }
  
  @Override
  public Scheduler createScheduler() {
    return _impl.createScheduler();
  }

  @Override
  public PlatformExecutor createIOThreadPoolExecutor(String label, String stat, int size) {
    return _impl.createIOThreadPoolExecutor(label, stat, size);
  }

  @Override
  public PlatformExecutor createProcessingThreadPoolExecutor(String label, String stat, int size) {
    return _impl.createProcessingThreadPoolExecutor(label, stat, size);
  }

  @Override
  public PlatformExecutor createThreadQueueExecutor(String name, ThreadFactory factory) {
    return _impl.createThreadQueueExecutor(name, factory);
  }
}
