package com.alcatel.as.util.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.ServiceReference;

public class OSGiHelper  {
  public static Dictionary<String, ?> serviceRefPropsToDictionary(ServiceReference<?> ref) {
    Hashtable<String, Object> h = new Hashtable<String, Object>();
    String[] keys = ref.getPropertyKeys();
    if (keys != null) {
      for (String key : keys) {
        h.put(key, ref.getProperty(key));
      }
    }
    return h;
  }
  
  @SuppressWarnings("unchecked")
  public static <V> Map<String, V> serviceRefPropsToMap(ServiceReference<?> ref) {
    Map<String, V> h = new HashMap<String, V>();
    String[] keys = ref.getPropertyKeys();
    if (keys != null) {
      for (String key : keys) {
        h.put(key, (V) ref.getProperty(key));
      }
    }
    return h;
  }
}
