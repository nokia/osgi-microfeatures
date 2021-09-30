package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import com.nextenso.proxylet.ProxyletData;

public abstract class Criterion {
  
  public static final int FALSE = 0;
  public static final int TRUE = 1;
  
  /**
   * The method that should be written
   */
  public abstract int match(ProxyletData data);
  
  public abstract int getDepth();
  
  /**
   * Tests whether or not the current criterion implies the criterion passed as
   * a parameter Tests : A implies B (A=this, B=parameter)
   */
  public boolean includes(Criterion c) {
    // we know that A includes (!!A)
    if (c instanceof LogicalNOTCriterion) {
      LogicalNOTCriterion not = (LogicalNOTCriterion) c;
      return (this.excludes(not.getOriginalCriterion()));
    }
    // we know that A implies (A && A)
    if (c instanceof LogicalANDCriterion) {
      LogicalANDCriterion and = (LogicalANDCriterion) c;
      return (this.includes(and.getArg1()) && this.includes(and.getArg2()));
    }
    // we know that A implies (A || B)
    if (c instanceof LogicalORCriterion) {
      LogicalORCriterion or = (LogicalORCriterion) c;
      return (this.includes(or.getArg1()) || this.includes(or.getArg2()));
    }
    return (c instanceof TrueCriterion);
  }
  
  /**
   * Tests whether or not the current criterion excludes the criterion passed as
   * a parameter Tests : A implies !B (A=this, B=parameter)
   */
  public boolean excludes(Criterion c) {
    // we know that A rejects (!A)
    if (c instanceof LogicalNOTCriterion) {
      LogicalNOTCriterion not = (LogicalNOTCriterion) c;
      return (this.includes(not.getOriginalCriterion()));
    }
    // we know that A rejects (!A && B)
    if (c instanceof LogicalANDCriterion) {
      LogicalANDCriterion and = (LogicalANDCriterion) c;
      return (this.excludes(and.getArg1()) || this.excludes(and.getArg2()));
    }
    // we know that A rejects (!A || !A)
    if (c instanceof LogicalORCriterion) {
      LogicalORCriterion or = (LogicalORCriterion) c;
      return (this.excludes(or.getArg1()) && this.excludes(or.getArg2()));
    }
    return (c instanceof FalseCriterion);
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof Criterion) {
      Criterion c = (Criterion) o;
      return (this.includes(c) && c.includes(this));
    }
    return false;
  }
  
  public static boolean areOpposite(Criterion c1, Criterion c2) {
    return (c1.equals(LogicalNOTCriterion.getInstance(c2)));
  }
  
}
