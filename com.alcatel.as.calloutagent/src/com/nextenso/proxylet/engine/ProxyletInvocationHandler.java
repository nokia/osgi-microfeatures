// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic proxy used to centralize code common to proxylets such as setting
 * thread context ClassLoaders.
 */
public class ProxyletInvocationHandler implements InvocationHandler {
  
  private Object _pxlet; // a proxylet, or a proxylet listener.
  private ClassLoader _pxletCL;
  
  public static Object newInstance(ClassLoader cl, Object pxlet) {
    return java.lang.reflect.Proxy.newProxyInstance(cl, getAllInterfaces(pxlet.getClass()),
        new ProxyletInvocationHandler(pxlet, cl));
  }
  
  private ProxyletInvocationHandler(Object pxlet, ClassLoader pxletCL) {
    _pxlet = pxlet;
    _pxletCL = pxletCL;
  }
  
  public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
    ClassLoader currThreadCL = Thread.currentThread().getContextClassLoader();
    Object result = null;
    
    try {
      Thread.currentThread().setContextClassLoader(_pxletCL);
      result = m.invoke(_pxlet, args);
    }
    
    catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
    
    catch (Exception e) {
      throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
    }
    
    finally {
      Thread.currentThread().setContextClassLoader(currThreadCL);
    }
    
    return result;
  }
  
  public Object getProxylet() {
    return _pxlet;
  }
  
  public static Object getProxylet(Object pxlet) {
    if (Proxy.isProxyClass(pxlet.getClass())) {
      return ((ProxyletInvocationHandler) Proxy.getInvocationHandler(pxlet))._pxlet;
    }
    return pxlet;
  }
  
  public static Class getProxyletClass(Object pxlet) {
    return getProxylet(pxlet).getClass();
  }
  
  private static Class[] getAllInterfaces(Class clazz) {
    Set<Class> set = new HashSet<Class>();
    Class cls = clazz;
    while (cls != null) {
      Class[] interfaces = cls.getInterfaces();
      set.addAll(Arrays.asList(interfaces));
      for (Class iface : interfaces) {
        set.addAll(Arrays.asList(getAllInterfaces(iface)));
      }
      cls = cls.getSuperclass();
    }
    return set.toArray(new Class[set.size()]);
  }
}
