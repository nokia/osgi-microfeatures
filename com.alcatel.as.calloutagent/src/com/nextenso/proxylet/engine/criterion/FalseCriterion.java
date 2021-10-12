// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import com.nextenso.proxylet.ProxyletData;

public class FalseCriterion extends Criterion {
  private static FalseCriterion basicFalseCriterion = new FalseCriterion();
  
  private FalseCriterion() {
  }
  
  @Override
  public int match(ProxyletData data) {
    return FALSE;
  }
  
  @Override
  public String toString() {
    return ("[" + Utils.FALSE + "]");
  }
  
  public static Criterion getInstance() {
    return basicFalseCriterion;
  }
  
  @Override
  public boolean includes(Criterion c) {
    return (c instanceof TrueCriterion);
  }
  
  @Override
  public boolean excludes(Criterion c) {
    return (c instanceof FalseCriterion);
  }
  
  @Override
  public boolean equals(Object o) {
    return (o instanceof FalseCriterion);
  }
  
  @Override
  public int getDepth() {
    return 1;
  }
}
