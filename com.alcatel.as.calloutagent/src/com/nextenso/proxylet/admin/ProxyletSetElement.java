// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

public interface ProxyletSetElement {
  public static final String SET_ID_ATTRIBUTE = "setid";
  public static final String SET_NAME = "name";
  public static final String SET_VERSION = "version";
  
  /**
   * Gets the identifier of the proxylet set the objet belongs to.
   * @return The identifier.
   */
  public String getSetID();
  
  /**
   * Sets the identifier of the proxylet set the objet belongs to.
   * @param id The identifier.
   */
  public void setSetID(String id);
}
