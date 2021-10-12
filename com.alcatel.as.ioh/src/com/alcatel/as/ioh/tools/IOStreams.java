// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.tools;

import java.io.IOException;
import java.io.OutputStream;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;

/**
 * This class contains various methods for manipulating streams.
 */
public class IOStreams {  
  /**
   * Wraps a reactor channel into an output stream.
   * 
   * @param channel the reactor channel to be be wrapped into the returned output stream
   * @return an output stream redirecting all written bytes into the given reactor channel
   */
  public static OutputStream getOutputStream(final AsyncChannel channel) {
    // bytes are duplicated and copied in the reactor sendqueue, in case you modify the array 
    // after you write it to the output stream.
    return getOutputStream(channel, true);
  }
  
  /**
   * Wraps a reactor channel into an output stream.
   * 
   * @param channel the reactor channel to be be wrapped into the returned output stream
   * @param copy true if the written byte array must be duplicated in the reactor sendqueue,
   * false if not. Byte array written to the output stream is first inserted in a send queue 
   * before being really sent out by the reactor. So, you must pass <code>copy=true</code> 
   * if you modify the byte array after you have written it to the output stream. 
   * If you don't modify the byte arrays after you have written it to the output stream, then you
   * can use <code>copy=false</code>. 
   * @return an output stream redirecting all written bytes into the given reactor channel
   */
  public static OutputStream getOutputStream(final AsyncChannel out, final boolean copy) {
    return new OutputStream() {
      public void write(int b) {
        out.send(new byte[] { (byte) b }, false);
      }
      
      public void write(byte b[]) throws IOException {
        out.send(b, copy);
      }
      
      public void write(byte b[], int off, int len) throws IOException {
        out.send(b, off, len, copy);
      }
    };
  }
}