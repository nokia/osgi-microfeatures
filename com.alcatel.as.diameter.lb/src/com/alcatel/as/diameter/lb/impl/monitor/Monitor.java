package com.alcatel.as.diameter.lb.impl.monitor;

public interface Monitor {
  public void add(long n);
  public void increment();
  public void decrement();
  public long get();
  public <T> T attachment ();
}