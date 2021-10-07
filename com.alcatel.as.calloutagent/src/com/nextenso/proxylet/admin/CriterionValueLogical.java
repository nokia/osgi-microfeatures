package com.nextenso.proxylet.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Logical criterion abstract class
 */
public class CriterionValueLogical extends CriterionValue implements Cloneable {
  String _value = null;
  List<CriterionValue> _criterions = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tag name of the criterion.
   */
  public CriterionValueLogical(String tagName) {
    super(tagName);
    _criterions = new ArrayList<CriterionValue>();
  }
  
  /**
   * Gets the list of values.
   * @return The list of values.
   */
  public Iterator<CriterionValue> getCriterionValues() {
    return _criterions.iterator();
  }
  
  /**
   * Adds a value in the list of values.
   * @param value The value to be added.
   */
  public void addCriterionValue(CriterionValue value) {
    _criterions.add(value);
  }
  
  /**
   * Reads only one criterion.
   * @param node The node to ceanalysed
   */
  protected CriterionValue readCriterion(Node node) {
    String nnVal = node.getNodeName().trim();
    CriterionValue res = Criterion.getCriterionValueInstance(nnVal);
    
    if (res != null)
      res.setNode(node);
    
    return res;
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
      CriterionValue val = readCriterion(n);
      if (val != null)
        addCriterionValue(val);
    }
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    
    res.append("           <" + getTagName() + ">\n");
    // Criterions
    Iterator it = getCriterionValues();
    while (it.hasNext()) {
      XMLable cr = (XMLable) it.next();
      res.append(cr.toXML());
    }
    res.append("           </" + getTagName() + ">\n");
    
    return res.toString();
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    CriterionValueLogical toRet = new CriterionValueLogical(new String(this.getTagName()));
    Iterator critIt = getCriterionValues();
    while (critIt.hasNext()) {
      CriterionValue aNewValue = Criterion.getCriterionValueInstance(((CriterionValue) critIt.next())
          .getTagName());
      toRet.addCriterionValue(aNewValue);
    }
    return toRet;
  }
  
  /**
   * Check if this criterion value is equal to another one
   *
   * @param o Other criterion value to compare to
   * @return true if both criterion are considered equals, false otherwise
   */
  public boolean equals(Object o) {
    if (o == null || !(o instanceof CriterionValueLogical)) {
      return false;
    }
    List<CriterionValue> other = ((CriterionValueLogical) o)._criterions;
    if (_criterions.size() != other.size()) {
      return false;
    }
    for (int i = 0; i < _criterions.size(); ++i) {
      if (!_criterions.get(i).equals(other.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public String toString() {
    return _criterions.get(0).toString();
  }
  
  /**
   * Display the criterion value
   *
   * @return Criterion value
   */
  public String valueToString() {
    return _criterions.get(0).valueToString();
  }
}
