// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

/**
 * This class provides a standard mean to break a String message into basic tokens.
 * It supports quoted strings, and do its best in order to avoid string allocation.
 *
 * Here is a sample code:
 *
 * <hr><blockquote><pre>
 * String str = "\"ga; bu"- zo;foo=bar; ;";
 * Tokenizer tok = Tokenizer.acquire(str);
 * while (tok.nextToken("-;")) {
 *   String token = tok.trimToken('"');
 *   System.out.println("Token=" + token + " (delimiter=" + tok.delimiter() + ")";
 * }
 * Tokenizer.release(tok);
 *
 * The corresponding output is:
 *
 * Token="ga; bu" (delim='-')
 * Token="zo" (delim=';')
 * Token="foo=bar" (delim=';')
 * Token="" (delim=';')
 *
 * </pre></blockquote></hr>
 *
 * For a complete example, see alcatel.tess.hometop.gateways.test.TestTokenizerc lass.
 */
public class Tokenizer implements Recyclable {
  
  // -------------------------------------------------------------------------------------------------
  //		Public Static methods.
  // -------------------------------------------------------------------------------------------------
  
  public static Tokenizer acquire(String s) {
    Tokenizer tok = new Tokenizer();
    tok.init(s);
    return tok;
  }
  
  public static void release(Tokenizer sl) {
    // not using object pool anymore.
  }
  
  // -------------------------------------------------------------------------------------------------
  //		Public methods.
  // -------------------------------------------------------------------------------------------------
  
  public Tokenizer() {
  }
  
  public boolean nextToken(String delims) {
    _delims = delims;
    
    /*
     *  Skip starting whitespace if any
     */
    if (!skipWhitespace()) {
      return false;
    }
    
    /*
     * Process input characters until we reach a delimiter or the end of the input buffer
     */
    _startOfToken = _off;
    _endOfToken = _off;
    
    while (_off < _len) {
      /*
       *  If we got some white space the last time around, we need to check if we have one of our separators
       */
      char c = _buf.charAt(_off++);
      
      /*
       *  Skip an eventual string literal
       */
      if (c == '"') {
        skipLiteral();
        continue;
      }
      
      /*
       *  All done if this is one of our delimiters
       */
      if (isDelim(c)) {
        _endOfToken = _off - 1;
        _delim = c;
        return true;
      }
    }
    
    /**
     * Trim spaces from end of token.
     */
    _endOfToken = _len;
    _delim = -1;
    return true;
  }
  
  public String token() {
    return trim(_startOfToken, _endOfToken, -1);
  }
  
  public String trimToken(int charToTrim) {
    return trim(_startOfToken, _endOfToken, charToTrim);
  }
  
  public int delimiter() {
    return _delim;
  }
  
  // -------------------------------------------------------------------------------------------------
  //		Recyclable interface.
  // -------------------------------------------------------------------------------------------------
  
  public void recycled() {
    // not using object pool anymore.
  }
  
  public boolean isValid() {
    return true;
  }
  
  // -------------------------------------------------------------------------------------------------
  //		Private methods.
  // -------------------------------------------------------------------------------------------------
  
  private void init(String buf) {
    _buf = buf;
    _off = 0;
    _len = buf.length();
  }
  
  /**
   *  Check if the specified character is a delimiter or not.
   *
   *@param  c  Character to check
   *@return    true if this is a delimiter, false otherwise
   */
  private boolean isDelim(char c) {
    int n = _delims.length();
    for (int i = 0; i < n; ++i) {
      if (c == _delims.charAt(i)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Skip all whitespace charecters off the input buffer.
   *
   * @return    false if end of buffer reached
   */
  private boolean skipWhitespace() {
    for (; _off < _len; ++_off) {
      char c = _buf.charAt(_off);
      if (c != ' ' && c != '\t') {
        return true;
      }
    }
    return false;
  }
  
  /**
   *  Skip a string literal
   */
  private void skipLiteral() {
    boolean gotEscape = false;
    while (_off < _len) {
      char c = _buf.charAt(_off++);
      if (c == '\\') {
        gotEscape = (gotEscape) ? false : true;
        continue;
      }
      if (c == '"') {
        if (gotEscape) {
          gotEscape = false;
          continue;
        }
        return;
      }
      gotEscape = false;
    }
    throw new RuntimeException("String literal is not terminated");
  }
  
  private String trim(int start, int end, int charToTrim) {
    String buf = _buf;
    char c = 0;
    
    if (charToTrim == -1) {
      while (start < end && ((c = buf.charAt(start)) == ' ' || c == '\t')) {
        start++;
      }
      
      while ((start < end) && ((c = buf.charAt(end - 1)) == ' ') || c == '\t') {
        end--;
      }
    } else {
      while (start < end && ((c = buf.charAt(start)) == ' ' || c == '\t' || c == charToTrim)) {
        start++;
      }
      
      while ((start < end) && ((c = buf.charAt(end - 1)) == ' ' || c == '\t' || c == charToTrim)) {
        end--;
      }
    }
    
    return (buf.substring(start, end));
  }
  
  private String _buf;
  private int _off;
  private int _len;
  private String _delims;
  private int _delim;
  private int _startOfToken;
  private int _endOfToken;
}
