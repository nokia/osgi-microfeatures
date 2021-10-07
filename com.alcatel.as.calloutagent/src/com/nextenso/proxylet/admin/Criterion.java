package com.nextenso.proxylet.admin;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Criterion class.
 */
public class Criterion extends AdminObject implements ProxyletSetElement, XMLable, Cloneable {
  
  protected static final String CRITERION_TAG = "criterion";
  
  String _description = null;
  String _name = null;
  String _id = null;
  String _setName = null;
  String _setVersion = null;
  CriterionValue _value = null;
  
  public String toString() {
    return new StringBuilder("Criterion[name=").append(_name).append(",id=").append(_id).append(",setName=")
        .append(_setName).append(",setVersion=").append(_setVersion).append(",desc=").append(_description)
        .append(",value=").append((_value != null ? _value.toXML() : null)).toString();
  }
  
  /**
   * Builds a new criterion.
   * @param name The name of the criterion.
   */
  public Criterion(String name) {
    this();
    setName(name);
  }
  
  /**
   * Builds a new empty criterion.
   */
  public Criterion() {
    super();
  }
  
  /** Gets the unique identifier of the criterion
   * @return The Criterion identifier.
   */
  public final String getIdentifier() { // SETID:NAME
  
    if (getName() == null) {
      return null;
    } /* 
         if (getSetID() == null) {
         return getName();
         } 	
      */
    return getName();
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
   * Gets the criterion value.
   * @return The criterion value.
   */
  public final CriterionValue getValue() {
    return _value;
  }
  
  /**
   * Sets the criterion value.
   * @param value The criterion value.
   */
  public void setValue(CriterionValue value) {
    _value = value;
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
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return CRITERION_TAG;
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("      <" + getTagName());
    if ((getSetID() != null) && (!(getSetID().trim().equals("")))) {
      res.append(" " + SET_ID_ATTRIBUTE + "=\"" + getSetID() + "\"");
    }
    res.append(">\n");
    res.append("        <name>" + getName() + "</name>\n");
    
    res.append("        <criterion-value>\n");
    if (getValue() != null)
      res.append(getValue().toXML());
    res.append("        </criterion-value>\n");
    
    if (getDescription() != null)
      res.append("        <description>" + getDescription() + "</description>\n");
    
    res.append("      </" + getTagName() + ">\n\n");
    return res.toString();
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    Element crTag = (Element) node;
    String setID = crTag.getAttribute(SET_ID_ATTRIBUTE);
    setSetID(setID);
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      
      if ("name".equals(nn))
        setName(nv);
      
      else if ("description".equals(nn))
        setDescription(nv);
      
      else if ("criterion-value".equals(nn)) {
        NodeList listVal = n.getChildNodes();
        int nbVal = listVal.getLength();
        int iVal = 0;
        while (iVal < nbVal) {
          Node nVal = listVal.item(iVal);
          iVal++;
          String nnVal = nVal.getNodeName().trim();
          CriterionValue cr = Criterion.getCriterionValueInstance(nnVal);
          if (cr != null) {
            cr.setNode(nVal);
            setValue(cr);
          }
        }
      }
    }
    if (_value != null)
      _value.setCriterionName(getName());
  }
  
  /**
   * Gets an empty instance of criterion value according to type.
   * @param type The type.
   * @return The criterion value.
   */
  public static final CriterionValue getCriterionValueInstance(String type) {
    CriterionValue cr = null;
    
    if ("port".equals(type) || "day".equals(type) || "date".equals(type) || "month".equals(type)
        || "time".equals(type) || "clid".equals(type) || "ipdest".equals(type) || "ipsrc".equals(type)
        || "domain".equals(type) || "path".equals(type) || "reference".equals(type)
        || "client-ip".equals(type) || "sender".equals(type) || "recipient".equals(type)
        || "remote-ip".equals(type) || "local-ip".equals(type) || "local-port".equals(type)
        || "application".equals(type)) {
      cr = new CriterionValueData(type);
    } else if ("and".equals(type) || "or".equals(type)) {
      cr = new CriterionValueLogical(type);
    } else if ("not".equals(type)) {
      cr = new CriterionValueNot(type);
      
    } else if ("header".equals(type) || "rfc822-hdr".equals(type) || "content-type".equals(type)) {
      cr = new CriterionValueNamed(type);
    } else if ("session-attr".equals(type) || "message-attr".equals(type) || "segment-attr".equals(type)) {
      cr = new CriterionValueNamedWithDesc(type);
    } else if ("until".equals(type) || "from".equals(type)) {
      cr = new CriterionValueDate(type);
    } else if ("type-hex".equals(type) || "type-text".equals(type) || "class-0".equals(type)
        || "class-1".equals(type) || "class-2".equals(type) || "class-3".equals(type)
        || "alphabet-8bit".equals(type) || "alphabet-ucs2".equals(type) || "alphabet-def".equals(type)
        || "all".equals(type) || "client-socket".equals(type) || "server-socket".equals(type)
        || "all-applications".equals(type)) {
      cr = new CriterionValueEmpty(type);
    } else if ("dest-address".equals(type)) {
      cr = new CriterionValueData(type);
    }
    
    return cr;
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
   * Clonable implementation.
   */
  public Object clone() {
    Criterion toRet = new Criterion();
    toRet.setName(new String(this.getName()));
    toRet.setSetID(new String(this.getSetID()));
    toRet.setDescription((this.getDescription() == null) ? null : new String(this.getDescription()));
    toRet.setValue((CriterionValue) ((CriterionValue) this.getValue().clone()));
    return toRet;
  }
}
