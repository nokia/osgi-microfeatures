// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin;

public class PxletUtils {
  public static final String SEPARATOR = "_";
  public static final String DEPLOYED_PROXYLETSET_ID_LIST_PROPERTY_KEY = "deployed.proxyletsetid.list";
  public static final String DEPLOYED_BEARER_CONTEXT_KEY_PROPERTY = "deployed.proxyletcontext."; // append protocol
  
  public static String getId(String name, String version) {
    StringBuilder sb = new StringBuilder(name);
    sb.append(SEPARATOR).append(version);
    return sb.toString();
  }
  
  public static String getNameFromId(String id) {
    if (id == null)
      return null;
    
    int index;
    
    if ((index = id.indexOf(SEPARATOR)) < 0)
      return id;
    else
      return id.substring(0, index);
  }
  
  public static String getVersionFromId(String id) {
    if (id == null)
      return null;
    
    int index;
    int begin;
    
    if ((index = id.indexOf(SEPARATOR)) < 0)
      return null;
    
    if ((begin = (index + SEPARATOR.length())) >= id.length())
      return null;
    else {
      return id.substring(begin);
    }
  }
  
  public static int getIntVersionFromId(String appId) {
    String sVersionId = getVersionFromId(appId);
    int versionId = 0;
    
    if (sVersionId == null)
      return 0;
    
    if (sVersionId.indexOf(".") != -1) {
      // transform dotted notation into int
      StringBuilder noDot = new StringBuilder(sVersionId.length());
      for (int i = 0; i < sVersionId.length(); i++) {
        char c = sVersionId.charAt(i);
        if ((c >= '0') && (c <= '9'))
          noDot.append(c);
      }
      sVersionId = noDot.toString();
    }
    
    try {
      return Integer.parseInt(sVersionId);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }
  
}
