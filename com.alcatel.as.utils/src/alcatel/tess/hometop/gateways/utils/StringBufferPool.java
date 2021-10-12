// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

public class StringBufferPool implements Recyclable {
  private final static StringBufferPool _instance = new StringBufferPool();
  
  public StringBufferPool() {
  }
  
  public StringBuffer getStringBuffer() {
    // not using pool anymore.
    return new StringBuffer();
  }
  
  public static StringBufferPool acquire() {
    // not using pool anymore.
    return _instance;
  }
  
  public void release() {
    // not using pool anymore.
  }
  
  public void recycled() {
    // not using pool any more.
  }
  
  public boolean isValid() {
    return true;
  }
}
