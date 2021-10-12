// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

import org.w3c.dom.Node;

public interface XMLable {
  /**
   * Gets the XML representation of this bearer.
   * @return The XML representation of this bearer.
   */
  public String toXML();
  
  /**
   * Gets the tag name of this object.
   * @return The tag name.
   */
  public String getTagName();
  
  /**
   * Builds object with DOM Node object.
   * @param node The node representing the object.
   */
  public void setNode(Node node);
}
