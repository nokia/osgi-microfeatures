package alcatel.tess.hometop.gateways.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A class to represent a pool of regular expressions.  A string
 * can be matched against the whole pool all at once. 
 * For now, only handles regexp strings that either begin or end with
 * a '*'.
 */
public class RegexpPool {
  public static void main(String args[]) throws Exception {
    RegexpPool regexps = new RegexpPool();
    
    regexps.add("gabuzo", "foo1");
    regexps.add("gabu*", "foo2");
    regexps.add("*zo", "foo3");
    regexps.add("ga*zo", "foo4");
    regexps.add("*", "foo5");
    regexps.add("babar", "bar");
    
    Enumeration e = regexps.match("gabuzo");
    
    while (e.hasMoreElements()) {
      System.out.println(e.nextElement() + " match ! ");
    }
  }
  
  /** 
   * Add a regular expression to the pool of regular expressions.
   * @param	regexp  The regular expression to add to the pool.
   * For now, only handles strings that either begin or end with
   * a '*'.
   *
   * @param	ret The object to be returned when this regular expression is
   * matched.  If ret is an instance of the RegexpTarget class, ret.found
   * is called with the string fragment that matched the '*' as its 
   * parameter.
   */
  public void add(String regexp, Object ret) {
    ArrayList arr = (ArrayList) _regexps.get(regexp);
    if (arr == null) {
      arr = new ArrayList();
      _regexps.put(regexp, arr);
    }
    
    if (!arr.contains(ret)) {
      arr.add(ret);
    }
  }
  
  /**
   * Delete the regular expression and its target. 
   * @param regexp The regular expression to be deleted from the pool.
   *           must begin or end with a '*'
   * @return target - the old target.
   */
  public Object delete(String regexp, Object ret) {
    Object old = null;
    
    ArrayList arr = (ArrayList) _regexps.get(regexp);
    if (arr != null) {
      old = arr.remove(ret);
      if (arr.size() == 0) {
        _regexps.remove(regexp);
      }
    }
    return old;
  }
  
  public Enumeration match(String str) {
    return new Enumerator(str);
  }
  
  private Hashtable _regexps = new Hashtable();
  
  private class Enumerator implements Enumeration {
    private Enumeration _currRegexpEnum;
    private String _currRegexp;
    private int _currIndex = -1;
    private String _str;
    
    Enumerator(String str) {
      _str = str;
      _currRegexpEnum = _regexps.keys();
    }
    
    public boolean hasMoreElements() {
      if (_currRegexp != null && _currIndex != -1) {
        ArrayList currRegexpListeners = (ArrayList) _regexps.get(_currRegexp);
        if (_currIndex < currRegexpListeners.size()) {
          return true;
        }
      }
      
      while (_currRegexpEnum.hasMoreElements()) {
        String currRegexp = (String) _currRegexpEnum.nextElement();
        if (match(_str, currRegexp)) {
          _currRegexp = currRegexp;
          _currIndex = 0;
          return true;
        }
      }
      
      _currRegexp = null;
      _currIndex = -1;
      return false;
    }
    
    public Object nextElement() {
      if (!hasMoreElements()) {
        throw new NoSuchElementException();
      }
      ArrayList arr = (ArrayList) _regexps.get(_currRegexp);
      return arr.get(_currIndex++);
    }
  }
  
  private boolean match(String str, String pattern) {
    boolean fromStart = false, fromEnd = false, fromMiddle = false;
    String patternStart = null, patternEnd = null;
    
    if (pattern.charAt(0) == '*') {
      pattern = pattern.substring(1);
      fromEnd = true;
    } else if (pattern.charAt(pattern.length() - 1) == '*') {
      fromStart = true;
      pattern = pattern.substring(0, pattern.length() - 2);
    } else if (pattern.indexOf("*") != -1) {
      fromMiddle = true;
      patternStart = pattern.substring(0, pattern.indexOf("*"));
      patternEnd = pattern.substring(pattern.indexOf("*") + 1);
    }
    
    if (fromStart) {
      if (str.startsWith(pattern)) {
        return true;
      }
    } else if (fromEnd) {
      if (str.endsWith(pattern)) {
        return true;
      }
    } else if (fromMiddle) {
      if (str.startsWith(patternStart) && str.endsWith(patternEnd)) {
        return true;
      }
    } else {
      if (str.equals(pattern)) {
        return true;
      }
    }
    
    return false;
  }
}
