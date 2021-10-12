// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.util;

// Jdk
import java.nio.ByteBuffer;

/**
 * Array of byte buffer.
 */
@Deprecated
public class ByteBufferArray {
  /**
   * Constructor.
   */
  public ByteBufferArray() {
    this(16);
  }
  
  /**
   * Constructor.
   */
  public ByteBufferArray(int initialCapacity) {
    this.array = new ByteBuffer[initialCapacity];
  }
  
  /**
   * Trims the capacity of this <tt>ByteBufferArray</tt> instance to be the list's current
   * size. An application can use this operation to minimize the storage of an
   * <tt>ByteBufferArray</tt> instance.
   */
  public void trimToSize() {
    int oldCapacity = array.length;
    if (size < oldCapacity) {
      ByteBuffer oldData[] = array;
      array = new ByteBuffer[size];
      System.arraycopy(oldData, 0, array, 0, size);
    }
  }
  
  /**
   * Increases the capacity of this <tt>ByteBufferArray</tt> instance, if necessary, to ensure
   * that it can hold at least the number of elements specified by the minimum capacity
   * argument.
   * 
   * @param minCapacity the desired minimum capacity.
   */
  public void ensureCapacity(int minCapacity) {
    int oldCapacity = array.length;
    if (minCapacity > oldCapacity) {
      ByteBuffer oldData[] = array;
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if (newCapacity < minCapacity)
        newCapacity = minCapacity;
      array = new ByteBuffer[newCapacity];
      System.arraycopy(oldData, 0, array, 0, size);
    }
  }
  
  /**
   * Returns the number of elements in this list.
   * 
   * @return the number of elements in this list.
   */
  public int size() {
    return size;
  }
  
  /**
   * Returns an array containing all of the elements in this list in the correct order.
   * 
   * @return an array containing all of the elements in this list in the correct order.
   */
  public ByteBuffer[] toArray(boolean copy) {
    if (copy) {
      ByteBuffer[] result = new ByteBuffer[size];
      System.arraycopy(array, 0, result, 0, size);
      return result;
    }
    return (array);
  }
  
  /**
   * Returns the element at the specified position in this list.
   * 
   * @param index index of element to return.
   * @return the element at the specified position in this list.
   * @throws IndexOutOfBoundsException if index is out of range <tt>(index
   * 		  &lt; 0 || index &gt;= size())</tt>.
   */
  public ByteBuffer get(int index) {
    return array[index];
  }
  
  /**
   * Replaces the element at the specified position in this list with the specified element.
   * 
   * @param index index of element to replace.
   * @param element element to be stored at the specified position.
   * @return the element previously at the specified position.
   * @throws IndexOutOfBoundsException if index out of range
   *             <tt>(index &lt; 0 || index &gt;= size())</tt>.
   */
  public ByteBuffer set(int index, ByteBuffer element) {
    ByteBuffer oldValue = array[index];
    array[index] = element;
    return oldValue;
  }
  
  /**
   * Appends the specified element to the end of this list.
   * 
   * @param o element to be appended to this list.
   * @return <tt>true</tt> (as per the general contract of Collection.add).
   */
  public boolean add(ByteBuffer o) {
    ensureCapacity(size + 1); // Increments modCount!!
    array[size++] = o;
    return true;
  }
  
  /**
   * Removes the element at the specified position in this list. Shifts any subsequent
   * elements to the left (subtracts one from their indices).
   * 
   * @param index the index of the element to removed.
   * @return the element that was removed from the list.
   * @throws IndexOutOfBoundsException if index out of range <tt>(index
   * 		  &lt; 0 || index &gt;= size())</tt>.
   */
  public ByteBuffer remove(int index) {
    ByteBuffer oldValue = array[index];
    
    int numMoved = size - index - 1;
    if (numMoved > 0)
      System.arraycopy(array, index + 1, array, index, numMoved);
    array[--size] = null; // Let gc do its work
    return oldValue;
  }
  
  /**
   * Removes all of the elements from this list. The list will be empty after this call
   * returns.
   */
  public void clear() {
    // Let gc do its work
    for (int i = 0; i < size; i++)
      array[i] = null;
    
    size = 0;
  }
  
  public void removeRange(int fromIndex, int toIndex) {
    int numMoved = size - toIndex;
    System.arraycopy(array, toIndex, array, fromIndex, numMoved);
    
    // Let gc do its work
    int newSize = size - (toIndex - fromIndex);
    while (size != newSize)
      array[--size] = null;
  }
  
  private ByteBuffer array[];
  private int size;
}
