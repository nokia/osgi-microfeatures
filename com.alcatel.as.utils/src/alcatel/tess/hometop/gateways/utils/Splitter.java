// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

/**
 * String general splitter utility.
 */
public class Splitter {
  
  public static void main(String args[]) throws Exception {
    String s = "125 agent:128 \"warn txt\" \"Tue 29 Nov 1945 00:24:00 GMT\"";
    String[] list;
    String[] params;
    Splitter sp = new Splitter();
    
    list = sp.splitrim(s, ",");
    
    System.out.println(list.length);
    System.out.println(list[0]);
    
    params = sp.split(list[0], " ");
    
    for (int j = 0; j < params.length; j++) {
      System.out.println(params[j]);
    }
  }
  
  static void display(String msg, String[] s) {
    System.out.println(msg);
    for (int i = 0; i < s.length; i++)
      System.out.println("\t" + s[i]);
  }
  
  public Splitter() {
    st = new QuotedStringTokenizer();
  }
  
  /**
   * Splits a String in an array of Strings.<P>
   * This method works like java.util.StringTokenizer.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @return an array of tokens.
   */
  public String[] split(String s) {
    st.reset(s);
    String ret[] = new String[st.countTokens()];
    
    for (int i = 0; st.hasMoreTokens(); i++) {
      ret[i] = st.nextToken();
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of Strings.<P>
   * This method works like java.util.StringTokenizer.
   *
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @param c String containing token delimiters
   * @return an array of tokens.
   */
  public String[] split(String s, String c) {
    st.reset(s, c);
    String ret[] = new String[st.countTokens()];
    
    for (int i = 0; st.hasMoreTokens(); i++) {
      ret[i] = st.nextToken();
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of trimmed Strings.<P>
   * This method works like java.util.StringTokenizer.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @return an array of tokens.
   */
  public String[] splitrim(String s) {
    st.reset(s);
    String ret[] = new String[st.countTokens()];
    
    for (int i = 0; st.hasMoreTokens(); i++) {
      ret[i] = st.nextToken().trim();
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of trimmed Strings.<P>
   * This method works like java.util.StringTokenizer.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @param c String containing token delimiters
   * @return an array of tokens.
   */
  public String[] splitrim(String s, String c) {
    st.reset(s, c);
    String ret[] = new String[st.countTokens()];
    
    for (int i = 0; st.hasMoreTokens(); i++) {
      ret[i] = st.nextToken().trim();
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of trimmed Strings.<P>
   * This method works like java.util.StringTokenizer.
   * The third parameter is put at the first position of the
   * resulting array.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @param c String containing token delimiters
   * @param head The String that is put at the begining of the array
   * @return an array of tokens.
   */
  public String[] splitrim(String s, String c, String head) {
    st.reset(s, c);
    String ret[] = new String[st.countTokens() + 1];
    
    ret[0] = head;
    
    for (int i = 0; st.hasMoreTokens(); i++) {
      ret[i + 1] = st.nextToken().trim();
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of Strings with length n.<P>
   * This method works like java.util.StringTokenizer, but if
   * number of tokens is less than n set the rest to empty Strings,
   * and if number of tokens is more than n in last token there is
   * the rest of the String.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @param c String containing token delimiters
   * @param n number of token
   * @return an array of tokens.
   */
  public String[] split(String s, String c, int n) {
    String ret[] = new String[n];
    
    if (s == null || c == null) {
      for (int i = 0; i < n; i++) {
        ret[i] = EMPTY;
      }
      
      return ret;
    }
    
    st.reset(s, c);
    int i;
    
    for (i = 0; st.hasMoreTokens() && i < (n - 1); i++) {
      ret[i] = st.nextToken();
    }
    
    while (st.hasMoreTokens()) {
      ret[i++] = st.nextToken(EMPTY);
    }
    
    for (; i < n; i++) {
      ret[i] = EMPTY;
    }
    
    return ret;
  }
  
  /**
   * Splits a String in an array of trimmed Strings with length n.<P>
   * This method works like java.util.StringTokenizer, but if
   * number of tokens is less than n set the rest to empty Strings,
   * and if number of tokens is more than n in last token there is
   * the rest of the String.
   * @see java.util.StringTokenizer
   * @param s String to split.
   * @param c String containing token delimiters
   * @param n number of token
   * @return an array of tokens.
   */
  public String[] splitrim(String s, String c, int n) {
    String ret[] = new String[n];
    
    if (s == null || c == null) {
      for (int i = 0; i < n; i++) {
        ret[i] = EMPTY;
      }
      
      return ret;
    }
    
    st.reset(s, c);
    int i;
    
    for (i = 0; st.hasMoreTokens() && i < (n - 1); i++) {
      ret[i] = st.nextToken().trim();
    }
    
    while (st.hasMoreTokens()) {
      ret[i++] = st.nextToken(EMPTY).trim();
    }
    
    for (; i < n; i++) {
      ret[i] = EMPTY;
    }
    
    return ret;
  } // split
  
  private QuotedStringTokenizer st;
  private final static String EMPTY = "";
}
