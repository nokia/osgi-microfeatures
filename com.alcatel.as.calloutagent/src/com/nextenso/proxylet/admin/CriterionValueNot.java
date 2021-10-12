// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Logical criterion abstract class
 */
public class CriterionValueNot extends CriterionValueLogical {
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueNot(String tagName) {
    super(tagName);
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    boolean cont = true;
    while (i < nb && cont) {
      Node n = list.item(i);
      i++;
      CriterionValue val = readCriterion(n);
      if (val != null) {
        addCriterionValue(val);
        cont = false;
      }
    }
  }
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public String toString() {
    String s = super.toString();
    return "Not." + s;
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
