package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Proxylet Param class.
 */

public class Param extends AdminObject implements XMLable, Cloneable {
  String _name = null;
  String _value = null;
  
  /**
   * Builds a new param.
   * @param name The name of the parameter
   * @param value The value of the parameter
   */
  public Param(String name, String value) {
    setName(name);
    setValue(value);
  }
  
  /**
   * Builds a new empty param.
   */
  public Param() {
  }
  
  /**
   * Gets the name of this paramater.
   * @return The name.
   */
  public final String getName() {
    return _name;
  }
  
  /**
   * Sets the name of this parameter.
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
   * Sets the value of this parameter.
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
    res.append("      <" + getTagName() + ">\n");
    res.append("        <name>" + getName() + "</name>\n");
    res.append("        <value>" + getValue() + "</value>\n");
    res.append("      </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return "param";
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    return new Param(new String(getName()), new String(getValue()));
  }
}
