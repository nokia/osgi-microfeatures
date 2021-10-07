package alcatel.tess.hometop.gateways.concurrent;

public interface QueueIF {
  /**
   * Return the current number of elements stored in this queue.
   */
  public int size();
  
  /**
   * Clear all elements from this queue.
   */
  public void clear();
  
  /** 
   * Place item in the channel, possibly waiting indefinitely until
   * it can be accepted. Channels implementing the BoundedChannel
   * subinterface are generally guaranteed to block on puts upon
   * reaching capacity, but other implementations may or may not block.
   * @param item the element to be inserted. Should be non-null.
   * @exception InterruptedException if the current thread has
   * been interrupted at a point at which interruption
   * is detected, in which case the element is guaranteed not
   * to be inserted. Otherwise, on normal return, the element is guaranteed
   * to have been inserted.
   */
  public void put(Object x) throws InterruptedException;
  
  /** 
   * Place item in the channel, without blocking.
   * @return true if the item has been inserted, false if the queue if full.
   */
  public boolean offer(Object x);
  
  /** 
   * Return and remove an item from channel, 
   * possibly waiting indefinitely until
   * such an item exists.
   * @return  some item from the channel. Different implementations
   *  may guarantee various properties (such as FIFO) about that item
   * @exception InterruptedException if the current thread has
   * been interrupted at a point at which interruption
   * is detected, in which case state of the channel is unchanged.
   *
   */
  public Object take() throws InterruptedException;
  
  /** 
   * Return and remove several items from channel, possibly waiting indefinitely until
   * such an item exists. 
   *
   * @param out The buffer used to retrieve items
   * @return the number of items read from the channel. 
   * @exception InterruptedException if the current thread has
   * been interrupted at a point at which interruption
   * is detected, in which case state of the channel is unchanged.
   */
  public int take(Object[] out) throws InterruptedException;
  
  /** 
   * Return and remove an item from channel only if one is available within
   * msecs milliseconds. The time bound is interpreted in a coarse
   * grained, best-effort fashion.
   *
   * @param msecs the number of milliseconds to wait. If less than
   *  or equal to zero, the operation does not perform any timed waits,
   * but might still require
   * access to a synchronization lock, which can impose unbounded
   * delay if there is a lot of contention for the channel.
   * @return some item, or null if the channel is empty.
   * @exception InterruptedException if the current thread has
   * been interrupted at a point at which interruption
   * is detected, in which case state of the channel is unchanged
   * (i.e., equivalent to a null return).
   */
  public Object poll(long msecs) throws InterruptedException;
  
  /** 
   * Return and remove several items from channel only if one is available within
   * msecs milliseconds. The time bound is interpreted in a coarse
   * grained, best-effort fashion.
   *
   * @param out The buffer used to retrieve items
   * @param msecs the number of milliseconds to wait. If less than
   *  or equal to zero, the operation does not perform any timed waits,
   * but might still require
   * access to a synchronization lock, which can impose unbounded
   * delay if there is a lot of contention for the channel.
   * @return some item, or null if the channel is empty.
   * @exception InterruptedException if the current thread has
   * been interrupted at a point at which interruption
   * is detected, in which case state of the channel is unchanged
   * (i.e., equivalent to a null return).
   */
  public int poll(Object[] out, long msecs) throws InterruptedException;
}
