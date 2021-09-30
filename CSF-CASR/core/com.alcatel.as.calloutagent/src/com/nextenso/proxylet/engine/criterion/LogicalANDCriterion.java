package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import com.nextenso.proxylet.ProxyletData;

public class LogicalANDCriterion extends Criterion {
  
  private Criterion c1, c2;
  
  private LogicalANDCriterion(Criterion c1, Criterion c2) {
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
    if (m1 == FALSE)
      return FALSE;
    int m2 = c2.match(data);
    return m1 * m2;
  }
  
  public String toString() {
    return ("[ " + c1.toString() + " " + Utils.AND + " " + c2.toString() + " ]");
  }
  
  public static Criterion getInstance(Criterion c1, Criterion c2) {
    if (c1.includes(c2))
      return c1;
    if (c2.includes(c1))
      return c2;
    
    if (c1.excludes(c2))
      return FalseCriterion.getInstance();
    if (c2.excludes(c1))
      return FalseCriterion.getInstance();
    
    return new LogicalANDCriterion(c1, c2);
  }
  
  public boolean includes(Criterion c) {
    // A implies X or B implies X -> (A & B) implies X
    // NOTE: it is NOT an equivalence (ex: X = A&B)
    if (c1.includes(c) || c2.includes(c))
      return true;
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // A excludes X or B excludes X -> (A & B) excludes X
    // NOTE: it is NOT an equivalence (ex: X = !(A&B))
    if (c1.excludes(c) || c2.excludes(c))
      return true;
    return super.excludes(c);
  }
  
  public int getDepth() {
    return 1 + c1.getDepth() + c2.getDepth();
  }
  
}
