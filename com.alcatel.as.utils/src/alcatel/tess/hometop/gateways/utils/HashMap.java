// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class HashMap extends Hashtable implements Map {
  
  public HashMap() {
    super();
  }
  
  public boolean containsKey(Object k) {
    return (get(k) != null);
  }
  
  public boolean containsValue(Object v) {
    throw new RuntimeException("method not supported");
  }
  
  public Collection values() {
    throw new RuntimeException("method not supported");
  }
  
  public void putAll(java.util.Map map) {
    throw new RuntimeException("method not supported");
  }
  
  public Set entrySet() {
    throw new RuntimeException("method not supported");
  }
  
  public Set keySet() {
    throw new RuntimeException("method not supported");
  }
}
