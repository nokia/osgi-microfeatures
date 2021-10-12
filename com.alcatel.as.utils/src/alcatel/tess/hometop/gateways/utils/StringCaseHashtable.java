// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.util.Enumeration;
import java.util.Random;

/**
 * This class implements an hashtable that stores string keys/values.
 * Keys are case insensitive strings.
 *
 */
public class StringCaseHashtable extends Hashtable {
  
  /**
   * Creates a hash table.
   */
  public StringCaseHashtable() {
    super();
  }
  
  /**
   * Creates a sized hash table.
   */
  public StringCaseHashtable(int n) {
    super(n);
  }
  
  /**
   * Returns the value to which the specified key is mapped in this hashtable.
   * The key is a case-insensitive string.
   *
   *
   * @param   key   a key in the hashtable.
   * @return  the value to which the key is mapped in this hashtable;
   *          <code>null</code> if the key is not mapped to any value in
   *          this hashtable.
   */
  public final String get(String key) {
    return ((String) super.get(key));
  }
  
  /**
   * Returns the object value to which the specified key is mapped in this hashtable.
   * The key is a case-insensitive string.
   *
   *
   * @param   key   a key in the hashtable.
   * @return  the Object value to which the key is mapped in this hashtable;
   *          <code>null</code> if the key is not mapped to any value in
   *          this hashtable.
   */
  public final Object getObject(String key) {
    return (super.get(key));
  }
  
  /**
   * Maps the specified <code>key</code> to the specified 
   * <code>value</code> in this hashtable. The key is case insensitive.
   *
   * @param key the strcase key
   * @param value the string value
   * @return the key old value, or null
   */
  public final String put(String key, String value) {
    return ((String) super.put(key, value));
  }
  
  /**
   * Maps the specified <code>key</code> to the specified 
   * Object <code>value</code> in this hashtable. The key is case insensitive.
   *
   * @param key the strcase key
   * @param value the Object value
   * @return the key old value, or null
   */
  public final Object putObject(String key, Object value) {
    return (super.put(key, value));
  }
  
  /**
   * Removes the key and its value from this hashtable.
   * The string key is case insensitive.
   *
   * @param key the strcase key
   * @param value the string value
   * @return the key old value, or null
   */
  public final String remove(String key) {
    return ((String) super.remove(key));
  }
  
  /**
   * Removes the key and its value from this hashtable.
   * The string key is case insensitive.
   *
   * @param key the strcase key
   * @return the removed Object value, or null
   */
  public final Object removeObject(String key) {
    return (super.remove(key));
  }
  
  /**
   * Test equality between two string keys (case ignored).
   */
  protected boolean equals(Object o1, Object o2) {
    return (((String) o1).equalsIgnoreCase((String) o2));
  }
  
  /**
   * Gets the hash code for the given string key.
   */
  protected final int firstIndex(Object key) {
    String s = (String) key;
    int hash = 0;
    char[] llc = lc;
    int len = s.length();
    char c;
    
    for (int idx = 0; idx < len; idx++) {
      if ((c = s.charAt(idx)) <= (char) 255) {
        hash = 31 * hash + llc[c];
      } else {
        hash = 31 * hash + Character.toLowerCase(c);
      }
    }
    
    return hash & (halfTableLength - 1);
  }
  
  /** mapping between chars and lowerchars */
  private static final char[] lc = new char[256];
  
  /** Initialize a buffer of lowercase chars */
  static {
    for (char idx = 0; idx < 256; idx++)
      lc[idx] = Character.toLowerCase(idx);
  }
  
  public static void main(String args[]) throws Exception {
    GetOpt opt = new GetOpt(args, "n:");
    String arg = null;
    int remCycle = -1;
    int max = -1;
    
    while ((arg = opt.nextArg()) != null) {
      if (arg.equals("n")) {
        max = opt.nextInt();
        continue;
      }
      
      if (arg.equals("r")) {
        remCycle = opt.nextInt();
        continue;
      }
      
      System.err.println("unknown option; " + arg);
    }
    
    if (max == -1 || remCycle == -1) {
      System.err.println("Usage: " + Hashtable.class.getName()
          + " -n <number of iterations> -r <remove cycle>");
      System.exit(1);
    }
    
    StringCaseHashtable h = new StringCaseHashtable();
    
    // ----------------------- Object storage tests
    
    System.out.println("----------------- starting get/put/remove Object test ...\n");
    
    Object obj = new Object();
    h.putObject("objkey", obj);
    
    Object o = h.getObject("ObjKey");
    if (o == null || !o.equals(obj)) {
      throw new RuntimeException("Error");
    }
    
    o = h.removeObject("OBJKEY");
    if (o == null || !o.equals(obj)) {
      throw new RuntimeException("Error");
    }
    
    o = h.getObject("objKEY");
    if (o != null) {
      throw new RuntimeException("Error");
    }
    
    System.out.println("ok");
    
    // ----------------------- StringCase tests
    
    System.out.println("\n----------------- starting strcase test ...\n");
    h.put("foo", "foo");
    
    if (h.get("foo") == null || h.get("foO") == null || h.get("FOo") == null || h.get("FOO") == null
        || h.get("Foo") == null || h.get("FoO") == null || h.get("FoO") == null || h.get("FOO") == null) {
      throw new RuntimeException("error");
    }
    
    String bar = (String) h.put("bAr", "bar");
    if (bar != null) {
      throw new RuntimeException("error");
    }
    
    bar = (String) h.get("BAR");
    if (bar == null || !bar.equals("bar")) {
      throw new RuntimeException("error");
    }
    
    String s = (String) h.put("BaR", "bar2");
    if (s == null || !s.equals("bar")) {
      throw new RuntimeException("error");
    }
    
    s = (String) h.get("BAR");
    if (s == null || !s.equals("bar2")) {
      throw new RuntimeException("error");
    }
    
    s = (String) h.remove("bar");
    if (s == null || !s.equals("bar2")) {
      throw new RuntimeException("error");
    }
    
    System.out.println(h.toString());
    
    StringCaseHashtable h2 = (StringCaseHashtable) h.clone();
    System.out.println("clone: " + h2.toString());
    h.clear();
    
    h.put("k1", "v1");
    h.put("k2", "v2");
    
    Enumeration e = h.keys();
    while (e.hasMoreElements()) {
      String k = (String) e.nextElement();
      String v = (String) h.get(k);
      System.out.println(k + ":" + v);
    }
    
    // ----------------------- Load tests
    
    System.out.println("\n----------------- starting load test (iteration=" + max + ") ...\n");
    Character.toLowerCase('A');
    
    long start = System.currentTimeMillis();
    
    h = new StringCaseHashtable();
    Random rnd = new Random();
    System.out.println("will test remove at every " + remCycle + " puts.");
    
    // insert random values.
    
    System.out.println("puting ...");
    String[] keys = new String[max];
    
    for (int i = 0, j = 0; i < max; i++, j++) {
      String k;
      while (h.get(k = String.valueOf(rnd.nextInt())) != null)
        ;
      keys[i] = k;
      
      String value = String.valueOf(keys[i]);
      h.put(keys[i], value);
      
      if (j % remCycle == 0 && j != 0) {
        if (h.remove(keys[i]) == null) {
          throw new RuntimeException("failed to remove " + keys[i]);
        }
        h.put(keys[i], value);
        j = 0;
      }
    }
    
    System.out.println("removing all ...");
    
    for (int i = 0; i < max; i++) {
      if (h.remove(keys[i]) == null) {
        throw new RuntimeException("failed to remove " + keys[i]);
      }
    }
    
    System.out.println("done: h=" + h);
    
    System.out.println("test time (in millis): " + String.valueOf(System.currentTimeMillis() - start));
  }
}
