// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.mux.impl;

import java.nio.ByteBuffer;

public class MuxBuffer {
  
  private byte[] _data;
  private ByteBuffer _dataBuffer;
  
  public MuxBuffer() {
  }
  
  public void reset(ByteBuffer databuffer) {
    _dataBuffer = databuffer;
    _data = _dataBuffer.array();
  }
  
  /**
   * Reads some bytes from the data buffer.
   * 
   * @param size the bytes to read
   * @return true if required size has been read, false if not.
   */
  public boolean read(int size) {
    if (_dataBuffer.remaining() < size) {
      return false;
    }
    
    _dataBuffer.position(_dataBuffer.position() + size);
    return true;
  }
  
  public ByteBuffer getByteBuffer() {
    return _dataBuffer;
  }
  
  public byte[] getData() {
    return _data;
  }
  
  public int getConsumed() {
    return _dataBuffer.position();
  }
  
  public boolean hasRemaining() {
    return _dataBuffer.hasRemaining();
  }
  
}
