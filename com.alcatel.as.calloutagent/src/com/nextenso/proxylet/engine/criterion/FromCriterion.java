// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

// Callout
import java.util.Calendar;

import com.nextenso.proxylet.ProxyletData;

public class FromCriterion extends Criterion {
  
  private CalendarCriterion _criterion;
  
  private FromCriterion(CalendarCriterion criterion) {
    _criterion = criterion;
  }
  
  protected CalendarCriterion getCriterion() {
    return _criterion;
  }
  
  @Override
  public int match(ProxyletData data) {
    return (_criterion.from(Calendar.getInstance())) ? TRUE : FALSE;
  }
  
  @Override
  public String toString() {
    return ("[ " + Utils.FROM + " " + _criterion.toString() + " ]");
  }
  
  public static Criterion getInstance(CalendarCriterion c) {
    return new FromCriterion(c);
  }
  
  @Override
  public boolean includes(Criterion c) {
    // (from 10) implies (from 12)
    if (c instanceof FromCriterion) {
      CalendarCriterion cc = ((FromCriterion) c).getCriterion();
      if (_criterion.getClass().getName().equals(cc.getClass().getName()))
        return (_criterion.getValue() <= cc.getValue());
      return false;
    }
    return super.includes(c);
  }
  
  @Override
  public boolean excludes(Criterion c) {
    // (from 21) rejects (until 18)
    if (c instanceof UntilCriterion) {
      CalendarCriterion cc = ((UntilCriterion) c).getCriterion();
      if (_criterion.getClass().getName().equals(cc.getClass().getName()))
        return (_criterion.getValue() > cc.getValue());
      return false;
    }
    return super.excludes(c);
  }
  
  @Override
  public int getDepth() {
    return 1 + _criterion.getDepth();
  }
}
