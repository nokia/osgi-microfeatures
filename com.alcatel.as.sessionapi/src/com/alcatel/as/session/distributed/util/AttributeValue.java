package com.alcatel.as.session.distributed.util;


/**
 * @deprecated
 * @internal
 * Defines an optimized Attribute value.
 * <p/>The two optimizations are:
 * <ul>
 * <li>implementation of java.io.Externalizable
 * <li>use of an id to prevent broadcasting the classname
 * </ul>
 */

public interface AttributeValue extends java.io.Externalizable {

  /**
   * Returns the associated id.
   * @return the id
   */
  public int getAttributeValueId ();

}
