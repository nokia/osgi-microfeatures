// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.engine;

import com.nextenso.proxylet.ProxyletConfig;
import com.nextenso.proxylet.engine.ProxyletConstants;

public class HttpProxyletUtils {
  
  //
  // ProxyletConfig constants
  //
  public static final String PARAM_NAME_TYPE = "ProxyletType";
  public static final String PARAM_VALUE_TYPE_REQUEST = "RequestProxylet";
  public static final String PARAM_VALUE_TYPE_RESPONSE = "ResponseProxylet";
  
  public static final String ATTR_NAME_DNS_RESULT = ProxyletConstants.ATTR_NAME_DNS_RESULT;
  
  //
  // Specifies if a Proxylet is deployed in the Request chain
  //
  public static boolean isRequestProxylet(ProxyletConfig cnf) {
    String type = cnf.getStringParameter(PARAM_NAME_TYPE, null);
    return (PARAM_VALUE_TYPE_REQUEST.equals(type));
  }
  
  //
  // Specifies if a Proxylet is deployed in the Response chain
  //
  public static boolean isResponseProxylet(ProxyletConfig cnf) {
    String type = cnf.getStringParameter(PARAM_NAME_TYPE, null);
    return (PARAM_VALUE_TYPE_RESPONSE.equals(type));
  }
  
}
