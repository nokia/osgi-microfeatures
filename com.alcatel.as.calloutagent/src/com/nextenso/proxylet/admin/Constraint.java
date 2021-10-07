package com.nextenso.proxylet.admin;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Criterion class.
 */
public class Constraint extends AdminObject implements XMLable, Cloneable {
  
  protected static final String CONSTRAINT_TAG = "constraint";
  protected static final String CONSTRAINT_NAME_TAG = "name";
  protected static final String CONSTRAINT_VALUE_TAG = "constraint-value";
  protected static final String CONSTRAINT_DESCRIPTION_TAG = "description";
  public static final String CONSTRAINT_PREFIX_TAG = "prefix";
  public static final String CONSTRAINT_POSTFIX_TAG = "postfix";
  protected static final String CONSTRAINT_UNCOMPATIBLE_WITH_TAG = "uncompatible-with";
  
  String _description = null;
  
  String _name = null;
  
  ConstraintValue _value = null;
  
  //Reference proxylet name: the name of the proxylet to which the constraint applies.
  String _refPxletName = null;
  
  /**
   * Builds a new constraint.
   * @param name The name of the constraint.
   * @param refPxletName The name of the reference proxylet.
   */
  public Constraint(String name, String refPxletName) {
    this(name);
    setRefPxletName(refPxletName);
  }
  
  /**
   * Builds a new constraint.
   * @param name The name of the constraint.
   */
  public Constraint(String name) {
    this();
    setName(name);
  }
  
  /**
   * Builds a new empty constraint.
   */
  public Constraint() {
    super();
  }
  
  /**
   * Apply a constraint rule in given chain.
   */
  public void apply(Chain chain) throws ConstraintException {
    ArrayList list = (ArrayList) chain.getPxletsList();
    
    int refPxletIndex = chain.indexOf(getRefPxletName());
    int argPxletIndex = chain.indexOf(((ConstraintValueData) getValue()).getValue());
    
    if (refPxletIndex == -1) {
      throw new ConstraintException(ConstraintException.UNKNOWN_REFERENCE_PROXYLET,
          "*** Constraint *** apply(" + chain.toString() + ")"
              + " *** ) the reference proxylet is not in this chain. ");
    }
    if (argPxletIndex == -1) {
      throw new ConstraintException(ConstraintException.UNKNOWN_ARGUMENT_PROXYLET,
          "*** Constraint *** apply(" + chain.toString() + ")"
              + " *** ) the argument proxylet is not in this chain. ");
    }
    if (refPxletIndex == argPxletIndex) {
      throw new ConstraintException(ConstraintException.IMPOSSIBLE_CONSTRAINT, "*** Constraint *** apply("
          + chain.toString() + ")" + " *** ) This constraint does not make any sense. ");
    }
    
    if (getValue().getTagName().equals(CONSTRAINT_PREFIX_TAG)) {
      Proxylet refPxlet = (Proxylet) list.remove(refPxletIndex);
      list.add(argPxletIndex, refPxlet);
      //chain.setPxletsList(list);
    } else if (getValue().getTagName().equals(CONSTRAINT_POSTFIX_TAG)) {
      Proxylet refPxlet = (Proxylet) list.remove(refPxletIndex);
      list.add(argPxletIndex + 1, refPxlet);
      //chain.setPxletsList(list);
    } else {
      throw new ConstraintException(ConstraintException.UNKNOWN_OR_UNIMPLEMENTED_CONSTRAINT,
          "*** Constraint *** apply(" + chain.toString() + ")" + " *** unknown or unimplemented constraint ["
              + getValue().getTagName() + "]");
    }
  }
  
  /**
   * Checks a constraint inside the given chain. This method should be overrided by subclasses.
   */
  public boolean check(Chain chain) throws ConstraintException {
    int refPxletIndex = chain.indexOf(getRefPxletName());
    int argPxletIndex = chain.indexOf(((ConstraintValueData) getValue()).getValue());
    
    if (refPxletIndex == -1) {
      throw new ConstraintException(ConstraintException.UNKNOWN_REFERENCE_PROXYLET,
          "*** Constraint *** check(" + chain.toString() + ")"
              + " *** ) the reference proxylet is not in this chain. ");
    }
    
    if (argPxletIndex == -1) {
      // The name of the proxylet is unknown, The decision is to skip it (return as an ok )
      return true;
      //throw new ConstraintException(ConstraintException.UNKNOWN_ARGUMENT_PROXYLET, "*** Constraint *** check(" + chain.toString() + ")" + " *** ) the argument proxylet is not in this chain. ");
    }
    
    if (refPxletIndex == argPxletIndex) {
      throw new ConstraintException(ConstraintException.IMPOSSIBLE_CONSTRAINT, "*** Constraint *** check("
          + chain.toString() + ")" + " *** ) This constraint does not make any sense. ");
    }
    
    if (getValue().getTagName().equals(CONSTRAINT_PREFIX_TAG)) {
      if (refPxletIndex < argPxletIndex) {
        return true;
      } else {
        return false;
      }
    } else if (getValue().getTagName().equals(CONSTRAINT_POSTFIX_TAG)) {
      if (refPxletIndex > argPxletIndex) {
        return true;
      } else {
        return false;
      }
    } else {
      throw new ConstraintException(ConstraintException.UNKNOWN_OR_UNIMPLEMENTED_CONSTRAINT,
          "*** Constraint *** check(" + chain.toString() + ")" + " *** unknown or unimplemented constraint ["
              + getValue().getTagName() + "]");
    }
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
   * Gets the constraint value.
   * @return The constraint value.
   */
  public final ConstraintValue getValue() {
    return _value;
  }
  
  /**
   * Sets the constraint value.
   * @param value The constraint value.
   */
  public void setValue(ConstraintValue value) {
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
   * Gets the name of the refence proxylet.
   * @return The name of the reference proxylet.
   */
  public final String getRefPxletName() {
    return _refPxletName;
  }
  
  /**
   * Sets the name of the reference proxylet.
   * @param name The name of the reference proxylet.
   */
  public final void setRefPxletName(String refPxletName) {
    _refPxletName = refPxletName;
  }
  
  /**
   * Returns a boolean according to the constraint parameters.
   * @param cnst  a Constraint object reference
   * @return a boolean
   */
  public boolean equals(Constraint cnst) {
    boolean condition1 = false;
    boolean condition2 = false;
    boolean condition3 = false;
    
    if (getRefPxletName().equals(((ConstraintValueData) cnst.getValue()).getValue())) {
      condition1 = true;
    }
    if (((ConstraintValueData) getValue()).getValue().equals(cnst.getRefPxletName())) {
      condition2 = true;
    }
    if ((getValue().getTagName().equals(CONSTRAINT_PREFIX_TAG) && cnst.getValue().getTagName()
        .equals(CONSTRAINT_POSTFIX_TAG))
        || (getValue().getTagName().equals(CONSTRAINT_POSTFIX_TAG) && cnst.getValue().getTagName()
            .equals(CONSTRAINT_PREFIX_TAG))) {
      condition3 = true;
    }
    
    if (condition1 && condition2 && condition3) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Returns a boolean if the two constraints are mutually incompatible.
   * @param cnst  a Constraint object reference
   * @return a boolean
   */
  public boolean isOpposite(Constraint cnst) {
    boolean condition1 = false;
    boolean condition2 = false;
    boolean condition3 = false;
    boolean case1 = false;
    
    boolean condition4 = false;
    boolean condition5 = false;
    boolean condition6 = false;
    boolean case2 = false;
    
    if (getRefPxletName().equals(((ConstraintValueData) cnst.getValue()).getValue())) {
      condition1 = true;
    }
    if (((ConstraintValueData) getValue()).getValue().equals(cnst.getRefPxletName())) {
      condition2 = true;
    }
    if (getValue().getTagName().equals(cnst.getValue().getTagName())) {
      condition3 = true;
    }
    
    if (condition1 && condition2 && condition3) {
      case1 = true;
    }
    
    if (getRefPxletName().equals(cnst.getRefPxletName())) {
      condition4 = true;
    }
    if (((ConstraintValueData) getValue()).getValue().equals(
        ((ConstraintValueData) cnst.getValue()).getValue())) {
      condition5 = true;
    }
    if ((getValue().getTagName().equals(CONSTRAINT_PREFIX_TAG) && cnst.getValue().getTagName()
        .equals(CONSTRAINT_POSTFIX_TAG))
        || (getValue().getTagName().equals(CONSTRAINT_POSTFIX_TAG) && cnst.getValue().getTagName()
            .equals(CONSTRAINT_PREFIX_TAG))) {
      condition6 = true;
    }
    
    if (condition4 && condition5 && condition6) {
      case2 = true;
    }
    
    if (case1 || case2) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return CONSTRAINT_TAG;
  }
  
  /**
   * Gets the XML representation.
   * @return The XML representation.
   */
  public String toXML() {
    StringBuffer res = new StringBuffer();
    res.append("      <" + getTagName() + ">\n");
    if (getName() == null) {
      setName("no name");
    }
    res.append("        <" + CONSTRAINT_NAME_TAG + ">" + getName() + "</" + CONSTRAINT_NAME_TAG + ">\n");
    res.append("        <" + CONSTRAINT_VALUE_TAG + ">\n");
    if (getValue() != null)
      res.append(getValue().toXML());
    res.append("        </" + CONSTRAINT_VALUE_TAG + ">\n");
    if (getDescription() != null)
      res.append("        <" + CONSTRAINT_DESCRIPTION_TAG + ">" + getDescription() + "</"
          + CONSTRAINT_DESCRIPTION_TAG + ">\n");
    res.append("      </" + getTagName() + ">\n\n");
    return res.toString();
  }
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node) {
    //Firstly we retrieve the name of the reference proxylet.
    Node pxletNameNode = node.getParentNode().getFirstChild();
    String pxletNameNodeN = pxletNameNode.getNodeName();
    String pxletNameNodeV = pxletNameNode.getNodeValue();
    if (Proxylet.PROXYLET_NAME_TAG.equals(pxletNameNodeN)) {
      setRefPxletName(pxletNameNodeV);
    }
    
    NodeList list = node.getChildNodes();
    int nb = list.getLength();
    int i = 0;
    while (i < nb) {
      Node n = list.item(i);
      i++;
      String nn = n.getNodeName().trim();
      String nv = getNodeValue(n);
      
      if (CONSTRAINT_NAME_TAG.equals(nn)) {
        setName(nv);
      } else if (CONSTRAINT_DESCRIPTION_TAG.equals(nn)) {
        setDescription(nv);
      } else if (CONSTRAINT_VALUE_TAG.equals(nn)) {
        NodeList listVal = n.getChildNodes();
        int nbVal = listVal.getLength();
        int j = 0;
        while (j < nbVal) {
          Node nVal = listVal.item(j);
          j++;
          String nValName = nVal.getNodeName().trim();
          if ((CONSTRAINT_PREFIX_TAG.equals(nValName)) || (CONSTRAINT_POSTFIX_TAG.equals(nValName))
              || (CONSTRAINT_UNCOMPATIBLE_WITH_TAG.equals(nValName))) {
            ConstraintValue cr = Constraint.getConstraintValueInstance(nValName);
            if (cr != null) {
              cr.setNode(nVal);
              setValue(cr);
            }
          }
        }
      }
    }
  }
  
  /**
   * Gets an empty instance of constraint value according to type.
   * @param type The type.
   * @return The constraint value.
   */
  public static final ConstraintValue getConstraintValueInstance(String type) {
    ConstraintValue cr = null;
    
    if ((type != null) && (!(type.trim().equals("")))) {
      cr = new ConstraintValueData(type); // only existing type currently
    }
    return cr;
  }
  
  /**
   * Clonable implementation.
   */
  public Object clone() {
    Constraint toRet = new Constraint();
    toRet.setName((this.getName() == null) ? null : new String(this.getName()));
    toRet.setDescription((this.getDescription() == null) ? null : new String(this.getDescription()));
    toRet.setValue((ConstraintValue) ((ConstraintValue) this.getValue().clone()));
    toRet.setRefPxletName((this.getRefPxletName() == null) ? null : new String(this.getRefPxletName()));
    return toRet;
  }
}
