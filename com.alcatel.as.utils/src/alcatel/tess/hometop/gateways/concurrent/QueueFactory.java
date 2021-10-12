// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.concurrent;

import java.lang.reflect.Constructor;

/**
 * Factory used to instanciate queues.
 */
public class QueueFactory {
  public final static String CNF_QUEUE_IMPL = "system.queue.kind";
  public final static String CNF_QUEUE_LIMIT = "system.queue.limit"; // -1 = unlimited.
  
  public static QueueIF create(String impl, int capacity) {
    return create();
  }
  
  public static QueueIF create(int capacity) {
    return create();
  }
  
  public static QueueIF create() {
    String impl = System.getProperty(CNF_QUEUE_IMPL, LinkedQueue.class.getName());
    String cap = System.getProperty(CNF_QUEUE_LIMIT, "-1");
    Integer capacity;
    
    try {
      capacity = Integer.valueOf(cap);
    }
    
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("property " + CNF_QUEUE_LIMIT + " must be an integer value: " + cap);
    }
    
    try {
      Class clazz = Class.forName(impl);
      Constructor constr = clazz.getConstructor(new Class[] { Integer.TYPE });
      return (QueueIF) constr.newInstance(new Object[] { capacity });
    }
    
    catch (Throwable t) {
      throw new RuntimeException("Can not create queue " + impl, t);
    }
  }
}
