// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

// Callout
import com.nextenso.proxylet.ProxyletData;

public class MessageAttrCriterion extends RegexpCriterion {
  
  private String name;
  
  private MessageAttrCriterion(String name, String regexp) throws CriterionException {
    super(regexp);
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_MESSAGE_ATTR, null);
    this.name = name;
  }
  
  private MessageAttrCriterion(String name) throws CriterionException {
    super();
    if (name == null || name.length() == 0)
      throw new CriterionException(CriterionException.INVALID_MESSAGE_ATTR, null);
    this.name = name;
  }
  
  public String getAttrName() {
    return name;
  }
  
  public int match(ProxyletData data) {
    Object o = data.getAttribute (name);
    if (o == null) return FALSE;
    return (match(o.toString ())) ? TRUE : FALSE;
  }
  
  public String toString() {
    String value = getRegexp();
    if (value == null)
      value = "*";
    return ("[" + Utils.MESSAGE_ATTR + "=" + name + ':' + value + "]");
  }
  
  public static Criterion getInstance(String name, String regexp) throws CriterionException {
    Criterion c = RegexpCriterion.getInstance(regexp);
    return (c != null) ? new MessageAttrCriterion(name) : new MessageAttrCriterion(name, regexp);
  }
  
  public boolean includes(Criterion c) {
    if (c instanceof MessageAttrCriterion) {
      boolean b = ((MessageAttrCriterion) c).getAttrName().equals(name);
      return (b && super.includes(c));
    }
    return super.includes(c);
  }
  
  public boolean excludes(Criterion c) {
    if (c instanceof MessageAttrCriterion) {
      boolean b = ((MessageAttrCriterion) c).getAttrName().equals(name);
      return (b && super.excludes(c));
    }
    return super.excludes(c);
  }
  
  public boolean equals(Object o) {
    if (o instanceof MessageAttrCriterion) {
      boolean b = ((MessageAttrCriterion) o).getAttrName().equals(name);
      return (b && super.equals(o));
    }
    return false;
  }
}
