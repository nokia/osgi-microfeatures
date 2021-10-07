package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;

/**
 * Criterion class.
 */
public class CriterionValueEmpty extends CriterionValue implements Cloneable {
  
  /**
   * Builds a new criterion value.
   * @param tagname The tagname of the criterion.
   */
  public CriterionValueEmpty(String tagname) {
    super(tagname);
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("          <" + getTagName() + "/>\n");
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueEmpty toRet = new CriterionValueEmpty(new String(getTagName()));
    return toRet;
  }
  
  /**
   * Check if this criterion value is equal to another one
   *
   * @param o Other criterion value to compare to
   * @return true if both criterion are considered equals, false otherwise
   */
  public boolean equals(Object o) {
    return (o == null || !(o instanceof CriterionValueEmpty)) ? false : true;
  }
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public String toString() {
    return _tagName + "()";
  }
  
  /**
   * Display the criterion value
   *
   * @return Criterion value
   */
  public String valueToString() {
    return "";
  }
}
