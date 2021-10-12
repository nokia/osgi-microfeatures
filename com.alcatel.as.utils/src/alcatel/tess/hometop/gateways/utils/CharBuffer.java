// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.io.Writer;

/**
 * This class is similar to java.io.CharArrayWriter, but is not synchronized
 * and do not copy the internal char array when calling the toCharArray method.
 */
public class CharBuffer {
  /**
   * The buffer where data is stored.
   */
  protected char buf[];
  
  /**
   * The number of chars in the buffer.
   */
  protected int count;
  
  /**
   * Creates a new CharWriter.
   */
  public CharBuffer() {
    this(32);
  }
  
  /**
   * Creates a new CharBuffer with the specified initial size.
   *
   * @param initialSize  an int specifying the initial buffer size.
   * @exception IllegalArgumentException if initialSize is negative
   */
  public CharBuffer(int initialSize) {
    if (initialSize < 0) {
      throw new IllegalArgumentException("Negative initial size: " + initialSize);
    }
    buf = new char[initialSize];
  }
  
  /**
   * Writes a character to the buffer.
   */
  public void append(int c) {
    int newcount = count + 1;
    if (newcount > buf.length) {
      char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
    buf[count] = (char) c;
    count = newcount;
  }
  
  /**
   * Appends characters to the buffer.
   * @param c	the data to be written
   * @param off	the start offset in the data
   * @param len	the number of chars that are written
   */
  public void append(char c[], int off, int len) {
    if ((off < 0) || (off > c.length) || (len < 0) || (off + len) > c.length) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }
    
    int newcount = count + len;
    if (newcount > buf.length) {
      char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
    System.arraycopy(c, off, buf, count, len);
    count = newcount;
  }
  
  /**
   * Append a portion of a string to the buffer.
   * @param  str  String to be written from
   * @param  off  Offset from which to start reading characters
   * @param  len  Number of characters to be written
   */
  public void append(String str, int off, int len) {
    int newcount = count + len;
    if (newcount > buf.length) {
      char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
    str.getChars(off, off + len, buf, count);
    count = newcount;
  }
  
  /**
   * Appends the contents of the buffer to another character stream.
   *
   * @param out	the output stream to append to
   * @throws IOException If an I/O error occurs.
   */
  public void appendTo(Writer out) throws IOException {
    out.write(buf, 0, count);
  }
  
  /**
   * Resets the buffer so that you can use it again without
   * throwing away the already allocated buffer.
   */
  public void reset() {
    count = 0;
  }
  
  /**
   * Returns a copy of the input data.
   *
   * @param copy true if a fresh copy of internal buffer must be returned, false
   *	if the internal buffer must be returned.
   * @return an array of chars copied from the input data.
   */
  public char[] toCharArray(boolean copy) {
    if (copy) {
      char newbuf[] = new char[count];
      System.arraycopy(buf, 0, newbuf, 0, count);
      return newbuf;
    } else {
      return (buf);
    }
  }
  
  public char charAt(int index) {
    return (buf[index]);
  }
  
  /**
   * Returns the current size of the buffer.
   *
   * @return an int representing the current size of the buffer.
   */
  public int size() {
    return count;
  }
  
  /**
   * Converts input data to a string.
   * @return the string.
   */
  public String toString() {
    return new String(buf, 0, count);
  }
  
  /**
   * Converts input data to a string.
   * @return the string.
   */
  public String toString(int offset, int length) {
    return new String(buf, offset, length);
  }
  
  /**
   * Converts input data to a string.
   * @return the string.
   */
  public String trim(int offset, int length) {
    int len = length;
    int st = offset;
    int max = offset + length;
    
    while ((st < max) && (buf[st] <= ' ')) {
      st++;
      len--;
    }
    
    while ((len > 0) && (buf[st + len - 1]) <= ' ') {
      len--;
    }
    
    return (toString(st, len));
  }
  
  public String toASCIIString(boolean modif, boolean trim, boolean capitalizeFirstLetter) {
    return (toASCIIString(0, count, modif, trim, capitalizeFirstLetter));
  }
  
  public String toASCIIString(int off, boolean modif, boolean trim, boolean capitalizeFirstLetter) {
    return (toASCIIString(off, count - off, modif, trim, capitalizeFirstLetter));
  }
  
  public String toASCIIString(int off, int len, boolean modif, boolean trim, boolean capitalizeFirstLetter) {
    int max = off + len;
    
    if (trim) {
      while ((off < max) && (buf[off] <= ' ')) {
        off++;
        len--;
      }
      
      while ((len > 0) && (buf[off + len - 1]) <= ' ') {
        len--;
      }
    }
    
    char buf[] = this.buf;
    
    if (capitalizeFirstLetter) {
      
      if (modif == false) {
        char tmp[] = new char[len];
        System.arraycopy(this.buf, off, tmp, 0, len);
        buf = tmp;
        off = 0;
      }
      
      max = off + len;
      
      boolean postDash = true;
      for (int i = off; i < max; i++) {
        char c = buf[i];
        if (postDash) {
          // To Upper Case
          if ((c > '\u0060') && (c < '\u007b')) // means 'a'<=c<='z'
            buf[i] = (char) (c - UPPER_TO_LOWER);
        } else {
          // To Lower Case
          if ((c > '\u0040') && (c < '\u005b')) // means 'A'<=c<='Z'
            buf[i] = (char) (c + UPPER_TO_LOWER);
        }
        postDash = (c == '-');
      }
    }
    
    return (new String(buf, off, len));
  }
  
  /**
   * Converts input data to a string.
   * @return the string.
   */
  public String trim() {
    return (trim(0, count));
  }
  
  public static void main(String args[]) throws Exception {
    CharBuffer buf = new CharBuffer();
    for (int i = 0; i < args[0].length(); i++)
      buf.append(args[0].charAt(i));
    
    System.out.println(buf.toASCIIString(0, args[0].length(), false, true, true));
  }
  
  private static final int UPPER_TO_LOWER = (int) ('a' - 'A');
}
