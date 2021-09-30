package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

import com.nextenso.proxylet.ProxyletData;

public class UntilCriterion extends Criterion {
  private CalendarCriterion criterion;
  
  private UntilCriterion(CalendarCriterion criterion) {
    this.criterion = criterion;
  }
  
  protected CalendarCriterion getCriterion() {
    return criterion;
  }
  
  public int match(ProxyletData data) {
    return (criterion.until(Calendar.getInstance())) ? TRUE : FALSE;
  }
  
  public String toString() {
    return ("[ " + Utils.UNTIL + " " + criterion.toString() + " ]");
  }
  
  public static Criterion getInstance(CalendarCriterion c) {
    return new UntilCriterion(c);
  }
  
  public boolean includes(Criterion c) {
    // (until 12) implies (until 10)
    if (c instanceof UntilCriterion) {
      CalendarCriterion cc = ((UntilCriterion) c).getCriterion();
      if (criterion.getClass().getName().equals(cc.getClass().getName()))
        return (criterion.getValue() >= cc.getValue());
      return false;
    }
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    // (until 12) rejects (from 18)
    if (c instanceof FromCriterion) {
      CalendarCriterion cc = ((FromCriterion) c).getCriterion();
      if (criterion.getClass().getName().equals(cc.getClass().getName()))
        return (criterion.getValue() < cc.getValue());
      return false;
    }
    return super.excludes(c);
  }
  
  public int getDepth() {
    return 1 + criterion.getDepth();
  }
}
