// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Fifo queue class. This class is <b>NOT </b> thread-safe.
 */
public class FifoQueue implements Serializable {
  /**
   * Creates a new Fifo queue.
   */
  public FifoQueue() {
    this(32);
  }
  
  /**
   * Creates a new Fifo queue.
   * @param initialCapacity The initial queue size.
   */
  public FifoQueue(int initialCapacity) {
    _initialCapacity = roundToPower(initialCapacity);
    _items = new Object[_initialCapacity];
  }
  
  /**
   * Extract the first element from this queue.
   *
   * @return null if queue is empty, or the first queue element.
   */
  public Object pop() {
    if (_size == 0) {
      return null;
    }
    
    Object ret = _items[_first];
    _items[_first] = null;
    _first = (_first + 1) & (_items.length - 1);
    _size--;
    
    return ret;
  }
  
  /**
   * Enqueue into this queue.
   */
  public boolean push(Object obj) {
    if (_size == _items.length) {
      // expand queue
      final int oldLen = _items.length;
      Object[] tmp = new Object[oldLen * 2];
      
      if (_first < _last) {
        System.arraycopy(_items, _first, tmp, 0, _last - _first);
      } else {
        System.arraycopy(_items, _first, tmp, 0, oldLen - _first);
        System.arraycopy(_items, 0, tmp, oldLen - _first, _last);
      }
      
      _first = 0;
      _last = oldLen;
      _items = tmp;
    }
    
    _items[_last] = obj;
    _last = (_last + 1) & (_items.length - 1);
    _size++;
    return true;
  }
  
  /**
   * Returns the first element of the queue.
   *
   * @return <code>null</code>, if the queue is empty, or the element is
   *         really <code>null</code>.
   */
  public Object peekFirst() {
    if (_size == 0) {
      return null;
    }
    
    return _items[_first];
  }
  
  /**
   * Clears this queue.
   */
  public void clear() {
    clear(false);
  }
  
  /**
   * Clears this queue.
   */
  public void clear(boolean resetCapacity) {
    if (_size > 0) {
      if (resetCapacity) {
        if (_items.length > _initialCapacity) {
          _items = new Object[_initialCapacity];
        } else {
          Arrays.fill(_items, null);
        }
      } else {
        Arrays.fill(_items, null);
      }
    }
    
    _first = 0;
    _last = 0;
    _size = 0;
  }
  
  /**
   * Returns <code>true</code> if the queue is empty.
   */
  public boolean isEmpty() {
    return (_size == 0);
  }
  
  /**
   * Returns the number of elements in the queue.
   */
  public int size() {
    return _size;
  }
  
  private int roundToPower(int n) {
    int p = 1;
    while (p < n)
      p <<= 1;
    return (p);
  }
  
  private Object[] _items;
  private int _initialCapacity;
  private int _first = 0;
  private int _last = 0;
  private int _size = 0;
}
