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
public class CriterionValueNamedWithDesc extends CriterionValueNamed implements Cloneable {
  String _description = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueNamedWithDesc(String tagName) {
    super(tagName);
  }
  
  /**
   * Gets the description.
   * @return The description.
   */
  public final String getDescription() {
    return _description;
  }
  
  /**
   * Sets the description.
   * @param description The description.
   */
  public final void setDescription(String description) {
    if (description == null || "".equals(description.trim()))
      _description = null;
    else
      _description = description;
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    super.setNode(node);
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      if ("description".equals(nn))
        setDescription(nv);
    }
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("          <" + getTagName() + ">\n");
    res.append("            <name>" + getName() + "</name>\n");
    res.append("            <value>" + getValue() + "</value>\n");
    if (getDescription() != null)
      res.append("            <description>" + getDescription() + "</description>\n");
    res.append("          </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueNamedWithDesc toRet = new CriterionValueNamedWithDesc(new String(getTagName()));
    toRet.setName(new String(getName()));
    toRet.setValue(new String(getValue()));
    toRet.setDescription((this.getDescription() == null) ? null : new String(this.getDescription()));
    return toRet;
  }
  
  /**
   * Check if this criterion value is equal to another one
   *
   * @param o Other criterion value to compare to
   * @return true if both criterion are considered equals, false otherwise
   */
  public boolean equals(Object o) {
    if (o == null || !(o instanceof CriterionValueNamedWithDesc)) {
      return false;
    }
    if(_description == null )
    	return false;
    return _description.equals(((CriterionValueNamedWithDesc) o)._description);
  }
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public String toString() {
    return super.toString();
  }
  
  /**
   * Display the criterion value
   *
   * @return Criterion value
   */
  public String valueToString() {
    return super.valueToString();
  }
}
