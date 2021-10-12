// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.monitor;

public interface Monitor {
  public void add(long n);
  public void increment();
  public void decrement();
  public long get();
  public <T> T attachment ();
}