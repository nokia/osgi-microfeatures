// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Quoted string version of the standard java.util.StringTokenizer.
 * If delimiters are part of quoted string, they are not considered as
 * separators.
 * 
 */
public class QuotedStringTokenizer implements Enumeration {
  
  public QuotedStringTokenizer() {
  }
  
  /**
   * Constructs a string tokenizer for the specified string. The
   * characters in the <code>delim</code> argument are the delimiters
   * for separating tokens.
   * <p>
   * If the <code>removeQuotes</code> flag is <code>true</code>, then
   * the quotes are removed if any.
   * 
   * @param   str            a string to be parsed.
   * @param   delim          the delimiters.
   * @param   returnTokens   flag indicating whether to return the delimiters
   * as tokens.
   * @since   JDK1.0
   */
  public QuotedStringTokenizer(String str, String delim, boolean removeQuotes) {
    reset(str, delim, removeQuotes);
  }
  
  /**
   * Constructs a string tokenizer for the specified string. The
   * characters in the <code>delim</code> argument are the delimiters
   * for separating tokens.
   * <p>
   * If the <code>returnTokens</code> flag is <code>true</code>, then
   * the delimiter characters are also returned as tokens. Each
   * delimiter is returned as a string of length one. If the flag is
   * <code>false</code>, the delimiter characters are skipped and only
   * serve as separators between tokens.
   * 
   * @param   str            a string to be parsed.
   * @param   delim          the delimiters.
   * @param   returnTokens   flag indicating whether to return the delimiters
   * as tokens.
   * @since   JDK1.0
   */
  public QuotedStringTokenizer(String str, String delim) {
    this(str, delim, false);
  }
  
  /**
   * Constructs a string tokenizer for the specified string. The
   * tokenizer uses the default delimiter set, which is
   * <code>"&#92;t&#92;n&#92;r"</code>: the space character, the tab
   * character, the newline character, and the carriage-return character.
   * 
   * @param   str   a string to be parsed.
   * @since   JDK1.0
   */
  public QuotedStringTokenizer(String s) {
    this(s, SPTRN, false);
  }
  
  public void reset(String s) {
    reset(s, SPTRN, false);
  }
  
  public void reset(String s, String delim) {
    reset(s, delim, false);
  }
  
  public void reset(String s, String delim, boolean removeQuotes) {
    this.currentPosition = 0;
    this.str = s;
    this.delimiters = delim;
    this.removeQuotes = removeQuotes;
    this.maxPosition = str.length();
  }
  
  /**
   * Tests if there are more tokens available from this tokenizer's string.
   * 
   * @return  <code>true</code> if there are more tokens available from this
   * tokenizer's string; <code>false</code> otherwise.
   * @since   JDK1.0
   */
  public boolean hasMoreTokens() {
    skipDelimiters();
    
    return (currentPosition < maxPosition);
  }
  
  /**
   * Returns the next token from this string tokenizer.
   * 
   * @return     the next token from this string tokenizer.
   * @exception  NoSuchElementException  if there are no more tokens in this
   * tokenizer's string.
   * @since      JDK1.0
   */
  public String nextToken() {
    skipDelimiters();
    
    StringBuffer buffer = new StringBuffer();
    
    if (currentPosition >= maxPosition) {
      throw new NoSuchElementException();
    }
    
    int start = currentPosition;
    int quotes = 0;
    
    while ((currentPosition < maxPosition) && (delimiters.indexOf(str.charAt(currentPosition)) < 0)
        || isOdd(quotes)) {
      char c = str.charAt(currentPosition);
      
      if (c == QUOTE) {
        quotes++;
        
        if (this.removeQuotes == false) {
          buffer.append(c);
        }
      } else {
        buffer.append(c);
      }
      
      currentPosition++;
    }
    
    // return str.substring(start, currentPosition);
    return (buffer.toString());
  }
  
  /**
   * Returns the next token in this string tokenizer's string. The new
   * delimiter set remains the default after this call.
   * 
   * @param      delim   the new delimiters.
   * @return     the next token, after switching to the new delimiter set.
   * @exception  NoSuchElementException  if there are no more tokens in this
   * tokenizer's string.
   * @since   JDK1.0
   */
  public String nextToken(String delim) {
    delimiters = delim;
    
    return nextToken();
  }
  
  /**
   * Returns the same value as the <code>hasMoreTokens</code>
   * method. It exists so that this class can implement the
   * <code>Enumeration</code> interface.
   * 
   * @return  <code>true</code> if there are more tokens;
   * <code>false</code> otherwise.
   * @see     java.util.Enumeration
   * @see     #hasMoreTokens()
   * @since   JDK1.0
   */
  public boolean hasMoreElements() {
    return hasMoreTokens();
  }
  
  /**
   * Returns the same value as the <code>nextToken</code> method,
   * except that its declared return value is <code>Object</code> rather than
   * <code>String</code>. It exists so that this class can implement the
   * <code>Enumeration</code> interface.
   * 
   * @return     the next token in the string.
   * @exception  NoSuchElementException  if there are no more tokens in this
   * tokenizer's string.
   * @see        java.util.Enumeration
   * @see        #nextToken()
   * @since      JDK1.0
   */
  public Object nextElement() {
    return nextToken();
  }
  
  /**
   * Calculates the number of times that this tokenizer's
   * <code>nextToken</code> method can be called before it generates an
   * exception.
   * 
   * @return  the number of tokens remaining in the string using the current
   * delimiter set.
   * @see     #nextToken()
   * @since   JDK1.0
   */
  public int countTokens() {
    int count = 0;
    int currpos = currentPosition;
    int quotes = 0;
    
    while (currpos < maxPosition) {
      
      /*
       * This is just skipDelimiters(); but it does not affect
       * currentPosition.
       */
      while ((currpos < maxPosition) && delimiters.indexOf(str.charAt(currpos)) >= 0 && !isOdd(quotes)) {
        currpos++;
      }
      
      if (currpos >= maxPosition) {
        break;
      }
      
      int start = currpos;
      
      while ((currpos < maxPosition) && ((delimiters.indexOf(str.charAt(currpos)) < 0) || isOdd(quotes))) {
        if (str.charAt(currpos) == QUOTE) {
          quotes++;
        }
        
        currpos++;
      }
      
      count++;
    }
    
    return count;
  }
  
  /**
   * Method declaration
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    String msg = "aa,bb,\",1,2,,\",\"cc\",\"d,d\"";
    QuotedStringTokenizer tok = new QuotedStringTokenizer(msg, ",", true);
    
    System.out.println(tok.countTokens());
    System.out.println();
    
    while (tok.hasMoreTokens()) {
      System.out.println(tok.nextToken());
    }
  }
  
  /**
   * Skips delimiters.
   */
  private void skipDelimiters() {
    while ((currentPosition < maxPosition) && (delimiters.indexOf(str.charAt(currentPosition)) >= 0)) {
      currentPosition++;
    }
  }
  
  /**
   * Test if an integer is odd.
   * @return true if the parameter is odd, false if its even
   */
  private boolean isOdd(int n) {
    return ((n & 0x1) == 0x1);
  }
  
  private int currentPosition;
  private int maxPosition;
  private boolean removeQuotes;
  private String str;
  private String delimiters;
  private final static char QUOTE = '\"';
  private final static String SPTRN = " \t\n\r";
}
