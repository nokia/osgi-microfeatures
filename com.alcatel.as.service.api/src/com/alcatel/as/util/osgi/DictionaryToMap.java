/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.alcatel.as.util.osgi;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Convert a Dictionary to an unmodifiable Map.
 */
public class DictionaryToMap<K, V> extends AbstractMap<K, V> {  
  private final Dictionary<K, V> _dictionary;
  
  public DictionaryToMap(Dictionary<K, V> dictionary) {
    _dictionary = dictionary;
  }
  
  @Override
  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      @Override
      public Iterator<java.util.Map.Entry<K, V>> iterator() {
        final Enumeration<K> keys = _dictionary.keys();
        
        return new Iterator<Map.Entry<K, V>>() {
          @Override
          public boolean hasNext() {
            return keys.hasMoreElements();
          }
          
          @Override
          public java.util.Map.Entry<K, V> next() {
            final K key = keys.nextElement();
            return new Entry<K, V>() {
              @Override
              public K getKey() {
                return key;
              }
              
              @Override
              public V getValue() {
                return _dictionary.get(key);
              }
              
              @Override
              public V setValue(V value) {
                throw new UnsupportedOperationException();
              }
            };
          }
          
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
      
      @Override
      public int size() {
        return _dictionary.size();
      }
    };
  }
}