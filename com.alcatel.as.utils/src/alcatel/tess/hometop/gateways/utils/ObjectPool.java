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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * WeakReference based ObjectPool. Recyclable objects are gracefully reclaimed 
 * by the GC, using Weak references. 
 * 
 */
public class ObjectPool {
	
  public ObjectPool() {
	  this ((ObjectPoolFactory) null, -1);
  }
	  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param clazz a <code>Class</code> matching the class name of the objects beeing managed by this pool.
   * @exception ClassNotFoundException if the class can not be instanciated
   */
  public ObjectPool(Class clazz) throws ClassNotFoundException {
    this(clazz, -1);
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param clazz a <code>Class</code> matching the class name of the objects beeing managed by this pool.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *			created by the acquired method. The acquired method will block if too much instances
   *			are currently created.
   *			
   * @exception ClassNotFoundException if the class can not be instanciated
   */
  public ObjectPool(Class clazz, int maxAcquired) throws ClassNotFoundException {
    _maxAcquired = maxAcquired;
    _class = clazz;
    _factory = null;
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param clazz a <code>Class</code> matching the class name of the objects beeing managed by this pool.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *			created by the acquired method. The acquired method will block if too much instances
   *			are currently created.
   * @param cacheSize The max objects that will be cached by the release() method.
   *			
   * @exception ClassNotFoundException if the class can not be instanciated
   * @deprecated The cache size is no more used because we use weak references for automatic object
   *	         reclamation.
   */
  public ObjectPool(Class clazz, int maxAcquired, int cacheSize) throws ClassNotFoundException {
    this(clazz, maxAcquired);
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param clazz a <code>String</code> value matching the class name of the objects beeing managed by 
   * this pool.
   *
   * @exception ClassNotFoundException if the class can not be instanciated
   */
  public ObjectPool(String clazz) throws ClassNotFoundException {
    this(Class.forName(clazz));
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param clazz a <code>String</code> value matching the class name of the objects beeing managed by 
   * this pool.
   * @param cacheSize The max objects that will be cached by the release() method.
   *
   * @exception ClassNotFoundException if the class can not be instanciated
   * @deprecated The cache size is no more used because we uses weak references for automatic object
   *	         reclamation.
   */
  public ObjectPool(String clazz, int cacheSize) throws ClassNotFoundException {
    this(Class.forName(clazz), -1);
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param factory an <code>ObjectPoolFactory</code> value used to
   *	creates objects from the pool.
   */
  public ObjectPool(ObjectPoolFactory factory) {
    this(factory, -1);
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param factory an <code>ObjectPoolFactory</code> value used to
   *	creates objects from the pool.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *			created by the acquired method. The acquired method will block if too much instances
   *			are currently created.
   */
  public ObjectPool(ObjectPoolFactory factory, int maxAcquired) {
    _factory = factory;
    _class = null;
    _maxAcquired = maxAcquired;
  }
  
  /**
   * Creates a new <code>ObjectPool</code> instance.
   *
   * @param factory an <code>ObjectPoolFactory</code> value used to
   *	creates objects from the pool.
   * @param maxAcquired an <code>int</code> value representing the max number of instances allowed to be 
   *			created by the acquired method. The acquired method will block if too much instances
   *			are currently created.
   * @param cacheSize The max objects that will be cached by the release() method.
   *			
   * @deprecated The cache size is no more used because we uses weak references for automatic object
   *	         reclamation.
   */
  public ObjectPool(ObjectPoolFactory factory, int maxAcquired, int cacheSize) {
    this(factory, maxAcquired);
  }
  
  /**
   * The <code>acquire</code> method acquires an object from the pool. If the AbstractPool has been initialized 
   * with a max number of acquired instances, then this method will block until one other thread release an 
   * object.
   *
   * @return a <code>Recyclable</code> value
   */
  public Recyclable acquire() {
	  return acquire(null);
  }
  
  public Recyclable acquire(ObjectPoolFactory factory) {
    if (_maxAcquired == -1) {
      return doAcquire(factory);
    } else {
      Recyclable r;
      _lock.lock();
      
      try {
        while (_acquired >= _maxAcquired) {
          _notFull.await();
        }
        
        r = doAcquire(factory);
        _acquired++;
      }
      
      catch (InterruptedException e) {
        throw new RuntimeException("Operation interrupted");
      }
      
      finally {
        _lock.unlock();
      }
      
      return r;
    }
  }
  
  /**
   * The <code>acquire</code> method acquires an object from the pool. If the AbstractPool has been initialized 
   * with a max number of acquired instances, then this method will block until one other thread release an 
   * object.
   *
   * @param timeout an <code>int</code> value representing the max time in millis until a thread will wait 
   * for an available object from the pool.
   *
   * @return a <code>T</code> value or null if the instance could not be acquired timely.
   * @throws InterruptedException if the current thread has been interrupted by another.
   */
  public Recyclable acquire(long timeout) throws InterruptedException {
	  return acquire(timeout, null);
  }
  
  public Recyclable acquire(long timeout, ObjectPoolFactory factory) throws InterruptedException {
    if (_maxAcquired == -1) {
      return doAcquire(factory);
    } else {
      Recyclable r;
      _lock.lock();
      
      try {
        long start = (timeout <= 0) ? 0 : System.currentTimeMillis();
        long waitTime = timeout;
        
        while (_acquired >= _maxAcquired) {
          if (waitTime <= 0) {
            throw new InterruptedException("object pool allocation timed out");
          }
          
          _notFull.await(waitTime, TimeUnit.MILLISECONDS);
          waitTime = timeout - (System.currentTimeMillis() - start);
        }
        
        r = doAcquire(factory);
        _acquired++;
      }
      
      finally {
        _lock.unlock();
      }
      
      return r;
    }
  }
  
  public void release(Recyclable obj) {
    obj.recycled();
    
    if (_logger.isDebugEnabled()) {
      _logger.debug("released " + obj.getClass().getName() + "@" + System.identityHashCode(obj));
    }
    
    _pool.offer(new WeakReference(obj));
    
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
  
  public void releaseAll() {
  }
  
  public String toString() {
    return "size=" + _pool.size();
  }
  
  private Recyclable doAcquire(ObjectPoolFactory factory) {
    Queue<WeakReference> pool = _pool;
    WeakReference tref = null;
    Recyclable t;
    
    int loops = 0;
    do {
      t = null;
      while ((tref = pool.poll()) != null && (t = (Recyclable) tref.get()) == null) {
        loops++;
      }
    } while (t != null && !t.isValid());
    
    if (loops > 0 && _logger.isInfoEnabled()) {
      _logger.info(toString() + ",WeakRef iterations=" + loops);
    }
    
    if (t == null) {
      t = create(factory);
    }
    
    if (_logger.isDebugEnabled()) {
      _logger.debug("acquired " + t.getClass().getName() + "@" + System.identityHashCode(t));
    }
    
    return t;
  }
  
  private Recyclable create(ObjectPoolFactory factory) {
    try {
      if (_class != null) {
        return (Recyclable) _class.newInstance();
      } else {
    	if (factory == null) {
    		factory = _factory;
    	}
    	if (factory == null) {
    		throw new IllegalStateException("ObjectPoolFactory not set");
    	}
        return factory.newInstance();
      }
    }
    
    catch (Throwable t) {
      throw new RuntimeException("Can not instanciate object from " + toString(), t);
    }
  }
    
  private final static Logger _logger = Logger.getLogger("as.util.ObjectPool");
  private final Class<?> _class;
  private final ObjectPoolFactory _factory;
  private final ReentrantLock _lock = new ReentrantLock();
  private final Condition _notFull = _lock.newCondition();
  private final int _maxAcquired;
  private final ConcurrentLinkedQueue<WeakReference> _pool = new ConcurrentLinkedQueue<>();
  private int _acquired;
}
