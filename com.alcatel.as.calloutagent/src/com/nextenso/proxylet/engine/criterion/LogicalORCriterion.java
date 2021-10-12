// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import com.nextenso.proxylet.ProxyletData;

public class LogicalORCriterion extends Criterion {
  
  private Criterion c1, c2;
  
  private LogicalORCriterion(Criterion c1, Criterion c2) {
    this.c1 = c1;
    this.c2 = c2;
  }
  
  public Criterion getArg1() {
    return c1;
  }
  
  public Criterion getArg2() {
    return c2;
  }
  
  public int match(ProxyletData data) {
    int m1 = c1.match(data);
    if (m1 == TRUE)
      return TRUE;
    if (m1 == FALSE)
      return c2.match(data);
    int m2 = c2.match(data);
    if (m2 == TRUE)
      return TRUE;
    if (m2 == FALSE)
      return m1;
    return m1 * m2;
  }
  
  public String toString() {
    return ("[ " + c1.toString() + " " + Utils.OR + " " + c2.toString() + " ]");
  }
  
  public static Criterion getInstance(Criterion c1, Criterion c2) {
    if (c1.includes(c2))
      return c2;
    if (c2.includes(c1))
      return c1;
    
    if (c1 instanceof FalseCriterion)
      return c2;
    if (c2 instanceof FalseCriterion)
      return c1;
    
    if (areOpposite(c1, c2))
      return TrueCriterion.getInstance();
    
    return new LogicalORCriterion(c1, c2);
  }
  
  public boolean includes(Criterion c) {
    // A implies X and B implies X <-> (A || B) implies X
    return (c1.includes(c) && c2.includes(c));
  }
  
  public boolean excludes(Criterion c) {
    // A excludes X and B excludes X <-> (A || B) excludes X
    return (c1.excludes(c) && c2.excludes(c));
  }
  
  public int getDepth() {
    return 1 + c1.getDepth() + c2.getDepth();
  }
  
}
