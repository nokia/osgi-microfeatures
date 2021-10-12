// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

import alcatel.tess.hometop.gateways.utils.Utils;

/**
 * A simple FIFO queue class which causes the calling thread to wait
 * if the queue is empty and notifies threads that are waiting when it
 * is not empty.
 *
 * @author Anil V (akv@eng.sun.com)
 */
public class BufferQueue implements QueueIF {
  /**
   * Create a buffer with the current default capacity
   */
  public BufferQueue() {
    this(1024);
  }
  
  /**
   * Create a BoundedBuffer with the given capacity.
   * @exception IllegalArgumentException if capacity less or equal to zero
   */
  public BufferQueue(int capacity) throws IllegalArgumentException {
    if (capacity < 1) {
      throw new IllegalArgumentException("BufferQueue must have a capacity greater than " + capacity);
    }
    capacity = roundToPower(capacity);
    this.array = new Object[capacity];
    this.emptySlots = capacity;
  }
  
  public synchronized void clear() {
    try {
      while (poll(0) != null)
        ;
    } catch (InterruptedException e) {
    }
  }
  
  /**
   * Return the current number of pending elements.
   */
  public int size() {
    synchronized (this) {
      return (this.usedSlots);
    }
  }
  
  /**
   * Insert a new message in this Queue.
   *
   * @param x The message to put
   */
  public void put(Object x) throws InterruptedException {
    synchronized (this.putMonitor) {
      if (emptySlots <= 0) {
        try {
          waitingPuts++;
          do {
            putMonitor.wait();
          } while (emptySlots <= 0);
        }
        
        catch (InterruptedException ex) {
          putMonitor.notify();
          throw ex;
        }
        
        finally {
          waitingPuts--;
        }
      }
      
      --emptySlots;
      array[putPtr] = x;
      putPtr = (putPtr + 1) & (array.length - 1);
    }
    
    synchronized (this) {
      ++usedSlots; // increment used slots.
      notify(); // wakeup any readers.
    }
  }
  
  /**
   * Insert a new message in this Queue.
   *
   * @param x The message to put
   */
  public boolean offer(Object x) {
    synchronized (this.putMonitor) {
      if (emptySlots <= 0) {
        return false;
      }
      
      --emptySlots;
      array[putPtr] = x;
      putPtr = (putPtr + 1) & (array.length - 1);
    }
    
    synchronized (this) {
      ++usedSlots; // increment used slots.
      notify(); // wakeup any readers.
    }
    
    return true;
  }
  
  public Object take() throws InterruptedException {
    Object old = null;
    
    synchronized (this) {
      while (usedSlots <= 0) {
        try {
          wait();
        } catch (InterruptedException ex) {
          notify();
          throw ex;
        }
      }
      
      --usedSlots;
      old = array[takePtr];
      array[takePtr] = null;
      takePtr = (takePtr + 1) & (array.length - 1);
    }
    
    synchronized (putMonitor) {
      ++emptySlots;
      if (waitingPuts > 0)
        putMonitor.notify();
    }
    
    return old;
  }
  
  public int take(Object[] out) throws InterruptedException {
    Object[] array;
    int avail = 0;
    int n = 0, n2 = 0;
    
    synchronized (this) {
      while (usedSlots <= 0) {
        try {
          wait();
        } catch (InterruptedException ex) {
          notify();
          throw ex;
        }
      }
      
      array = this.array;
      usedSlots -= (avail = Math.min(out.length, usedSlots));
      n = Math.min(avail, array.length - takePtr);
      
      System.arraycopy(array, takePtr, out, 0, n);
      Utils.clearArray(array, takePtr, n);
      
      if ((n2 = (avail - n)) > 0) {
        System.arraycopy(array, 0, out, n, n2);
        Utils.clearArray(array, 0, n2);
      }
      
      takePtr = (takePtr + avail) & (array.length - 1);
    }
    
    synchronized (putMonitor) {
      emptySlots += avail;
      n = Math.min(avail, waitingPuts);
      for (int i = 0; i < n; i++) {
        putMonitor.notify();
      }
    }
    
    return avail;
  }
  
  public Object poll(long msecs) throws InterruptedException {
    Object old = null;
    
    synchronized (this) {
      long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
      long waitTime = msecs;
      
      while (usedSlots <= 0) {
        if (waitTime <= 0)
          return null;
        
        try {
          wait(waitTime);
        }
        
        catch (InterruptedException ex) {
          notify();
          throw ex;
        }
        
        waitTime = msecs - (System.currentTimeMillis() - start);
      }
      
      --usedSlots;
      old = array[takePtr];
      array[takePtr] = null;
      takePtr = (takePtr + 1) & (array.length - 1);
    }
    
    synchronized (putMonitor) {
      ++emptySlots;
      if (waitingPuts > 0)
        putMonitor.notify();
    }
    
    return old;
  }
  
  public int poll(Object[] out, long msecs) throws InterruptedException {
    Object[] array;
    int avail = 0;
    int n = 0, n2 = 0;
    
    synchronized (this) {
      long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
      long waitTime = msecs;
      
      while (usedSlots <= 0) {
        if (waitTime <= 0)
          return 0;
        
        try {
          wait(waitTime);
        }
        
        catch (InterruptedException ex) {
          notify();
          throw ex;
        }
        
        waitTime = msecs - (System.currentTimeMillis() - start);
      }
      
      array = this.array;
      usedSlots -= (avail = Math.min(out.length, usedSlots));
      n = Math.min(avail, array.length - takePtr);
      
      System.arraycopy(array, takePtr, out, 0, n);
      Utils.clearArray(array, takePtr, n);
      
      if ((n2 = (avail - n)) > 0) {
        System.arraycopy(array, 0, out, n, n2);
        Utils.clearArray(array, 0, n2);
      }
      
      takePtr = (takePtr + avail) & (array.length - 1);
    }
    
    synchronized (putMonitor) {
      emptySlots += avail;
      n = Math.min(avail, waitingPuts);
      for (int i = 0; i < n; i++) {
        putMonitor.notify();
      }
    }
    
    return avail;
  }
  
  private int roundToPower(int n) {
    int p = 1;
    while (p < n)
      p <<= 1;
    return (p);
  }
  
  /** the elements */
  private Object[] array;
  
  /** circular indices */
  private int takePtr = 0;
  private int putPtr = 0;
  
  /** length */
  private int usedSlots = 0;
  private int waitingPuts = 0;
  
  /** capacity - length */
  private int emptySlots;
  
  /** Helper monitor to handle puts */
  private final Object putMonitor = new Object();
}
