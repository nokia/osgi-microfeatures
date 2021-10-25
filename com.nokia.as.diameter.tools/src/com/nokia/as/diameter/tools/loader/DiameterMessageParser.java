// // Copyright 2000-2021 Nokia
// //
// // Licensed under the Apache License 2.0
// // SPDX-License-Identifier: Apache-2.0
// //
//
//

package com.nokia.as.diameter.tools.loader;

import java.nio.ByteBuffer;

public class DiameterMessageParser {
  private final static DiameterMessageParser _instance = new DiameterMessageParser();
  
  public static DiameterMessageParser getInstance() {
    return _instance;
  }
  
  /**
   * @see com.nextenso.mux.socket.TcpMessageParser#parse(java.nio.ByteBuffer)
   */
  public int parse(ByteBuffer buffer) {
    buffer.mark();
    try {
      if (buffer.remaining() < 4)
        return buffer.remaining() - 4;
      buffer.get();
      int size = buffer.get() & 0xFF;
      size <<= 8;
      size |= buffer.get() & 0xFF;
      size <<= 8;
      size |= buffer.get() & 0xFF;
      int needed = size - 4;
      if (buffer.remaining() >= needed) {
        return size;
      }
      return buffer.remaining() - needed;
    } finally {
      buffer.reset();
    }
  }
}
