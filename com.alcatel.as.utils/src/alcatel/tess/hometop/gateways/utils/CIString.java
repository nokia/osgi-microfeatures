package alcatel.tess.hometop.gateways.utils;

/**
 * Case Insensitive string. 
 */
public class CIString {
  /**
   * Creates a new case insentive string.
   */
  public CIString(String s) {
    char llc[] = lc;
    int len = s.length();
    int hash = 0;
    
    for (int idx = 0; idx < len; idx++)
      hash = 31 * hash + llc[s.charAt(idx)];
    
    this.hash = hash;
    this.string = s;
  }
  
  /**
   * Compares this Case insentive string with a string.
   * @param obj the object with which to compare in a case insensitive manner.
   *		The obj should be a String or a CIString
   */
  public boolean equals(Object obj) {
    if (obj instanceof CIString)
      return string.equalsIgnoreCase(((CIString) obj).string);
    
    if (obj instanceof String)
      return string.equalsIgnoreCase((String) obj);
    
    return false;
  }
  
  /** 
   * Return a case insensive hashcode.
   * @return a case insensive hashcode.
   */
  public int hashCode() {
    return hash;
  }
  
  /**
   * Return the case insentitive string.
   * @return the case insentitive string.
   */
  public String toString() {
    return string;
  }
  
  /** the string */
  private final String string;
  
  /** the hash code */
  private final int hash;
  
  /** mapping between chars and lowerchars */
  private static final char[] lc = new char[256];
  
  /** Initialize a buffer of lowercase chars */
  static {
    for (char idx = 0; idx < 256; idx++)
      lc[idx] = Character.toLowerCase(idx);
  }
}
