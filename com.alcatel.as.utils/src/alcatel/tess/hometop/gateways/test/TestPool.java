// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ObjectPool;
import alcatel.tess.hometop.gateways.utils.ObjectPoolFactory;
import alcatel.tess.hometop.gateways.utils.Recyclable;

/**
 * The class <code>TestPool</code> tests the ObjectPool class.
 *
 */
public class TestPool implements Recyclable {
  
  static Logger _logger = Logger.getLogger("test");
  
  public static void main(String args[]) throws Exception {
    //
    // Perform some allocations on the Test1 inner class.
    //
    System.out.println("\nTEST1");
    ObjectPool pool = new ObjectPool(TestPool.class);
    TestPool t = (TestPool) pool.acquire();
    pool.release(t);
    TestPool T = (TestPool) pool.acquire();
    if (t != T) {
      throw new RuntimeException("Failed");
    }
    pool.release(T);
    
    System.out.println("\nTEST2");
    //
    // Same test as above, but using a special factory for
    // Test1 instanciation.
    //
    pool = new ObjectPool(new Factory());
    t = (TestPool) pool.acquire();
    pool.release(t);
    T = (TestPool) pool.acquire();
    if (t != T) {
      throw new RuntimeException("Failed2");
    }
    pool.release(T);
  }
  
  /********************************* Recyclable interface *********************/
  
  public void recycled() {
    System.out.println("Recycling " + toString());
  }
  
  public boolean isValid() {
    System.out.println("isValid " + toString());
    return true;
  }
  
  /**
   * The class <code>TestPoolFactory</code> is used to allocate
   * <code>TestPool</code> instances.
   */
  public static class Factory implements ObjectPoolFactory {
    public Recyclable newInstance() {
      return (new TestPool());
    }
  }
}
