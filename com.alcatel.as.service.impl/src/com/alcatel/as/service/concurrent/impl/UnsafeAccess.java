package com.alcatel.as.service.concurrent.impl;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** 
 * Back door to the magic sun "Unsafe" class.
 **/
public class UnsafeAccess {
  public static final Unsafe UNSAFE;
  static {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      UNSAFE = (Unsafe) field.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
