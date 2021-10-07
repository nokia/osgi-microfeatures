package com.alcatel.as.service.concurrent.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.ExecutorPolicy;

public abstract class PlatformExecutorBase extends AbstractExecutorService {
  private volatile Object _attachment;
  private final Map<ExecutorPolicy, Executor> _executorsWithPolicy;
  
  PlatformExecutorBase() {
    _executorsWithPolicy = new HashMap<>();
    for (final ExecutorPolicy policy : ExecutorPolicy.values()) {
      _executorsWithPolicy.put(policy, command -> PlatformExecutorBase.this.execute(command, policy));
    }    
  }
  
  /**
   * Attaches a context to this executor.
   * @param attachment the context to attach
   */
  public void attach(Object attachment) {
    _attachment = attachment;
  }
  
  /**
   * Gets the context attached to this executor.
   */
  @SuppressWarnings("unchecked")
  public <T> T attachment() {
    return (T) _attachment;
  }
  
  /**
   * Executes a task, using a given policy
   * @param r
   * @param policy
   */
  public abstract void execute(Runnable r, ExecutorPolicy policy);

  /**
   * Wraps this PlatformExecutor to a java.util.concurrent.Executor using a given Execution Policy.
   */
  public Executor toExecutor(final ExecutorPolicy policy) {
    return _executorsWithPolicy.get(policy);
  }
}
