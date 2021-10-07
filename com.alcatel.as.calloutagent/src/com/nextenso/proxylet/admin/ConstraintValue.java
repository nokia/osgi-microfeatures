package com.nextenso.proxylet.admin;

/**
 * ConstraintVAlue class.
 */
public abstract class ConstraintValue extends AdminObject implements XMLable, Cloneable {
  
  String _tagName = null;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tagname of the criterion.
   */
  protected ConstraintValue(String tagname) {
    this();
    setTagName(tagname);
  }
  
  /**
   * Builds a new empty criterion.
   */
  private ConstraintValue() {
    super();
  }
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName() {
    return _tagName;
  }
  
  /**
   * Sets the tag name of this object.
   * @param tagname The tag name.
   */
  public void setTagName(String tagname) {
    _tagName = tagname;
  }
  
  /**
   * Clonable implementation.
   */
  public abstract Object clone();
}
