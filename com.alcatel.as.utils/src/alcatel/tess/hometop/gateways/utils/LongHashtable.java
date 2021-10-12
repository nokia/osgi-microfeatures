// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Integer key version of the Hashtable.
 * WARNING: -1 is a forbiden key.
 */
public class LongHashtable implements Externalizable {
  public final static long NULL_KEY = -1;
  
  public LongHashtable() {
    this(INIT_SIZE, 0.5f);
  }
  
  /**
   * Creates a hash table with the specified initial capacity.
   */
  public LongHashtable(int n) {
    this(n, 0.5f);
  }
  
  public LongHashtable(int n, float loadFactor) {
    n = (int) (n / LOAD_FACTOR);
    halfTableLength = Utils.round2(n);
    table = new Object[halfTableLength];
    keys = new long[halfTableLength];
    if (NULL_KEY != 0)
      nullifyKeys(keys);
    usedLimit = (int) (n * LOAD_FACTOR);
  }
  
  public final int size() {
    return used;
  }
  
  public final boolean isEmpty() {
    return used == 0;
  }
  
  public final KeyEnumerator keys() {
    return new KeyEnumerator();
  }
  
  public final Enumeration elements() {
    return new ValueEnumerator();
  }
  
  public final Object get(long key) {
    if (key == NULL_KEY)
      throw new IllegalArgumentException("invalid key (" + key + ")");
    
    long[] keys = this.keys;
    
    if (used != 0) {
      for (int i = firstIndex(key); keys[i] != NULL_KEY; i = nextIndex(i))
        if (keys[i] == key)
          return table[i];
    }
    
    return null;
  }
  
  public final Object put(long key, Object value) {
    int h;
    long[] keys = this.keys;
    Object[] table = this.table;
    
    if (value == null || key == NULL_KEY)
      throw new IllegalArgumentException("key or value (" + key + "," + value);
    
    for (h = firstIndex(key); keys[h] != NULL_KEY; h = nextIndex(h)) {
      if (key == keys[h]) {
        Object item = table[h];
        table[h] = value;
        return item;
      }
    }
    
    if (used >= usedLimit) {
      // rehash ...
      // System.out.println ("rehashing (used=" + used + ", usedLimit=" + usedLimit + ", halfLength=" + halfTableLength);
      
      halfTableLength = table.length << 1;
      usedLimit = (int) (halfTableLength * LOAD_FACTOR);
      Object[] oldTable = table;
      long[] oldKeys = keys;
      
      this.table = table = new Object[halfTableLength];
      this.keys = keys = new long[halfTableLength];
      if (NULL_KEY != 0)
        nullifyKeys(keys);
      
      for (int i = oldTable.length; i > 0;) {
        --i;
        if (oldKeys[i] != NULL_KEY) {
          int j;
          for (j = firstIndex(oldKeys[i]); keys[j] != NULL_KEY; j = nextIndex(j))
            ;
          keys[j] = oldKeys[i];
          // copy the value
          table[j] = oldTable[i];
        }
      }
      
      for (h = firstIndex(key); keys[h] != NULL_KEY; h = nextIndex(h))
        ;
    }
    
    used++;
    keys[h] = key;
    table[h] = value;
    return null;
  }
  
  /**
   * Removes the object with the specified key from the table.
   * Returns the object removed or null if there was no such object
   * in the table.
   */
  public final Object remove(long key) {
    if (key == NULL_KEY)
      throw new IllegalArgumentException("invalid key (" + key + ")");
    
    long[] keys = this.keys;
    
    if (used > 0) {
      for (int i = firstIndex(key); keys[i] != NULL_KEY; i = nextIndex(i))
        if (keys[i] == key) {
          return (removeFrom(i));
        }
    }
    return null;
  }
  
  /**
   * Removes all objects from the hash table, so that the hash table
   * becomes empty.
   */
  public final void clear() {
    nullifyKeys(keys);
    Utils.clearArray(table);
    used = 0;
  }
  
  /**
   * Returns a string representation of this <tt>Hashtable</tt> object 
   * in the form of a set of entries, enclosed in braces and separated 
   * by the ASCII characters "<tt>,&nbsp;</tt>" (comma and space). Each 
   * entry is rendered as the key, an equals sign <tt>=</tt>, and the 
   * associated element, where the <tt>toString</tt> method is used to 
   * convert the key and element to strings. <p>Overrides to 
   * <tt>toString</tt> method of <tt>Object</tt>.
   *
   * @return  a string representation of this hashtable.
   */
  public synchronized String toString() {
    StringBuffer buf = new StringBuffer();
    int i = halfTableLength;
    
    buf.append("[");
    
    while (--i >= 0) {
      if (keys[i] != NULL_KEY) {
        
        if (buf.length() > 1) {
          buf.append(", ");
        }
        
        buf.append(keys[i] + "=" + table[i]);
      }
    }
    
    buf.append("]");
    return buf.toString();
  }
  
  /**
   * The <code>writeExternal</code> method writes the current hashtable state into
   * an output stream.
   *
   * @param out an <code>ObjectOutput</code> value
   * @exception IOException if an error occurs
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    //
    // Write our attributes.
    //
    out.writeInt(halfTableLength);
    out.writeInt(used);
    out.writeInt(usedLimit);
    
    //
    // Now write all persistent values.
    //
    long[] keys = this.keys;
    
    for (int i = 0, j = 0; i < keys.length; i++) {
      if (keys[i] != NULL_KEY) {
        long key = keys[i];
        Object value = get(key);
        
        if (value instanceof Serializable) {
          out.writeLong(key);
          out.writeObject(value);
        } else {
          out.writeLong(NULL_KEY);
        }
      }
    }
  }
  
  /**
   * The <code>readExternal</code> method reconstruct an hashtable from
   * an input stream.
   *
   * @param out an <code>ObjectOutput</code> value
   * @exception IOException if an error occurs
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    //
    // Read the hashtable length
    //
    halfTableLength = in.readInt();
    
    //
    // Read the number of entries. Notice that we do not store this value
    // into this.used because put() method will adjust it (see bellow).
    //
    int used = in.readInt();
    
    //
    // Get the rehashing parameters.
    //
    usedLimit = in.readInt();
    
    //
    // Allocates our hashtables.
    keys = new long[halfTableLength];
    if (NULL_KEY != 0)
      nullifyKeys(keys);
    table = new Object[halfTableLength];
    
    //
    // Read the number of persistent values to restore.
    //
    for (int i = 0; i < used; i++) {
      long key = in.readLong();
      if (key != NULL_KEY) {
        Object value = in.readObject();
        put(key, value);
      }
    }
  }
  
  private Object removeFrom(int i) {
    long[] keys = this.keys;
    Object[] table = this.table;
    Object obj = table[i];
    
    do {
      table[i] = null;
      keys[i] = NULL_KEY;
      
      int j = i;
      int r;
      do {
        i = nextIndex(i);
        if (keys[i] == NULL_KEY)
          break;
        r = firstIndex(keys[i]);
      } while ((i <= r && r < j) || (r < j && j < i) || (j < i && i <= r));
      
      keys[j] = keys[i];
      table[j] = table[i];
    } while (keys[i] != NULL_KEY);
    
    --used;
    return obj;
  }
  
  private final int nextIndex(int i) {
    return i == 0 ? halfTableLength - 1 : i - 1;
  }
  
  private final int firstIndex(long key) {
    // Multiple the hashCode by a prime (because our hashtable size of a power of two, not a prime number).
    long hash = key * 3;
    return ((int) (hash & ((long) halfTableLength - 1)));
  }
  
  private static void nullifyKeys(long[] array) {
    int n = array.length;
    ;
    
    while (n > 0) {
      System.arraycopy(NULL_ARRAY, 0, array, array.length - n, Math.min(n, NULL_ARRAY.length));
      n -= NULL_ARRAY.length;
    }
  }
  
  /**
   * Class used to enumerate hashtable keys. Elements can be removed, like in
   * Iterators.
   */
  public class KeyEnumerator {
    protected int index, nextIndex;
    
    KeyEnumerator() {
      long[] keys = LongHashtable.this.keys;
      int nextIndex = keys.length;
      while (--nextIndex >= 0 && keys[nextIndex] == NULL_KEY)
        ;
      this.nextIndex = this.index = nextIndex;
    }
    
    public boolean hasMoreElements() {
      return (nextIndex >= 0);
    }
    
    public long nextKey() {
      long[] keys = LongHashtable.this.keys;
      int nextIndex = this.nextIndex;
      
      if (nextIndex < 0)
        throw new NoSuchElementException();
      
      long key = keys[nextIndex];
      while (--nextIndex >= 0 && keys[nextIndex] == NULL_KEY)
        ;
      this.index = this.nextIndex;
      this.nextIndex = nextIndex;
      return (key);
    }
    
    public Object nextElement() {
      return (new Long(nextKey()));
    }
    
    /**
     * Removes from the underlying collection the last element returned by the Enumerator.
     * This method can be called only once per call to nextElement. 
     * The behavior of an iterator is unspecified if the underlying collection is modified while the iteration 
     * is in progress in any way other than by calling this method.
     */
    public void remove() {
      if (index < 0) {
        throw new NoSuchElementException();
      }
      
      removeFrom(index);
      if (keys[index] != NULL_KEY) {
        nextIndex = index;
      }
    }
  }
  
  /**
   * Class used to enumerate hashtable values. Elements can be removed, like in
   * Iterators.
   */
  public class ValueEnumerator extends KeyEnumerator implements Enumeration {
    ValueEnumerator() {
    }
    
    public Object nextElement() {
      if (nextIndex < 0)
        throw new NoSuchElementException();
      
      Object item = table[nextIndex];
      nextKey();
      return (item);
    }
  }
  
  private long[] keys;
  private Object[] table;
  private int halfTableLength;
  private int used;
  private int usedLimit;
  private static final int INIT_SIZE = 16;
  private static final float LOAD_FACTOR = 0.5f;
  private static final long[] NULL_ARRAY = new long[128];
  
  static {
    for (int i = 0; i < NULL_ARRAY.length; i++)
      NULL_ARRAY[i] = NULL_KEY;
  }
}
