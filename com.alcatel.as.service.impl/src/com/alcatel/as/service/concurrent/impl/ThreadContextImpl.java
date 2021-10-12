// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.ThreadContext;

public class ThreadContextImpl implements ThreadContext {
  private PlatformExecutor _rootExecutor;
  private PlatformExecutor _currentExecutor;
  private PlatformExecutor _preferredCallbackExecutor;
  
  public ThreadContextImpl() {
  }
  
  public void setPreferredCallbackExecutor(PlatformExecutor executor) {
    _preferredCallbackExecutor = executor;
  }
  
  public PlatformExecutor getPreferredCallbackExecutor() {
    return _preferredCallbackExecutor;
  }
  
  public PlatformExecutor getCallbackExecutor() {
    PlatformExecutor pe = _preferredCallbackExecutor;
    _preferredCallbackExecutor = null;
    if (pe == null) {
      if (_currentExecutor != null) {
        pe = _currentExecutor;
      } else {
        PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
        pe = execs.getThreadPoolExecutor();
      }
    }
    return pe;
  }
  
  public PlatformExecutor getCurrentExecutor() {
    PlatformExecutor pe = _currentExecutor;
    if (pe == null) {
      PlatformExecutorsImpl execs = Helper.getPlatformExecutors();
      pe = execs.getThreadPoolExecutor();
    }
    return pe;
  }
  
  public PlatformExecutor getRootExecutor() {
    return _rootExecutor;
  }
  
  public PlatformExecutor.WorkerThread getCurrentWorkerThread() {
    if (_currentExecutor instanceof ThreadPoolExecutor) {
      return new WorkerThreadImpl(Thread.currentThread().getName());
    }
    return null;
  }
  
  void setCurrentThreadExecutor(PlatformExecutor current) {
    _currentExecutor = current;
  }
  
  void setRootExecutor(PlatformExecutor root) {
    _rootExecutor = root;
  }
  
  private static class WorkerThreadImpl implements PlatformExecutor.WorkerThread {
    private final String _id;
    
    WorkerThreadImpl(String id) {
      _id = id;
    }
    
    @Override
    public String getId() {
      return _id;
    }
  }
}
