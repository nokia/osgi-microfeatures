package com.alcatel.as.service.metering.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

/**
 * This is a simple class that implements a <tt>Dictionary</tt> from a <tt>Map</tt>. The
 * resulting dictionary is immutatable.
 **/
@SuppressWarnings("unchecked")
public class Map2Dictionary extends Dictionary {
  /**
   * Map source.
   **/
  private Map map = null;
  
  public Map2Dictionary(Map map) {
    this.map = map;
  }
  
  @Override
  public Enumeration elements() {
    return Collections.enumeration(map.values());
  }
  
  @Override
  public Object get(Object key) {
    return map.get(key);
  }
  
  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }
  
  @Override
  public Enumeration keys() {
    return Collections.enumeration(map.keySet());
  }
  
  @Override
  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public int size() {
    return map.size();
  }
}
