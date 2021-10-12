// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.util;

// Jdk
import java.nio.ByteBuffer;

/**
 * The ByteBufferPool class is meant to allocate efficiently byte buffers, provided that you
 * release allocated buffers using the ByteBufferPool.release() method.
 * 
 * @deprecated in modern JDK, it's not a good idea to use any kind of object pooling.
 * So this class does not use any object pooling anymore, and directly invokes 
 * ByteBuffer.allocate and ByteBuffer.allocateDirect.
 */
@Deprecated
public class ByteBufferPool {
  
  public static ByteBuffer acquire(int len) {
    return (acquire(len, false));
  }
  
  /**
   * @param tryDirect unused: allocating a direct buffer is a very time consuming operation,
   * and we don't use direct buffers anymore. 
   */
  public static ByteBuffer acquire(int len, boolean tryDirect) {
    return ByteBuffer.allocate(len);
  }
  
  /**
   * @param buf unused 
   */
  public static void release(ByteBuffer buf) {
  }
}
