// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.parser;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.CharBuffer;

/**
 * This class de-chunks an input stream and may be used
 * and restarted even after having caught interrupted io 
 * exception.
 */
public class HttpChunkedInputStream extends FilterInputStream {
  /**
   * @param is the input stream to dechunk
   */
  public HttpChunkedInputStream(InputStream is) {
    super(is);
  }
  
  /**
   * @param is the input stream to dechunk
   */
  public HttpChunkedInputStream() {
    this(null);
  }
  
  public boolean needsInput() {
    return (state > READY);
  }
  
  public void setInputStream(InputStream in) {
    super.in = in;
  }
  
  public int read() throws IOException {
    int n = read(one, 0, 1);
    return ((n != -1) ? (one[0] & 0xff) : n);
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    int rcvd = 0;
    int done = 0;
    int c;
    
    while (true) {
      if (done == len)
        return 0;
      switch (state) {
      case READING_CHUNK_LENGTH:
        switch (c = in.read()) {
        case -1:
          return -1;
          
        case '\r':
          done++;
          break;
        
        case '\n':
          done++;
          parseChunkLength(this.line);
          line.reset();
          
          if (chunkLength > 0) {
            state = READING_CHUNK;
          } else {
            state = READING_CHUNK_FOOTER;
          }
          break;
        
        default:
          done++;
          line.append((char) c);
          break;
        }
        break;
      
      case READING_CHUNK:
        int left = len - done; // number of bytes to read
        if (left > chunkLength) {
          left = (int) chunkLength;
        }
        
        if ((rcvd = in.read(buf, off, left)) == -1) {
          return -1;
        }
        
        if ((chunkLength -= rcvd) == 0) {
          // the whole chunk has been read. Now, we must read cr, and lf
          if (in.read() == -1) {
            crlf = 2;
            state = READING_CHUNK_END;
            return rcvd;
          }
          done++;
          
          if (in.read() == -1) {
            crlf = 1;
            state = READING_CHUNK_END;
            return rcvd;
          }
          done++;
          state = READING_CHUNK_LENGTH;
          return rcvd;
        }
        
        // We stay in the READING_CHUNK state .
        return rcvd;
        
      case READING_CHUNK_END:
        //
        // Discard crlf after chunk of data.
        //
        if (in.read() == -1) {
          return -1;
        }
        
        done++;
        this.crlf--;
        
        if (this.crlf == 0)
          state = READING_CHUNK_LENGTH;
        break;
      
      case READING_CHUNK_FOOTER:
        switch (c = in.read()) {
        case -1:
          return -1;
          
        case '\r':
          done++;
          continue;
          
        case '\n':
          done++;
          if (line.size() == 0) {
            state = READY;
            return -1; // all chunks correctly read.
          }
          
          line.reset();
          break;
        
        default:
          done++;
          line.append((char) c);
          break;
        }
      }
    }
  }
  
  public long skip(long num) throws IOException {
    byte[] tmp = new byte[(int) num];
    int got = read(tmp, 0, (int) num);
    
    if (got > 0)
      return (long) got;
    else
      return 0L;
  }
  
  public int available() throws IOException {
    return (in.available());
  }
  
  public void init() {
    line.reset();
    state = READING_CHUNK_LENGTH;
    chunkLength = -1;
  }
  
  /**
   * Gets the length of the chunk.
   *
   * @param  input  the stream from which to read the next chunk.
   * @return  the length of chunk to follow (w/o trailing CR LF).
   * @exception IOException    If any exception during reading occured.
   */
  private void parseChunkLength(CharBuffer line) throws IOException {
    int semi = -1;
    this.chunkLength = -1;
    
    for (int i = 0; i < line.size(); i++) {
      if (line.charAt(i) == ';') {
        semi = i;
        break;
      }
    }
    
    String len = line.trim(0, (semi != -1) ? semi : line.size());
    
    try {
      len = len.trim();
      this.chunkLength = Long.parseLong(len, 16);
    }
    
    catch (NumberFormatException nfe) {
      throw new IOException("Didn't find valid chunk length: " + len);
    }
  }
  
  // Internal buffer used by the read () method.
  private byte[] one = new byte[1];
  
  // Chunk length found in chunked header message.
  private long chunkLength = -1;
  
  // Counter used to old number of times we have read some chars.
  private int crlf;
  
  // Internal char buffer used to store lines.
  private CharBuffer line = new CharBuffer();
  
  // State of this class
  private int state = READY;
  
  private final static int READY = -1;
  private final static int READING_CHUNK_LENGTH = 0;
  private final static int READING_CHUNK = 1;
  private final static int READING_CHUNK_END = 2;
  private final static int READING_CHUNK_FOOTER = 3;
  
//  private static String CHUNKED = "1a; ignore-stuff-here\r\n" + "abcdefghijklmnopqrstuvwxyz\r\n" + "10\r\n"
//      + "1234567890abcdef\r\n" + "0\r\n" + "some-footer: some-value\r\n"
//      + "another-footer: another-value\r\n" + "\r\n";
//  
//  private static String CHUNKED2 = "5\r\n" + "hello\r\n" + "3\r\n" + "bye\r\n" + "0\r\n\r\n";
//  
//  private static String CHUNKED3 = "C\r\n" + "Some data...\r\n" + "11\r\n" + "Some more data...\r\n"
//      + "0\r\n";
  
  private static String CHUNKED4 = "9\r\n" + "Message#0\r\n" + "0\r\n\r\n";
  
  public static void main(String args[]) throws Exception {
    byte[] msg = CHUNKED4.getBytes();
    
    InputStream in = new ByteArrayInputStream(msg);
    HttpChunkedInputStream chunked = new HttpChunkedInputStream(in);
    chunked.init();
    ByteBuffer buf = new ByteBuffer();
    buf.append(chunked);
    
    System.out.println(buf.toString());
    buf.close();
  }
}
