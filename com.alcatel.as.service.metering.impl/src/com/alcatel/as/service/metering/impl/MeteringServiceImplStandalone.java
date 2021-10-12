// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.service.metering.Rate;
import com.alcatel.as.service.metering.impl.MeteringServiceImpl;

/**
 * Metering Service implementation used when running outside OSGi. 
 * It can be instantiated using ServiceLoader.load(MeteringService.class), or 
 * ServiceLoader.load(MeteringService.class, null, new Class[] { Dictionary.class }, new Object[] { myDictionary })
 */
@SuppressWarnings("unchecked")
public class MeteringServiceImplStandalone implements MeteringService {
  MeteringServiceImpl _impl = new MeteringServiceImpl();
  
  public MeteringServiceImplStandalone() {
    this(null);
  }
  
  public MeteringServiceImplStandalone(Dictionary conf) {
    _impl.start(conf != null ? dictionary2Map(conf) : null, null);
  }
  
  public Counter getCounter(String name) {
    return _impl.getCounter(name);
  }
  
  public Gauge getGauge(String name) {
    return _impl.getGauge(name);
  }
  
  public Rate getRate(String name) {
    return _impl.getRate(name);
  }
  
  private static Map dictionary2Map(Dictionary d) {
    Map m = new HashMap();
    Enumeration<?> e = d.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      Object val = d.get(key);
      m.put(key, val);
    }
    return m;
  }
}