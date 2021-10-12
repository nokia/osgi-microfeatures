// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.monitor;

import java.util.concurrent.atomic.LongAdder;

/*
 * Simple Monitor, based on standard jdk8 LongAdder.
 */
public class LongAdderMonitor implements Monitor {
  private final Object _attachment;
  private final LongAdder _counter = new LongAdder();
  
  public LongAdderMonitor (){ this (null);}
  public LongAdderMonitor (Object o){
    _attachment = o;
  }
  
  @Override
  public void add(long n) {
    _counter.add(n);
  }
  
  @Override
  public void increment() {
    _counter.increment();
  }

  @Override
  public void decrement() {
    _counter.decrement();
  }
  
  @Override
  public long get() {
    return _counter.sum();
  }

  @Override
  public <T> T attachment (){
    return (T) _attachment;
  }
  
}
