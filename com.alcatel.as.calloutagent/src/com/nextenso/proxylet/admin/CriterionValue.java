// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

/**
 * Criterion class.
 */
public abstract class CriterionValue extends AdminObject implements XMLable, Cloneable {
  
  String _tagName = null;
  String _criterionName;
  
  /**
   * Builds a new criterion value.
   * @param tagname The tagname of the criterion.
   */
  protected CriterionValue(String tagname) {
    this();
    setTagName(tagname);
  }
  
  /**
   * Builds a new empty criterion.
   */
  private CriterionValue() {
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
  
  /**
   * Compare this criterion value to another one
   *
   * @param o Other criterion value
   * @return true if the two values are considered equals, false otherwise
   */
  public abstract boolean equals(Object o);
  
  /**
   * Display the criterion in human readable form
   *
   * @return Criterion description
   */
  public abstract String toString();
  
  /**
   * Display the criterion value
   *
   * @return Criterion value
   */
  public abstract String valueToString();
  
  /**
   * Retrieve criterion name. 
   * @return criterion name if {@link #setCriterionNamei(String)} method has been previously called.
   * Otherwise method returns null. 
   */
  public String getCriterionName() {
    return _criterionName;
  }
  
  /**
   * Set criterion name.
   */
  public void setCriterionName(String critName) {
    _criterionName = critName;
  }
}
