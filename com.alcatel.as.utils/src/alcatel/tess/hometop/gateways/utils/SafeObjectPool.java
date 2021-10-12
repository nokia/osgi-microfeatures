// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * WeakReference based and Fail-Fast Object Pool. Recyclable objects are gracefully reclaimed 
 * by the GC, using Weak references. This pool also manages a reference counter
 * which is used to safely release reusable objects. Acquired objects must be released 
 * at most one time. 
 * If your application needs to release the same object multiple times, then you can use the reference 
 * counter provided by this class in order to fastly detect illegal object release.
 * This class is thread safe and uses a concurrent queue which avoids thread contention while this class
 * is accessed by many threads.
 * <p>If you want to log acquired/released objects, you can turn on <b>debug</b> mode on this class logger.
 * If you want to log weak reference loops, you can turn on <b>info</b> mode on this class logger.
 * Debug traces are usefull to debug your application, when invalid releases are done.
 *
 * <p>For example, a Recyclabe object can be implemented like the following:
 * <p><hr><blockquote><pre>
 *  public class MyRecyclable implements SafeObjectPool.Recyclable {
 *    private final SafeObjectPool.ReferenceCounter _refCount = new SafeObjectPool.ReferenceCounter();
 *    private String _myAttribute;
 *    
 *    public boolean acquired() {
 *      _myAttribute = "ACQUIRED";
 *      return true; // This method could return false if it consider that this object is no more reusable.
 *    }
 *    
 *    public void recycled() {
 *      _myAttribute = null;
 *    }
 *    
 *    public SafeObjectPool.ReferenceCounter getReferenceCounter() {
 *      return _refCount;
 *    }
 *  }
 * </pre></blockquote><hr>
 *
 * <p>
 * The following code uses a SafeObjectPool in order to acquire/release MyRecyclable instances <b>safely</b>
 *
 * <p><hr><blockquote><pre>
 *  SafeObjectPool<MyRecyclable> pool = new SafeObjectPool&lt;MyRecyclable&gt;(MySafeObjectPool.class);
 *  MyRecyclable m = pool.acquire();
 *  pool.release(m);
 * </pre></blockquote><hr>
 *
 * The following code releases several times the same Recyclable instance <b>safely</b>, using reference couting:
 *
 * <p><hr><blockquote><pre>
 *  SafeObjectPool&lt;MyRecyclable&gt; pool = new SafeObjectPool&lt;MyRecyclable&gt>(MySafeObjectPool.class);
 *  <b>final MyRecyclable m = pool.acquire();</b>
 *
 *  // Starts 10 threads (each thread will use, and then release the "m" instance.
 *
 *  Thread[] threads = new Thread[10];
 *
 *  for (int i = 0; i < threads.length; i ++) {
 *    <b>m.getReferenceCounter().addReference();</b>
 *
 *    (threads[i] = new Thread(new Runnable() {
 *        public void run() {
 *	    try { Thread.sleep(1); } catch (InterruptedException e) {}
 *	    <b>pool.release(m);</b>
 *	  }
 *	})).start();
 *  }
 *
 *  <b>pool.release(m);</b>
 * </pre></blockquote><hr>
 *
 * The following code has a bug because it releases a recyclable object more than one time:
 *
 * <p><hr><blockquote><pre>
 *  MyRecyclable t = pool.acquire();
 *  pool.release(t); 
 *  pool.release(t);
 *
 *  Here is the corresponding debug logs which are usefull to debug the problem:
 *
 *  SafeObjectPool/test.TestSafeObjectPool$MyRecyclable: <b>acquired object @12014584</b>
 *  SafeObjectPool/test.TestSafeObjectPool$MyRecyclable: <b>released object @12014584</b>
 *  Exception in thread "main" java.lang.IllegalStateException: <b>object @12014584</b> is already recycled
 *      at alcatel.tess.hometop.gateways.utils.SafeObjectPool.recycled(SafeObjectPool.java:386)
 *      at alcatel.tess.hometop.gateways.utils.SafeObjectPool.release(SafeObjectPool.java:297)
 *      at alcatel.tess.hometop.gateways.test.TestSafeObjectPool.test1(TestSafeObjectPool.java:57)
 *      at alcatel.tess.hometop.gateways.test.TestSafeObjectPool.main(TestSafeObjectPool.java:45)
 * </pre></blockquote>
 *
 */
public class SafeObjectPool<T extends SafeObjectPool.Recyclable> {
  /**
   * Every reusable objects must implement this interface.
   */
  public interface Recyclable {
    /**
     * Method called when this object is acquired from the pool.
     * This method can check if some internal attributes are still valid 
     * (for example: a jdbc connection), and return false if the object
     * can not be reused.
     *
     * @return true if this object can be acquired, false if this object
     *	       can not be reused.
     */
    boolean acquired();
    
    /**
     * Method called when this object returns to the pool. 
     * This method should cleanup useless attributes in order
     * to avoid keeping useless object references while this object is in the pool.
     */
    void recycled();
    
    /**
     * Get the Reference counter associated with this instance.
     *
     * @return The Reference Counter associated with this recyclable object.
     *	       When the object is acqcuired, the ref counter is set to one.
     *	       the SafeObjectPool.release() method decrements the ref counter, and 
     *	       release the object only when the refcount equals 0. If this recyclable is
     *	       referenced by severeal objects or threads and must be relased more
     *	       than 1 time, then you can call the refcount.addReference method.
     */
    ReferenceCounter getReferenceCounter();
  }
  
  /**
   * Reference counter used to safely release recyclable objects.
   */
  public final static class ReferenceCounter {
    /**
     * Increment this ref counter by one. This method is thread safe, since it is based on AtomicInteger.
     * @return the new reference counter value.
     */
    public int addReference() {
      return _refCount.incrementAndGet();
    }
    
    /**
     * Return a string representing for this counter.
     */
    public String toString() {
      return _refCount.toString();
    }
    
    /** Internal reference counter. */
    private final AtomicInteger _refCount = new AtomicInteger(0);
  }
  
  /**
   * Factory used to create Recyclable instances while our pool is empty.
   * This factory may be used when Class descriptor can not be used to create
   * recyclable instances.
   */
  public interface Factory<T extends SafeObjectPool.Recyclable> {
    T newRecyclable();
  }
  
  /**
   * Creates a new <code>SafeObjectPool</code> instance.
   *
   * @param clazz an <code>Class</code> value used to creates objects when the pool
   *	is empty.
   */
  public SafeObjectPool(Class<T> clazz) {
    this(clazz, -1);
  }
  
  /**
   * Creates a new <code>SafeObjectPool</code> instance.
   *
   * @param clazz The <code>Class</code> used to creates recycalble objects when the pool is empty.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *	created by the acquired method. The acquired method will block if too much instances are currently 
   *    created.
   */
  public SafeObjectPool(Class<T> clazz, int maxAcquired) {
    _class = clazz;
    _maxAcquired = maxAcquired;
    _factory = null;
  }
  
  /**
   * Creates a new <code>SafeObjectPool</code> instance.
   *
   * @param factory an <code>SafeObjectPoolFactory</code> value used to creates objects when the pool
   *	is empty.
   */
  public SafeObjectPool(Factory<T> factory) {
    this(factory, -1);
  }
  
  /**
   * Creates a new <code>SafeObjectPool</code> instance.
   *
   * @param factory The factory used to creates recycalble objects when the pool is empty.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *	created by the acquired method. The acquired method will block if too much instances are currently 
   *    created.
   */
  public SafeObjectPool(Factory<T> factory, int maxAcquired) {
    _factory = factory;
    _maxAcquired = maxAcquired;
    _class = null;
  }
  
  /**
   * The <code>acquire</code> method acquires an object from the pool. If the SafeObjectPool has been initialized 
   * with a <code>maxAcquired</code> parameter, and if the current number of acquired instances equals this
   * max number, then this method will block until one other thread release an object.
   *
   * @return a <code>T</code> value
   * @throws IllegalStateException if an already acquired object is found in the free list.
   * @throws RuntimeException if an exception was caught while creating a new instance, or if any other
   *			      unexpected exception was caught.
   */
  public T acquire() {
    if (_maxAcquired == -1) {
      return doAcquire();
    } else {
      T r;
      _lock.lock();
      
      try {
        while (_acquired >= _maxAcquired) {
          _notFull.await();
        }
        
        r = doAcquire();
        _acquired++;
      }
      
      catch (InterruptedException e) {
        throw new RuntimeException("Operation interrupted", e);
      }
      
      finally {
        _lock.unlock();
      }
      
      return r;
    }
  }
  
  /**
   * The <code>acquire</code> method acquires an object from the pool. If the SafeObjectPool has been initialized 
   * with a <code>maxAcquired</code> parameter, then this method will block until one other thread release an
   * object.
   *
   * @param timeout an <code>int</code> value representing the max time in millis until a thread will wait 
   * for an available object from the pool.
   *
   * @return a <code>T</code> value or null if the instance could not be acquired timely.
   * @throws IllegalStateException if an already acquired object is found in the free list.
   * @throws RuntimeException if an exception was caught while creating a new instance, or if any other
   *			      unexpected exception was caught.
   */
  public T acquire(long timeout) {
    if (_maxAcquired == -1) {
      return doAcquire();
    } else {
      T r;
      _lock.lock();
      
      try {
        long start = (timeout <= 0) ? 0 : System.currentTimeMillis();
        long waitTime = timeout;
        
        while (_acquired >= _maxAcquired) {
          if (waitTime <= 0) {
            return null;
          }
          
          _notFull.await(waitTime, TimeUnit.MILLISECONDS);
          waitTime = timeout - (System.currentTimeMillis() - start);
        }
        
        r = doAcquire();
        _acquired++;
      }
      
      catch (InterruptedException e) {
        throw new RuntimeException("Operation interrupted", e);
      }
      
      finally {
        _lock.unlock();
      }
      
      return r;
    }
  }
  
  /**
   * Get back an object to its pool.
   *
   * @param obj The obj that is coming back to the pool.
   * @throws IllegalStateException if the object has already been released
   */
  public void release(T obj) {
    if (recycled(obj)) {
      if (_logger.isDebugEnabled()) {
        _logger.debug(getName() + ": released object @" + System.identityHashCode(obj));
      }
      
      _pool.offer(new WeakReference<T>(obj));
      
      if (_maxAcquired > -1) {
        // Wake up one thread possibly blocked in the acquire method.
        _lock.lock();
        
        try {
          if (--_acquired < 0) {
            _acquired = 0;
          }
          _notFull.signal();
        }
        
        finally {
          _lock.unlock();
        }
      }
    }
  }
  
  /**
   * Return a string representation for this instance. <b>Warning</b>: this method should be called only 
   * for debug purpose because it counts the current number of available object in the pool. 
   */
  public String toString() {
    return getName() + ",size=" + _pool.size();
  }
  
  /************************************* Private methods *********************/
  
  private T doAcquire() {
    Queue<WeakReference<T>> pool = _pool;
    WeakReference<T> tref = null;
    T t;
    
    int loops = 0;
    do {
      t = null;
      while ((tref = pool.poll()) != null && (t = tref.get()) == null) {
        loops++;
      }
    } while (t != null && !acquired(t));
    
    if (loops > 0 && _logger.isInfoEnabled()) {
      _logger.info(toString() + ",WeakRef iterations=" + loops);
    }
    
    if (t == null) {
      t = create();
      acquired(t);
    }
    
    if (_logger.isDebugEnabled()) {
      _logger.debug(getName() + ": acquired object @" + System.identityHashCode(t));
    }
    
    return t;
  }
  
  private T create() {
    if (_class != null) {
      try {
        return _class.newInstance();
      }
      
      catch (Throwable t) {
        throw new RuntimeException("Can not instanciate class " + _class.getName(), t);
      }
    } else {
      return _factory.newRecyclable();
    }
  }
  
  private boolean acquired(T obj) {
    if (!obj.getReferenceCounter()._refCount.compareAndSet(0, 1)) {
      throw new IllegalStateException("Recyclable object is still referenced");
    }
    return obj.acquired();
  }
  
  private boolean recycled(T obj) {
    int count = obj.getReferenceCounter()._refCount.decrementAndGet();
    
    if (count == 0) {
      obj.recycled();
      return true;
    } else if (count > 0) {
      return false;
    } else {
      throw new IllegalStateException("object @" + System.identityHashCode(obj) + " is already recycled");
    }
  }
  
  private String getName() {
    String cname = ((_class != null) ? _class.getName() : _factory.getClass().getName());
    // Displays only the class name and the topmost package name.
    
    int dot = cname.lastIndexOf(".");
    if (dot != -1 && dot > 0) {
      int dot2 = cname.lastIndexOf(".", dot - 1);
      if (dot2 != -1) {
        dot = dot2;
      }
      cname = cname.substring(dot + 1);
    }
    
    return "SafeObjectPool/" + cname;
  }
  
  /************************************* Private attributes *********************/
  
  private final static Logger _logger = Logger.getLogger("as.util.SafeObjectPool");
  private final Factory<T> _factory;
  private final Class<T> _class;
  private final ReentrantLock _lock = new ReentrantLock();
  private final Condition _notFull = _lock.newCondition();
  private final int _maxAcquired;
  private final ConcurrentLinkedQueue<WeakReference<T>> _pool = new ConcurrentLinkedQueue<WeakReference<T>>();
  private int _acquired;
}
