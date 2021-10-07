package com.alcatel.as.util.osgi;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

/**
 * Convert a Map to an immutable Dictionary.
 */
public class MapToDictionary<K, V> extends Dictionary<K, V> {
  private Map<K, V> _map;
  
  public MapToDictionary(Map<K, V> map) {
    _map = map;
  }
  
  @Override
  public int size() {
    return _map.size();
  }
  
  @Override
  public boolean isEmpty() {
    return _map.isEmpty();
  }
  
  @Override
  public Enumeration<K> keys() {
    return Collections.enumeration(_map.keySet());
  }
  
  @Override
  public Enumeration<V> elements() {
    return Collections.enumeration(_map.values());
  }
  
  @Override
  public V get(Object key) {
    return _map.get(key);
  }
  
  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }
}
