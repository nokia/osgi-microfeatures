package alcatel.tess.hometop.gateways.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import alcatel.tess.hometop.gateways.utils.GetOpt;
import alcatel.tess.hometop.gateways.utils.Hashtable;
import alcatel.tess.hometop.gateways.utils.IntHashtable;
import alcatel.tess.hometop.gateways.utils.LongHashtable;

public class TestHashtable {
  
  /**
   * The <code>main</code> method tests hashtable serialization.
   *
   * @param args[] a <code>String</code> value
   * @exception Exception if an error occurs
   */
  public static void main(String args[]) throws Exception {
    try {
      GetOpt opt = new GetOpt(args, "n: i: f: hashmap intmap longmap");
      String arg = null;
      int max = -1, initialSize = -1;
      float loadFactor = -1f;
      boolean hashmap = false;
      boolean intmap = false;
      boolean longmap = false;
      
      while ((arg = opt.nextArg()) != null) {
        if (arg.equals("n")) {
          max = opt.nextInt();
          continue;
        }
        
        if (arg.equals("i")) {
          initialSize = opt.nextInt();
          continue;
        }
        
        if (arg.equals("f")) {
          loadFactor = opt.nextFloat();
          continue;
        }
        
        if (arg.equals("hashmap")) {
          hashmap = true;
          continue;
        }
        
        if (arg.equals("intmap")) {
          intmap = true;
          continue;
        }
        
        if (arg.equals("longmap")) {
          longmap = true;
          continue;
        }
        
        System.err.println("unknown option; " + arg);
      }
      
      if (max == -1) {
        System.err.println("Usage: " + Hashtable.class.getName()
            + " -i <initial size> -f <load factor> -n <number of puts>");
        System.exit(1);
      }
      
      // ------------------------- Load test -----------------------------------------------
      
      System.out.println("Lauching infinite load tests (press ctrl-c to terminate the test).");
      
      // First generate our random key/values ...
      
      HashMap h = new HashMap(initialSize, loadFactor);
      Integer[] keys = new Integer[max];
      Random rnd = new Random();
      
      for (int i = 0; i < max; i++) {
        do {
          //keys[i] = new Integer(rnd.nextInt(Integer.MAX_VALUE));
          keys[i] = new Integer(i);
        } while (h.get(keys[i]) != null);
        h.put(keys[i], keys[i]);
      }
      h.clear();
      
      if (hashmap) {
        testHashMap(initialSize, loadFactor, max, keys);
      } else if (intmap) {
        testIntMap(initialSize, loadFactor, max, keys);
      } else if (longmap) {
        testLongMap(initialSize, loadFactor, max, keys);
      } else {
        testNxMap(initialSize, loadFactor, max, keys);
      }
    }
    
    catch (Throwable t) {
      t.printStackTrace();
    }
  }
  
  private static void testHashMap(int initialSize, float loadFactor, int max, Integer[] keys) {
    System.out.println("Benching HashMap ...");
    
    HashMap h = new HashMap(initialSize, loadFactor);
    
    while (true) {
      h.clear();
      
      System.out.print("starting loadtest ... ");
      long start = System.currentTimeMillis();
      for (int i = 0; i < 1; i++) {
        loadtest(h, max, keys);
      }
      long end = System.currentTimeMillis();
      float duration = end - start;
      System.out.println(" test time=" + duration);
    }
  }
  
  private static void loadtest(HashMap h, int max, Integer[] keys) {
    byte[] v;
    long theValue;
    
    // Put n values.
    
    for (int i = 0; i < max; i++) {
      h.put(keys[i], keys[i]);
    }
    
    // Get n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i]);
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Remove n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.remove(keys[i]);
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Check if value is really removed.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i]);
      if (I != null) {
        fatal("value not removed: " + keys[i]);
      }
    }
    
    // Reinsert values and mesure the time spent when enumerating/removing all elements
    
    h.clear();
    for (int i = 0; i < max; i++) {
      h.put(keys[i], keys[i]);
    }
    
    int c = 0;
    Iterator it = h.keySet().iterator();
    
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      c++;
      it.remove();
      if (h.get(i) != null)
        new RuntimeException("removed " + i + " using enumerator, but value is still in the map");
    }
    
    // Ensure that the table is empty.
    
    it = h.keySet().iterator();
    if (it.hasNext()) {
      throw new RuntimeException("Hashtable not empty");
    }
  }
  
  private static void testNxMap(int initialSize, float loadFactor, int max, Integer[] keys) {
    System.out.println("Benching Nx Map ...");
    
    Hashtable h = new Hashtable(initialSize, loadFactor);
    
    while (true) {
      h.clear();
      
      System.out.print("starting loadtest ... ");
      long start = System.currentTimeMillis();
      for (int i = 0; i < 1; i++) {
        loadtest(h, max, keys);
      }
      long end = System.currentTimeMillis();
      float duration = end - start;
      System.out.println(" test time=" + duration);
    }
  }
  
  private static void loadtest(Hashtable h, int max, Integer[] keys) {
    byte[] v;
    long theValue;
    
    // Put n values.
    
    for (int i = 0; i < max; i++) {
      h.put(keys[i], keys[i]);
    }
    
    // Get n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i]);
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Remove n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.remove(keys[i]);
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Check if value is really removed.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i]);
      if (I != null) {
        fatal("value not removed: " + keys[i]);
      }
    }
    
    // Reinsert values and mesure the time spent when enumerating/removing all elements
    
    h.clear();
    for (int i = 0; i < max; i++) {
      h.put(keys[i], keys[i]);
    }
    if (h.size() != max)
      System.err.println("size=" + h.size());
    
    int c = 0;
    Hashtable.Enumerator e = (Hashtable.Enumerator) h.keys();
    
    while (e.hasMoreElements()) {
      Integer i = (Integer) e.nextElement();
      c++;
      e.remove();
      if (h.get(i) != null)
        new RuntimeException("removed " + i + " using enumerator, but value is still in the map");
    }
    
    // Ensure that the table is empty.
    e = (Hashtable.Enumerator) h.keys();
    if (e.hasMoreElements()) {
      throw new RuntimeException("Hashtable not empty");
    }
  }
  
  private static void testIntMap(int initialSize, float loadFactor, int max, Integer[] keys) {
    System.out.println("Benching Int Map ...");
    
    IntHashtable h = new IntHashtable(initialSize, loadFactor);
    
    while (true) {
      h.clear();
      
      System.out.print("starting loadtest ... ");
      long start = System.currentTimeMillis();
      for (int i = 0; i < 1; i++) {
        loadtest(h, max, keys);
      }
      long end = System.currentTimeMillis();
      float duration = end - start;
      System.out.println(" test time=" + duration);
    }
  }
  
  private static void loadtest(IntHashtable h, int max, Integer[] keys) {
    byte[] v;
    long theValue;
    
    // Put n values.
    
    for (int i = 0; i < max; i++) {
      h.put(keys[i].intValue(), keys[i]);
    }
    
    // Get n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i].intValue());
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Remove n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.remove(keys[i].intValue());
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Check if value is really removed.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i].intValue());
      if (I != null) {
        fatal("value not removed: " + keys[i]);
      }
    }
    
    // Reinsert values and mesure the time spent when enumerating/removing all elements
    
    h.clear();
    for (int i = 0; i < max; i++) {
      h.put(keys[i].intValue(), keys[i]);
    }
    if (h.size() != max)
      System.err.println("size=" + h.size());
    
    int c = 0;
    IntHashtable.KeyEnumerator e = (IntHashtable.KeyEnumerator) h.keys();
    
    while (e.hasMoreElements()) {
      int i = e.nextKey();
      c++;
      e.remove();
      if (h.get(i) != null)
        new RuntimeException("removed " + i + " using enumerator, but value is still in the map");
    }
    
    // Ensure that the table is empty.
    e = (IntHashtable.KeyEnumerator) h.keys();
    if (e.hasMoreElements()) {
      throw new RuntimeException("Hashtable not empty");
    }
  }
  
  private static void testLongMap(int initialSize, float loadFactor, int max, Integer[] keys) {
    System.out.println("Benching Long Map ...");
    
    LongHashtable h = new LongHashtable(initialSize, loadFactor);
    
    while (true) {
      h.clear();
      
      System.out.print("starting loadtest ... ");
      long start = System.currentTimeMillis();
      for (int i = 0; i < 1; i++) {
        loadtest(h, max, keys);
      }
      long end = System.currentTimeMillis();
      float duration = end - start;
      System.out.println(" test time=" + duration);
    }
  }
  
  private static void loadtest(LongHashtable h, int max, Integer[] keys) {
    byte[] v;
    long theValue;
    
    // Put n values.
    
    for (int i = 0; i < max; i++) {
      h.put(keys[i].intValue(), keys[i]);
    }
    
    // Get n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i].intValue());
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Remove n values.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.remove(keys[i].intValue());
      if (I == null || !I.equals(keys[i])) {
        fatal("did not find value for key " + keys[i]);
      }
    }
    
    // Check if value is really removed.
    
    for (int i = 0; i < max; i++) {
      Integer I = (Integer) h.get(keys[i].intValue());
      if (I != null) {
        fatal("value not removed: " + keys[i]);
      }
    }
    
    // Reinsert values and mesure the time spent when enumerating/removing all elements
    
    h.clear();
    for (int i = 0; i < max; i++) {
      h.put(keys[i].intValue(), keys[i]);
    }
    if (h.size() != max)
      System.err.println("size=" + h.size());
    
    int c = 0;
    LongHashtable.KeyEnumerator e = (LongHashtable.KeyEnumerator) h.keys();
    
    while (e.hasMoreElements()) {
      long i = e.nextKey();
      c++;
      e.remove();
      if (h.get(i) != null)
        new RuntimeException("removed " + i + " using enumerator, but value is still in the map");
    }
    
    // Ensure that the table is empty.
    e = (LongHashtable.KeyEnumerator) h.keys();
    if (e.hasMoreElements()) {
      throw new RuntimeException("Hashtable not empty");
    }
  }
  
  protected final static void fatal(String msg) {
    System.err.println(msg);
    Thread.dumpStack();
    System.exit(1);
  }
}
