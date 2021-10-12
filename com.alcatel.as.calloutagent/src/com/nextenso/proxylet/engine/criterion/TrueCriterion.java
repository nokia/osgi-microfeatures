// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.tracer.TracerManager;

import com.nextenso.proxylet.ProxyletData;

public class TrueCriterion extends Criterion {
  private final static Logger _logger = Logger.getLogger(TrueCriterion.class); 
  private static TrueCriterion basicTrueCriterion = new TrueCriterion();
  
  private TrueCriterion() {
  }
  
  public int match(ProxyletData data) {
    return TRUE;
  }
  
  public String toString() {
    return ("[" + Utils.TRUE + "]");
  }
  
  public static Criterion getInstance() {
    return basicTrueCriterion;
  }
  
  public boolean includes(Criterion c) {
    if (_logger.isDebugEnabled())
      _logger.debug("\t" + this + " includes ? " + c);
    return (c instanceof TrueCriterion);
  }
  
  public boolean excludes(Criterion c) {
    if (_logger.isDebugEnabled())
      _logger.debug("\t" + this + " excludes ? " + c);
    return (c instanceof FalseCriterion);
  }
  
  public boolean equals(Object o) {
    return (o instanceof TrueCriterion);
  }
  
  public int getDepth() {
    return 1;
  }
}
