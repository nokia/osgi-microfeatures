// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple priority queue, based on JDK concurrent linked Dequeue.
 */
public class PriorityQueue {  
  private final ConcurrentLinkedDeque<Runnable>[] _queues = new ConcurrentLinkedDeque[TaskPriority.values().length];
  
  public PriorityQueue() {
    for (int i = 0; i < TaskPriority.values().length; i++) {
      _queues[i] = new ConcurrentLinkedDeque<Runnable>();
    }
  }
  
  public void addLast(Runnable x, TaskPriority p) {
    _queues[p.ordinal()].addLast(x);
  }
  
  public void addLast(Runnable x) {
   addLast(x, TaskPriority.DEFAULT);
  }
  
  public void addFirst(Runnable x, TaskPriority p) {
    _queues[p.ordinal()].addFirst(x);
  }
  
  public void addFirst(Runnable x) {
    addFirst(x, TaskPriority.DEFAULT);
  }

  public Runnable pollFirst() {
    Runnable x = _queues[TaskPriority.HIGH.ordinal()].pollFirst();
    if (x == null) {
      x = _queues[TaskPriority.DEFAULT.ordinal()].pollFirst();
    }
    return x;
  }
  
  public Runnable pollLast() {
    Runnable x = _queues[TaskPriority.HIGH.ordinal()].pollLast();
    if (x == null) {
      x = _queues[TaskPriority.DEFAULT.ordinal()].pollLast();
    }
    return x;
  }
  
  public Runnable peekFirst() {
    Runnable x = _queues[TaskPriority.HIGH.ordinal()].peekFirst();
    if (x == null) {
      x = _queues[TaskPriority.DEFAULT.ordinal()].peekFirst();
    }
    return x;
  }
}
