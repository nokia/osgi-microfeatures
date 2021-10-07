package com.alcatel.as.service.concurrent.impl;

import java.util.concurrent.Executor;

import com.alcatel.as.service.concurrent.PlatformExecutors;

/**
 * Pool of Queue Executors. This class is used to reuse a pool of serial queue executors,
 * and allows to implement the {@link PlatformExecutors#getProcessingThreadPoolExecutor(Object queue)}
 * and {@link PlatformExecutors#getThreadPoolExecutor(Object queue)} methods.
 * The queue method parameter is hashed in order to pick up a QueueExecutor from the pool.
 */
public class QueueExecutorPool {
  /**
   * The queues table, Length must always be a power of two.
   */
  private final QueueExecutor[] _queues;
  
  /**
   * Creates a new QueueExecutor instance.
   * 
   * @param tpool the internal thread pool used to execute queues
   * @param tpoolPri the queues will be executed in the tpool using this priority
   * @param maxQueues the max number of queues to maintain.
   */
  public QueueExecutorPool(ThreadPoolBase tpool, int maxQueues, Meters meters) {
    // Find a power of 2 >= maxQueues
    int capacity = 1;
    while (capacity < maxQueues)
      capacity <<= 1;
    _queues = new QueueExecutor[capacity];
    for (int i = 0; i < _queues.length; i++) {
      _queues[i] = new QueueExecutor(tpool, null, meters);
    }
  }
  
  /**
   * Runs a task on a specific queue.
   * 
   * @param queue an actor queue identifier. If the queue is a String, then 
   * the String.hashCode method can be used as the identifier.
   * @param task the actor task.
   * @return the QueueExecutor whose match the hashcode of the <code>queue</code> 
   * parameter
   */
  public QueueExecutor getQueueExecutorFor(Object queue) {
    int hash = hash(queue.hashCode()); // defends against poor quality hash
    int i = indexFor(hash, _queues.length);
    return _queues[i];
  }
  
  /**
   * Applies a supplemental hash function to a given actor hashCode, which
   * defends against poor quality hash functions.  This is critical
   * because we are using power-of-two length hash tables, that
   * otherwise encounter collisions for hashCodes that do not differ
   * in lower bits.
   */
  private int hash(int h) {
    // This function ensures that hashCodes that differ only by
    // constant multiples at each bit position have a bounded
    // number of collisions (approximately 8 at default load factor).
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
  }
  
  /**
   * Returns index for hash code h. Here, we'are doing a modulo using a magic 
   * optimized bitwize operation because we know that our table length is a power of two. 
   */
  private int indexFor(int h, int length) {
    return h & (length - 1);
  }
}
