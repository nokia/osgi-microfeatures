// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

// Utils
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import alcatel.tess.hometop.gateways.utils.SafeObjectPool;

/**
 * This class shows how to use the SafeObjectPool pool.
 * It also shows how to use reference counting when releasing reusable objects.
 */
public class TestSafePool {
  
  static Logger _logger = Logger.getLogger(TestSafePool.class);
  
  /**
   * Our reusable object.
   */
  public static class MyRecyclable implements SafeObjectPool.Recyclable {
    private final SafeObjectPool.ReferenceCounter _refCount = new SafeObjectPool.ReferenceCounter();
    
    // Recyclable interface
    
    public boolean acquired() {
      return true;
    }
    
    public void recycled() {
    }
    
    public SafeObjectPool.ReferenceCounter getReferenceCounter() {
      return _refCount;
    }
  }
  
  public static void main(String args[]) throws Exception {
    if (args.length > 1) {
      Properties p = new Properties();
      p.load(new FileInputStream(args[0]));
      PropertyConfigurator.configure(p);
    }
    
    SafeObjectPool<MyRecyclable> pool = new SafeObjectPool<MyRecyclable>(MyRecyclable.class);
    System.out.println("Pool=" + pool);
    
    test1(pool);
    test2(pool);
    test3();
    LogManager.shutdown();
  }
  
  /**
   * Simple test: acquire an object, release it, and reuse it again ...
   */
  static void test1(SafeObjectPool<MyRecyclable> pool) {
    MyRecyclable t = pool.acquire();
    pool.release(t);
    MyRecyclable T = pool.acquire();
    if (t != T) {
      throw new RuntimeException("test1 Failed");
    }
    pool.release(T);
    System.out.println("test1 PASSED\n");
  }
  
  /**
   * Reference Counting test: acquire an object used by N threads.
   * Each thread releases the object, once the work is done.
   */
  static void test2(final SafeObjectPool<MyRecyclable> pool) throws InterruptedException {
    final MyRecyclable m = pool.acquire();
    
    // Starts two threads (each thread will use, and then release "t".
    
    Thread[] threads = new Thread[10];
    
    for (int i = 0; i < threads.length; i++) {
      m.getReferenceCounter().addReference();
      
      (threads[i] = new Thread(new Runnable() {
        public void run() {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
          }
          pool.release(m);
        }
      })).start();
    }
    
    pool.release(m);
    
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    
    MyRecyclable m2 = pool.acquire();
    if (m != m2) {
      throw new RuntimeException("test2 Failed");
    }
    pool.release(m2);
    System.out.println("test2 PASSED\n");
  }
  
  /**
   * Allocates N threads, but only permits N/2 object allocaction.
   */
  static void test3() throws InterruptedException {
    int maxInstances = 5;
    final SafeObjectPool<MyRecyclable> pool = new SafeObjectPool<MyRecyclable>(MyRecyclable.class,
        maxInstances);
    
    // Starts two threads (each thread will use, and then release "t".
    
    Thread[] threads = new Thread[maxInstances << 1];
    
    for (int i = 0; i < threads.length; i++) {
      (threads[i] = new Thread(new Runnable() {
        public void run() {
          try {
            MyRecyclable m = pool.acquire();
            System.out.println("Thread" + Thread.currentThread() + "/Acquired " + m.hashCode());
            Thread.sleep(100);
            System.out.println("Thread" + Thread.currentThread() + "/Released " + m.hashCode());
            pool.release(m);
          }
          
          catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }, "Thread/" + (i + 1))).start();
    }
    
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    
    System.out.println("test3 PASSED\n");
  }
}
