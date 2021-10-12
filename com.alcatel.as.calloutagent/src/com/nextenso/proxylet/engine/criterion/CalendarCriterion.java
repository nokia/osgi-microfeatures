// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

import java.util.Calendar;

import com.nextenso.proxylet.ProxyletData;

public abstract class CalendarCriterion extends Criterion {
  
  public abstract int getValue();
  
  public abstract int getValue(Calendar calendar);
  
  public boolean match(Calendar calendar) {
    return (getValue(calendar) == getValue());
  }
  
  public boolean from(Calendar calendar) {
    return (getValue(calendar) >= getValue());
  }
  
  public boolean until(Calendar calendar) {
    return (getValue(calendar) <= getValue());
  }
  
  @Override
  public int match(ProxyletData data) {
    return (match(Calendar.getInstance())) ? TRUE : FALSE;
  }
  
  @SuppressWarnings("unused")
  public static Criterion getInstance(String value) throws CriterionException {
    if ("*".equals(value) || "**".equals(value))
      return TrueCriterion.getInstance();
    return null;
  }
  
  @Override
  public boolean includes(Criterion c) {
    if (c instanceof FromCriterion) {
      // 12 includes from 10
      CalendarCriterion cc = ((FromCriterion) c).getCriterion();
      if (getClass().getName().equals(cc.getClass().getName()))
        return (getValue() >= cc.getValue());
      return false;
    }
    if (c instanceof UntilCriterion) {
      // 12 includes until 20
      CalendarCriterion cc = ((UntilCriterion) c).getCriterion();
      if (getClass().getName().equals(cc.getClass().getName()))
        return (getValue() <= cc.getValue());
      return false;
    }
    return super.includes(c);
  }
  
  @Override
  public boolean excludes(Criterion c) {
    if (c instanceof FromCriterion) {
      // 12 excludes from 15
      CalendarCriterion cc = ((FromCriterion) c).getCriterion();
      if (getClass().getName().equals(cc.getClass().getName()))
        return (getValue() < cc.getValue());
      return false;
    }
    if (c instanceof UntilCriterion) {
      // 12 excludes until 10
      CalendarCriterion cc = ((UntilCriterion) c).getCriterion();
      if (getClass().getName().equals(cc.getClass().getName()))
        return (getValue() > cc.getValue());
      return false;
    }
    return super.excludes(c);
  }
}
