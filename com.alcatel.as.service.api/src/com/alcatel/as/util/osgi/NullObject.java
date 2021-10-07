package com.alcatel.as.util.osgi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Null objects are used as place holders for optional services that are not available.
 * Typically, this class can be used with the DependencyManager
 * {@link ServiceDependency#setDefaultImplementation} method (for optional dependencies which
 * can be wrapped by a NullObject).
 */
public final class NullObject implements InvocationHandler
{
  private static final Short NULL_SHORT = new Short((short) 0);
  private static final Integer NULL_INT = new Integer(0);
  private static final Long NULL_LONG = new Long(0);
  private static final Float NULL_FLOAT = new Float(0.0f);
  private static final Double NULL_DOUBLE = new Double(0.0);
  private static final Byte NULL_BYTE = new Byte((byte) 0);
  private static final Boolean NULL_BOOLEAN = Boolean.FALSE;
  private static final Date NULL_DATE = new Date();
  
  private ConcurrentHashMap<Class<?>, Object> _returnTypes;
  private static Map<Class<?>, Object> nullObjectsMap = new ConcurrentHashMap<Class<?>, Object>();

  /**
   * Returns a NullObject for a given class.
   * @param c the class of the null object
   * @return a null object for the given class c.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getNullObjectFor(Class<T> c)
  {
    T nullObject = (T) nullObjectsMap.get(c);
    if (nullObject == null)
    {
      nullObject =
          (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, new NullObject());
      nullObjectsMap.put(c, nullObject);
    }
    return nullObject;
  }

  /**
   * Invokes a method on this null object. The method will return a default value without doing
   * anything.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
  {
    Class<?> returnType = method.getReturnType();
    if (returnType == Void.TYPE)
    {
      return null;
    }
    Object returnValue = _returnTypes.get(returnType);
    if (returnValue == null && returnType.isInterface())
    {
      returnValue = getNullObjectFor(returnType);
      _returnTypes.put(returnType, returnValue);
    }
    return returnValue;
  }

  @SuppressWarnings("serial")
  private NullObject()
  {
    // initialize primitive return types.
    _returnTypes = new ConcurrentHashMap<Class<?>, Object>()
    {
      {
        put(Short.TYPE, NULL_SHORT);
        put(Integer.TYPE, NULL_INT);
        put(Long.TYPE, NULL_LONG);
        put(Double.TYPE, NULL_DOUBLE);
        put(Float.TYPE, NULL_FLOAT);
        put(Byte.TYPE, NULL_BYTE);
        put(Boolean.TYPE, NULL_BOOLEAN);
        put(Date.class, NULL_DATE);
        put(String.class, "");
      }
    };
  }
}
