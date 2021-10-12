// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.service.jetty.webconnector;

// Jdk
import java.util.*;
import com.alcatel_lucent.ha.services.Flattable;
import com.alcatel_lucent.convergence.services.PrivateAttributesNames;

/**
 * The web container will use this interface when a servlet will invoke one of the 
 * javax.servlet.http.HttpServlet methods.
 */
public interface WebSession extends Flattable {
	
  String getId();
  
  long getCreationTime();
  long getLastAccessedTime();

  long getAccessedTime();
  
  void invalidate();
  boolean isInvalid();
  
  void setCookie();
  void setCookie(String contextPath);
  long getCookieSetTime();
  
  // Session timeout
  int  getMaxInactiveInterval();
  void setMaxInactiveInterval(int seconds);

  // Session attributes
  Object getAttribute(String name);
  
  @SuppressWarnings("rawtypes")
  Enumeration getAttributeNames();
  void setAttribute(String name, Object val);
  void removeAttribute(String name);
  
  void complete();
    
  public void setPrivateAttribute(PrivateAttributesNames name, Object attribute);
  public Object getPrivateAttribute(PrivateAttributesNames name);
  
}
