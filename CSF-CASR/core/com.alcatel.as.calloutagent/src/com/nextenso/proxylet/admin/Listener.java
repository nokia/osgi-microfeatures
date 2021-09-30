package com.nextenso.proxylet.admin;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Listener class.
 */
public class Listener extends AdminObject implements ProxyletSetElement, XMLable {
  static final String LISTENER_NAME_TAG = "name";
  static final String LISTENER_CLASS_TAG = "class";
  static final String LISTENER_REFERENCE_TAG = "reference";
  
  protected static final String IS_ACTIVATED_ATTRIBUTE = "activated";
  String _identifier = null;
  String _type = null;
  String _pClassName = null;
  String _pGivenName = null;
  String _pReference = null;
  
  String _id = null;
  String _setName = null;
  String _setVersion = null;
  boolean _isActivated = true;
  
  public String toString() {
    return new StringBuilder("Listener[type=").append(_type).append(", class=").append(_pClassName)
        .append(", givenname=").append(_pGivenName).append(", ref=").append(_pReference).append(", setname=")
        .append(_setName).append(", setversion=").append(_setVersion).append(", id=").append(_id)
        .append(", active=").append(_isActivated).toString();
  }
  
  String getIdent() {
    return "listen_" + toString().hashCode();
  }
  
  private Listener() {
    super();
    _identifier = getIdent();
  }
  
  /**
   * Builds a new listener.
   * @param type The type of the listener.
   */
  public Listener(String type) {
    this();
    setType(type);
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
  
  public String getDisplaySetVersion() {
    /*FIXME ProxyAppBean appBean = ProxyAppBeanFactory.getProxyAppBean(_id);
    if (appBean != null) {
    return appBean.getDisplayVersion();
    } else {*/
    return _setVersion;
    //}
  }
  
  /**
    Sets the proxy application version.
  */
  public void setSetVersion(String setVersion) {
    _setVersion = setVersion;
  }
  
  /** 
   * Gets the isActivated parameter of this ProxyletSet Element.
   * @return boolean True if this ProxyletSet Element is activated, False otherwise.
   */
  public boolean getIsActivated() {
    return _isActivated;
  }
  
  /** 
   * Sets the isActivated parameter of this ProxyletSet Element.
   * @param isActivated the boolean.
   */
  public void setIsActivated(boolean isActivated) {
    _isActivated = isActivated;
  }
  
  /**
   * Gets the type.
   * @return The type.
   */
  public final String getType() {
    return _type;
  }
  
  /**
   * Sets the type.
   * @param type The type.
   */
  public final void setType(String type) {
    _type = type;
  }
  
  /**
   * Gets the className.
   * @return The className.
   */
  public final String getClassName() {
    return _pClassName;
  }
  
  /**
   * Sets the class name.
   * @param className The class name.
   */
  public final void setClassName(String classname) {
    _pClassName = classname;
  }
  
  /**
  * Gets the givenName.
  * @return The givenName.
  */
  public final String getGivenName() {
    return _pGivenName;
  }
  
  /**
   * Sets the given name.
   * @param givenName The given name.
   */
  public final void setGivenName(String givenname) {
    _pGivenName = givenname;
  }
  
  /**
  * Gets the reference.
  * @return The reference
  */
  public final String getReference() {
    return _pReference;
  }
  
  /**
   * Sets the reference
   * @param reference The reference
   */
  public final void setReference(String reference) {
    _pReference = reference;
  }
  
  /**
   * Gets the identifier.
   * @return The identifier.
   */
  public final String getIdentifier() {
    return _identifier;
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return getType();
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("  <" + getTagName());
    if ((getSetID() != null) && (!(getSetID().trim().equals("")))) {
      res.append(" " + SET_ID_ATTRIBUTE + "=\"" + getSetID() + "\"");
    }
    if (getIsActivated() == false) {
      res.append(" " + IS_ACTIVATED_ATTRIBUTE + "=\"false\"");
    }
    res.append(">\n");
    if (getGivenName() != null)
      res.append("    <" + LISTENER_NAME_TAG + ">" + getGivenName() + "</" + LISTENER_NAME_TAG + ">\n");
    
    if (getClassName() != null)
      res.append("    <" + LISTENER_CLASS_TAG + ">" + getClassName() + "</" + LISTENER_CLASS_TAG + ">\n");
    
    if (getReference() != null)
      res.append("    <" + LISTENER_REFERENCE_TAG + ">" + getReference() + "</" + LISTENER_REFERENCE_TAG
          + ">\n");
    
    res.append("  </" + getTagName() + ">\n");
    return res.toString();
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    Element lTag = (Element) node;
    String setId = lTag.getAttribute(SET_ID_ATTRIBUTE);
    setSetID(setId);
    String isAct = lTag.getAttribute(IS_ACTIVATED_ATTRIBUTE);
    if ((isAct != null) && (isAct.equalsIgnoreCase("false"))) {
      setIsActivated(false);
    }
    
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      if (LISTENER_CLASS_TAG.equals(nn)) {
        setClassName(nv);
      } else if (LISTENER_NAME_TAG.equals(nn)) {
        setGivenName(nv);
      } else if (LISTENER_REFERENCE_TAG.equals(nn)) {
        setReference(nv);
      }
    }
  }
}
