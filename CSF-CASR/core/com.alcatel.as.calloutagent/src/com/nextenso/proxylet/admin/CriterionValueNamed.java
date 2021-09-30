package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Criterion with name and value.
 */
public class CriterionValueNamed extends CriterionValue implements Cloneable {
  String _name = null;
  String _value = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueNamed(String tagName) {
    super(tagName);
  }
  
  /**
   * Gets the name of this paramater.
   * @return The name.
   */
  public final String getName() {
    return _name;
  }
  
  /**
   * Sets the name of this criterion.
   * @param name The name.
   */
  public final void setName(String name) {
    _name = name;
  }
  
  /**
   * Gets the value of this paramater.
   * @return The value.
   */
  public final String getValue() {
    return _value;
  }
  
  /**
   * Sets the value of this criterion.
   * @param value The value.
   */
  public final void setValue(String value) {
    _value = value;
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
      String nv = getNodeValue(n);
      nv = ( nv != null)?nv:""; // IN ORDER TO BE SYMMETRIC WITH THE WRITE
      if ("name".equals(nn))
        setName(nv);
      else if ("value".equals(nn))
        setValue(nv);
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
    res.append("          </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueNamed toRet = new CriterionValueNamed(new String(getTagName()));
    toRet.setName(new String(this.getName()));
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
    if (o == null || !(o instanceof CriterionValueNamed)) {
      return false;
    }
    if (!(_name.equals(((CriterionValueNamed) o)._name) && _value.equals(((CriterionValueNamed) o)._value))) {
      return false;
    }
    if (this instanceof CriterionValueNamedWithDesc) {
      return ((CriterionValueNamedWithDesc) this).equals(o);
    }
    return (o instanceof CriterionValueNamedWithDesc) ? false : true;
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
    String ret = "name:" + _name + ",value:" + _value;
    if (this instanceof CriterionValueNamedWithDesc) {
      ret = ret + ",desc:" + ((CriterionValueNamedWithDesc) this)._description;
    }
    return ret;
  }
}
