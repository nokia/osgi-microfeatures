// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.monitor;

import java.util.concurrent.atomic.AtomicLong;

/*
 * Simple Monitor, based on standard java AtomicLong.
 */
public class AtomicMonitor implements Monitor {
  private final Object _attachment;
  private final AtomicLong _counter = new AtomicLong();
  
  public AtomicMonitor (){ this (null);}
  public AtomicMonitor (Object o){
    _attachment = o;
  }
  
  @Override
  public void add(long n) {
    _counter.addAndGet(n);
  }
  
  @Override
  public void increment() {
    _counter.incrementAndGet();
  }

  @Override
  public void decrement() {
    _counter.decrementAndGet();
  }
  
  @Override
  public long get() {
    return _counter.get();
  }

  @Override
  public <T> T attachment (){
    return (T) _attachment;
  }
  
}