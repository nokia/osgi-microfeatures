// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;

public class CriterionValueData extends CriterionValue implements Cloneable {
  String _value = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueData(String tagName) {
    super(tagName);
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    String v = getNodeValue(node);
    v = ( v != null)?v:""; // IN ORDER TO BE SYMMETRIC WITH THE WRITE
    setValue(v);
  }
  
  /**
   * Gets the value.
   * @return The value.
   */
  public final String getValue() {
    return _value;
  }
  
  /**
   * Sets the value.
   * @param value The value.
   */
  public final void setValue(String value) {
    _value = value;
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("          <" + getTagName() + ">");
    res.append(getValue());
    res.append("</" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueData toRet = new CriterionValueData(new String(getTagName()));
    toRet.setValue(new String(this.getValue()));
    return toRet;
  }
  
  /**
   * Check if this criterion value is equal to another one
   *
   * @param o Other criterion value to compare to
   * @return true if both criterion are considered equals, false otherwise
   */
  public boolean equals(Object o) {
    if (o == null || !(o instanceof CriterionValueData)) {
      return false;
    }
    return _value.equals(((CriterionValueData) o)._value);
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
    return "value=" + _value;
  }
}
