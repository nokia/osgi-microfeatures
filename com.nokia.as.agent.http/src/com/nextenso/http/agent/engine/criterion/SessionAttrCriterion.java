// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.RegexpCriterion;
import com.nextenso.proxylet.engine.criterion.Utils;
import com.nextenso.proxylet.http.HttpMessage;

public class SessionAttrCriterion extends RegexpCriterion {
  
  private String name;
  
  private SessionAttrCriterion(String name, String regexp) throws CriterionException {
    super(regexp);
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_SESSION_ATTR, null);
    this.name = name;
  }
  
  private SessionAttrCriterion(String name) throws CriterionException {
    super();
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_SESSION_ATTR, null);
    this.name = name;
  }
  
  public String getAttrName() {
    return name;
  }
  
  public int match(ProxyletData data) {
    HttpMessage message = (HttpMessage) data;
    return (match((String) message.getSession().getAttribute(name))) ? TRUE : FALSE;
  }
  
  public String toString() {
    String value = getRegexp();
    if (value == null)
      value = "*";
    return ("[" + Utils.SESSION_ATTR + "=" + name + ':' + value + "]");
  }
  
  public static Criterion getInstance(String name, String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? new SessionAttrCriterion(name) : new SessionAttrCriterion(name, regexp);
  }
  
  public boolean includes(Criterion c) {
    if (c instanceof SessionAttrCriterion) {
      boolean b = ((SessionAttrCriterion) c).getAttrName().equals(name);
      return (b && super.includes(c));
    }
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    if (c instanceof SessionAttrCriterion) {
      boolean b = ((SessionAttrCriterion) c).getAttrName().equals(name);
      return (b && super.excludes(c));
    }
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof SessionAttrCriterion) {
      boolean b = ((SessionAttrCriterion) o).getAttrName().equals(name);
      return (b && super.equals(o));
    }
    return false;
  }
}
