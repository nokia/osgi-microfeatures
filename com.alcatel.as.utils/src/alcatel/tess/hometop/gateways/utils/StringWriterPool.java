// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.IOException;
import java.io.Writer;

public class StringWriterPool extends Writer implements Recyclable {
  public static StringWriterPool acquire(StringBuffer buf) {
    StringWriterPool swp = new StringWriterPool();
    swp.setBuffer(buf);
    return swp;
  }
  
  public StringWriterPool() {
  }
  
  public void recycle() {
    // Not using object pool anymore.
  }
  
  public void recycled() {
    // not using object pool anymore.
  }
  
  public boolean isValid() {
    return true;
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Writer implem
  // -----------------------------------------------------------------------------------------------
  
  public void write(int c) {
    buf.append((char) c);
  }
  
  public void write(char cbuf[], int off, int len) {
    if (len == 0) {
      return;
    }
    buf.append(cbuf, off, len);
  }
  
  public void write(String str) {
    buf.append(str);
  }
  
  public void write(String str, int off, int len) {
    buf.append(str.substring(off, off + len));
  }
  
  public Writer append(CharSequence csq) {
    if (csq == null) {
      buf.append("null");
    } else {
      buf.append(csq);
    }
    return this;
  }
  
  public Writer append(CharSequence csq, int start, int end) throws IOException {
    CharSequence cs = (csq == null ? "null" : csq);
    buf.append(cs, start, end);
    return this;
  }
  
  public StringBuffer getBuffer() {
    return buf;
  }
  
  public void flush() {
  }
  
  public void close() throws IOException {
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Private methods
  // -----------------------------------------------------------------------------------------------
  
  private void setBuffer(StringBuffer buf) {
    this.buf = buf;
  }
  
  // -----------------------------------------------------------------------------------------------
  //				Private attributes
  // -----------------------------------------------------------------------------------------------
  
  /** StringBuffer where we will write data. */
  private StringBuffer buf;
  
  public static void main(String args[]) throws Exception {
    StringBuffer sb = new StringBuffer();
    StringWriterPool swp = StringWriterPool.acquire(sb);
    swp.write("foo");
    swp.recycle();
    System.out.println(swp + ":" + sb);
    
    swp = StringWriterPool.acquire(sb);
    swp.write("bar");
    swp.recycle();
    System.out.println(swp + ":" + sb);
  }
}
