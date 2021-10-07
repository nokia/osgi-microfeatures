package com.alcatel.as.diameter.lb.impl.monitor;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A per-thread monitor, which uses a volatile long.
 * The volatile long is written lazily, but is read from the L1 cache, using volatile semantic.
 * @see ThreadLocalCounter in http://psy-lob-saw.blogspot.fr/2013/06/java-concurrent-counters-by-numbers.html
 */
public class ThreadLocalLazyMonitor implements Monitor {
  private final Object _attachment;
  private final AtomicLong deadThreadSum = new AtomicLong();

  public ThreadLocalLazyMonitor (){ this (null);}
  public ThreadLocalLazyMonitor (Object attachment){
    _attachment = attachment;
  }
  
  static class Padded1 {
    long p1, p2, p3, p4, p6, p7;
  }
  
  static class Padded2 extends Padded1 {
    private static final long VALUE_OFFSET;
    
    static {
      try {
        VALUE_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(Padded2.class.getDeclaredField("value"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    volatile long value; // volatile on read, but not volatile on write
    
    public long get() {
      return value;
    }
    
    // Performs a get on the long value from current working memory thread, not from L1 cache 
    // (without using the volatile semantic).
    public long localGet() {
      return UnsafeAccess.UNSAFE.getLong(this, VALUE_OFFSET);
    }
    
    public void lazySet(long v) {
      UnsafeAccess.UNSAFE.putOrderedLong(this, VALUE_OFFSET, v);
    }
  }
  
  static class Padded3 extends Padded2 {
    long p9, p10, p11, p12, p13, p14;
  }
  
  static class ThreadAtomicLong extends Padded3 {
    final Thread t = Thread.currentThread(); // size = 64 bit (one long)
  }
  
  private final CopyOnWriteArrayList<ThreadAtomicLong> counters = new CopyOnWriteArrayList<ThreadAtomicLong>();
  
  private final ThreadLocal<ThreadAtomicLong> tlc = new ThreadLocal<ThreadAtomicLong>() {
    @Override
    protected ThreadAtomicLong initialValue() {
      ThreadAtomicLong lc = new ThreadAtomicLong();
      counters.add(lc);
      for (ThreadAtomicLong tal : counters) {
        if (!tal.t.isAlive()) {
          deadThreadSum.addAndGet(tal.get());
          counters.remove(tal);
        }
      }
      return lc;
    }
  };
  
  @Override
  public void add(long n) {
    ThreadAtomicLong lc = tlc.get();
    lc.lazySet(lc.localGet() + n);
  }
  
  @Override
  public void increment() {
    add(1);
  }
  
  @Override
  public long get() {
    long dts;
    long sum;
    do {
      dts = deadThreadSum.get();
      sum = 0;
      for (ThreadAtomicLong lc : counters) {
        sum += lc.get();
      }
    } while (dts != deadThreadSum.get());
    return sum + dts;
  }

  @Override
  public void decrement() {
    add(-1);
  }

  @Override
  public <T> T attachment (){
    return (T) _attachment;
  }
  
}
