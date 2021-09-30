package com.nextenso.proxylet.engine.criterion;

// ProxyletAPI
import com.nextenso.proxylet.ProxyletData;

public class LogicalNOTCriterion extends Criterion {
  
  private Criterion criterion;
  
  private LogicalNOTCriterion(Criterion c) {
    this.criterion = c;
  }
  
  public Criterion getOriginalCriterion() {
    return criterion;
  }
  
  public int match(ProxyletData data) {
    int m = criterion.match(data);
    if (m == TRUE)
      return FALSE;
    if (m == FALSE)
      return TRUE;
    return m;
  }
  
  public String toString() {
    return ("[" + Utils.NOT + criterion.toString() + "]");
  }
  
  public static Criterion getInstance(Criterion c) {
    if (c instanceof TrueCriterion)
      return FalseCriterion.getInstance();
    if (c instanceof FalseCriterion)
      return TrueCriterion.getInstance();
    if (c instanceof LogicalNOTCriterion)
      return ((LogicalNOTCriterion) c).getOriginalCriterion();
    if (c instanceof LogicalANDCriterion) {
      LogicalANDCriterion and = (LogicalANDCriterion) c;
      return (LogicalORCriterion.getInstance(LogicalNOTCriterion.getInstance(and.getArg1()),
          LogicalNOTCriterion.getInstance(and.getArg2())));
    }
    if (c instanceof LogicalORCriterion) {
      LogicalORCriterion or = (LogicalORCriterion) c;
      return (LogicalANDCriterion.getInstance(LogicalNOTCriterion.getInstance(or.getArg1()),
          LogicalNOTCriterion.getInstance(or.getArg2())));
    }
    return new LogicalNOTCriterion(c);
  }
  
  public boolean includes(Criterion c) {
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // !X implies !A <-> A implies X
    return (c.includes(criterion));
  }
  
  public int getDepth() {
    return 1 + criterion.getDepth();
  }
  
}
