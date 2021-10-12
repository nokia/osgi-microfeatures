// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.Scheduler;

/**
 * Helper used to orchestrate execution of a task chain, using different executors.
 */
public class SchedulerImpl implements Scheduler {
  private volatile Object _attachment = null;
  private volatile F _completion = null;
  private volatile Executor _exec;
  
  @SuppressWarnings("unchecked")
  public <T> T attachment() {
    return (T) _attachment;
  }
  
  public void next() {
    next(null);
  }
  
  public void next(Object attachement) {
    _attachment = attachement;
    _exec.execute(new Runnable() {
      public void run() {
        _completion.f(SchedulerImpl.this);
      }
    });
  }
  
  public Scheduler atFirst(Executor exec, final F f) {
    return andThen(exec, f);
  }
  
  public Scheduler andThen(Executor exec, final F f) {
    final SchedulerImpl next = new SchedulerImpl();
    _exec = exec;
    _completion = new F() {
      public void f(Scheduler s) {
        next._attachment = ((SchedulerImpl) s)._attachment;
        f.f(next);
      }
    };
    return next;
  }
}
