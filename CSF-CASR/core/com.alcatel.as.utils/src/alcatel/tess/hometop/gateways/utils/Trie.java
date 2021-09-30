package alcatel.tess.hometop.gateways.utils;

/* 
 * Implementation of "Digital trie" usefull when needing to parse ASCII tokens at light speed ...
 * A Digital trie stores strings char by chars, and has a n-way branching, where n is the number of
 * possible chars in a string. (see Java Performance Tuning, page 348).
 *
 * Warning: this class is fast only for get operations, not for puts.
 * Moreover, keys are case insensitive ASCII strings (0 < chars < 128), and can't be empty.
 */
public class Trie {
  
  public static void main(String[] args) {
    Trie test = new Trie();
    for (int i = 0; i < args.length; i++) {
      test.put(args[i], args[i]);
    }
    for (int i = 0; i < args.length; i++) {
      System.out.println(test.get(args[i]));
    }
  }
  
  public Trie() {
    this.root = new Node();
  }
  
  /**
   * Add a key/value to the trie. 
   */
  public void put(String key, Object value) {
    int length = key.length();
    int charIndex = 0;
    char[] lc = Trie.lc;
    Node[] nodes = root.subtree;
    
    for (int i = 0; i < length; i++) {
      charIndex = lc[key.charAt(i)];
      if (charIndex >= 128) {
        throw new IllegalArgumentException("Trying to put a non-asckii key into a \"Trie\" table: " + key);
      }
      
      if (i < (length - 1)) {
        if (nodes[charIndex] == null) {
          nodes[charIndex] = new Node();
        }
        nodes = nodes[charIndex].subtree;
      } else {
        if (nodes[charIndex] == null) {
          nodes[charIndex] = new Node();
        }
        nodes[charIndex].value = value;
      }
    }
  }
  
  /**
   * Get the the value for a given key.
   * @param key The case insenstive ascii string key.
   * @return the value found, or null.
   */
  public Object get(String key) {
    int length = key.length();
    char[] lc = Trie.lc;
    Node[] nodes = this.root.subtree;
    Node node = null;
    
    for (int i = 0, charIndex = 0; i < length; i++) {
      charIndex = lc[key.charAt(i)];
      if (charIndex >= 128 || nodes == null || (node = nodes[charIndex]) == null) {
        return null;
      }
      nodes = nodes[charIndex].subtree;
    }
    
    return node.value;
  }
  
  /**
   * Get the the value for a given key.
   * @param key The case insenstive ascii string key.
   * @return the value found, or null.
   */
  public Object get(char[] key) {
    char[] lc = Trie.lc;
    Node[] nodes = this.root.subtree;
    Node node = null;
    
    for (int i = 0, charIndex = 0; i < key.length; i++) {
      charIndex = lc[key[i]];
      if (charIndex >= 128 || nodes == null || (node = nodes[charIndex]) == null) {
        return null;
      }
      nodes = nodes[charIndex].subtree;
    }
    
    return node.value;
  }
  
  /**
   * Get the the value for a given key.
   * @param key The case insenstive ascii string key.
   * @return the value found, or null.
   */
  public Object get(char[] key, int offset, int length) {
    char[] lc = Trie.lc;
    Node[] nodes = this.root.subtree;
    Node node = null;
    
    for (int i = offset, charIndex = 0; i < length; i++) {
      charIndex = lc[key[i]];
      if (charIndex >= 128 || nodes == null || (node = nodes[charIndex]) == null) {
        return null;
      }
      nodes = nodes[charIndex].subtree;
    }
    
    return node.value;
  }
  
  /** Our tree. */
  private Node root;
  
  /** mapping between chars and lowerchars */
  private static final char[] lc = new char[256];
  
  /** Initialize a buffer of lowercase chars */
  static {
    for (char c = 0; c < 256; c++)
      lc[c] = Character.toLowerCase(c);
  }
  
  static class Node {
    Node[] subtree;
    Object value;
    
    Node() {
      this.subtree = new Node[128];
    }
  }
}
