package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;

public class ConstraintValueData extends ConstraintValue implements Cloneable {
  String _value = null;
  
  /**
   * Builds a new constraint value.
   * @param tagname The tag name of the constraint.
   */
  public ConstraintValueData(String tagName) {
    super(tagName);
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    String v = getNodeValue(node);
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
    ConstraintValueData toRet = new ConstraintValueData(new String(getTagName()));
    toRet.setValue(new String(this.getValue()));
    return toRet;
  }
}
