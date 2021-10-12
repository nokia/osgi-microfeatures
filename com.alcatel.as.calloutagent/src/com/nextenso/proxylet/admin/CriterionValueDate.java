// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Criterion with name and value.
 */
public class CriterionValueDate extends CriterionValue implements Cloneable {
  CriterionValue _criterion = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueDate(String tagName) {
    super(tagName);
  }
  
  /**
   * Gets the criterion of this paramater.
   * @return The criterion.
   */
  public final CriterionValue getCriterion() {
    return _criterion;
  }
  
  /**
   * Sets the criterion of this criterion.
   * @param criterion The criterion.
   */
  public final void setCriterion(CriterionValue criterion) {
    _criterion = criterion;
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      
      String nn = n.getNodeName().trim();
      if ("day".equals(nn) || "date".equals(nn) || "month".equals(nn) || "time".equals(nn)) {
        CriterionValue cr = Criterion.getCriterionValueInstance(nn);
        cr.setNode(n);
        setCriterion(cr);
        return;
      }
    }
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("          <" + getTagName() + ">\n");
    res.append(getCriterion().toXML());
    res.append("          </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueDate toRet = new CriterionValueDate(new String(this.getTagName()));
    toRet.setCriterion(Criterion.getCriterionValueInstance(this.getTagName()));
    return toRet;
  }
  
  /**
   * Check if this criterion value is equal to another one
   *
   * @param o Other criterion value to compare to
   * @return true if both criterion are considered equals, false otherwise
   */
  public boolean equals(Object o) {
    if (o == null || !(o instanceof CriterionValueDate)) {
      return false;
    }
    return _criterion.equals(((CriterionValueDate) o)._criterion);
  }
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public String toString() {
    return _tagName + "(" + valueToString() + ")";
  }
  
  /**
   * Display the criterion value
   *
   * @return Criterion value
   */
  public String valueToString() {
    return "type:" + _criterion.getTagName() + "," + _criterion.valueToString();
  }
}
