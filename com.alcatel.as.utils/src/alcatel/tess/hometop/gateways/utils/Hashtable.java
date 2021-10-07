package alcatel.tess.hometop.gateways.utils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A more efficient version of <code>java.util.Hashtable</code>
 * It is not synchronized and does not performs allocations in put
 * method. Only the rehash method will perform allocation.
 */
public class Hashtable extends Dictionary implements Externalizable, Cloneable {
  /**
   * Creates a new hashtable.
   */
  public Hashtable() {
    this(INIT_SIZE);
  }
  
  /**
   * Creates a hash table with the specified initial capacity.
   * @param the initial hashtable capacity
   */
  public Hashtable(int n) {
    this(n, 0.5f);
  }
  
  public Hashtable(int n, float loadFactor) {
    this.loadFactor = loadFactor;
    n = (int) (n / loadFactor);
    halfTableLength = Utils.round2(n);
    table = new Object[halfTableLength << 1];
    usedLimit = (int) (halfTableLength * loadFactor);
    
    if (DEBUG) {
      System.out.println("init: " + "loadFactor=" + loadFactor + ", table length=" + table.length
          + ", usedLimit=" + usedLimit + ", n=" + n + ", halfTableLength=" + halfTableLength);
    }
  }
  
  /**
   * Return the hashtable size.
   * @return the hashtable size.
   */
  public int size() {
    return (used);
  }
  
  /**
   * Is the hashtable empty ?
   * @return true if the hashtable is empty, false if not
   */
  public boolean isEmpty() {
    return (used == 0);
  }
  
  /**
   * Returns an enumeration of all hashtable keys.
   * @return an enumeration of all hashtable keys.
   */
  public Enumeration keys() {
    return new Enumerator(0);
  }
  
  /**
   * Returns an iterator of all hashtable keys.
   * @return an enumeration of all hashtable keys.
   */
  public Iterator keysIterator() {
    return new IteratorWrapper(0);
  }
  
  /**
   * Returns an enumeration of all hashtable value.
   * @return an enumeration of all hashtable value.
   */
  public Enumeration elements() {
    return new Enumerator(halfTableLength);
  }
  
  /**
   * Returns an iterator of all hashtable value.
   * @return an enumeration of all hashtable value.
   */
  public Iterator elementsIterator() {
    return new IteratorWrapper(halfTableLength);
  }
  
  /**
   * Removes all objects from the hash table, so that the hash table
   * becomes empty.
   */
  public void clear() {
    if (used > 0) {
      for (int i = 0; i < table.length; i++)
        table[i] = null;
      used = 0;
    }
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
  public String toString() {
    int max = size() - 1;
    StringBuffer buf = new StringBuffer();
    Enumeration e = keys();
    
    buf.append("{");
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      Object val = get(key);
      buf.append(key + "=" + val);
      if (e.hasMoreElements()) {
        buf.append(", ");
      }
    }
    buf.append("}");
    return buf.toString();
  }
  
  /**
   * Close this hashtable to another one
   */
  public Object clone() {
    try {
      Hashtable s = (Hashtable) super.clone();
      
      if (table != null) {
        s.table = new Object[table.length];
        System.arraycopy(table, 0, s.table, 0, table.length);
      }
      
      return s;
    }
    
    catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }
  
  /**
   * Return the value (if found) for a given key.
   * @return The found value, or null
   */
  public Object get(Object key) {
    Object[] table = this.table;
    
    if (used != 0) {
      for (int i = firstIndex(key); table[i] != null; i = nextIndex(i))
        if (equals(table[i], key))
          return table[i | halfTableLength];
    }
    return null;
  }
  
  /**
   * Puts the key value pair.
   * @param key the strcase key 
   * @param value the value
   * @return the key old value, or null
   */
  public Object put(Object key, Object value) {
    if (value == null)
      throw new NullPointerException();
    
    // Check if the key already exists and replace the old value
    
    Object table[] = this.table;
    int h = -1;
    
    for (h = firstIndex(key); table[h] != null; h = nextIndex(h)) {
      if (equals(key, table[h])) {
        h |= halfTableLength;
        Object tem = table[h];
        table[h] = value;
        return tem;
      }
    }
    
    // The key does not exists: we have to allocate a new entry.
    // But before that, check if we must rehash.
    
    if (used >= usedLimit) {
      table = this.table = rehash();
      
      // Search again a new free hashtable entry
      
      for (h = firstIndex(key); table[h] != null; h = nextIndex(h))
        ;
    }
    
    // Insert the new key in the free hashtable entry.
    
    used++;
    table[h] = key;
    table[h | halfTableLength] = value;
    return null;
  }
  
  /**
   * Removes the object with the specified key from the table.
   * Returns the object removed or null if there was no such object
   * in the table.
   */
  public Object remove(Object key) {
    Object[] table = this.table;
    
    if (used > 0) {
      for (int i = firstIndex(key); table[i] != null; i = nextIndex(i))
        if (equals(table[i], key)) {
          return (remove(table, i));
        }
    }
    
    return null;
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
    // Now write all persistent values, except those not serializable.
    //
    Enumeration keys = keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = get(key);
      
      if (key instanceof Serializable && value instanceof Serializable) {
        out.writeObject(key);
        out.writeObject(value);
      } else {
        out.writeObject(null);
      }
    }
    
    out.writeFloat(loadFactor);
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
    // Read our internal class attributes.
    //
    halfTableLength = in.readInt();
    int used = in.readInt();
    usedLimit = in.readInt();
    
    //
    // Allocate our hash table.
    //
    table = new Object[halfTableLength << 1];
    
    //
    // Read the persistent values into our hashtable.
    //
    for (int i = 0; i < used; i++) {
      Object key = in.readObject();
      if (key != null) {
        Object value = in.readObject();
        put(key, value);
      }
    }
    
    // Read loadFactor.
    try {
      loadFactor = in.readFloat();
    } catch (Exception e) {
    }
  }
  
  /**
   * Rehash
   */
  protected Object[] rehash() {
    halfTableLength = this.table.length;
    usedLimit = (int) (halfTableLength * loadFactor);
    
    Object[] oldTable = this.table;
    Object[] newTable = new Object[halfTableLength << 1];
    
    for (int i = oldTable.length >> 1; i > 0;) {
      --i;
      if (oldTable[i] != null) {
        int j;
        for (j = firstIndex(oldTable[i]); newTable[j] != null; j = nextIndex(j))
          ;
        newTable[j] = oldTable[i];
        newTable[j | halfTableLength] = oldTable[i + (oldTable.length >> 1)];
      }
    }
    
    if (DEBUG) {
      System.out.println("rehashing (used=" + used + ", usedLimit=" + usedLimit + ", length="
          + newTable.length + ", halfLength=" + halfTableLength + ")");
    }
    
    return (newTable);
  }
  
  protected int nextIndex(int i) {
    return i == 0 ? halfTableLength - 1 : i - 1;
  }
  
  protected int firstIndex(Object key) {
    // Multiple the hashCode by a prime (because our hashtable size of a power of two, not a prime number).
    int hash = key.hashCode() * 3;
    return hash & (halfTableLength - 1);
  }
  
  protected boolean equals(Object o1, Object o2) {
    return (o1.equals(o2));
  }
  
  protected Object remove(Object[] table, int i) {
    Object obj = table[i | halfTableLength];
    
    do {
      table[i] = null;
      table[i | halfTableLength] = null;
      int j = i;
      int r;
      do {
        i = nextIndex(i);
        if (table[i] == null)
          break;
        r = firstIndex(table[i]);
      } while ((i <= r && r < j) || (r < j && j < i) || (j < i && i <= r));
      table[j] = table[i];
      table[j | halfTableLength] = table[i | halfTableLength];
    } while (table[i] != null);
    
    --used;
    return (obj);
  }
  
  /**
   * Class used to enumerate hashtable value. Elements can be removed, like in
   * Iterators.
   */
  public class Enumerator implements Enumeration {
    private int pos;
    private int index;
    private int nextIndex;
    
    public Enumerator(int pos) {
      Object[] table = Hashtable.this.table;
      int nextIndex = table.length >> 1;
      while (--nextIndex >= 0 && table[nextIndex] == null)
        ;
      this.pos = pos;
      this.nextIndex = this.index = nextIndex;
    }
    
    public boolean hasMoreElements() {
      return (nextIndex >= 0);
    }
    
    public Object nextElement() {
      Object[] table = Hashtable.this.table;
      int nextIndex = this.nextIndex;
      
      if (nextIndex < 0) {
        throw new NoSuchElementException();
      }
      
      Object item = table[nextIndex + pos];
      while (--nextIndex >= 0 && table[nextIndex] == null)
        ;
      this.index = this.nextIndex;
      this.nextIndex = nextIndex;
      return (item);
    }
    
    /**
     * Removes from the underlying collection the last element returned by the Enumerator.
     * This method can be called only once per call to nextElement. 
     * The behavior of an iterator is unspecified if the underlying collection is modified while the iteration 
     * is in progress in any way other than by calling this method.
     */
    public void remove() {
      if (index < 0 || table[index] == null) {
        throw new NoSuchElementException();
      }
      
      Hashtable.this.remove(table, index);
      if (table[index] != null) {
        nextIndex = index;
      }
    }
  }
  
  /**
   * Hashtable iterator. Rather similar to Enumeration.
   */
  public class IteratorWrapper extends Enumerator implements Iterator {
    public IteratorWrapper(int pos) {
      super(pos);
    }
    
    public boolean hasNext() {
      return super.hasMoreElements();
    }
    
    public Object next() {
      return super.nextElement();
    }
  }
  
  /**
   * The first half of table contains the keys, the second half the values.
   * The value for a key at index i is at index i + halfTableLength
   */
  protected Object[] table;
  protected int halfTableLength;
  protected int used;
  protected int usedLimit;
  protected float loadFactor;
  protected static final int INIT_SIZE = 16;
  private final static boolean DEBUG = false;
}
