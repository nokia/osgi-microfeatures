// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class declaration
 *
 *
 * @author
 */
public class Bytes implements Cloneable {
  public final static int DEFAULT_INITIAL_SIZE = 16;
  
  /**
   * Method declaration
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    Bytes tmp = new Bytes();
    
    /*
     * b.addByte((byte) 1);
     * b.addByte((byte) 2);
     * b.addByte((byte) 3);
     * b.addByte((byte) 4);
     * b.addByte((byte) 5);
     * System.out.println("size=" + b.getLength());
     * System.out.println(b.toString());
     * Bytes       clone = (Bytes) b.clone();
     * System.out.println("size=" + clone.getLength());
     * System.out.println(clone.toString());
     * System.out.println("real size=" + clone.getBytes().length);
     * clone.trim();
     * System.out.println("size=" + clone.getLength());
     * System.out.println(clone.toString());
     * System.out.println("real size=" + clone.getBytes().length);
     * tmp.addByte((byte) 1);
     * tmp.addByte((byte) 2);
     * clone.addByte(tmp, 0, 1);
     * System.out.println(clone.toString());
     */
    tmp = new Bytes();
    
    FileInputStream fin = new FileInputStream(args[0]);
    int maxBytes = Integer.parseInt(args[1]);
    
    if (maxBytes != -1) {
      tmp.copyIn(fin, maxBytes);
    } else {
      tmp.copyIn(fin);
    }
    
    System.out.println(new String(tmp.getBytes()));
  }
  
  /**
   * Constructor declaration
   */
  public Bytes() {
    this(DEFAULT_INITIAL_SIZE);
  }
  
  /**
   * Constructor declaration
   *
   * @param initialSize
   */
  public Bytes(int initialSize) {
    data = new byte[initialSize];
    length = 0;
  }
  
  /**
   * Constructor declaration
   *
   * @param data
   * @param length
   */
  public Bytes(byte[] data, int length) {
    this.data = data;
    this.length = length;
  }
  
  /**
   * Constructor declaration
   *
   * @param data
   */
  public Bytes(byte[] data) {
    this.data = data;
    this.length = data.length;
  }
  
  /**
   * Method declaration
   * @return
   */
  public byte[] getBytes() {
    if (length == 0) {
      return (null);
    }
    return (data);
  }
  
  /**
   * Method declaration
   * @return
   */
  public byte[] getBytes(boolean checkLen) {
    if (checkLen == true && length == 0) {
      return (null);
    }
    return (data);
  }
  
  /**
   * Method declaration
   *
   * @param b
   */
  public void addByte(byte b) {
    expand(length + 1);
    
    data[length++] = b;
  }
  
  /**
   * Method declaration
   *
   * @param b
   */
  public void addByte(Bytes b) {
    addByte(b.getBytes(), 0, b.getLength());
  }
  
  /**
   * Method declaration
   *
   * @param b
   * @param offset
   * @param length
   */
  public void addByte(Bytes b, int offset, int length) {
    addByte(b.getBytes(), offset, length);
  }
  
  /**
   * Method declaration
   *
   * @param barray
   */
  public void addByte(byte[] barray) {
    if (barray == null)
      return;
    addByte(barray, 0, barray.length);
  }
  
  /**
   * Method declaration
   *
   * @param barray
   * @param offset
   * @param length
   */
  public void addByte(byte[] barray, int offset, int length) {
    if (barray == null) {
      return;
    }
    
    expand(this.length + length);
    
    System.arraycopy(barray, offset, this.data, this.length, length);
    
    this.length += length;
  }
  
  /**
   * Method declaration
   */
  public void trim() {
    if (data.length != this.length) {
      byte[] data = new byte[this.length];
      
      System.arraycopy(this.data, 0, data, 0, this.length);
      this.data = data;
    }
  }
  
  /**
   * Method declaration
   * @return
   */
  public int getLength() {
    return (length);
  }
  
  /**
   * The <code>setLength</code> method set the current length for this byte array.
   *
   * @param length an <code>int</code> value
   * @return an <code>int</code> value
   */
  public void setLength(int length) {
    expand(length);
    this.length = length;
  }
  
  /**
   * Method declaration
   */
  public void reset() {
    length = 0;
  }
  
  /**
   * Method declaration
   *
   * @param data
   * @param length
   */
  public void reset(byte[] data, int length) {
    this.data = data;
    this.length = length;
  }
  
  /**
   * Method declaration
   *
   * @param length
   */
  public void reset(int length) {
    if (data.length < length) {
      this.data = new byte[length];
    }
    
    this.length = 0;
  }
  
  /**
   * Method declaration
   *
   * @param in
   *
   * @throws IOException
   */
  public void copyIn(InputStream in) throws IOException {
    this.length = 0;
    
    int n = 0;
    
    while (n >= 0) {
      
      expand(this.length + 1);
      
      // Try to totally fill our buffer.
      n = in.read(this.data, this.length, this.data.length - this.length);
      
      // Test if we have reach end of stream.
      if (n == -1) {
        break;
      }
      
      this.length += n;
    }
  }
  
  /**
   * Method declaration
   *
   * @param in
   * @param maxSize
   *
   * @throws IOException
   */
  public void copyIn(InputStream in, int maxSize) throws IOException {
    this.length = 0;
    
    expand(maxSize);
    
    int bytesRead = 0;
    int n = 0;
    
    while (n >= 0) {
      if (bytesRead >= maxSize) {
        break;
      }
      
      if ((n = in.read(this.data, bytesRead, maxSize - bytesRead)) == -1) {
        break;
      }
      
      bytesRead += n;
    }
    
    this.length = bytesRead;
  }
  
  /**
   * Method declaration
   * @return
   */
  public String toString() {
    return (Utils.dumpByteArray("", data, length));
  }
  
  /**
   * Method declaration
   * @return
   *
   * @throws CloneNotSupportedException
   */
  protected Object clone() throws CloneNotSupportedException {
    Bytes copy = (Bytes) super.clone();
    
    copy.data = new byte[data.length];
    
    System.arraycopy(data, 0, copy.data, 0, length);
    
    return (copy);
  }
  
  /**
   * Method declaration
   */
  private void expand(int len) {
    if (this.data.length >= len)
      return;
    
    int newLength = len + (len / 2);
    byte[] newData = new byte[newLength];;
    System.arraycopy(this.data, 0, newData, 0, this.length);
    this.data = newData;
  }
  
  private byte[] data;
  private int length;
}
