// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Proxylet class.
 */
public class Proxylet extends AdminObject implements ProxyletSetElement, XMLable, Cloneable {
  static final String PROXYLET_TAG = "proxylet";
  static final String PROXYLET_NAME_TAG = "name";
  static final String PROXYLET_DESCRIPTION_TAG = "description";
  static final String PROXYLET_PROTOCOL_TAG = "protocol";
  static final String PROXYLET_CLASS_TAG = "class";
  static final String PROXYLET_PARAM_TAG = "param";
  static final String PROXYLET_DIAMETER_APPLICATION_TAG = "diameter-application";
  
  static final String IS_ACTIVATED_ATTRIBUTE = "activated";
  static final String IS_HIDDEN_ATTRIBUTE = "hidden";
  
  List _orderedParams = null;
  String _name = null;
  String _description = null;
  String _protocol = null;
  String _diameterapplication = null;
  
  String _pClassName = null;
  Map _params = null;
  Criterion _criterion = null;
  String _id = null;
  String _setName = null;
  String _setVersion = null;
  boolean _isActivated = true;
  boolean _isHidden = false;
  LinkedList _constraints = null;
  
  public String toString() {
    StringBuilder sb = new StringBuilder("Proxylet-")
        .append(_protocol)
        .append("[name=")
        .append(_name)
        .append(", class=")
        .append(_pClassName)
        .append(", params=")
        .append(_params)
        .append(", criterion=")
        .append(
            ((_criterion != null && _criterion.getValue() != null) ? _criterion.getValue().toXML() : null))
        .append(", setname=").append(_setName).append(", setversion=").append(_setVersion).append(", id=")
        .append(_id).append(", active=").append(_isActivated);
    if (_diameterapplication != null) {
      sb.append(", diameterapplication=").append(_diameterapplication);
    }
    return sb.toString();
  }
  
  /**
   * Builds a new proxylet.
   */
  public Proxylet() {
    super();
    _params = new HashMap();
    _orderedParams = new ArrayList();
    _constraints = new LinkedList();
  }
  
  /**
   * Builds a new proxylet.
   * @param name The name of the parameter
   */
  public Proxylet(String name) {
    this();
    setName(name);
  }
  
  /** Gets the unique identifier of the proxylet
   * @return The Proxylet identifier.
   */
  public final String getIdentifier() { // SETID:NAME  
    if (getName() == null) {
      return null;
    }
    return getName() + toString().hashCode();
  }
  
  // ProxyletSetElement Interface
  /**
   * Gets the identifier of the proxylet set the objet belongs to.
   * @return The identifier.
   */
  public String getSetID() {
    return _id;
  }
  
  /**
   * Sets the identifier of the proxylet set the objet belongs to.
   * @param id The identifier.
   */
  public void setSetID(String id) {
    _id = id;
    if (id != null) {
      setSetName(PxletUtils.getNameFromId(id));
      setSetVersion(PxletUtils.getVersionFromId(id));
    }
  }
  
  /**
     Sets the proxy application name.
  */
  public String getSetName() {
    return _setName;
  }
  
  /**
     Gets the proxy application name.
  */
  public void setSetName(String setName) {
    _setName = setName;
  }
  
  /**
     Gets the proxy application version.
  */
  public String getSetVersion() {
    return _setVersion;
  }
  
  /**
     Sets the proxy application version.
  */
  public void setSetVersion(String setVersion) {
    _setVersion = setVersion;
  }
  
  /** 
   * Gets the isActivated parameter of this proxylet.
   * @return boolean True if this proxylet is activated, False otherwise.
   */
  public boolean getIsActivated() {
    return _isActivated;
  }
  
  /** 
   * Sets the isActivated parameter of this Proxylet.
   * @param isActivated The boolean.
   */
  public void setIsActivated(boolean isActivated) {
    _isActivated = isActivated;
  }
  
  /** 
   * Gets the isHidden parameter of this Proxylet.
   * @return true if this proxylet is hidden, false otherwise.
   */
  public boolean getIsHidden() {
    return _isHidden;
  }
  
  /** 
   * Sets the hidden parameter of this proxylet.
   * @param isHidden true if this proxylet is hidden.
   */
  public void setIsHidden(boolean isHidden) {
    _isHidden = isHidden;
  }
  
  /**
   * Adds a constraint.
   * @param constraint The constraint to be added.
   */
  public void addConstraint(Constraint constraint) {
    _constraints.add(constraint);
  }
  
  /**
   * Removes a constraint.
   * @param constraint The constraint to be removed.
   */
  public void removeConstraint(Constraint constraint) {
    _constraints.remove(constraint);
  }
  
  /**
   * Clears constraints
   */
  public void clearConstraints() {
    _constraints.clear();
  }
  
  /**
   * Gets the list of the constraints.
   * @return The list of the constraints.
   */
  public Iterator getConstraints() {
    return _constraints.iterator();
  }
  
  /**
   * Gets the name.
   * @return The name.
   */
  public final String getName() {
    return _name;
  }
  
  /**
   * Sets the name.
   * @param name The name.
   */
  public final void setName(String name) {
    _name = name;
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
    _description = description;
  }
  
  /**
   * Gets the protocol.
   * @return The protocol.
   */
  public final String getProtocol() {
    return _protocol;
  }
  
  /**
   * Sets the protocol.
   * @param protocol The protocol.
   */
  public final void setProtocol(String protocol) {
    _protocol = protocol;
  }
  
  /**
  * Gets the diameter application.
  * @return The diameter application.
  */
  public final String getDiameterApplication() {
    return _diameterapplication;
  }
  
  /**
   * Sets the diameter application.
   * @param diameterapplication.The diameter application.
   */
  public final void setDiameterApplication(String diameterapplication) {
    _diameterapplication = diameterapplication;
  }
  
  /**
   * Gets the class name.
   * @return The class name.
   */
  public final String getClassName() {
    return _pClassName;
  }
  
  /**
   * Sets the class name.
   * @param className The class ,ame.
   */
  public final void setClassName(String className) {
    _pClassName = className;
  }
  
  /**
   * Modifies a param.
   * <BR>If the parameter does not exist, it is added.
   * <BR>If the parameter value is null, it is removed.
   * @param name The name of the param to be added.
   * @param value The value of the param to be added.
   */
  public void setParam(String name, String value) {
    setParam(new Param(name, value));
  }
  
  /**
   * Clears all the parameters.
   */
  public void clearParams() {
    _orderedParams.clear();
    _params.clear();
  }
  
  /**
   * Gets the list of the params.
   * @return The list of the params.
   */
  public Iterator getParams() {
    return _orderedParams.iterator();
  }
  
  /**
   * Modifies a param.
   * <BR>If the param does not exist, it is added. 
   * <BR>If the parameter value is null, it is removed.
   * @param param The param to be modified.
   */
  public void setParam(Param param) {
    if (param == null || param.getName() == null)
      return;
    
    Param p = (Param) _params.get(param.getName());
    
    if (p != null && param.getValue() == null) {
      _params.put(param.getName(), null);
      try {
        _orderedParams.remove(p);
      } catch (Exception e) {
      }
    } else {
      _params.put(param.getName(), param);
      if (p != null) {
        int i = _orderedParams.indexOf(p);
        if (i >= 0)
          _orderedParams.set(i, param);
      } else {
        _orderedParams.add(param);
      }
    }
  }
  
  /**
   * Gets the criterion.
   * @return The criterion.
   */
  public final Criterion getCriterion() {
    return _criterion;
  }
  
  /**
   * Sets the criterion.
   * @param criterion The criterion.
   */
  public final void setCriterion(Criterion criterion) {
    _criterion = criterion;
  }
  
  /**
   * Gets a new instance of Criterion.
   * <BR>This method can be redefined by inherited classes if needed.
   * @param type The type of the criterion.
   * @return The new criterion object.
   */
  public Criterion getCriterionInstance() {
    return new Criterion();
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return PROXYLET_TAG;
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("\n    <" + getTagName());
    
    if (getSetID() != null && (!"".equals(getSetID().trim()))) {
      res.append(" " + SET_ID_ATTRIBUTE + "=\"" + getSetID() + "\"");
    }
    
    /*
      if ((getSetName()!=null)&&(!(getSetName().trim().equals("")))) {
      res.append(" "+SET_NAME+"=\""+getSetName()+"\"");
      }
      if ((getSetVersion()!=null)&&(!(getSetVersion().trim().equals("")))) {
      res.append(" "+SET_VERSION+"=\""+getSetVersion()+"\"");
      }
    */
    
    if (!getIsActivated()) {
      res.append(" " + IS_ACTIVATED_ATTRIBUTE + "=\"false\"");
    }
    if (getIsHidden()) {
      res.append(" " + IS_HIDDEN_ATTRIBUTE + "=\"true\"");
    }
    res.append(">\n");
    res.append("      <" + PROXYLET_NAME_TAG + ">" + getName() + "</" + PROXYLET_NAME_TAG + ">\n");
    
    if (getDescription() != null)
      res.append("      <" + PROXYLET_DESCRIPTION_TAG + ">" + getDescription() + "</"
          + PROXYLET_DESCRIPTION_TAG + ">\n");
    
    if (getProtocol() != null)
      res.append("      <" + PROXYLET_PROTOCOL_TAG + ">" + getProtocol() + "</" + PROXYLET_PROTOCOL_TAG
          + ">\n");
    
    if (getDiameterApplication() != null)
      res.append("      <" + PROXYLET_DIAMETER_APPLICATION_TAG + ">" + getDiameterApplication() + "</"
          + PROXYLET_DIAMETER_APPLICATION_TAG + ">\n");
    
    res.append("      <" + PROXYLET_CLASS_TAG + ">" + getClassName() + "</" + PROXYLET_CLASS_TAG + ">\n");
    
    // Params
    Iterator it = getParams();
    while (it.hasNext()) {
      XMLable param = (XMLable) it.next();
      if (param != null) {
        res.append(param.toXML());
      }
    }
    // Criterion
    if (getCriterion() != null) {
      res.append(getCriterion().toXML());
    }
    
    // Constraints
    Iterator itc = getConstraints();
    while (itc.hasNext()) {
      XMLable constraint = (XMLable) itc.next();
      if (constraint != null) {
        res.append(constraint.toXML());
      }
    }
    res.append("    </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    Element pTag = (Element) node;
    
    String setId = pTag.getAttribute(SET_ID_ATTRIBUTE);
    setSetID(setId);
    
    String isAct = pTag.getAttribute(IS_ACTIVATED_ATTRIBUTE);
    if ("false".equalsIgnoreCase(isAct)) {
      setIsActivated(false);
    }
    String isHidden = pTag.getAttribute(IS_HIDDEN_ATTRIBUTE);
    if ("true".equalsIgnoreCase(isHidden)) {
      setIsHidden(true);
    }
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      // get name
      if (PROXYLET_NAME_TAG.equals(nn)) {
        setName(nv);
      } else if (PROXYLET_DESCRIPTION_TAG.equals(nn)) {
        setDescription(nv);
      } else if (PROXYLET_PROTOCOL_TAG.equals(nn)) {
        setProtocol(nv);
      } else if (PROXYLET_DIAMETER_APPLICATION_TAG.equals(nn)) {
        setDiameterApplication(nv);
      } else if (PROXYLET_CLASS_TAG.equals(nn)) {
        setClassName(nv);
      } else if (PROXYLET_PARAM_TAG.equals(nn)) {
        Param param = new Param();
        param.setNode(n);
        setParam(param);
      } else if (Criterion.CRITERION_TAG.equals(nn)) {
        Criterion cr = getCriterionInstance();
        cr.setNode(n);
        setCriterion(cr);
      } else if (Constraint.CONSTRAINT_TAG.equals(nn)) {
        Constraint cs = new Constraint(null, getName());
        cs.setNode(n);
        addConstraint(cs);
      }
    }
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    Proxylet toRet = new Proxylet();
    toRet.setSetID(new String(this.getSetID()));
    toRet.setName(new String(this.getName()));
    toRet.setDescription((this.getDescription() == null) ? null : new String(this.getDescription()));
    toRet.setProtocol((this.getProtocol() == null) ? null : new String(this.getProtocol()));
    toRet.setDiameterApplication((this.getDiameterApplication() == null) ? null : new String(this
        .getDiameterApplication()));
    toRet.setClassName((this.getClassName() == null) ? null : new String(this.getClassName()));
    
    toRet.setIsActivated(this.getIsActivated());
    toRet.setIsHidden(this.getIsHidden());
    
    // ..................param
    Iterator paramIt = this.getParams();
    while (paramIt.hasNext()) {
      Param newParam = (Param) (((Param) paramIt.next()).clone());
      toRet.setParam(newParam);
    }
    // ..................contraint
    Iterator consIt = this.getConstraints();
    while (consIt.hasNext()) {
      Constraint newConstraint = (Constraint) (((Constraint) consIt.next()).clone());
      toRet.addConstraint(newConstraint);
    }
    //...................criterion
    Criterion newCriterion = (Criterion) (getCriterion().clone());
    toRet.setCriterion(newCriterion);
    return toRet;
  }
  
}
