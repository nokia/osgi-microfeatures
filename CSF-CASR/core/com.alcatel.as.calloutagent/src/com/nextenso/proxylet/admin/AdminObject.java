package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;

public abstract class AdminObject {
  
  String _className = null;
  
  /**
   * Abstract builder.
   */
  protected AdminObject() {
    String name = getClass().getName();
    int index = name.lastIndexOf(".");
    _className = name.substring(index + 1);
  }
  
  /**
   * Gets The node value of text node.
   * @param node The node.
   * @return The node value.
   */
  public String getNodeValue(Node node) {
    Node fc = node.getFirstChild();
    String s = null;
    if (fc != null)
      s = fc.getNodeValue();
    if (s != null)
      s = s.trim();
    return s;
  }
  
}
